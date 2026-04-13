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
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const screenshotFiles = []

const samplePng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kL9sAAAAASUVORK5CYII=',
  'base64'
)
const draftImage = path.join(outputDir, `assignment-draft-${stamp}.png`)
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
  } catch (error) {
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

async function batchTask(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/${batchId}`))
  return payload.body?.data?.task ?? null
}

async function waitForBatchPresence(apiContext, expectedPresent) {
  return waitFor(async () => {
    const codes = await mineBatchCodes(apiContext)
    const hasBatch = codes.includes(batchCode)
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

const browser = await chromium.launch({ headless: true })

const platformContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const operatorContext = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })
const supportContext = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })

const platformPage = await platformContext.newPage()
const operatorPage = await operatorContext.newPage()
const supportPage = await supportContext.newPage()

const platformSession = await login(platformPage, 'platform')
const operatorSession = await login(operatorPage, 'operator')
const supportSession = await login(supportPage, 'operator_support')

const platformApi = await apiContextFor(platformSession.token)
const operatorApi = await apiContextFor(operatorSession.token)
const supportApi = await apiContextFor(supportSession.token)

await platformPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-workbench-page').waitFor()

// Step 0: reset batch 2 to unassigned so the assignment action is exercised for real.
if (!(await platformPage.getByTestId('assignment-clear-button').isDisabled())) {
  await platformPage.getByTestId('assignment-clear-button').click()
}
const operatorCodesAfterReset = await waitForBatchPresence(operatorApi, false)
await gotoFieldEntry(operatorPage)
await operatorPage.screenshot({ path: screenshotPath('51-assignment-reset-operator-empty.png'), fullPage: true })

// Step 1: assign to operator and verify operator sees batch 2.
await platformPage.getByTestId('assignment-operator-select').selectOption('3')
await platformPage.getByTestId('assignment-save-button').click()
const operatorCodesAfterAssign = await waitForBatchPresence(operatorApi, true)
await gotoFieldEntry(operatorPage)
await operatorPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first().waitFor()
await platformPage.screenshot({ path: screenshotPath('52-assignment-platform-operator.png'), fullPage: true })
await operatorPage.screenshot({ path: screenshotPath('53-assignment-operator-todo.png'), fullPage: true })

// Step 2: reassign to operator_support and verify visibility switches.
await platformPage.getByTestId('assignment-operator-select').selectOption('5')
await platformPage.getByTestId('assignment-save-button').click()
const operatorCodesAfterReassign = await waitForBatchPresence(operatorApi, false)
const supportCodesAfterReassign = await waitForBatchPresence(supportApi, true)
await gotoFieldEntry(operatorPage)
await gotoFieldEntry(supportPage)
await supportPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first().waitFor()
await platformPage.screenshot({ path: screenshotPath('54-reassign-platform-support.png'), fullPage: true })
await supportPage.screenshot({ path: screenshotPath('55-reassign-support-todo.png'), fullPage: true })

// Step 3: clear assignment and verify batch 2 disappears from both operators.
await platformPage.getByTestId('assignment-clear-button').click()
const operatorCodesAfterClear = await waitForBatchPresence(operatorApi, false)
const supportCodesAfterClear = await waitForBatchPresence(supportApi, false)
await gotoFieldEntry(operatorPage)
await gotoFieldEntry(supportPage)
await platformPage.screenshot({ path: screenshotPath('56-clear-platform-unassigned.png'), fullPage: true })

// Step 4: assign back to operator, save a server draft, then force reassign with draft cleanup.
await platformPage.getByTestId('assignment-operator-select').selectOption('3')
await platformPage.getByTestId('assignment-save-button').click()
const operatorCodesBeforeDraft = await waitForBatchPresence(operatorApi, true)

await gotoFieldEntry(operatorPage)
await operatorPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first().getByTestId('field-todo-open-button').click()
await operatorPage.getByTestId('field-entry-page').waitFor()
await operatorPage.locator('input[type="text"]').nth(0).fill(`改派草稿 ${stamp}`)
await operatorPage.locator('textarea').fill(`批次 ${batchCode} 的服务端草稿，用于验证强制改派清草稿。`)
await operatorPage.locator('input[type="text"]').nth(1).fill('Cold chain assignment checkpoint')
await operatorPage.locator('input[type="text"]').nth(2).fill('Field Operator')
await operatorPage.locator('input[type="file"]').setInputFiles(draftImage)
await operatorPage.waitForFunction(() => {
  const rows = document.querySelectorAll('[data-testid="field-image-queue"] .image-row').length
  const uploading = document.querySelectorAll('[data-testid="field-image-queue"] .image-row.is-uploading').length
  return rows === 1 && uploading === 0
}, { timeout: 20000 })
await operatorPage.getByTestId('field-entry-save-draft').click()
const operatorDraftAfterSave = await waitFor(async () => {
  const drafts = await draftList(operatorApi)
  return drafts.length === 1 ? drafts : null
})

await platformPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-workbench-page').waitFor()
await waitFor(async () => {
  const task = await batchTask(platformApi)
  return task?.draftPending ? task : null
})
await platformPage.getByTestId('assignment-operator-select').selectOption('5')
await platformPage.getByTestId('assignment-save-button').click()
await platformPage.getByTestId('assignment-draft-confirm').waitFor()
await platformPage.screenshot({ path: screenshotPath('57-draft-blocked-reassign.png'), fullPage: true })
await platformPage.getByTestId('assignment-draft-force').click()

const operatorCodesAfterForcedReassign = await waitForBatchPresence(operatorApi, false)
const supportCodesAfterForcedReassign = await waitForBatchPresence(supportApi, true)
const operatorDraftsAfterForce = await waitFor(async () => {
  const drafts = await draftList(operatorApi)
  return drafts.length === 0 ? drafts : null
})
const finalTask = await waitFor(async () => {
  const task = await batchTask(platformApi)
  return task && task.assigneeUserId === 5 && task.taskStatus === 'PENDING' && task.todayCompleted === false && task.draftPending === false
    ? task
    : null
})

await gotoFieldEntry(operatorPage, 'drafts')
const operatorDraftRowCount = await operatorPage.getByTestId('field-draft-row').count()
await gotoFieldEntry(supportPage)
await supportPage.locator('[data-testid="field-task-row"]').filter({ hasText: batchCode }).first().waitFor()
await platformPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-workbench-page').waitFor()
await platformPage.screenshot({ path: screenshotPath('58-final-workbench-assignment.png'), fullPage: true })
await supportPage.screenshot({ path: screenshotPath('59-force-reassign-support-todo.png'), fullPage: true })
await operatorPage.screenshot({ path: screenshotPath('60-force-reassign-operator-drafts-empty.png'), fullPage: true })

const platformOperators = await readJson(platformApi.get(`${apiBaseUrl}/batches/lookup/operators?companyId=1`))
const finalWorkbench = await readJson(platformApi.get(`${apiBaseUrl}/batches/${batchId}`))

const result = {
  verifiedAt: new Date().toISOString(),
  users: {
    platform: platformSession.user,
    operator: operatorSession.user,
    operatorSupport: supportSession.user
  },
  operatorOptions: platformOperators.body?.data ?? [],
  assignmentChecks: {
    resetToUnassigned: {
      operatorMineCodes: operatorCodesAfterReset
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
  draftReassignChecks: {
    operatorMineBeforeDraft: operatorCodesBeforeDraft,
    operatorDraftAfterSave,
    operatorMineAfterForcedReassign: operatorCodesAfterForcedReassign,
    operatorSupportMineAfterForcedReassign: supportCodesAfterForcedReassign,
    operatorDraftsAfterForce,
    operatorDraftRowCount
  },
  finalTask,
  finalWorkbenchTask: finalWorkbench.body?.data?.task ?? null,
  screenshots: screenshotFiles
}

await fs.writeFile(path.join(outputDir, 'batch-assignment-round.json'), JSON.stringify(result, null, 2), 'utf8')

await platformApi.dispose()
await operatorApi.dispose()
await supportApi.dispose()
await platformContext.close()
await operatorContext.close()
await supportContext.close()
await browser.close()

console.log(JSON.stringify(result, null, 2))
