import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const traceBaseUrl = 'http://127.0.0.1:5173'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const batchId = 2
const batchCode = 'ORANGE-202603-D1'
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const screenshotFiles = []

const samplePng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAusB9Y9ycukAAAAASUVORK5CYII=',
  'base64'
)

const qualityImage = path.join(outputDir, `workbench-quality-${stamp}.png`)
const traceImageOneName = `workbench-field-1-${stamp}.png`
const traceImageTwoName = `workbench-field-2-${stamp}.png`
const traceImageOne = path.join(outputDir, traceImageOneName)
const traceImageTwo = path.join(outputDir, traceImageTwoName)

await fs.writeFile(qualityImage, samplePng)
await fs.writeFile(traceImageOne, samplePng)
await fs.writeFile(traceImageTwo, samplePng)

function screenshotPath(name) {
  const fullPath = path.join(outputDir, name)
  screenshotFiles.push(fullPath)
  return fullPath
}

function localDateTimeInput(offsetMinutes = 0) {
  const now = new Date(Date.now() + offsetMinutes * 60000)
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
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
    throw new Error(`Failed to load batch ${batchId}: ${payload.status}`)
  }
  return payload.body.data
}

async function waitForBatch(apiContext, predicate, timeoutMs = 20000) {
  return waitFor(async () => {
    const detail = await batchDetail(apiContext)
    return predicate(detail) ? detail : null
  }, timeoutMs)
}

async function waitForDialogClose(page, testId) {
  await waitFor(async () => (await page.getByTestId(testId).count()) === 0, 20000)
}

const browser = await chromium.launch({ headless: true })
const platformContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const operatorContext = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })
const publicContext = await browser.newContext({ viewport: { width: 1280, height: 900 } })

const platformPage = await platformContext.newPage()
const operatorPage = await operatorContext.newPage()
const publicPage = await publicContext.newPage()

const consoleErrors = []
for (const page of [platformPage, operatorPage, publicPage]) {
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push({ url: page.url(), text: message.text() })
    }
  })
}

const platformSession = await login(platformPage, 'platform')
const platformApi = await apiContextFor(platformSession.token)
let currentDetail = await batchDetail(platformApi)
const initialDetail = currentDetail

const currentAssigneeId = Number(currentDetail.task?.assigneeUserId || 0)
const operatorUsername = currentAssigneeId === 3 ? 'operator' : 'operator_support'
const operatorSession = await login(operatorPage, operatorUsername)
const operatorApi = await apiContextFor(operatorSession.token)

await platformPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-workbench-page').waitFor()
await platformPage.screenshot({ path: screenshotPath('71-workbench-overview.png'), fullPage: true })

const qualityReportNo = `WB-QA-${stamp}`
const qualitySummary = `工作台回归质检摘要 ${stamp}\n关键指标合格\n可以继续发布`
await platformPage.getByTestId('workbench-quality-panel').getByRole('button', { name: '上传质检' }).click()
const qualityDialog = platformPage.getByTestId('workbench-quality-dialog')
await qualityDialog.waitFor()
await qualityDialog.locator('input[type="text"]').nth(0).fill(qualityReportNo)
await qualityDialog.locator('input[type="text"]').nth(1).fill('省农检中心')
await qualityDialog.locator('select').selectOption('PASS')
await qualityDialog.locator('input[type="datetime-local"]').fill(localDateTimeInput())
await qualityDialog.locator('textarea').fill(qualitySummary)
await platformPage.locator('.dialog-card input[type="file"]').setInputFiles(qualityImage)
await platformPage.waitForFunction(() => {
  const uploaded = document.querySelectorAll('.dialog-card .uploaded-file-item').length
  const uploading = Array.from(document.querySelectorAll('.dialog-card')).some((node) => node.textContent?.includes('正在上传质检附件'))
  return uploaded >= 1 && !uploading
}, { timeout: 20000 })
await platformPage.locator('.dialog-card .dialog-actions .primary').click()
await waitForDialogClose(platformPage, 'workbench-quality-dialog')
currentDetail = await waitForBatch(platformApi, (detail) => detail.quality?.latestReport?.reportNo === qualityReportNo)
await platformPage.reload({ waitUntil: 'networkidle' })
await platformPage.getByTestId('workbench-quality-panel').getByText(qualityReportNo).waitFor()
await platformPage.screenshot({ path: screenshotPath('72-workbench-quality-uploaded.png'), fullPage: true })

let qrGeneratedNow = false
if (!currentDetail.qr?.generated) {
  await platformPage.getByTestId('workbench-qr-action-0').click()
  currentDetail = await waitForBatch(platformApi, (detail) => Boolean(detail.qr?.generated))
  qrGeneratedNow = true
  await platformPage.reload({ waitUntil: 'networkidle' })
}
await platformPage.getByTestId('workbench-qr-panel').waitFor()
await platformPage.screenshot({ path: screenshotPath('73-workbench-qr-ready.png'), fullPage: true })

const recordTitle = `工作台回归现场记录 ${stamp}`
const recordSummary = `工作台回归 ${stamp}，从操作员端补录并回查工作台图片顺序。`
await operatorPage.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await operatorPage.getByTestId('field-entry-page').waitFor()
const taskRow = operatorPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first()
await taskRow.waitFor({ timeout: 20000 })
await taskRow.getByTestId('field-todo-open-button').click()
await operatorPage.getByTestId('field-entry-page').waitFor()
await operatorPage.locator('input[type="text"]').nth(0).fill(recordTitle)
await operatorPage.locator('textarea').fill(recordSummary)
await operatorPage.locator('input[type="text"]').nth(1).fill('冷链分拣区 B 口')
await operatorPage.locator('input[type="text"]').nth(2).fill(operatorSession.user.realName || operatorUsername)
const consumerCheckbox = operatorPage.locator('input[type="checkbox"]').first()
if (!(await consumerCheckbox.isChecked())) {
  await consumerCheckbox.check()
}
await operatorPage.locator('input[type="file"]').setInputFiles([traceImageOne, traceImageTwo])
await operatorPage.getByTestId('field-image-queue').waitFor()
await operatorPage.waitForFunction(() => {
  const rows = document.querySelectorAll('[data-testid="field-image-queue"] .image-row').length
  const states = Array.from(document.querySelectorAll('[data-testid="field-image-queue"] .image-state')).map((node) => node.textContent || '')
  return rows === 2 && states.length >= 2 && states.every((text) => !text.includes('上传中'))
}, { timeout: 20000 })
await operatorPage.getByTestId('field-entry-submit').click()
await operatorPage.getByTestId('field-entry-success').waitFor()
await operatorPage.screenshot({ path: screenshotPath('74-workbench-operator-submit-success.png'), fullPage: true })

currentDetail = await waitForBatch(platformApi, (detail) => detail.trace?.recentRecords?.[0]?.title === recordTitle)
await platformPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-workbench-page').waitFor()
await platformPage.getByTestId('workbench-latest-record').getByText(recordTitle).waitFor()
const latestImageOrder = await platformPage.locator('[data-testid="workbench-latest-record"] img').evaluateAll((nodes) =>
  nodes.map((node) => node.getAttribute('alt') || '')
)
await platformPage.screenshot({ path: screenshotPath('75-workbench-record-synced.png'), fullPage: true })

let publishActionTaken = false
currentDetail = await batchDetail(platformApi)
if (currentDetail.status?.code !== 'PUBLISHED') {
  const publishAllowed = (currentDetail.actions || []).some((item) => item.code === 'PUBLISH' && item.enabled)
  if (publishAllowed) {
    await platformPage.locator('.release-actions button.success').click()
    await platformPage.getByTestId('workbench-status-dialog').waitFor()
    await platformPage.locator('.dialog-card .dialog-actions .primary').click()
    await waitForDialogClose(platformPage, 'workbench-status-dialog')
    currentDetail = await waitForBatch(platformApi, (detail) => detail.status?.code === 'PUBLISHED')
    publishActionTaken = true
    await platformPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
  }
}
await platformPage.getByTestId('batch-workbench-page').waitFor()
await platformPage.screenshot({ path: screenshotPath('76-workbench-published.png'), fullPage: true })

const publicUrl = currentDetail.qr?.publicUrl
if (!publicUrl || new URL(publicUrl).port !== '5173') {
  throw new Error(`Unexpected public url: ${publicUrl || 'empty'}`)
}
await publicPage.goto(publicUrl, { waitUntil: 'networkidle' })
await publicPage.getByTestId('public-trace-page').waitFor()
await publicPage.getByTestId('public-batch-code').getByText(batchCode).waitFor()
await publicPage.getByText(recordTitle).first().waitFor()
await publicPage.screenshot({ path: screenshotPath('77-public-trace-page.png'), fullPage: true })

const result = {
  verifiedAt: new Date().toISOString(),
  operatorLogin: operatorUsername,
  runtime: {
    backend: 'http://127.0.0.1:8080',
    adminWeb: adminBaseUrl,
    traceWeb: traceBaseUrl
  },
  actions: {
    qualityReportNo,
    qrGeneratedNow,
    publishActionTaken
  },
  initialState: {
    batchStatus: initialDetail.status?.label,
    assigneeName: initialDetail.task?.assigneeName,
    taskStatus: initialDetail.task?.taskStatusLabel,
    todayCompleted: initialDetail.task?.todayCompleted,
    qrGenerated: initialDetail.qr?.generated,
    qualityLabel: initialDetail.quality?.label
  },
  finalState: {
    batchStatus: currentDetail.status?.label,
    assigneeName: currentDetail.task?.assigneeName,
    taskStatus: currentDetail.task?.taskStatusLabel,
    todayCompleted: currentDetail.task?.todayCompleted,
    latestRecordTitle: currentDetail.trace?.recentRecords?.[0]?.title,
    latestRecordOperator: currentDetail.trace?.recentRecords?.[0]?.operatorName,
    latestRecordImages: (currentDetail.trace?.recentRecords?.[0]?.attachments || []).map((item) => item.fileName || item.fileUrl),
    publicUrl
  },
  uiChecks: {
    latestImageOrder,
    latestImageOrderMatchesUploadOrder:
      latestImageOrder.length >= 2 &&
      latestImageOrder[0]?.includes(traceImageOneName) &&
      latestImageOrder[1]?.includes(traceImageTwoName),
    consoleErrors
  },
  screenshots: screenshotFiles
}

await fs.writeFile(path.join(outputDir, 'workbench-closure-round.json'), JSON.stringify(result, null, 2), 'utf8')

await platformApi.dispose()
await operatorApi.dispose()
await platformContext.close()
await operatorContext.close()
await publicContext.close()
await browser.close()

console.log(JSON.stringify(result, null, 2))
