import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const batchId = 2
const screenshotFiles = []
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)

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
    if (value) return value
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

async function batchDetail(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/${batchId}`))
  if (payload.status !== 200 || !payload.body?.data) {
    throw new Error(`failed to load batch ${batchId}: ${payload.status}`)
  }
  return payload.body.data
}

function actionEnabled(detail, code) {
  return Boolean((detail.actions || []).find((item) => item.code === code)?.enabled)
}

function summaryOf(detail) {
  const latestRisk = detail.riskHandling?.history?.[0] ?? null
  return {
    batchStatus: detail.status?.label,
    batchStatusCode: detail.status?.code,
    riskStage: detail.riskHandling?.currentStage,
    riskStageLabel: detail.riskHandling?.currentStageLabel,
    canResume: detail.riskHandling?.canResume,
    latestRiskAction: latestRisk ? `${latestRisk.actionType}:${latestRisk.operatorName}` : 'NONE',
    latestRiskActionLabel: latestRisk ? `${latestRisk.actionType}:${latestRisk.comment || latestRisk.reason || ''}` : 'NONE',
    publishEnabled: actionEnabled(detail, 'PUBLISH'),
    resumeEnabled: actionEnabled(detail, 'RESUME'),
    freezeEnabled: actionEnabled(detail, 'FREEZE'),
    recallEnabled: actionEnabled(detail, 'RECALL'),
    publicUrl: detail.qr?.publicUrl || '',
    riskReason: detail.risk?.reason || '',
    riskTip: detail.risk?.tip || ''
  }
}

async function waitForBatch(apiContext, predicate, timeoutMs = 20000) {
  return waitFor(async () => {
    const detail = await batchDetail(apiContext)
    return predicate(detail) ? detail : null
  }, timeoutMs)
}

async function publicBannerState(page, publicUrl) {
  await page.goto(publicUrl, { waitUntil: 'networkidle' })
  await page.getByTestId('public-trace-page').waitFor()
  const hasBanner = (await page.getByTestId('public-risk-banner').count()) > 0
  if (!hasBanner) {
    return { visible: false, title: '', status: await page.getByTestId('public-status').innerText() }
  }
  return {
    visible: true,
    title: await page.getByTestId('public-risk-banner').locator('h2').innerText(),
    status: await page.getByTestId('public-status').innerText()
  }
}

async function openBatch(page) {
  await page.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-workbench-page').waitFor()
}

async function closeStatusDialog(page) {
  await waitFor(async () => (await page.getByTestId('workbench-status-dialog').count()) === 0, 20000)
}

async function closeRiskDialog(page) {
  await waitFor(async () => (await page.getByTestId('workbench-risk-dialog').count()) === 0, 20000)
}

async function ensurePublished(page, apiContext) {
  let detail = await batchDetail(apiContext)
  if (detail.status?.code === 'PUBLISHED') {
    return detail
  }

  await openBatch(page)
  if (actionEnabled(detail, 'RESUME')) {
    await page.getByRole('button', { name: '恢复发布' }).first().click()
    await page.getByTestId('workbench-status-dialog').waitFor()
    await page.locator('.dialog-card .dialog-actions .primary').click()
    await closeStatusDialog(page)
    return waitForBatch(apiContext, (next) => next.status?.code === 'PUBLISHED')
  }

  if (actionEnabled(detail, 'PUBLISH')) {
    await page.getByRole('button', { name: '发布批次' }).first().click()
    await page.getByTestId('workbench-status-dialog').waitFor()
    await page.locator('.dialog-card .dialog-actions .primary').click()
    await closeStatusDialog(page)
    return waitForBatch(apiContext, (next) => next.status?.code === 'PUBLISHED')
  }

  throw new Error(`batch ${batchId} is not published and cannot be published automatically`)
}

async function freezeBatch(page, apiContext) {
  await openBatch(page)
  await page.getByRole('button', { name: '冻结批次' }).first().click()
  await page.getByTestId('workbench-status-dialog').waitFor()
  await page.getByTestId('workbench-status-dialog').locator('textarea').fill(`风险回归冻结 ${stamp}，先暂停流通并进入风险处理。`)
  await page.locator('.dialog-card .dialog-actions .primary').click()
  await closeStatusDialog(page)
  return waitForBatch(apiContext, (detail) => detail.status?.code === 'FROZEN')
}

async function addRiskAction(page, apiContext, buttonName, actionType, { reason = '', comment = '' }, predicate) {
  await openBatch(page)
  await page.getByTestId('workbench-group-status').getByRole('button', { name: buttonName }).click()
  const dialog = page.getByTestId('workbench-risk-dialog')
  await dialog.waitFor()
  await dialog.locator('select').selectOption(actionType)
  await dialog.locator('input[type="text"]').fill('Platform Admin')
  const textareas = dialog.locator('textarea')
  await textareas.nth(0).fill(reason)
  await textareas.nth(1).fill(comment)
  await page.locator('.dialog-card .dialog-actions .primary').click()
  await closeRiskDialog(page)
  return waitForBatch(apiContext, predicate)
}

const browser = await chromium.launch({ headless: true })
const platformContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const publicContext = await browser.newContext({ viewport: { width: 1280, height: 900 } })
const platformPage = await platformContext.newPage()
const publicPage = await publicContext.newPage()

const consoleErrors = []
for (const page of [platformPage, publicPage]) {
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push({ url: page.url(), text: message.text() })
    }
  })
}

const platformSession = await login(platformPage, 'platform')
const platformApi = await apiContextFor(platformSession.token)
const steps = []

let detail = await ensurePublished(platformPage, platformApi)
const initialSummary = summaryOf(detail)
const publicBefore = await publicBannerState(publicPage, detail.qr.publicUrl)
steps.push({ step: '初始状态', after: initialSummary, publicState: publicBefore })

await openBatch(platformPage)
await platformPage.screenshot({ path: screenshotPath('78-risk-before-freeze.png'), fullPage: true })

const frozenDetail = await freezeBatch(platformPage, platformApi)
const publicAfterFreeze = await publicBannerState(publicPage, frozenDetail.qr.publicUrl)
steps.push({
  step: '进入风险处理',
  before: initialSummary,
  after: summaryOf(frozenDetail),
  publicState: publicAfterFreeze
})
await openBatch(platformPage)
await platformPage.screenshot({ path: screenshotPath('79-risk-frozen.png'), fullPage: true })
await publicPage.screenshot({ path: screenshotPath('80-public-risk-frozen.png'), fullPage: true })

const commentText = `风险说明 ${stamp}：已暂停流通，先核对批次去向与现场情况。`
const afterComment = await addRiskAction(
  platformPage,
  platformApi,
  '补处理说明',
  'COMMENT',
  { comment: commentText },
  (next) => (next.riskHandling?.history?.[0]?.comment || '').includes(commentText)
)
steps.push({
  step: '补处理说明',
  before: summaryOf(frozenDetail),
  after: summaryOf(afterComment)
})
await openBatch(platformPage)
await platformPage.screenshot({ path: screenshotPath('81-risk-commented.png'), fullPage: true })

const rectificationText = `整改记录 ${stamp}：已完成库位隔离、待复核与复测。`
const afterRectification = await addRiskAction(
  platformPage,
  platformApi,
  '补整改记录',
  'RECTIFICATION',
  { comment: rectificationText },
  (next) => (next.riskHandling?.history?.[0]?.comment || '').includes(rectificationText)
)
steps.push({
  step: '补整改记录',
  before: summaryOf(afterComment),
  after: summaryOf(afterRectification)
})
await openBatch(platformPage)
await platformPage.screenshot({ path: screenshotPath('82-risk-rectification.png'), fullPage: true })

const processingReason = `风险处理中 ${stamp}：复核已启动，继续保持冻结。`
const afterProcessing = await addRiskAction(
  platformPage,
  platformApi,
  '标记处理中',
  'PROCESSING',
  { reason: processingReason },
  (next) => String(next.riskHandling?.currentStage || '').toUpperCase() === 'PROCESSING'
)
const publicAfterProcessing = await publicBannerState(publicPage, afterProcessing.qr.publicUrl)
steps.push({
  step: '标记处理中',
  before: summaryOf(afterRectification),
  after: summaryOf(afterProcessing),
  publicState: publicAfterProcessing
})
await openBatch(platformPage)
await platformPage.screenshot({ path: screenshotPath('83-risk-processing.png'), fullPage: true })
await publicPage.screenshot({ path: screenshotPath('84-public-risk-processing.png'), fullPage: true })

const rectifiedReason = `已完成整改 ${stamp}：问题批次已复核，满足恢复发布条件。`
const afterRectified = await addRiskAction(
  platformPage,
  platformApi,
  '标记已整改',
  'RECTIFIED',
  { reason: rectifiedReason },
  (next) => String(next.riskHandling?.currentStage || '').toUpperCase() === 'RECTIFIED' && Boolean(next.riskHandling?.canResume)
)
const publicAfterRectified = await publicBannerState(publicPage, afterRectified.qr.publicUrl)
steps.push({
  step: '标记已整改',
  before: summaryOf(afterProcessing),
  after: summaryOf(afterRectified),
  publicState: publicAfterRectified
})
await openBatch(platformPage)
await platformPage.screenshot({ path: screenshotPath('85-risk-rectified.png'), fullPage: true })
await publicPage.screenshot({ path: screenshotPath('86-public-risk-rectified.png'), fullPage: true })

await openBatch(platformPage)
await platformPage.getByRole('button', { name: '恢复发布' }).first().click()
await platformPage.getByTestId('workbench-status-dialog').waitFor()
await platformPage.locator('.dialog-card .dialog-actions .primary').click()
await closeStatusDialog(platformPage)
const resumedDetail = await waitForBatch(platformApi, (next) => next.status?.code === 'PUBLISHED')
const publicAfterResume = await publicBannerState(publicPage, resumedDetail.qr.publicUrl)
steps.push({
  step: '恢复发布',
  before: summaryOf(afterRectified),
  after: summaryOf(resumedDetail),
  publicState: publicAfterResume
})
await openBatch(platformPage)
await platformPage.screenshot({ path: screenshotPath('87-risk-resumed.png'), fullPage: true })
await publicPage.screenshot({ path: screenshotPath('88-public-risk-cleared.png'), fullPage: true })

const result = {
  verifiedAt: new Date().toISOString(),
  runtime: {
    backend: 'http://127.0.0.1:8080',
    adminWeb: adminBaseUrl,
    traceWeb: detail.qr.publicUrl?.split('/t/')[0] || ''
  },
  batchId,
  steps,
  finalState: summaryOf(resumedDetail),
  consoleErrors,
  screenshots: screenshotFiles
}

await fs.writeFile(path.join(outputDir, 'workbench-risk-round.json'), JSON.stringify(result, null, 2), 'utf8')

await platformApi.dispose()
await platformContext.close()
await publicContext.close()
await browser.close()

console.log(JSON.stringify(result, null, 2))
