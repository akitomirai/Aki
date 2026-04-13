import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const qualityBatchId = 2
const riskBatchId = 3
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const screenshotFiles = []
const consoleErrors = []
const pageErrors = []
const httpErrors = []

function screenshotPath(name) {
  const fullPath = path.join(outputDir, name)
  screenshotFiles.push(fullPath)
  return fullPath
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

async function batchList(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches`))
  return payload.body?.data ?? []
}

async function waitForBatch(apiContext, batchId, predicate, timeoutMs = 20000) {
  return waitFor(async () => {
    const detail = await batchDetail(apiContext, batchId)
    return predicate(detail) ? detail : null
  }, timeoutMs)
}

async function bodyText(page) {
  return page.locator('body').innerText()
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

const initialList = await batchList(platformApi)
const initialQualityRow = initialList.find((item) => item.id === qualityBatchId) ?? null
const initialRiskRow = initialList.find((item) => item.id === riskBatchId) ?? null
const initialQualityDetail = await batchDetail(platformApi, qualityBatchId)
const initialRiskDetail = await batchDetail(platformApi, riskBatchId)
const riskActionType = initialRiskRow?.riskStatus === 'PROCESSING' ? 'RECTIFIED' : 'PROCESSING'
const riskBeforeTab = initialRiskRow?.riskStatus === 'PROCESSING' ? 'PROCESSING' : 'RECTIFIED'
const riskAfterTab = riskActionType === 'RECTIFIED' ? 'RECTIFIED' : 'PROCESSING'
const riskActionButtonTestId = riskActionType === 'RECTIFIED' ? `risk-rectified-${riskBatchId}` : `risk-processing-${riskBatchId}`

await page.goto(`${adminBaseUrl}/quality`, { waitUntil: 'networkidle' })
await page.getByTestId('quality-page').waitFor()
await page.getByTestId('quality-tab-PASS').click()
await page.locator(`[data-testid="quality-row-${qualityBatchId}"]`).waitFor()
await page.screenshot({ path: screenshotPath('123-quality-page.png'), fullPage: true })

await page.getByTestId(`quality-open-report-${qualityBatchId}`).click()
await page.getByTestId('quality-result-dialog').waitFor()
const qualityDialogText = await bodyText(page)
const qualityReportNo = await page.getByTestId('quality-report-no').innerText()
await page.screenshot({ path: screenshotPath('124-quality-report-dialog.png'), fullPage: true })
await page.getByTestId('quality-result-open-workbench').click()
await page.getByTestId('batch-workbench-page').waitFor()
const qualityWorkbenchPanelText = await page.getByTestId('workbench-quality-panel').innerText()
await page.screenshot({ path: screenshotPath('125-quality-workbench-check.png'), fullPage: true })

await page.goto(`${adminBaseUrl}/risk`, { waitUntil: 'networkidle' })
await page.getByTestId('risk-page').waitFor()
await page.getByTestId(`risk-tab-${riskBeforeTab}`).click()
await page.locator(`[data-testid="risk-row-${riskBatchId}"]`).waitFor()
const riskPageBeforeText = await bodyText(page)
await page.screenshot({ path: screenshotPath('126-risk-page-processing.png'), fullPage: true })

await page.getByTestId(riskActionButtonTestId).click()
await page.getByTestId('risk-action-dialog').waitFor()
if (riskActionType === 'RECTIFIED') {
  await page.locator('[data-testid="risk-action-dialog"] textarea').nth(1).fill(`已整改 ${stamp}：复检与处置留痕已补齐。`)
  await page.locator('[data-testid="risk-action-dialog"] textarea').nth(2).fill(`已整改 ${stamp}：风险页完成整改收口验证。`)
} else {
  await page.locator('[data-testid="risk-action-dialog"] textarea').nth(1).fill(`处理中 ${stamp}：已重新进入风险处理中，继续保持冻结复核。`)
  await page.locator('[data-testid="risk-action-dialog"] textarea').nth(2).fill(`处理中 ${stamp}：风险页完成处理中状态回归。`)
}
await page.screenshot({ path: screenshotPath('127-risk-action-dialog.png'), fullPage: true })
await page.getByTestId('risk-action-submit').click()
await waitFor(async () => (await page.getByTestId('risk-action-dialog').count()) === 0)

const rectifiedDetail = await waitForBatch(
  platformApi,
  riskBatchId,
  (detail) => detail.riskHandling?.currentStage === riskAfterTab
)

await page.getByTestId(`risk-tab-${riskAfterTab}`).click()
await page.locator(`[data-testid="risk-row-${riskBatchId}"]`).waitFor()
const riskPageAfterText = await bodyText(page)
await page.screenshot({ path: screenshotPath('128-risk-page-rectified.png'), fullPage: true })

await page.getByTestId(`risk-open-workbench-${riskBatchId}`).click()
await page.getByTestId('batch-workbench-page').waitFor()
const riskWorkbenchText = await bodyText(page)
const riskPanelText = await page.getByTestId('workbench-risk-panel').innerText()
await page.screenshot({ path: screenshotPath('129-risk-workbench-check.png'), fullPage: true })

const result = {
  verifiedAt: new Date().toISOString(),
  runtime: {
    backend: 'http://127.0.0.1:8080',
    adminWeb: adminBaseUrl,
    traceWeb: 'http://127.0.0.1:5173'
  },
  user: platformSession.user,
  qualityPage: {
    route: '/quality',
    batchId: qualityBatchId,
    batchCode: initialQualityRow?.batchCode || '',
    rowSummary: initialQualityRow,
    latestReportFromApi: initialQualityDetail.quality?.latestReport ?? null,
    dialogReportNo: qualityReportNo,
    dialogContainsReportNo: qualityDialogText.includes(qualityReportNo),
    workbenchQualityPanelText: qualityWorkbenchPanelText,
    workbenchContainsReportNo: qualityWorkbenchPanelText.includes(qualityReportNo)
  },
  riskPage: {
    route: '/risk',
    batchId: riskBatchId,
    batchCode: initialRiskRow?.batchCode || '',
    performedAction: riskActionType,
    before: {
      rowSummary: initialRiskRow,
      workbench: {
        batchStatus: initialRiskDetail.status?.label || '',
        riskStatus: initialRiskDetail.risk?.statusLabel || '',
        canResume: Boolean(initialRiskDetail.riskHandling?.canResume)
      },
      pageContainsSelectedTabStatus: riskPageBeforeText.includes(riskBeforeTab === 'PROCESSING' ? '风险处理中' : '已完成整改')
    },
    afterRectified: {
      workbench: {
        batchStatus: rectifiedDetail.status?.label || '',
        riskStatus: rectifiedDetail.risk?.statusLabel || '',
        canResume: Boolean(rectifiedDetail.riskHandling?.canResume),
        latestRiskAction: rectifiedDetail.riskHandling?.history?.[0]?.actionLabel || ''
      },
      pageContainsTargetStatus: riskPageAfterText.includes(riskAfterTab === 'RECTIFIED' ? '已完成整改' : '风险处理中'),
      workbenchRiskPanelText: riskPanelText,
      workbenchTextIncludesTargetStatus: riskWorkbenchText.includes(riskAfterTab === 'RECTIFIED' ? '已完成整改' : '风险处理中')
    }
  },
  uiErrors: {
    consoleErrors,
    pageErrors,
    httpErrors
  },
  screenshots: screenshotFiles
}

const resultPath = path.join(outputDir, 'quality-risk-pages-round.json')
await fs.writeFile(resultPath, JSON.stringify(result, null, 2))
console.log(JSON.stringify(result, null, 2))

await platformApi.dispose()
await context.close()
await browser.close()
