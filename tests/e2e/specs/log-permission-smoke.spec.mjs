import { expect, test } from '@playwright/test'
import { loginByApi, seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, apiBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

const platformCredentials = { username: 'platform', password: '123456' }
const enterpriseCredentials = { username: 'enterprise_admin', password: '123456' }
const operatorCredentials = { username: 'operator', password: '123456' }

test.describe.configure({ mode: 'serial' })

function buildQuery(params = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== '' && value !== null && value !== undefined) {
      query.set(key, String(value))
    }
  }
  const text = query.toString()
  return text ? `?${text}` : ''
}

async function readJson(response) {
  const payload = await response.json()
  return {
    ok: response.ok(),
    status: response.status(),
    payload
  }
}

async function fetchLogs(request, token, params = {}) {
  const response = await request.get(`${apiBaseUrl}/logs${buildQuery(params)}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
  const result = await readJson(response)
  return {
    ...result,
    items: result.payload?.data?.items ?? [],
    total: Number(result.payload?.data?.total ?? 0)
  }
}

async function waitForDeniedLog(request, token, predicate) {
  await expect
    .poll(async () => {
      const response = await fetchLogs(request, token, {
        actionType: 'LOG_ACCESS_DENIED',
        result: 'FAILED',
        pageSize: 100
      })
      const matched = response.items.find(predicate)
      return matched ? JSON.stringify(matched) : ''
    }, {
      timeout: 10_000,
      intervals: [250, 500, 1000]
    })
    .not.toBe('')

  const response = await fetchLogs(request, token, {
    actionType: 'LOG_ACCESS_DENIED',
    result: 'FAILED',
    pageSize: 100
  })
  return response.items.find(predicate)
}

async function expectWarningToast(page, parts) {
  const toast = page.locator('.el-message__content').filter({ hasText: parts[0] }).last()
  await expect(toast).toBeVisible()
  for (const part of parts.slice(1)) {
    await expect(toast).toContainText(part)
  }
}

test('platform admin can open the logs page with full-scope filters', async ({ page, request }) => {
  await seedAdminSession(page, request, platformCredentials)

  await page.goto(`${adminBaseUrl}/logs`)
  await expect(page.getByTestId('logs-page')).toBeVisible()
  await expect(page.getByTestId('logs-filter-company')).toBeVisible()
  await expect(page.getByTestId('logs-search-button')).toBeVisible()

  const companyOptionCount = await page.getByTestId('logs-filter-company').locator('option').count()
  expect(companyOptionCount).toBeGreaterThan(2)

  const platformLogin = await loginByApi(request, platformCredentials)
  const response = await fetchLogs(request, platformLogin.token, { pageSize: 20 })
  expect(response.status).toBe(200)

  await saveNamedScreenshot(page, 'round9-logs-platform-page')
})

test('enterprise admin stays inside own-company scope and foreign company queries are denied', async ({ page, request }) => {
  const platformLogin = await loginByApi(request, platformCredentials)
  const enterpriseLogin = await seedAdminSession(page, request, enterpriseCredentials)

  await page.goto(`${adminBaseUrl}/logs`)
  await expect(page.getByTestId('logs-page')).toBeVisible()
  await expect(page.getByTestId('logs-self-mode')).toBeVisible()
  await expect(page.getByTestId('logs-company-fixed')).toBeVisible()
  await expect(page.getByTestId('logs-company-hint')).toContainText('本企业')

  const ownScope = await fetchLogs(request, enterpriseLogin.token, { pageSize: 100 })
  expect(ownScope.status).toBe(200)
  expect(ownScope.items.every((item) => item.companyId === 1)).toBeTruthy()

  const forbiddenResponse = await request.get(`${apiBaseUrl}/logs?companyId=2`, {
    headers: {
      Authorization: `Bearer ${enterpriseLogin.token}`
    }
  })
  const forbiddenPayload = await readJson(forbiddenResponse)
  expect(forbiddenPayload.status).toBe(403)
  expect(forbiddenPayload.payload.message).toBe('你只能查看本企业日志。')

  const deniedLog = await waitForDeniedLog(
    request,
    platformLogin.token,
    (item) => item.operatorUserId === enterpriseLogin.user.id && item.summary === '你只能查看本企业日志。'
  )

  expect(deniedLog.roleCode).toBe('ENTERPRISE_ADMIN')
  expect(deniedLog.companyId).toBe(1)
  expect(deniedLog.result).toBe('FAILED')

  const scopedLogs = await fetchLogs(request, enterpriseLogin.token, { pageSize: 100 })
  expect(scopedLogs.status).toBe(200)
  expect(scopedLogs.total).toBeGreaterThan(0)
  expect(scopedLogs.items.every((item) => item.companyId === 1)).toBeTruthy()

  await saveNamedScreenshot(page, 'round9-logs-enterprise-scope')
})

test('operator is blocked from /logs and the denied access record is visible to platform admin', async ({ page, request, browser }) => {
  const platformLogin = await loginByApi(request, platformCredentials)
  const operatorLogin = await seedAdminSession(page, request, operatorCredentials)

  await page.goto(`${adminBaseUrl}/logs`)
  await expect(page).toHaveURL(/\/field-entry(?:\?|$)/)
  await expect(page.getByTestId('field-entry-page')).toBeVisible()
  await expectWarningToast(page, ['不能访问', '操作日志'])
  await saveNamedScreenshot(page, 'round9-logs-operator-denied')

  const forbiddenResponse = await request.get(`${apiBaseUrl}/logs`, {
    headers: {
      Authorization: `Bearer ${operatorLogin.token}`
    }
  })
  const forbiddenPayload = await readJson(forbiddenResponse)
  expect(forbiddenPayload.status).toBe(403)
  expect(forbiddenPayload.payload.message).toBe('你没有查看操作日志的权限。')

  const deniedLog = await waitForDeniedLog(
    request,
    platformLogin.token,
    (item) => item.operatorUserId === operatorLogin.user.id && item.summary === '你没有查看操作日志的权限。'
  )

  expect(deniedLog.roleCode).toBe('OPERATOR')
  expect(deniedLog.result).toBe('FAILED')

  const platformPage = await browser.newPage()
  try {
    await seedAdminSession(platformPage, request, platformCredentials)
    await platformPage.goto(`${adminBaseUrl}/logs`)
    await expect(platformPage.getByTestId('logs-page')).toBeVisible()
    await platformPage.getByTestId('logs-filter-action').selectOption('LOG_ACCESS_DENIED')
    await platformPage.getByTestId('logs-filter-operator').fill(operatorLogin.user.realName)
    await platformPage.getByTestId('logs-search-button').click()

    const row = platformPage.getByTestId(`logs-row-${deniedLog.id}`)
    await expect(row).toBeVisible()
    await expect(row).toContainText('日志访问越权拒绝')
    await expect(row).toContainText('你没有查看操作日志的权限。')

    await platformPage.getByTestId(`logs-detail-${deniedLog.id}`).click()
    await expect(platformPage.getByTestId('logs-detail-dialog')).toContainText('你没有查看操作日志的权限。')
    await saveNamedScreenshot(platformPage, 'round9-logs-denied-visible')
  } finally {
    await platformPage.close()
  }
})
