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
const operatorUsername = 'operator_d'
const operatorDisplayName = 'Field Operator D'
const defaultPassword = '123456'
const nextPassword = '12345678'
const foreignUsername = 'operator_security_tea'
const foreignDisplayName = 'Tea Security Operator'

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

async function login(page, username, password) {
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

async function fetchUserByKeyword(apiContext, keyword) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/users?keyword=${encodeURIComponent(keyword)}`))
  return (payload.body?.data ?? []).find((item) => item.username === keyword) ?? null
}

async function waitForUserByUsername(apiContext, username) {
  return waitFor(async () => fetchUserByKeyword(apiContext, username))
}

async function openBatchAssignment(page) {
  await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-list-page').waitFor()
  await page.getByTestId('batch-filter-code').fill(batchCode)
  await page.getByTestId('batch-search-button').click()
  await page.getByTestId(`batch-card-${batchId}`).waitFor()
  await page.getByTestId(`batch-assignment-open-${batchId}`).click()
  await page.locator('[data-testid="batch-assignment-dialog"]:visible').waitFor()
  await page.getByTestId('batch-list-assignment-select').waitFor()
}

async function waitForAssignmentOptions(page, minCount = 2) {
  return waitFor(async () => {
    const select = page.getByTestId('batch-list-assignment-select')
    const count = await select.locator('option').count()
    const disabled = await select.isDisabled()
    return !disabled && count >= minCount ? count : null
  })
}

async function assignmentOptions(page) {
  return page.getByTestId('batch-list-assignment-select').locator('option').evaluateAll((nodes) =>
    nodes.map((node) => ({
      value: node.getAttribute('value') || '',
      text: (node.textContent || '').trim()
    }))
  )
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

async function batchDetail(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/${batchId}`))
  if (payload.status !== 200 || !payload.body?.data) {
    throw new Error(`failed to load batch ${batchId}`)
  }
  return payload.body.data
}

async function mineBatchCodes(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches?mineOnly=true`))
  return (payload.body?.data ?? []).map((item) => item.batchCode)
}

const browser = await chromium.launch({ headless: true })
const platformContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const operatorFirstContext = await browser.newContext({ viewport: { width: 430, height: 932 } })
const operatorSecondContext = await browser.newContext({ viewport: { width: 430, height: 932 } })
const operatorThirdContext = await browser.newContext({ viewport: { width: 430, height: 932 } })
const enterpriseContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })

const pages = [
  await platformContext.newPage(),
  await operatorFirstContext.newPage(),
  await operatorSecondContext.newPage(),
  await operatorThirdContext.newPage(),
  await enterpriseContext.newPage()
]

for (const page of pages) {
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push({ url: page.url(), text: message.text() })
    }
  })
  page.on('pageerror', (error) => {
    pageErrors.push({ url: page.url(), text: String(error) })
  })
  page.on('response', (response) => {
    if (response.status() >= 400 && !response.url().includes('/reset-password')) {
      httpErrors.push({
        status: response.status(),
        url: response.url(),
        pageUrl: page.url()
      })
    }
  })
}

const [platformPage, operatorFirstPage, operatorSecondPage, operatorThirdPage, enterprisePage] = pages

const platformSession = await login(platformPage, 'platform', defaultPassword)
const platformApi = await apiContextFor(platformSession.token)

await platformPage.goto(`${adminBaseUrl}/dashboard`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('admin-layout').waitFor()
await platformPage.getByTestId('admin-change-password').click()
await platformPage.getByTestId('change-password-dialog').waitFor()
await platformPage.screenshot({ path: screenshotPath('146-account-self-change-entry.png'), fullPage: true })
await platformPage.getByTestId('change-password-cancel').click()

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
await platformPage.screenshot({ path: screenshotPath('147-account-create-operator-d.png'), fullPage: true })
await platformPage.getByTestId('user-form-submit').click()

const createdUser = await waitForUserByUsername(platformApi, operatorUsername)

const companiesPayload = await readJson(platformApi.get(`${apiBaseUrl}/batches/lookup/companies`))
const foreignCompany = (companiesPayload.body?.data ?? []).find((item) => Number(item.id) !== 1)
let foreignUser = null
if (foreignCompany) {
  const foreignCreate = await readJson(platformApi.post(`${apiBaseUrl}/users`, {
    data: {
      username: foreignUsername,
      password: defaultPassword,
      realName: foreignDisplayName,
      roleCode: 'OPERATOR',
      companyId: Number(foreignCompany.id)
    }
  }))
  foreignUser = foreignCreate.body?.data ?? null
}

await openBatchAssignment(platformPage)
await waitForAssignmentOptions(platformPage, 3)
const assignmentOptionsBefore = await assignmentOptions(platformPage)
const assignmentOption = assignmentOptionsBefore.find((item) => item.value === String(createdUser.id)) ?? null
const assignmentOptionVisible = Boolean(assignmentOption)
await platformPage.screenshot({ path: screenshotPath('148-account-assignment-option.png'), fullPage: true })
await platformPage.getByTestId('batch-list-assignment-select').selectOption(String(createdUser.id))
await platformPage.getByTestId('batch-list-assignment-submit').click()
await waitForDialogHidden(platformPage, 'batch-assignment-dialog')

const assignedBatch = await waitFor(async () => {
  const detail = await batchDetail(platformApi)
  return Number(detail.task?.assigneeUserId) === Number(createdUser.id) ? detail : null
})

const firstLoginSession = await login(operatorFirstPage, operatorUsername, defaultPassword)
await operatorFirstPage.getByTestId('change-password-dialog').waitFor()
await operatorFirstPage.screenshot({ path: screenshotPath('149-account-first-login-force-change.png'), fullPage: true })
await operatorFirstPage.getByTestId('change-password-current').fill(defaultPassword)
await operatorFirstPage.getByTestId('change-password-new').fill(nextPassword)
await operatorFirstPage.getByTestId('change-password-confirm').fill(nextPassword)
await operatorFirstPage.getByTestId('change-password-submit').click()
await waitFor(async () => (await operatorFirstPage.getByTestId('change-password-dialog').count()) === 0)
const firstLoginState = await operatorFirstPage.evaluate(() => JSON.parse(localStorage.getItem('admin_user') || '{}'))
const operatorFirstApi = await apiContextFor(firstLoginSession.token)
const operatorMineAfterChange = await mineBatchCodes(operatorFirstApi)
await operatorFirstApi.dispose()

const secondLoginSession = await login(operatorSecondPage, operatorUsername, nextPassword)
await waitFor(async () => {
  const count = await operatorSecondPage.getByTestId('change-password-dialog').count()
  return count === 0 ? true : null
}, 5000, 250)
const operatorSecondApi = await apiContextFor(secondLoginSession.token)
const operatorMineAfterRelogin = await mineBatchCodes(operatorSecondApi)
await operatorSecondPage.screenshot({ path: screenshotPath('150-account-relogin-new-password.png'), fullPage: true })

await platformPage.goto(`${adminBaseUrl}/users`, { waitUntil: 'networkidle' })
await platformPage.getByTestId('users-filter-keyword').fill(operatorUsername)
await platformPage.getByTestId('users-search-button').click()
await platformPage.getByTestId(`users-row-${createdUser.id}`).waitFor()
await platformPage.getByTestId(`user-reset-password-${createdUser.id}`).click()
await platformPage.getByTestId('user-reset-password-dialog').waitFor()
await platformPage.getByTestId('user-reset-password-value').fill(defaultPassword)
await platformPage.screenshot({ path: screenshotPath('151-account-admin-reset-password.png'), fullPage: true })
await platformPage.getByTestId('user-reset-password-submit').click()
await waitForDialogHidden(platformPage, 'user-reset-password-dialog')
const resetUser = await waitForUserByUsername(platformApi, operatorUsername)

const thirdLoginSession = await login(operatorThirdPage, operatorUsername, defaultPassword)
await operatorThirdPage.getByTestId('change-password-dialog').waitFor()
const thirdLoginState = await operatorThirdPage.evaluate(() => JSON.parse(localStorage.getItem('admin_user') || '{}'))
await operatorThirdPage.screenshot({ path: screenshotPath('152-account-force-change-after-reset.png'), fullPage: true })

const enterpriseSession = await login(enterprisePage, 'enterprise_admin', defaultPassword)
const enterpriseApi = await apiContextFor(enterpriseSession.token)
await enterprisePage.goto(`${adminBaseUrl}/users`, { waitUntil: 'networkidle' })
await enterprisePage.getByTestId('users-page').waitFor()
await enterprisePage.getByTestId('users-filter-keyword').fill(operatorUsername)
await enterprisePage.getByTestId('users-search-button').click()
await enterprisePage.getByTestId(`users-row-${createdUser.id}`).waitFor()
const ownCompanyResetVisible = await enterprisePage.getByTestId(`user-reset-password-${createdUser.id}`).isVisible()
if (foreignUser) {
  await enterprisePage.getByTestId('users-filter-keyword').fill(foreignUsername)
  await enterprisePage.getByTestId('users-search-button').click()
}
const seesForeignUser = foreignUser
  ? (await enterprisePage.getByTestId(`users-row-${foreignUser.id}`).count()) > 0
  : false
await enterprisePage.screenshot({ path: screenshotPath('153-account-enterprise-scope.png'), fullPage: true })

let enterpriseForeignReset = null
if (foreignUser) {
  enterpriseForeignReset = await readJson(enterpriseApi.post(`${apiBaseUrl}/users/${foreignUser.id}/reset-password`, {
    data: {
      newPassword: defaultPassword
    }
  }))
}

const result = {
  createdUser: {
    id: createdUser.id,
    username: createdUser.username,
    realName: createdUser.realName,
    needChangePassword: createdUser.needChangePassword
  },
  assignment: {
    optionVisible: assignmentOptionVisible,
    optionText: assignmentOption?.text || '',
    assigneeUserId: assignedBatch.task?.assigneeUserId ?? null,
    assigneeName: assignedBatch.task?.assigneeName || '',
    operatorMineAfterChange,
    operatorMineAfterRelogin
  },
  firstLogin: {
    forcedDialog: true,
    needChangePasswordBeforeChange: Boolean(firstLoginSession.user?.needChangePassword),
    needChangePasswordAfterChange: Boolean(firstLoginState.needChangePassword),
    reloginWithNewPasswordSucceeded: Boolean(secondLoginSession.token)
  },
  adminReset: {
    passwordResetTriggered: true,
    needChangePasswordAfterReset: Boolean(resetUser?.needChangePassword),
    reForcedOnNextLogin: Boolean(thirdLoginState.needChangePassword)
  },
  enterpriseBoundary: {
    ownCompanyResetVisible,
    seesForeignUser,
    foreignResetStatus: enterpriseForeignReset?.status ?? null,
    foreignResetMessage: enterpriseForeignReset?.body?.message ?? ''
  },
  selfChangeEntryVisible: true,
  screenshots: screenshotFiles,
  consoleErrors,
  pageErrors,
  httpErrors
}

await fs.writeFile(
  path.join(outputDir, 'account-security-round.json'),
  JSON.stringify(result, null, 2),
  'utf8'
)

await platformApi.dispose()
await operatorSecondApi.dispose()
await enterpriseApi.dispose()
await browser.close()

console.log(JSON.stringify(result, null, 2))
