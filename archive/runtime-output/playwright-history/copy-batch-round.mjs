import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const sourceBatchId = 2
const sourceBatchCode = 'ORANGE-202603-D1'
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const newBatchCode = `COPY-${stamp.slice(0, 8)}-${stamp.slice(8, 12)}`
const screenshots = []

function screenshotPath(name) {
  const fullPath = path.join(outputDir, name)
  screenshots.push(fullPath)
  return fullPath
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

async function batchDetail(apiContext, batchId) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/${batchId}`))
  if (payload.status !== 200 || !payload.body?.data) {
    throw new Error(`failed to load batch ${batchId}: ${payload.status}`)
  }
  return payload.body.data
}

function summarizeDetail(detail) {
  return {
    batchId: detail.batch?.id ?? null,
    batchCode: detail.batch?.batchCode || '',
    productName: detail.product?.name || '',
    companyName: detail.company?.name || '',
    originPlace: detail.batch?.originPlace || '',
    productionDate: detail.batch?.productionDate || '',
    statusCode: detail.status?.code || '',
    statusLabel: detail.status?.label || '',
    publishedAt: detail.batch?.publishedAt || detail.status?.changedAt || '',
    traceTotal: Number(detail.trace?.totalCount || 0),
    latestTraceTitle: detail.trace?.recentRecords?.[0]?.title || '',
    qualityCount: Number(detail.quality?.reportCount || 0),
    qualityLabel: detail.quality?.label || '',
    qrGenerated: Boolean(detail.qr?.generated),
    qrStatus: detail.qr?.statusLabel || '',
    qrToken: detail.qr?.token || '',
    riskStatus: detail.risk?.statusLabel || detail.riskHandling?.currentStageLabel || '',
    assigneeUserId: detail.task?.assigneeUserId ?? null,
    assigneeName: detail.task?.assigneeName || '',
    taskStatus: detail.task?.taskStatus || '',
    taskStatusLabel: detail.task?.taskStatusLabel || '',
    todayCompleted: Boolean(detail.task?.todayCompleted),
    draftPending: Boolean(detail.task?.draftPending)
  }
}

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({
  viewport: { width: 1440, height: 960 }
})
const page = await context.newPage()

try {
  const auth = await login(page, 'platform')
  const apiContext = await apiContextFor(auth.token)

  const sourceDetail = await batchDetail(apiContext, sourceBatchId)
  const sourceSummary = summarizeDetail(sourceDetail)

  await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-list-page').waitFor()
  await page.locator(`[data-testid="batch-copy-${sourceBatchId}"]`).waitFor()
  await page.screenshot({ path: screenshotPath('171-copy-batch-list-entry.png'), fullPage: true })

  await page.goto(`${adminBaseUrl}/batches/${sourceBatchId}`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-workbench-page').waitFor()
  await page.screenshot({ path: screenshotPath('172-copy-workbench-entry.png'), fullPage: true })

  await page.getByTestId('workbench-copy-batch-button').click()
  await page.getByTestId('batch-edit-dialog').waitFor()
  await page.getByTestId('batch-copy-source-note').waitFor()
  await page.screenshot({ path: screenshotPath('173-copy-dialog-prefill.png'), fullPage: true })

  const dialog = page.getByTestId('batch-edit-dialog')
  const sourceNote = (await page.getByTestId('batch-copy-source-note').innerText()).trim()
  const selectedCompanyText = (await dialog.locator('.field-note').nth(1).innerText()).trim()
  const selectedProductText = (await dialog.locator('.field-note').nth(3).innerText()).trim()
  const originValue = await dialog.locator('input[placeholder="例如 江西赣州信丰"]').inputValue()
  const productionDateValue = await dialog.locator('input[type="date"]').inputValue()

  const batchCodeInput = dialog.locator('input[type="text"]').first()
  await batchCodeInput.fill(newBatchCode)
  await page.getByTestId('batch-dialog-submit').click()

  await page.getByTestId('batch-workbench-page').waitFor()
  await page.getByTestId('fresh-batch-banner').waitFor()
  await page.getByTestId('fresh-batch-copy-source').waitFor()
  await page.screenshot({ path: screenshotPath('174-copy-workbench-fresh.png'), fullPage: true })

  const workbenchUrl = page.url()
  const newBatchId = Number(workbenchUrl.match(/\/batches\/(\d+)/)?.[1] || 0)
  if (!newBatchId) {
    throw new Error(`unable to parse new batch id from url: ${workbenchUrl}`)
  }

  const copiedDetail = await batchDetail(apiContext, newBatchId)
  const copiedSummary = summarizeDetail(copiedDetail)
  const freshBannerText = (await page.getByTestId('fresh-batch-banner').innerText()).trim()
  const todoTexts = await page.locator('.mini-list li').evaluateAll((nodes) => nodes.map((node) => node.textContent?.trim() || ''))
  const workbenchMeta = (await page.locator('.workbench-meta').innerText()).trim()

  const result = {
    runtime: {
      adminBaseUrl,
      apiBaseUrl
    },
    sourceBatch: sourceSummary,
    copiedBatch: copiedSummary,
    copyDialog: {
      sourceNote,
      selectedCompanyText,
      selectedProductText,
      originValue,
      productionDateValue
    },
    workbench: {
      url: workbenchUrl,
      freshBannerText,
      workbenchMeta,
      todoTexts
    },
    assertions: {
      sourceHasTrace: sourceSummary.traceTotal > 0,
      sourceHasQuality: sourceSummary.qualityCount > 0,
      sourceHasQr: sourceSummary.qrGenerated,
      copiedCompanyInherited: copiedSummary.companyName === sourceSummary.companyName,
      copiedProductInherited: copiedSummary.productName === sourceSummary.productName,
      copiedOriginInherited: copiedSummary.originPlace === sourceSummary.originPlace,
      copiedStartsDraft: copiedSummary.statusCode === 'DRAFT' && copiedSummary.statusLabel === '草稿',
      copiedHasNoTrace: copiedSummary.traceTotal === 0,
      copiedHasNoQuality: copiedSummary.qualityCount === 0,
      copiedHasNoQr: copiedSummary.qrGenerated === false && !copiedSummary.qrToken,
      copiedHasNoAssignment: copiedSummary.assigneeUserId == null && !copiedSummary.assigneeName,
      copiedTaskIsInitial: copiedSummary.taskStatusLabel === '待处理' && copiedSummary.todayCompleted === false && copiedSummary.draftPending === false,
      copiedRiskReset: copiedSummary.riskStatus === '当前无风险',
      workbenchShowsCopySource: freshBannerText.includes(sourceBatchCode),
      workbenchTodoReset:
        todoTexts.some((item) => item.includes('补录首条追溯')) &&
        todoTexts.some((item) => item.includes('上传质检')) &&
        todoTexts.some((item) => item.includes('生成二维码')) &&
        todoTexts.some((item) => item.includes('满足发布条件'))
    },
    screenshots
  }

  const resultPath = path.join(outputDir, 'copy-batch-round.json')
  await fs.writeFile(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  console.log(`Copy batch round saved to ${resultPath}`)
} finally {
  await context.close()
  await browser.close()
}
