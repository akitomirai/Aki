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
const operatorUsername = 'operator_c'
const operatorDisplayName = 'Field Operator C'
const foreignUsername = 'operator_tea'
const foreignDisplayName = 'Tea Operator'
const defaultPassword = '123456'
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

async function directLogin(username, password = defaultPassword) {
  const anonymousApi = await request.newContext()
  const payload = await readJson(anonymousApi.post(`${apiBaseUrl}/auth/login`, {
    data: { username, password }
  }))
  await anonymousApi.dispose()
  return payload
}

async function gotoUsers(page) {
  await page.goto(`${adminBaseUrl}/users`, { waitUntil: 'networkidle' })
  await page.getByTestId('users-page').waitFor()
}

async function gotoBatchList(page) {
  await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'networkidle' })
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

async function waitForAssignmentOptions(page, minCount = 2) {
  return waitFor(async () => {
    const count = await page.getByTestId('batch-list-assignment-select').locator('option').count()
    return count >= minCount ? count : null
  })
}

async function assignFromBatchList(page, assigneeUserId) {
  await openAssignmentDialog(page)
  await page.getByTestId('batch-list-assignment-select').selectOption(String(assigneeUserId))
  await page.getByTestId('batch-list-assignment-submit').click()
  await waitForAssignmentDialogHidden(page)
}

async function fetchUserByKeyword(apiContext, keyword) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/users?keyword=${encodeURIComponent(keyword)}`))
  return (payload.body?.data ?? []).find((item) => item.username === keyword) ?? null
}

async function waitForUserByUsername(apiContext, username) {
  return waitFor(async () => fetchUserByKeyword(apiContext, username))
}

async function waitForUserStatus(apiContext, username, status) {
  return waitFor(async () => {
    const user = await fetchUserByKeyword(apiContext, username)
    return Number(user?.status) === Number(status) ? user : null
  })
}

async function mineBatchCodes(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches?mineOnly=true`))
  return (payload.body?.data ?? []).map((item) => item.batchCode)
}

async function waitForMinePresence(apiContext, expectedPresent) {
  return waitFor(async () => {
    const codes = await mineBatchCodes(apiContext)
    const present = codes.includes(batchCode)
    return present === expectedPresent ? codes : null
  })
}

async function batchDetail(apiContext) {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/${batchId}`))
  if (payload.status !== 200 || !payload.body?.data) {
    throw new Error(`failed to load batch ${batchId}: ${payload.status}`)
  }
  return payload.body.data
}

async function bodyText(page) {
  return page.locator('body').innerText()
}

async function openWorkbench(page) {
  await page.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-workbench-page').waitFor()
}

const browser = await chromium.launch({ headless: true })
const platformContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const enterpriseContext = await browser.newContext({ viewport: { width: 1440, height: 960 } })
const loginContext = await browser.newContext({ viewport: { width: 1280, height: 900 } })

const platformPage = await platformContext.newPage()
const enterprisePage = await enterpriseContext.newPage()
const disabledLoginPage = await loginContext.newPage()

for (const page of [platformPage, enterprisePage, disabledLoginPage]) {
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
}

const platformSession = await login(platformPage, 'platform')
const enterpriseSession = await login(enterprisePage, 'enterprise_admin')
const platformApi = await apiContextFor(platformSession.token)
const enterpriseApi = await apiContextFor(enterpriseSession.token)

const companiesPayload = await readJson(platformApi.get(`${apiBaseUrl}/batches/lookup/companies`))
const companyOptions = companiesPayload.body?.data ?? []
const batch = await batchDetail(platformApi)
const batchCompanyId = Number(batch.company?.id || batch.product?.companyId || 0)
const foreignCompany = companyOptions.find((item) => Number(item.id) !== batchCompanyId) ?? null

await gotoUsers(platformPage)
await platformPage.screenshot({ path: screenshotPath('136-users-page-initial.png'), fullPage: true })

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
await platformPage.getByTestId('user-form-company').selectOption(String(batchCompanyId))
await platformPage.screenshot({ path: screenshotPath('137-users-create-dialog.png'), fullPage: true })
await platformPage.getByTestId('user-form-submit').click()

const createdUser = await waitForUserByUsername(platformApi, operatorUsername)
await gotoUsers(platformPage)
await platformPage.getByTestId('users-filter-keyword').fill(operatorUsername)
await platformPage.getByTestId('users-search-button').click()
await platformPage.getByTestId(`users-row-${createdUser.id}`).waitFor()
await platformPage.screenshot({ path: screenshotPath('138-users-created-operator.png'), fullPage: true })

let foreignUser = null
if (foreignCompany) {
  const foreignCreateResponse = await readJson(platformApi.post(`${apiBaseUrl}/users`, {
    data: {
      username: foreignUsername,
      password: defaultPassword,
      realName: foreignDisplayName,
      roleCode: 'OPERATOR',
      companyId: Number(foreignCompany.id)
    }
  }))
  foreignUser = foreignCreateResponse.body?.data ?? null
}

await openAssignmentDialog(platformPage)
await waitForAssignmentOptions(platformPage, 4)
const assignmentOptionsBeforeDisable = await platformPage.getByTestId('batch-list-assignment-select').locator('option').evaluateAll((nodes) =>
  nodes.map((node) => ({
    value: node.getAttribute('value') || '',
    text: node.textContent || ''
  }))
)
const optionContainsNewOperator = assignmentOptionsBeforeDisable.some((item) =>
  Number(item.value || 0) === Number(createdUser.id) || item.text.includes(operatorDisplayName) || item.text.includes(operatorUsername)
)
await platformPage.screenshot({ path: screenshotPath('139-users-assignment-option-visible.png'), fullPage: true })
await platformPage.getByTestId('batch-list-assignment-select').selectOption(String(createdUser.id))
const selectedAssignmentLabelBeforeSubmit = await platformPage.getByTestId('batch-list-assignment-select').locator('option:checked').innerText()
await platformPage.getByTestId('batch-list-assignment-submit').click()
await waitForAssignmentDialogHidden(platformPage)

const operatorLoginPayload = await directLogin(operatorUsername)
if (operatorLoginPayload.status !== 200 || !operatorLoginPayload.body?.data?.token) {
  throw new Error(`failed to login newly created operator: ${JSON.stringify(operatorLoginPayload.body)}`)
}
const operatorCApi = await apiContextFor(operatorLoginPayload.body.data.token)
const batchCodesForNewOperator = await waitForMinePresence(operatorCApi, true)

await gotoBatchList(platformPage)
const batchListAssigneeAfterAssign = await platformPage.getByTestId(`batch-task-assignee-${batchId}`).innerText()
await platformPage.screenshot({ path: screenshotPath('140-users-batch-assigned-operator-c.png'), fullPage: true })

await gotoUsers(platformPage)
await platformPage.getByTestId('users-filter-keyword').fill(operatorUsername)
await platformPage.getByTestId('users-search-button').click()
await platformPage.getByTestId(`users-row-${createdUser.id}`).waitFor()
platformPage.once('dialog', (dialog) => dialog.accept())
await platformPage.getByTestId(`user-toggle-${createdUser.id}`).click()
const disabledUser = await waitForUserStatus(platformApi, operatorUsername, 0)
await gotoUsers(platformPage)
await platformPage.getByTestId('users-filter-keyword').fill(operatorUsername)
await platformPage.getByTestId('users-search-button').click()
await platformPage.getByTestId(`users-row-${createdUser.id}`).waitFor()
const disabledRowText = await platformPage.getByTestId(`users-row-${createdUser.id}`).innerText()
await platformPage.screenshot({ path: screenshotPath('141-users-operator-disabled.png'), fullPage: true })

await gotoBatchList(platformPage)
const batchListAssigneeAfterDisable = await platformPage.getByTestId(`batch-task-assignee-${batchId}`).innerText()
await openAssignmentDialog(platformPage)
await waitForAssignmentOptions(platformPage, 3)
const assignmentOptionsAfterDisable = await platformPage.getByTestId('batch-list-assignment-select').locator('option').evaluateAll((nodes) =>
  nodes.map((node) => ({
    value: node.getAttribute('value') || '',
    text: node.textContent || ''
  }))
)
const optionStillVisibleAfterDisable = assignmentOptionsAfterDisable.some((item) => item.text.includes(operatorDisplayName) || item.text.includes(operatorUsername))
await platformPage.screenshot({ path: screenshotPath('142-users-assignment-option-hidden-after-disable.png'), fullPage: true })
await platformPage.getByRole('button', { name: '取消' }).click()
await waitForAssignmentDialogHidden(platformPage)

await openWorkbench(platformPage)
const workbenchText = await bodyText(platformPage)
const workbenchAssigneeMarkedDisabled = workbenchText.includes(`${operatorDisplayName}（已停用）`) || workbenchText.includes('已停用')
await platformPage.screenshot({ path: screenshotPath('143-users-workbench-disabled-assignee.png'), fullPage: true })

await disabledLoginPage.goto(`${adminBaseUrl}/login`, { waitUntil: 'networkidle' })
await disabledLoginPage.getByTestId('login-page').waitFor()
await disabledLoginPage.locator('input').nth(0).fill(operatorUsername)
await disabledLoginPage.locator('input').nth(1).fill(defaultPassword)
const disabledLoginResponsePromise = readJson(
  disabledLoginPage.waitForResponse((response) => response.url().includes('/api/auth/login') && response.request().method() === 'POST')
)
await disabledLoginPage.getByTestId('login-submit').click()
const disabledLoginResponse = await disabledLoginResponsePromise
await waitFor(async () => disabledLoginPage.url().includes('/login') ? true : null)
const disabledLoginText = await bodyText(disabledLoginPage)
await disabledLoginPage.screenshot({ path: screenshotPath('144-users-disabled-login-rejected.png'), fullPage: true })

await gotoUsers(enterprisePage)
const enterpriseUsersText = await bodyText(enterprisePage)
const enterpriseSeesNewOperator = enterpriseUsersText.includes(operatorUsername) || enterpriseUsersText.includes(operatorDisplayName)
const enterpriseSeesForeignOperator = foreignUser
  ? enterpriseUsersText.includes(foreignUser.username) || enterpriseUsersText.includes(foreignUser.realName)
  : false
await enterprisePage.screenshot({ path: screenshotPath('145-users-enterprise-scope.png'), fullPage: true })

let enterpriseForeignUpdate = null
if (foreignUser) {
  enterpriseForeignUpdate = await readJson(enterpriseApi.patch(`${apiBaseUrl}/users/${foreignUser.id}`, {
    data: {
      realName: `${foreignDisplayName} Updated`,
      roleCode: 'OPERATOR',
      companyId: Number(foreignCompany.id)
    }
  }))
}

const result = {
  verifiedAt: new Date().toISOString(),
  runtime: {
    backend: 'http://127.0.0.1:8080',
    adminWeb: adminBaseUrl
  },
  users: {
    platform: platformSession.user,
    enterpriseAdmin: enterpriseSession.user,
    createdOperator: createdUser,
    disabledOperator: disabledUser,
    foreignOperator: foreignUser
  },
  creation: {
    companyId: batchCompanyId,
    optionContainsNewOperator,
    assignmentOptionsBeforeDisable,
    selectedAssignmentLabelBeforeSubmit
  },
  assignment: {
    batchCodesForNewOperator,
    batchListAssigneeAfterAssign
  },
  disable: {
    disabledRowText,
    batchListAssigneeAfterDisable,
    optionStillVisibleAfterDisable,
    assignmentOptionsAfterDisable,
    workbenchAssigneeMarkedDisabled
  },
  disabledLogin: {
    status: disabledLoginResponse.status,
    message: disabledLoginResponse.body?.message || '',
    stayedOnLogin: disabledLoginPage.url().includes('/login'),
    pageContainsError: disabledLoginText.includes('用户名或密码错误')
  },
  enterpriseBoundary: {
    seesNewOperator: enterpriseSeesNewOperator,
    seesForeignOperator: enterpriseSeesForeignOperator,
    foreignUpdateDenied: enterpriseForeignUpdate
  },
  uiErrors: {
    consoleErrors,
    pageErrors,
    httpErrors
  },
  screenshots: screenshotFiles
}

await fs.writeFile(path.join(outputDir, 'user-manage-round.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
console.log(JSON.stringify(result, null, 2))

await platformApi.dispose()
await enterpriseApi.dispose()
await operatorCApi.dispose()
await platformContext.close()
await enterpriseContext.close()
await loginContext.close()
await browser.close()
