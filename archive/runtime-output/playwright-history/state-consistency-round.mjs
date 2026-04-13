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
const publicUrl = `${traceBaseUrl}/t/orange-202603-d1`
const screenshotFiles = []

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

function visibleEnums(text) {
  return [...new Set(String(text || '').match(/\b(?:PUBLISHED|DRAFT|FROZEN|RECALLED|COMPLETED|PROCESSING|RECTIFIED)\b/g) ?? [])]
}

function visibleEnglishSeeds(text) {
  return [...new Set(String(text || '').match(/Xinfeng Orchard Base|Wuyuan Tea Base|Public trace page is available for this batch\.|Used to verify released-batch linkage with the workbench\.|The batch has been created and still needs field records, QA and QR data\.|Used for continuous field-entry verification before publish\.|The batch is paused and waiting for follow-up handling\.|Used to review frozen-batch rectification flow\.|Latest QA failed and the batch is waiting for recheck\./g) ?? [])]
}

async function bodyText(page) {
  return page.locator('body').innerText()
}

async function fieldEntryForAssignee(browser, detail) {
  const usernameByAssignee = {
    3: 'operator',
    5: 'operator_support'
  }
  const username = usernameByAssignee[detail.task?.assigneeUserId] || 'operator_support'
  const context = await browser.newContext({ viewport: { width: 430, height: 932 } })
  const page = await context.newPage()
  await login(page, username)
  await page.goto(`${adminBaseUrl}/field-entry?batchId=${batchId}`, { waitUntil: 'networkidle' })
  await page.getByTestId('field-entry-page').waitFor()
  return { context, page, username }
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
const detail = await batchDetail(platformApi)

await platformPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-workbench-page').waitFor()
await platformPage.screenshot({ path: screenshotPath('89-state-workbench.png'), fullPage: true })
const workbenchText = await bodyText(platformPage)
const workbenchResult = {
  batchStatus: await platformPage.locator('[data-testid="workbench-next-step-card"] .status-badge').innerText(),
  taskExec: await platformPage.locator('[data-testid="workbench-top-grid"] .summary-card').nth(1).locator('.card-title').innerText(),
  workbenchEnums: visibleEnums(workbenchText),
  workbenchEnglishSeeds: visibleEnglishSeeds(workbenchText)
}

await platformPage.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('batch-list-page').waitFor()
await platformPage.screenshot({ path: screenshotPath('90-state-batches.png'), fullPage: true })
const listText = await bodyText(platformPage)
const listResult = {
  assignee: await platformPage.getByTestId(`batch-task-assignee-${batchId}`).innerText(),
  assignedAt: await platformPage.getByTestId(`batch-task-assigned-at-${batchId}`).innerText(),
  taskStatus: await platformPage.getByTestId(`batch-task-status-${batchId}`).innerText(),
  todayStatus: await platformPage.getByTestId(`batch-task-today-${batchId}`).innerText(),
  draftStatus: await platformPage.getByTestId(`batch-task-draft-${batchId}`).innerText(),
  listEnums: visibleEnums(listText),
  listEnglishSeeds: visibleEnglishSeeds(listText)
}

const { context: operatorContext, page: operatorPage, username: operatorUsername } = await fieldEntryForAssignee(browser, detail)
operatorPage.on('console', (message) => {
  if (message.type() === 'error') {
    consoleErrors.push({ url: operatorPage.url(), text: message.text() })
  }
})
await operatorPage.screenshot({ path: screenshotPath('91-state-field-entry.png'), fullPage: true })
const fieldText = await bodyText(operatorPage)
const fieldResult = {
  operatorUsername,
  headerPills: await operatorPage.locator('.field-header-pills .pill').allInnerTexts(),
  fieldEnums: visibleEnums(fieldText),
  fieldEnglishSeeds: visibleEnglishSeeds(fieldText)
}

await publicPage.goto(publicUrl, { waitUntil: 'networkidle' })
await publicPage.getByTestId('public-trace-page').waitFor()
await publicPage.screenshot({ path: screenshotPath('92-state-public-trace.png'), fullPage: true })
const publicText = await bodyText(publicPage)
const publicStatus = await publicPage.getByTestId('public-status').innerText()
const publicQuality = await publicPage.getByTestId('public-quality').innerText()
const publicOrigin = await publicPage.getByTestId('public-origin').innerText()
const publishedAtText = await publicPage.locator('.summary-grid > div').filter({ hasText: '公开时间' }).locator('strong').innerText()
const latestRecordTitle = await publicPage.locator('.recent-card strong').innerText()
const riskBannerCount = await publicPage.getByTestId('public-risk-banner').count()
const publicResult = {
  publicStatus,
  publicQuality,
  publicOrigin,
  publishedAtText,
  latestRecordTitle,
  riskBannerVisible: riskBannerCount > 0,
  publicEnums: visibleEnums(publicText),
  publicEnglishSeeds: visibleEnglishSeeds(publicText),
  publishedConflict: (publicStatus === '草稿' && publishedAtText !== '尚未公开') || (publicStatus !== '草稿' && publishedAtText === '尚未公开')
}

const result = {
  checkedAt: new Date().toISOString(),
  platformUser: platformSession.user?.username || 'platform',
  batchId,
  batchAssignee: {
    assigneeUserId: detail.task?.assigneeUserId ?? null,
    assigneeName: detail.task?.assigneeName ?? ''
  },
  workbench: workbenchResult,
  batchList: listResult,
  fieldEntry: fieldResult,
  publicTrace: publicResult,
  consoleErrors,
  screenshots: screenshotFiles
}

await fs.writeFile(path.join(outputDir, 'state-consistency-round.json'), JSON.stringify(result, null, 2), 'utf8')

await operatorContext.close()
await platformApi.dispose()
await platformContext.close()
await publicContext.close()
await browser.close()
