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
const batchCode = 'ORANGE-202603-D1'
const riskBatchId = 3
const riskBatchCode = 'TEA-202603-F1'
const defaultPassword = '123456'
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const operatorUsername = `operator_log_${stamp.slice(-6)}`
const operatorDisplayName = 'Field Audit Operator'
const screenshotFiles = []
const consoleErrors = []
const pageErrors = []
const samplePng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAusB9Y9ycukAAAAASUVORK5CYII=',
  'base64'
)
const qualityFile = path.join(outputDir, `logs-quality-${stamp}.png`)

await fs.writeFile(qualityFile, samplePng)

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

async function login(page, username, password = defaultPassword) {
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

async function waitForUser(apiContext, username) {
  return waitFor(async () => {
    const payload = await readJson(apiContext.get(`${apiBaseUrl}/users?keyword=${encodeURIComponent(username)}`))
    return (payload.body?.data ?? []).find((item) => item.username === username) ?? null
  })
}

async function batchDetail(apiContext, id) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/${id}`))
  if (payload.status !== 200 || !payload.body?.data) {
    throw new Error(`failed to load batch ${id}`)
  }
  return payload.body.data
}

async function logsPage(apiContext, params = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== '' && value !== null && value !== undefined) {
      query.set(key, String(value))
    }
  }
  const suffix = query.toString() ? `?${query.toString()}` : ''
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/logs${suffix}`))
  return {
    status: payload.status,
    body: payload.body,
    items: payload.body?.data?.items ?? [],
    total: payload.body?.data?.total ?? 0
  }
}

async function openAssignmentDialog(page) {
  await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-list-page').waitFor()
  await page.getByTestId('batch-filter-code').fill(batchCode)
  await page.getByTestId('batch-search-button').click()
  await page.getByTestId(`batch-card-${batchId}`).waitFor()
  await page.getByTestId(`batch-assignment-open-${batchId}`).click()
  await page.getByTestId('batch-assignment-dialog').waitFor()
  await page.getByTestId('batch-list-assignment-select').waitFor()
}

async function waitForDialogHidden(page, testId) {
  await waitFor(async () => {
    try {
      return (await page.getByTestId(testId).count()) === 0
    } catch {
      return true
    }
  })
}

function findLog(items, actionType, matcher) {
  return items.find((item) => item.actionType === actionType && matcher(item))
}

const browser = await chromium.launch({ headless: true })
const platformContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const enterpriseContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })

const platformPage = await platformContext.newPage()
const enterprisePage = await enterpriseContext.newPage()

for (const page of [platformPage, enterprisePage]) {
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push({ url: page.url(), text: message.text() })
    }
  })
  page.on('pageerror', (error) => {
    pageErrors.push({ url: page.url(), text: String(error) })
  })
}

const platformSession = await login(platformPage, 'platform', defaultPassword)
const platformApi = await apiContextFor(platformSession.token)

await platformPage.goto(`${adminBaseUrl}/logs`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('logs-page').waitFor()
await platformPage.screenshot({ path: screenshotPath('154-logs-page-initial.png'), fullPage: true })

await platformPage.goto(`${adminBaseUrl}/users`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('users-page').waitFor()
await platformPage.getByTestId('users-open-create').click()
await platformPage.getByTestId('user-form-dialog').waitFor()
await platformPage.getByTestId('user-form-username').fill(operatorUsername)
await platformPage.getByTestId('user-form-password').fill(defaultPassword)
await platformPage.getByTestId('user-form-real-name').fill(operatorDisplayName)
await platformPage.getByTestId('user-form-role').selectOption('OPERATOR')
await waitFor(async () => {
  const count = await platformPage.getByTestId('user-form-company').locator('option').count()
  return count > 1 ? count : null
})
await platformPage.getByTestId('user-form-company').selectOption('1')
await platformPage.getByTestId('user-form-submit').click()
const createdUser = await waitForUser(platformApi, operatorUsername)
await platformPage.screenshot({ path: screenshotPath('155-logs-user-created.png'), fullPage: true })

await openAssignmentDialog(platformPage)
await platformPage.getByTestId('batch-list-assignment-clear').click()
await waitForDialogHidden(platformPage, 'batch-assignment-dialog')
await waitFor(async () => {
  const detail = await batchDetail(platformApi, batchId)
  return detail.task?.assigneeUserId === null ? detail : null
})

await openAssignmentDialog(platformPage)
await platformPage.getByTestId('batch-list-assignment-select').selectOption(String(createdUser.id))
await platformPage.getByTestId('batch-list-assignment-submit').click()
await waitForDialogHidden(platformPage, 'batch-assignment-dialog')
const assignedBatch = await waitFor(async () => {
  const detail = await batchDetail(platformApi, batchId)
  return Number(detail.task?.assigneeUserId) === Number(createdUser.id) ? detail : null
})
await platformPage.screenshot({ path: screenshotPath('156-logs-batch-assigned.png'), fullPage: true })

await platformPage.goto(`${adminBaseUrl}/quality`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('quality-page').waitFor()
await platformPage.getByTestId('quality-tab-PASS').click()
await platformPage.getByTestId('quality-filter-keyword').fill(batchCode)
await platformPage.getByTestId('quality-search-button').click()
await platformPage.getByTestId(`quality-row-${batchId}`).waitFor()
await platformPage.getByTestId(`quality-upload-${batchId}`).click()
await platformPage.getByTestId('quality-upload-dialog').waitFor()
const qualityDialog = platformPage.getByTestId('quality-upload-dialog')
await qualityDialog.locator('input[type="text"]').nth(0).fill(`QA-LOG-${stamp}`)
await qualityDialog.locator('input[type="text"]').nth(1).fill('江西省农产品质检中心')
await qualityDialog.locator('select').selectOption('PASS')
await qualityDialog.locator('textarea').fill('日志回归补充质检\n允许继续发布')
await qualityDialog.locator('input[type="file"]').setInputFiles(qualityFile)
await waitFor(async () => {
  const count = await qualityDialog.locator('.uploaded-file-item').count()
  return count > 0 ? count : null
})
await qualityDialog.getByTestId('quality-upload-submit').click()
const qualityUploadLog = await waitFor(async () => {
  const response = await logsPage(platformApi, { actionType: 'QUALITY_UPLOAD', pageSize: 50 })
  return findLog(response.items, 'QUALITY_UPLOAD', (item) => item.summary.includes(batchCode)) ?? null
})
await platformPage.screenshot({ path: screenshotPath('157-logs-quality-upload.png'), fullPage: true })

await platformPage.goto(`${adminBaseUrl}/risk`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('risk-page').waitFor()
await platformPage.getByTestId('risk-tab-PROCESSING').click()
await platformPage.getByTestId('risk-filter-keyword').fill(riskBatchCode)
await platformPage.getByTestId(`risk-row-${riskBatchId}`).waitFor()
await platformPage.getByTestId(`risk-comment-${riskBatchId}`).click()
await platformPage.getByTestId('risk-action-dialog').waitFor()
const riskDialog = platformPage.getByTestId('risk-action-dialog')
await riskDialog.locator('input[type="text"]').nth(1).fill('平台管理员')
await riskDialog.locator('textarea').nth(1).fill('日志回归补充处理说明')
await riskDialog.locator('textarea').nth(2).fill('日志回归留痕：已记录处理判断和核查范围')
await riskDialog.getByTestId('risk-action-submit').click()
await waitForDialogHidden(platformPage, 'risk-action-dialog')
const riskCommentLog = await waitFor(async () => {
  const response = await logsPage(platformApi, { actionType: 'RISK_COMMENT', pageSize: 50 })
  return findLog(response.items, 'RISK_COMMENT', (item) => item.summary.includes(riskBatchCode)) ?? null
})
await platformPage.screenshot({ path: screenshotPath('158-logs-risk-action.png'), fullPage: true })

const userCreateLog = await waitFor(async () => {
  const response = await logsPage(platformApi, { actionType: 'USER_CREATE', pageSize: 50 })
  return findLog(response.items, 'USER_CREATE', (item) => item.summary.includes(operatorUsername)) ?? null
})

const batchUnassignLog = await waitFor(async () => {
  const response = await logsPage(platformApi, { actionType: 'BATCH_UNASSIGN', pageSize: 50 })
  return findLog(response.items, 'BATCH_UNASSIGN', (item) => item.summary.includes(batchCode)) ?? null
})

const batchAssignLog = await waitFor(async () => {
  const response = await logsPage(platformApi, { actionType: 'BATCH_ASSIGN', pageSize: 50 })
  return findLog(response.items, 'BATCH_ASSIGN', (item) => item.summary.includes(operatorDisplayName)) ?? null
})

const loginSuccessLog = await waitFor(async () => {
  const response = await logsPage(platformApi, { actionType: 'AUTH_LOGIN_SUCCESS', pageSize: 50 })
  return response.items.find((item) => item.operatorName.includes('平台')) ?? null
})

await platformPage.goto(`${adminBaseUrl}/logs`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('logs-page').waitFor()
await platformPage.getByTestId('logs-filter-date-from').fill(new Date().toISOString().slice(0, 10))
await platformPage.getByTestId('logs-search-button').click()
await waitFor(async () => {
  const count = await platformPage.locator('[data-testid^="logs-row-"]').count()
  return count > 0 ? count : null
})
await platformPage.screenshot({ path: screenshotPath('159-logs-page-list.png'), fullPage: true })
await platformPage.getByTestId('logs-filter-action').selectOption('USER_CREATE')
await platformPage.getByTestId('logs-search-button').click()
await waitFor(async () => {
  const text = await platformPage.locator('[data-testid^="logs-row-"]').first().textContent()
  return String(text || '').includes(operatorUsername) ? text : null
})
await platformPage.locator('[data-testid^="logs-detail-"]').first().click()
await platformPage.getByTestId('logs-detail-dialog').waitFor()
await platformPage.screenshot({ path: screenshotPath('160-logs-detail-dialog.png'), fullPage: true })

const enterpriseSession = await login(enterprisePage, 'enterprise_admin', defaultPassword)
const enterpriseApi = await apiContextFor(enterpriseSession.token)
await enterprisePage.goto(`${adminBaseUrl}/logs`, { waitUntil: 'networkidle' })
await enterprisePage.getByTestId('logs-page').waitFor()
await waitFor(async () => {
  const count = await enterprisePage.locator('[data-testid^="logs-row-"]').count()
  return count > 0 ? count : null
})
const enterpriseText = await enterprisePage.locator('[data-testid="logs-page"]').textContent()
await enterprisePage.screenshot({ path: screenshotPath('161-logs-enterprise-scope.png'), fullPage: true })
const enterpriseForeignQuery = await readJson(enterpriseApi.get(`${apiBaseUrl}/logs?companyId=2`))

const platformAllLogs = await logsPage(platformApi, { pageSize: 100 })
const enterpriseOwnLogs = await logsPage(enterpriseApi, { pageSize: 100 })

const result = {
  createdUser: {
    id: createdUser.id,
    username: createdUser.username,
    realName: createdUser.realName
  },
  batchAssignment: {
    batchId,
    batchCode,
    assigneeUserId: assignedBatch.task?.assigneeUserId ?? null,
    assigneeName: assignedBatch.task?.assigneeName || ''
  },
  logChecks: {
    loginSuccess: Boolean(loginSuccessLog),
    userCreate: Boolean(userCreateLog),
    batchUnassign: Boolean(batchUnassignLog),
    batchAssign: Boolean(batchAssignLog),
    qualityUpload: Boolean(qualityUploadLog),
    riskComment: Boolean(riskCommentLog)
  },
  logEntries: {
    userCreate: userCreateLog,
    batchUnassign: batchUnassignLog,
    batchAssign: batchAssignLog,
    qualityUpload: qualityUploadLog,
    riskComment: riskCommentLog
  },
  enterpriseScope: {
    visibleTextHasOwnCompanyBatch: String(enterpriseText || '').includes(batchCode),
    visibleTextHasForeignRiskBatch: String(enterpriseText || '').includes(riskBatchCode),
    total: enterpriseOwnLogs.total,
    foreignQueryStatus: enterpriseForeignQuery.status,
    foreignQueryMessage: enterpriseForeignQuery.body?.message || ''
  },
  platformLogTotal: platformAllLogs.total,
  consoleErrors,
  pageErrors,
  screenshots: screenshotFiles
}

await fs.writeFile(
  path.join(outputDir, 'logs-round.json'),
  JSON.stringify(result, null, 2),
  'utf8'
)

await platformApi.dispose()
await enterpriseApi.dispose()
await browser.close()
