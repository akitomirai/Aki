import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const screenshotFiles = []

const samplePng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kL9sAAAAASUVORK5CYII=',
  'base64'
)

const imageOne = path.join(outputDir, `premerge-photo-1-${stamp}.png`)
const imageTwo = path.join(outputDir, `premerge-photo-2-${stamp}.png`)
const imageThree = path.join(outputDir, `premerge-photo-3-${stamp}.png`)
await fs.writeFile(imageOne, samplePng)
await fs.writeFile(imageTwo, samplePng)
await fs.writeFile(imageThree, samplePng)

function trackScreenshot(name) {
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

async function logout(page) {
  await page.evaluate(() => {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
    sessionStorage.clear()
  })
}

async function waitForQueue(page, expectedRows, options = {}) {
  const allowFailed = Boolean(options.allowFailed)
  await page.waitForFunction(
    ({ expectedRows, allowFailed }) => {
      const rows = document.querySelectorAll('[data-testid="field-image-queue"] .image-row').length
      const uploading = document.querySelectorAll('[data-testid="field-image-queue"] .image-state.uploading').length
      const failed = document.querySelectorAll('[data-testid="field-image-queue"] .image-row.is-failed').length
      if (rows !== expectedRows) {
        return false
      }
      if (uploading > 0) {
        return false
      }
      if (!allowFailed && failed > 0) {
        return false
      }
      return true
    },
    { expectedRows, allowFailed },
    { timeout: 25000 }
  )
}

async function getQueueLabels(page) {
  return page.locator('[data-testid="field-image-queue"] .image-row strong').evaluateAll((nodes) =>
    nodes.map((node) => (node.textContent || '').replace(/^\d+\.\s*/, '').trim())
  )
}

async function getQueueState(page) {
  return page.evaluate(() => {
    const rows = Array.from(document.querySelectorAll('[data-testid="field-image-queue"] .image-row'))
    return rows.map((row) => ({
      label: row.querySelector('strong')?.textContent?.replace(/^\d+\.\s*/, '').trim() || '',
      status: row.className,
      error: row.querySelector('.image-error')?.textContent?.trim() || ''
    }))
  })
}

async function createApiContext(token) {
  return request.newContext({
    extraHTTPHeaders: {
      Authorization: `Bearer ${token}`
    }
  })
}

async function fillEntryForm(page, values) {
  const textInputs = page.locator('input[type="text"]')
  await textInputs.nth(0).fill(values.title)
  await page.locator('textarea').fill(values.summary)
  await textInputs.nth(1).fill(values.location)
  await textInputs.nth(2).fill(values.operatorName)
}

const browser = await chromium.launch({ headless: true })
const mobileContext = await browser.newContext({
  viewport: { width: 390, height: 844 },
  isMobile: true,
  hasTouch: true
})
const operatorPage = await mobileContext.newPage()
const operatorConsoleErrors = []
const operatorPageErrors = []

operatorPage.on('console', (message) => {
  if (message.type() === 'error') {
    operatorConsoleErrors.push(message.text())
  }
})
operatorPage.on('pageerror', (error) => operatorPageErrors.push(String(error)))

let operatorSession = await login(operatorPage, 'operator')
let operatorApi = await createApiContext(operatorSession.token)

await operatorApi.delete(`${apiBaseUrl}/batches/2/field-draft`).catch(() => null)
await operatorPage.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await operatorPage.getByTestId('field-entry-page').waitFor()

const operatorMineBefore = await readJson(operatorApi.get(`${apiBaseUrl}/batches?mineOnly=true`))
const operatorBlockedDetail = await readJson(operatorApi.get(`${apiBaseUrl}/batches/3`))
const operatorBlockedSubmit = await readJson(operatorApi.post(`${apiBaseUrl}/batches/3/records/quick`, {
  data: {
    stage: 'PRODUCE',
    title: `Blocked submit ${stamp}`,
    summary: 'blocked submit check',
    eventTime: '2026-03-30 18:05',
    operatorName: 'Field Operator',
    location: 'Unauthorized check',
    attachmentIds: [],
    visibleToConsumer: true
  }
}))

await operatorPage.screenshot({ path: trackScreenshot('31-premerge-operator-todo.png'), fullPage: true })

await operatorPage.getByTestId('field-todo-open-button').first().click()
await operatorPage.locator('.back-button').waitFor()

const firstDraftTitle = `Premerge Draft ${stamp}`
const firstDraftSummary = `Premerge draft save ${stamp}`
await fillEntryForm(operatorPage, {
  title: firstDraftTitle,
  summary: firstDraftSummary,
  location: 'Cold chain loading bay A',
  operatorName: 'Field Operator'
})
await operatorPage.locator('input[type="file"]').setInputFiles(imageOne)
await waitForQueue(operatorPage, 1)
await operatorPage.getByTestId('field-entry-save-draft').click()

const draftsAfterFirstSave = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches/field-drafts`))
  return payload.body?.data?.length === 1 ? payload : null
})

const mineAfterFirstSave = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches?mineOnly=true`))
  const item = payload.body?.data?.[0]
  return item?.taskStatus === 'DRAFT' ? payload : null
})

await operatorPage.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await operatorPage.evaluate(() => localStorage.removeItem('field_entry_drafts_v1'))
await operatorPage.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await operatorPage.getByTestId('field-list-mode-drafts').click()
await operatorPage.getByTestId('field-draft-row').first().waitFor()
const draftRowAfterRefresh = (await operatorPage.getByTestId('field-draft-row').first().textContent()) || ''
await operatorPage.screenshot({ path: trackScreenshot('32-premerge-draft-refresh.png'), fullPage: true })

await logout(operatorPage)
await operatorPage.goto(`${adminBaseUrl}/login`, { waitUntil: 'networkidle' })
operatorSession = await login(operatorPage, 'operator')
await operatorApi.dispose()
operatorApi = await createApiContext(operatorSession.token)
await operatorPage.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await operatorPage.getByTestId('field-list-mode-drafts').click()
await operatorPage.getByTestId('field-draft-row').first().waitFor()
const draftRowAfterRelogin = (await operatorPage.getByTestId('field-draft-row').first().textContent()) || ''
await operatorPage.screenshot({ path: trackScreenshot('33-premerge-draft-relogin.png'), fullPage: true })

operatorPage.once('dialog', (dialog) => dialog.accept())
await operatorPage.getByTestId('field-draft-delete').first().click()
const draftsAfterDelete = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches/field-drafts`))
  return payload.body?.data?.length === 0 ? payload : null
})
const mineAfterDelete = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches?mineOnly=true`))
  const item = payload.body?.data?.[0]
  return item?.taskStatus === 'PENDING' && item?.todayCompleted === false ? payload : null
})

await operatorPage.getByTestId('field-list-mode-todo').click()
await operatorPage.getByTestId('field-task-filter-pending').click()
await operatorPage.getByTestId('field-task-row').first().waitFor()
await operatorPage.screenshot({ path: trackScreenshot('34-premerge-draft-deleted.png'), fullPage: true })

await operatorPage.getByTestId('field-todo-open-button').first().click()
await operatorPage.locator('.back-button').waitFor()

const secondDraftTitle = `Premerge Submit ${stamp}`
const submitSummary = `Premerge submit ${stamp} after retry and reorder`
await fillEntryForm(operatorPage, {
  title: secondDraftTitle,
  summary: submitSummary,
  location: 'Cold chain loading bay B',
  operatorName: 'Field Operator'
})
await operatorPage.locator('input[type="file"]').setInputFiles(imageOne)
await waitForQueue(operatorPage, 1)
await operatorPage.getByTestId('field-entry-save-draft').click()

const draftsAfterSecondSave = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches/field-drafts`))
  return payload.body?.data?.length === 1 ? payload : null
})
const mineAfterSecondSave = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches?mineOnly=true`))
  const item = payload.body?.data?.[0]
  return item?.taskStatus === 'DRAFT' ? payload : null
})

await operatorPage.locator('.back-button').click()
await operatorPage.getByTestId('field-task-filter-draft').click()
await operatorPage.getByTestId('field-task-row').first().waitFor()
await operatorPage.screenshot({ path: trackScreenshot('35-premerge-draft-filter.png'), fullPage: true })
await operatorPage.getByTestId('field-todo-open-button').first().click()
await operatorPage.getByTestId('field-entry-draft-banner').waitFor()
await operatorPage.locator('textarea').fill(submitSummary)

let failNextUpload = false
let uploadFailureCount = 0
await operatorPage.route('**/api/batches/files/upload', async (route) => {
  if (failNextUpload) {
    failNextUpload = false
    uploadFailureCount += 1
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({
        success: false,
        message: 'mock upload failure'
      })
    })
    return
  }
  await route.continue()
})

failNextUpload = true
await operatorPage.locator('input[type="file"]').setInputFiles(imageTwo)
await waitForQueue(operatorPage, 2, { allowFailed: true })
const failedQueueState = await getQueueState(operatorPage)
await operatorPage.screenshot({ path: trackScreenshot('36-premerge-upload-failed.png'), fullPage: true })

await operatorPage.locator('[data-testid="field-image-queue"] .image-row').nth(1).getByRole('button', { name: '重试' }).click()
await waitForQueue(operatorPage, 2)
await operatorPage.locator('input[type="file"]').setInputFiles(imageThree)
await waitForQueue(operatorPage, 3)

const orderBeforeReorder = await getQueueLabels(operatorPage)
await operatorPage.locator('[data-testid="field-image-queue"] .image-row').nth(2).getByRole('button', { name: '上移' }).click()
await operatorPage.locator('[data-testid="field-image-queue"] .image-row').nth(1).getByRole('button', { name: '上移' }).click()
await waitFor(async () => {
  const labels = await getQueueLabels(operatorPage)
  return labels[0]?.includes(path.basename(imageThree)) ? labels : null
})
await operatorPage.locator('[data-testid="field-image-queue"] .image-row').nth(1).getByRole('button', { name: '删除' }).click()
await waitForQueue(operatorPage, 2)

const finalQueueOrder = await getQueueLabels(operatorPage)
await operatorPage.screenshot({ path: trackScreenshot('37-premerge-queue-ready.png'), fullPage: true })

await operatorPage.getByTestId('field-entry-submit').click()
await operatorPage.getByTestId('field-entry-success').waitFor()
await operatorPage.screenshot({ path: trackScreenshot('38-premerge-submit-success.png'), fullPage: true })

const draftsAfterSubmit = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches/field-drafts`))
  return payload.body?.data?.length === 0 ? payload : null
})
const mineAfterSubmit = await waitFor(async () => {
  const payload = await readJson(operatorApi.get(`${apiBaseUrl}/batches?mineOnly=true`))
  const item = payload.body?.data?.[0]
  return item?.taskStatus === 'COMPLETED' && item?.todayCompleted === true ? payload : null
})
const operatorWorkbenchApi = await readJson(operatorApi.get(`${apiBaseUrl}/batches/2`))

await operatorPage.getByTestId('field-entry-success').getByRole('button', { name: '查看批次工作台' }).click()
await operatorPage.waitForURL(/\/batches\/2/, { timeout: 20000 })
await operatorPage.getByTestId('batch-workbench-page').waitFor()
await operatorPage.getByTestId('workbench-recent-records').locator('.record-card').first().waitFor()
const operatorWorkbenchImageAlts = await operatorPage
  .getByTestId('workbench-recent-records')
  .locator('.record-card')
  .first()
  .locator('img.record-image')
  .evaluateAll((nodes) => nodes.map((node) => node.getAttribute('alt') || ''))
await operatorPage.screenshot({ path: trackScreenshot('39-premerge-operator-workbench.png'), fullPage: true })

await operatorPage.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await operatorPage.getByTestId('field-task-filter-draft').click()
await operatorPage.waitForTimeout(500)
const draftRowsAfterSubmit = await operatorPage.getByTestId('field-task-row').count()
await operatorPage.getByTestId('field-task-filter-done_today').click()
await operatorPage.getByTestId('field-task-row').first().waitFor()
await operatorPage.screenshot({ path: trackScreenshot('40-premerge-done-today.png'), fullPage: true })

const desktopContext = await browser.newContext({
  viewport: { width: 1440, height: 960 }
})
const platformPage = await desktopContext.newPage()
const platformConsoleErrors = []
const platformPageErrors = []

platformPage.on('console', (message) => {
  if (message.type() === 'error') {
    platformConsoleErrors.push(message.text())
  }
})
platformPage.on('pageerror', (error) => platformPageErrors.push(String(error)))

const platformSession = await login(platformPage, 'platform')
const platformApi = await createApiContext(platformSession.token)
const platformMine = await readJson(platformApi.get(`${apiBaseUrl}/batches?mineOnly=true`))
const platformAll = await readJson(platformApi.get(`${apiBaseUrl}/batches`))
const staleCompletedBatch = (platformAll.body?.data || []).find((item) => item.batchCode === 'ORANGE-202603-A1') || null
const platformWorkbenchApi = await readJson(platformApi.get(`${apiBaseUrl}/batches/2`))

await platformPage.goto(`${adminBaseUrl}/batches/2`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-workbench-page').waitFor()
await platformPage.screenshot({ path: trackScreenshot('41-premerge-platform-workbench.png'), fullPage: true })

const result = {
  verifiedAt: new Date().toISOString(),
  operatorUser: operatorSession.user,
  platformUser: platformSession.user,
  permissionChecks: {
    operatorMineCount: operatorMineBefore.body?.data?.length ?? 0,
    operatorMineBatchCodes: (operatorMineBefore.body?.data ?? []).map((item) => item.batchCode),
    operatorBlockedDetailStatus: operatorBlockedDetail.status,
    operatorBlockedDetailMessage: operatorBlockedDetail.body?.message ?? null,
    operatorBlockedSubmitStatus: operatorBlockedSubmit.status,
    operatorBlockedSubmitMessage: operatorBlockedSubmit.body?.message ?? null,
    platformMineCount: platformMine.body?.data?.length ?? 0,
    platformAllCount: platformAll.body?.data?.length ?? 0
  },
  draftLifecycle: {
    firstDraftServerCount: draftsAfterFirstSave.body?.data?.length ?? 0,
    draftRowAfterRefresh,
    draftRowAfterRelogin,
    draftsAfterDeleteCount: draftsAfterDelete.body?.data?.length ?? 0,
    draftsAfterSubmitCount: draftsAfterSubmit.body?.data?.length ?? 0
  },
  taskStatusChecks: {
    before: operatorMineBefore.body?.data?.[0] ?? null,
    afterFirstSave: mineAfterFirstSave.body?.data?.[0] ?? null,
    afterDelete: mineAfterDelete.body?.data?.[0] ?? null,
    afterSecondSave: mineAfterSecondSave.body?.data?.[0] ?? null,
    afterSubmit: mineAfterSubmit.body?.data?.[0] ?? null,
    draftRowsAfterSubmit,
    staleCompletedBatch
  },
  imageChecks: {
    uploadFailureCount,
    failedQueueState,
    orderBeforeReorder,
    finalQueueOrder,
    operatorWorkbenchImageAlts,
    apiAttachmentNames: (operatorWorkbenchApi.body?.data?.trace?.recentRecords?.[0]?.attachments ?? []).map((item) => item.fileName)
  },
  workbenchChecks: {
    operatorTask: operatorWorkbenchApi.body?.data?.task ?? null,
    operatorRecentRecord: operatorWorkbenchApi.body?.data?.trace?.recentRecords?.[0] ?? null,
    platformTask: platformWorkbenchApi.body?.data?.task ?? null,
    platformRecentRecord: platformWorkbenchApi.body?.data?.trace?.recentRecords?.[0] ?? null
  },
  browserErrors: {
    operatorConsoleErrors,
    operatorPageErrors,
    platformConsoleErrors,
    platformPageErrors
  },
  screenshots: screenshotFiles
}

await fs.writeFile(path.join(outputDir, 'premerge-check-result.json'), JSON.stringify(result, null, 2), 'utf8')

await platformApi.dispose()
await operatorApi.dispose()
await desktopContext.close()
await mobileContext.close()
await browser.close()

console.log(JSON.stringify(result, null, 2))
