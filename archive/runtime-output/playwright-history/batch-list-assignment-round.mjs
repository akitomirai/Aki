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
const supportBatchCode = 'ORANGE-202603-A1'
const foreignBatchId = 3
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const screenshotFiles = []

const samplePng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kL9sAAAAASUVORK5CYII=',
  'base64'
)
const draftImage = path.join(outputDir, `batch-list-draft-${stamp}.png`)
await fs.writeFile(draftImage, samplePng)

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

async function mineBatchCodes(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches?mineOnly=true`))
  return (payload.body?.data ?? []).map((item) => item.batchCode)
}

async function draftList(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/field-drafts`))
  return payload.body?.data ?? []
}

async function waitForBatchPresence(apiContext, code, expectedPresent) {
  return waitFor(async () => {
    const codes = await mineBatchCodes(apiContext)
    const hasBatch = codes.includes(code)
    if (hasBatch === expectedPresent) {
      return codes
    }
    return null
  })
}

async function gotoFieldEntry(page, mode = 'todo') {
  await page.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
  await page.getByTestId('field-entry-page').waitFor()
  if (mode === 'drafts') {
    await page.getByTestId('field-list-mode-drafts').click()
  } else {
    await page.getByTestId('field-list-mode-todo').click()
  }
}

async function gotoBatchList(page) {
  await page.goto(`${adminBaseUrl}/batches`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-list-page').waitFor()
  await page.getByTestId('batch-filter-code').fill(batchCode)
  await page.getByTestId('batch-search-button').click()
  await page.getByTestId(`batch-card-${batchId}`).waitFor()
}

async function openAssignmentDialog(page) {
  await gotoBatchList(page)
  await page.getByTestId(`batch-assignment-open-${batchId}`).click()
  await page.locator('[data-testid="batch-assignment-dialog"]:visible').waitFor()
  await page.getByTestId('batch-list-assignment-select').waitFor()
}

async function waitForAssignmentDialogHidden(page) {
  return waitFor(async () => {
    try {
      return (await page.locator('[data-testid="batch-assignment-dialog"]:visible').count()) === 0
    } catch {
      return true
    }
  })
}

async function assignFromList(page, assigneeUserId) {
  await openAssignmentDialog(page)
  if (assigneeUserId == null) {
    if (!(await page.getByTestId('batch-list-assignment-clear').isDisabled())) {
      await page.getByTestId('batch-list-assignment-clear').click()
    } else {
      await page.getByRole('button', { name: '取消' }).click()
    }
  } else {
    await page.getByTestId('batch-list-assignment-select').selectOption(String(assigneeUserId))
    await page.getByTestId('batch-list-assignment-submit').click()
  }
  await waitForAssignmentDialogHidden(page)
}

async function readBatchRowTask(page) {
  await gotoBatchList(page)
  const assignee = await page.getByTestId(`batch-task-assignee-${batchId}`).innerText()
  const assignedAt = await page.getByTestId(`batch-task-assigned-at-${batchId}`).innerText()
  const taskStatus = await page.getByTestId(`batch-task-status-${batchId}`).innerText()
  const todayCompleted = await page.getByTestId(`batch-task-today-${batchId}`).innerText()
  const draftStatus = await page.getByTestId(`batch-task-draft-${batchId}`).innerText()
  return { assignee, assignedAt, taskStatus, todayCompleted, draftStatus }
}

async function saveDraftForBatch(page, code, titlePrefix, operatorName, location, summary) {
  await gotoFieldEntry(page)
  const row = page.locator('[data-testid="field-task-row"]').filter({ hasText: code }).first()
  await row.waitFor()
  await row.getByTestId('field-todo-open-button').click()
  await page.getByTestId('field-entry-page').waitFor()
  await page.locator('input[type="text"]').nth(0).fill(`${titlePrefix} ${stamp}`)
  await page.locator('textarea').fill(summary)
  await page.locator('input[type="text"]').nth(1).fill(location)
  await page.locator('input[type="text"]').nth(2).fill(operatorName)
  await page.locator('input[type="file"]').setInputFiles(draftImage)
  await page.waitForFunction(() => {
    const rows = document.querySelectorAll('[data-testid="field-image-queue"] .image-row').length
    const uploading = document.querySelectorAll('[data-testid="field-image-queue"] .image-row.is-uploading').length
    return rows >= 1 && uploading === 0
  }, { timeout: 20000 })
  await page.getByTestId('field-entry-save-draft').click()
}

const browser = await chromium.launch({ headless: true })

const platformContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const enterpriseContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const operatorContext = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })
const supportContext = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })

const platformPage = await platformContext.newPage()
const enterprisePage = await enterpriseContext.newPage()
const operatorPage = await operatorContext.newPage()
const supportPage = await supportContext.newPage()

const platformSession = await login(platformPage, 'platform')
const enterpriseSession = await login(enterprisePage, 'enterprise_admin')
const operatorSession = await login(operatorPage, 'operator')
const supportSession = await login(supportPage, 'operator_support')

const platformApi = await apiContextFor(platformSession.token)
const enterpriseApi = await apiContextFor(enterpriseSession.token)
const operatorApi = await apiContextFor(operatorSession.token)
const supportApi = await apiContextFor(supportSession.token)

await operatorApi.delete(`${apiBaseUrl}/batches/${batchId}/field-draft`)
await supportApi.delete(`${apiBaseUrl}/batches/1/field-draft`)

// Step 0: reset batch 2 to unassigned from list page for a stable starting point.
await assignFromList(platformPage, null)
const operatorCodesAfterReset = await waitForBatchPresence(operatorApi, batchCode, false)
const supportCodesAfterReset = await waitForBatchPresence(supportApi, batchCode, false)
const operatorAccessAfterReset = await readJson(operatorApi.get(`${apiBaseUrl}/batches/${batchId}`))

// Step 1: list page shows task info and supports assigning to operator.
const initialListTask = await readBatchRowTask(platformPage)
await platformPage.screenshot({ path: screenshotPath('61-batch-list-task-info.png'), fullPage: true })
await assignFromList(platformPage, 3)
const operatorCodesAfterAssign = await waitForBatchPresence(operatorApi, batchCode, true)
const listTaskAfterAssign = await waitFor(async () => {
  const row = await readBatchRowTask(platformPage)
  return row.assignee.includes('Field Operator') ? row : null
})
await gotoFieldEntry(operatorPage)
await operatorPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first().waitFor()
await platformPage.screenshot({ path: screenshotPath('62-batch-list-assign-operator.png'), fullPage: true })
await operatorPage.screenshot({ path: screenshotPath('63-batch-list-operator-todo.png'), fullPage: true })

// Step 2: reassign to operator_support directly from list page.
await assignFromList(platformPage, 5)
const operatorCodesAfterReassign = await waitForBatchPresence(operatorApi, batchCode, false)
const supportCodesAfterReassign = await waitForBatchPresence(supportApi, batchCode, true)
const listTaskAfterReassign = await waitFor(async () => {
  const row = await readBatchRowTask(platformPage)
  return row.assignee.includes('Field Operator B') ? row : null
})
await gotoFieldEntry(supportPage)
await supportPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first().waitFor()
await platformPage.screenshot({ path: screenshotPath('64-batch-list-reassign-support.png'), fullPage: true })
await supportPage.screenshot({ path: screenshotPath('65-batch-list-support-todo.png'), fullPage: true })

// Step 3: clear assignment directly from list page.
await assignFromList(platformPage, null)
const operatorCodesAfterClear = await waitForBatchPresence(operatorApi, batchCode, false)
const supportCodesAfterClear = await waitForBatchPresence(supportApi, batchCode, false)
const listTaskAfterClear = await waitFor(async () => {
  const row = await readBatchRowTask(platformPage)
  return row.assignee.includes('未分配操作员') ? row : null
})
await platformPage.screenshot({ path: screenshotPath('66-batch-list-clear-assignment.png'), fullPage: true })

// Step 4: create an unrelated support draft on batch 1, then operator draft on batch 2.
await assignFromList(platformPage, 3)
await waitForBatchPresence(operatorApi, batchCode, true)
await saveDraftForBatch(
  supportPage,
  supportBatchCode,
  '支持岗批次一草稿',
  'Field Operator B',
  'Batch 1 draft checkpoint',
  `批次 ${supportBatchCode} 的服务端草稿，用于验证无关草稿不会被误清。`
)
const supportDraftsBeforeForce = await waitFor(async () => {
  const drafts = await draftList(supportApi)
  return drafts.some((item) => item.batchCode === supportBatchCode) ? drafts : null
})

await saveDraftForBatch(
  operatorPage,
  batchCode,
  '列表页改派草稿',
  'Field Operator',
  'Batch 2 reassignment checkpoint',
  `批次 ${batchCode} 的服务端草稿，用于验证列表页强制改派清理规则。`
)
const operatorDraftsBeforeForce = await waitFor(async () => {
  const drafts = await draftList(operatorApi)
  return drafts.some((item) => item.batchCode === batchCode) ? drafts : null
})

// Step 5: blocked reassign prompt on list page, then force reassign.
await openAssignmentDialog(platformPage)
await platformPage.getByTestId('batch-list-assignment-select').selectOption('5')
await platformPage.getByTestId('batch-list-assignment-submit').click()
await platformPage.getByTestId('batch-list-assignment-confirm').waitFor()
const blockedPromptText = await platformPage.getByTestId('batch-list-assignment-confirm').innerText()
await platformPage.screenshot({ path: screenshotPath('67-batch-list-draft-blocked.png'), fullPage: true })
await platformPage.getByTestId('batch-list-assignment-force').click()
await waitForAssignmentDialogHidden(platformPage)

const operatorCodesAfterForce = await waitForBatchPresence(operatorApi, batchCode, false)
const supportCodesAfterForce = await waitForBatchPresence(supportApi, batchCode, true)
const operatorDraftsAfterForce = await waitFor(async () => {
  const drafts = await draftList(operatorApi)
  return drafts.every((item) => item.batchCode !== batchCode) ? drafts : null
})
const supportDraftsAfterForce = await waitFor(async () => {
  const drafts = await draftList(supportApi)
  return drafts.some((item) => item.batchCode === supportBatchCode) ? drafts : null
})
const finalListTask = await waitFor(async () => {
  const row = await readBatchRowTask(platformPage)
  return row.assignee.includes('Field Operator B') && row.draftStatus.includes('无草稿') ? row : null
})

await gotoFieldEntry(supportPage)
await supportPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first().waitFor()
await gotoFieldEntry(operatorPage, 'drafts')
const operatorDraftRowCount = await operatorPage.getByTestId('field-draft-row').count()
await platformPage.screenshot({ path: screenshotPath('68-batch-list-force-reassign-result.png'), fullPage: true })
await supportPage.screenshot({ path: screenshotPath('69-batch-list-force-reassign-support.png'), fullPage: true })
await operatorPage.screenshot({ path: screenshotPath('70-batch-list-operator-drafts-empty.png'), fullPage: true })

// Step 6: permission messages in Chinese.
const enterpriseCrossCompanyLookup = await readJson(enterpriseApi.get(`${apiBaseUrl}/batches/lookup/operators?companyId=2`))
const enterpriseForeignAssign = await readJson(enterpriseApi.post(`${apiBaseUrl}/batches/${foreignBatchId}/assignment`, {
  data: {
    assigneeUserId: 3,
    forceClearDraft: false
  }
}))
const operatorLookupDenied = await readJson(operatorApi.get(`${apiBaseUrl}/batches/lookup/operators`))

const result = {
  verifiedAt: new Date().toISOString(),
  users: {
    platform: platformSession.user,
    enterpriseAdmin: enterpriseSession.user,
    operator: operatorSession.user,
    operatorSupport: supportSession.user
  },
  listTaskChecks: {
    initialListTask,
    listTaskAfterAssign,
    listTaskAfterReassign,
    listTaskAfterClear,
    finalListTask
  },
  assignmentChecks: {
    resetToUnassigned: {
      operatorMineCodes: operatorCodesAfterReset,
      operatorSupportMineCodes: supportCodesAfterReset,
      operatorAccessDenied: operatorAccessAfterReset
    },
    assignToOperator: {
      operatorMineCodes: operatorCodesAfterAssign
    },
    reassignToOperatorSupport: {
      operatorMineCodes: operatorCodesAfterReassign,
      operatorSupportMineCodes: supportCodesAfterReassign
    },
    clearAssignment: {
      operatorMineCodes: operatorCodesAfterClear,
      operatorSupportMineCodes: supportCodesAfterClear
    }
  },
  draftForceReassignChecks: {
    blockedPromptText,
    operatorDraftsBeforeForce,
    supportDraftsBeforeForce,
    operatorDraftsAfterForce,
    supportDraftsAfterForce,
    operatorMineAfterForce: operatorCodesAfterForce,
    operatorSupportMineAfterForce: supportCodesAfterForce,
    operatorDraftRowCount
  },
  permissionMessages: {
    operatorLookupDenied,
    enterpriseCrossCompanyLookup,
    enterpriseForeignAssign
  },
  screenshots: screenshotFiles
}

await fs.writeFile(
  path.join(outputDir, 'batch-list-assignment-round.json'),
  `${JSON.stringify(result, null, 2)}\n`,
  'utf8'
)

console.log(JSON.stringify(result, null, 2))

await platformApi.dispose()
await enterpriseApi.dispose()
await operatorApi.dispose()
await supportApi.dispose()
await browser.close()
