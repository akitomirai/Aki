import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const screenshotFiles = []
const consoleErrors = []
const pageErrors = []
const httpErrors = []

function screenshotPath(name) {
  const fullPath = path.join(outputDir, name)
  screenshotFiles.push(fullPath)
  return fullPath
}

function stamp() {
  return new Date().toISOString().replace(/[^\d]/g, '').slice(0, 14)
}

async function readJson(responsePromise) {
  const response = await responsePromise
  let body = null
  try {
    body = await response.json()
  } catch {
    body = null
  }
  return { status: response.status(), body }
}

async function waitFor(check, timeoutMs = 20000, intervalMs = 400) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    const value = await check()
    if (value) {
      return value
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  throw new Error('Timed out while waiting for condition')
}

async function login(page, username, password = '123456') {
  await page.goto(`${adminBaseUrl}/login`, { waitUntil: 'networkidle' })
  await page.getByTestId('login-page').waitFor()
  await page.locator('input').nth(0).fill(username)
  await page.locator('input').nth(1).fill(password)
  await page.getByTestId('login-submit').click()
  await page.waitForFunction(() => Boolean(localStorage.getItem('admin_token')), { timeout: 20000 })
  await page.waitForFunction(() => !window.location.pathname.includes('/login'), { timeout: 20000 })
  await page.waitForLoadState('networkidle')
  return page.evaluate(() => ({
    token: localStorage.getItem('admin_token'),
    user: JSON.parse(localStorage.getItem('admin_user') || '{}')
  }))
}

async function apiContextFor(token) {
  return request.newContext({
    extraHTTPHeaders: {
      Authorization: `Bearer ${token}`
    }
  })
}

async function batchDetail(apiContext, batchId) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/${batchId}`))
  if (payload.status !== 200 || !payload.body?.data) {
    throw new Error(`failed to load batch ${batchId}: ${payload.status}`)
  }
  return payload.body.data
}

async function createDraftBatch(apiContext, code) {
  const payload = await readJson(apiContext.post(`${apiBaseUrl}/batches`, {
    data: {
      batchCode: code,
      productId: 1,
      companyId: 1,
      originPlace: '江西省赣州市信丰果园基地',
      productionDate: '2026-03-31',
      publicRemark: `二维码与发布页回归 ${code}`,
      internalRemark: '用于验证二维码生成与发布全局页联动'
    }
  }))
  if (payload.status !== 200 || !payload.body?.data?.batch?.id) {
    throw new Error(`failed to create batch: ${payload.status}`)
  }
  return payload.body.data.batch.id
}

async function createQuality(apiContext, batchId, code) {
  const payload = await readJson(apiContext.post(`${apiBaseUrl}/batches/${batchId}/quality-reports`, {
    data: {
      reportNo: `QA-${code}`,
      agency: '江西省农产品质检中心',
      result: 'PASS',
      reportTime: '2026-03-31T11:50',
      highlights: ['农残指标合格', '批次允许生成二维码并继续发布']
    }
  }))
  if (payload.status !== 200) {
    throw new Error(`failed to create quality report: ${payload.status}`)
  }
}

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const page = await context.newPage()

page.on('console', (message) => {
  if (message.type() === 'error') {
    consoleErrors.push({ url: page.url(), text: message.text() })
  }
})
page.on('pageerror', (error) => {
  pageErrors.push({ url: page.url(), text: String(error) })
})
page.on('response', (response) => {
  if (response.status() >= 400) {
    httpErrors.push({
      pageUrl: page.url(),
      status: response.status(),
      url: response.url()
    })
  }
})

const platformSession = await login(page, 'platform')
const platformApi = await apiContextFor(platformSession.token)
const caseCode = `QRPUB-${stamp()}`
const createdBatchId = await createDraftBatch(platformApi, caseCode)
await createQuality(platformApi, createdBatchId, caseCode)

await page.goto(`${adminBaseUrl}/qr`, { waitUntil: 'networkidle' })
await page.getByTestId('qr-publish-page').waitFor()
await page.getByTestId('qr-filter-keyword').fill(caseCode)
await page.getByTestId('qr-tab-NEED_QR').click()
await page.locator(`[data-testid="qr-row-${createdBatchId}"]`).waitFor()
await page.screenshot({ path: screenshotPath('130-qr-publish-page.png'), fullPage: true })

await page.getByTestId(`qr-generate-${createdBatchId}`).click()
const afterQr = await waitFor(async () => {
  const detail = await batchDetail(platformApi, createdBatchId)
  return detail.qr?.generated ? detail : null
})

await page.getByTestId('qr-tab-READY').click()
await page.locator(`[data-testid="qr-row-${createdBatchId}"]`).waitFor()
await page.getByTestId(`qr-preview-${createdBatchId}`).click()
await page.getByTestId('qr-preview-dialog').waitFor()
const previewToken = await page.getByTestId('qr-preview-token').innerText()
await page.screenshot({ path: screenshotPath('131-qr-preview-dialog.png'), fullPage: true })
await page.getByTestId('qr-preview-close').click()

await page.getByTestId(`qr-publish-${createdBatchId}`).click()
await page.getByTestId('qr-publish-dialog').waitFor()
await page.getByTestId('qr-publish-reason').fill('二维码与发布页回归：条件已齐，执行真实发布。')
await page.screenshot({ path: screenshotPath('132-qr-publish-dialog.png'), fullPage: true })
await page.getByTestId('qr-publish-submit').click()

const afterPublish = await waitFor(async () => {
  const detail = await batchDetail(platformApi, createdBatchId)
  return detail.status?.code === 'PUBLISHED' ? detail : null
})

await page.getByTestId('qr-tab-PUBLISHED').click()
await page.locator(`[data-testid="qr-row-${createdBatchId}"]`).waitFor()
await page.screenshot({ path: screenshotPath('133-qr-published-row.png'), fullPage: true })

const publicPopupPromise = page.waitForEvent('popup')
await page.getByTestId(`qr-public-${createdBatchId}`).click()
const publicPage = await publicPopupPromise
await publicPage.waitForLoadState('networkidle')
const publicBodyText = await publicPage.locator('body').innerText()
await publicPage.screenshot({ path: screenshotPath('134-qr-public-page.png'), fullPage: true })

await page.getByTestId(`qr-workbench-${createdBatchId}`).click()
await page.getByTestId('batch-workbench-page').waitFor()
const workbenchStatusCardText = await page.getByTestId('workbench-next-step-card').innerText()
const workbenchQrPanel = await page.getByTestId('workbench-qr-panel').innerText()
await page.screenshot({ path: screenshotPath('135-qr-workbench-sync.png'), fullPage: true })

const result = {
  verifiedAt: new Date().toISOString(),
  runtime: {
    backend: 'http://127.0.0.1:8080',
    adminWeb: adminBaseUrl,
    traceWeb: 'http://127.0.0.1:5173'
  },
  user: platformSession.user,
  createdBatch: {
    id: createdBatchId,
    batchCode: caseCode
  },
  qrFlow: {
    before: {
      tab: 'NEED_QR'
    },
    afterGenerate: {
      qrGenerated: Boolean(afterQr.qr?.generated),
      qrToken: afterQr.qr?.token || '',
      qrPublicUrl: afterQr.qr?.publicUrl || '',
      listPreviewToken: previewToken
    }
  },
  publishFlow: {
    afterPublish: {
      batchStatus: afterPublish.status?.label || '',
      qrStatus: afterPublish.qr?.statusLabel || '',
      publicUrl: afterPublish.qr?.publicUrl || ''
    },
    publicPage: {
      statusVisible: publicBodyText.includes('已发布'),
      batchCodeVisible: publicBodyText.includes(caseCode),
      latestRecordVisible: publicBodyText.includes('最新记录')
    },
    workbench: {
      statusCardText: workbenchStatusCardText,
      statusCardContainsPublished: workbenchStatusCardText.includes('已发布'),
      qrPanelContainsToken: workbenchQrPanel.includes(afterPublish.qr?.token || ''),
      qrPanelText: workbenchQrPanel
    }
  },
  uiErrors: {
    consoleErrors,
    pageErrors,
    httpErrors
  },
  screenshots: screenshotFiles
}

const resultPath = path.join(outputDir, 'qr-publish-page-round.json')
await fs.writeFile(resultPath, JSON.stringify(result, null, 2))
console.log(JSON.stringify(result, null, 2))

await platformApi.dispose()
await publicPage.close()
await context.close()
await browser.close()
