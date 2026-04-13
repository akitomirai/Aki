import { expect, test } from '@playwright/test'
import { loginByApi, seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, apiBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

const platformCredentials = { username: 'platform', password: '123456' }
const regulatorCredentials = { username: 'regulator', password: '123456' }

test.describe.configure({ mode: 'serial' })

async function readJson(response) {
  const payload = await response.json()
  return {
    ok: response.ok(),
    status: response.status(),
    payload
  }
}

async function fetchLogs(request, token, actionType) {
  const response = await request.get(`${apiBaseUrl}/logs?actionType=${encodeURIComponent(actionType)}&result=FAILED&pageSize=100`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
  const result = await readJson(response)
  return result.payload?.data?.items ?? []
}

async function waitForDeniedLog(request, token, actionType, predicate) {
  await expect
    .poll(async () => {
      const items = await fetchLogs(request, token, actionType)
      const matched = items.find(predicate)
      return matched ? JSON.stringify(matched) : ''
    }, {
      timeout: 10_000,
      intervals: [250, 500, 1000]
    })
    .not.toBe('')

  const items = await fetchLogs(request, token, actionType)
  return items.find(predicate)
}

test('regulator lands on /risk and only sees read-only regulator menu entries', async ({ page, request }) => {
  await seedAdminSession(page, request, regulatorCredentials)

  await page.goto(adminBaseUrl)
  await expect(page).toHaveURL(/\/risk(?:\?|$)/)
  await expect(page.getByTestId('risk-page')).toBeVisible()
  await expect(page.getByTestId('risk-readonly-banner')).toBeVisible()

  const sidebar = page.locator('.sidebar')
  await expect(sidebar).toContainText('风险处理')
  await expect(sidebar).toContainText('批次管理')
  await expect(sidebar).not.toContainText('用户管理')
  await expect(sidebar).not.toContainText('企业管理')
  await expect(sidebar).not.toContainText('产品管理')
  await expect(sidebar).not.toContainText('二维码与发布')

  await saveNamedScreenshot(page, 'round11-regulator-risk-default')
})

test('regulator can read /batches, /batches/2, /quality and /risk without seeing write actions', async ({ page, request }) => {
  await seedAdminSession(page, request, regulatorCredentials)

  await page.goto(`${adminBaseUrl}/batches`)
  await expect(page.getByTestId('batch-list-page')).toBeVisible()
  await expect(page.getByTestId('batch-regulator-banner')).toBeVisible()
  await expect(page.getByTestId('batch-list-page')).toContainText('监管查看模式')
  await expect(page.getByTestId('batch-list-page')).toContainText('质检')
  await expect(page.getByTestId('batch-list-page')).toContainText('风险')
  const firstBatchCard = page.locator('[data-testid^="batch-card-"]').first()
  const firstNextLabel = page.locator('[data-testid^="batch-next-"]').first()
  await expect(firstBatchCard).toBeVisible()
  await expect(firstBatchCard).toContainText('最近更新')
  await expect(firstBatchCard).toContainText(/已发布|已冻结|已召回/)
  await expect(firstNextLabel).toBeVisible()
  await expect(page.getByTestId('batch-create-button')).toHaveCount(0)
  await expect(page.locator('[data-testid^="batch-assignment-open-"]')).toHaveCount(0)
  await expect(page.locator('[data-testid^="batch-copy-"]')).toHaveCount(0)
  await expect(page.locator('[data-testid^="batch-recommend-"]')).toHaveCount(0)

  await page.goto(`${adminBaseUrl}/batches/2`)
  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  await expect(page.getByTestId('workbench-readonly-banner')).toBeVisible()
  const simpleAssignmentPanel = page.locator('.simple-main-grid [data-testid="workbench-assignment-panel"]').first()
  await expect(page.getByText('质检与二维码')).toBeVisible()
  await expect(page.getByTestId('workbench-risk-panel')).toBeVisible()
  await expect(simpleAssignmentPanel).toContainText('任务分配')
  await expect(page.locator('.simple-main-grid')).toContainText('报告编号')
  await expect(page.getByTestId('workbench-simple-records').getByTestId('workbench-latest-record')).toBeVisible()
  await expect(page.getByTestId('workbench-copy-batch-button')).toHaveCount(0)
  await expect(page.getByTestId('workbench-field-entry-button')).toHaveCount(0)
  await expect(page.getByTestId('workbench-action-groups')).toHaveCount(0)
  await expect(page.getByTestId('workbench-qr-action-0')).toHaveCount(0)
  await expect(page.getByTestId('workbench-group-status')).toHaveCount(0)

  await page.goto(`${adminBaseUrl}/quality`)
  await expect(page.getByTestId('quality-page')).toBeVisible()
  await expect(page.getByTestId('quality-readonly-banner')).toBeVisible()
  await expect(page.locator('[data-testid^="quality-upload-"]')).toHaveCount(0)
  await page.getByTestId('quality-tab-PASS').click()
  const reportButton = page.locator('[data-testid^="quality-open-report-"]:not([disabled])').first()
  await expect(reportButton).toBeVisible()
  await reportButton.click()
  await expect(page.getByTestId('quality-result-dialog')).toBeVisible()
  await expect(page.getByTestId('quality-report-no')).not.toBeEmpty()
  await page.getByRole('button', { name: '关闭' }).last().click()

  await page.goto(`${adminBaseUrl}/risk`)
  await expect(page.getByTestId('risk-page')).toBeVisible()
  await expect(page.getByTestId('risk-readonly-banner')).toBeVisible()
  await expect(page.locator('[data-testid^="risk-comment-"]')).toHaveCount(0)
  await expect(page.locator('[data-testid^="risk-rectification-"]')).toHaveCount(0)
  await expect(page.locator('[data-testid^="risk-processing-"]')).toHaveCount(0)
  await expect(page.locator('[data-testid^="risk-rectified-"]')).toHaveCount(0)
  await expect(page.locator('[data-testid^="risk-resume-"]')).toHaveCount(0)
  await page.getByTestId('risk-tab-PROCESSING').click()
  const firstRiskRow = page.locator('[data-testid^="risk-row-"]').first()
  await expect(firstRiskRow).toBeVisible()
  await expect(firstRiskRow).toContainText('风险')
  await expect(firstRiskRow).toContainText('整改结果')

  await saveNamedScreenshot(page, 'round11-regulator-readonly-pages')
})

test('regulator write APIs stay forbidden and denied operations are logged', async ({ page, request }) => {
  const platformLogin = await loginByApi(request, platformCredentials)
  const regulatorLogin = await seedAdminSession(page, request, regulatorCredentials)

  const qrDeniedResponse = await request.post(`${apiBaseUrl}/batches/2/qr`, {
    headers: {
      Authorization: `Bearer ${regulatorLogin.token}`
    }
  })
  const qrDeniedPayload = await readJson(qrDeniedResponse)
  expect(qrDeniedPayload.status).toBe(403)
  expect(qrDeniedPayload.payload.message).toBe('当前账号不能执行“生成二维码”操作。')

  const publishDeniedResponse = await request.post(`${apiBaseUrl}/batches/2/status`, {
    headers: {
      Authorization: `Bearer ${regulatorLogin.token}`,
      'Content-Type': 'application/json'
    },
    data: {
      targetStatus: 'PUBLISHED',
      reason: '监管账号不应能发布批次',
      operatorName: 'Regulator'
    }
  })
  const publishDeniedPayload = await readJson(publishDeniedResponse)
  expect(publishDeniedPayload.status).toBe(403)
  expect(publishDeniedPayload.payload.message).toBe('当前账号不能执行“发布批次”操作。')

  const riskDeniedResponse = await request.post(`${apiBaseUrl}/batches/2/risk-actions`, {
    headers: {
      Authorization: `Bearer ${regulatorLogin.token}`,
      'Content-Type': 'application/json'
    },
    data: {
      actionType: 'COMMENT',
      reason: '监管账号不应能处理风险动作',
      comment: '只读验证',
      operatorName: 'Regulator'
    }
  })
  const riskDeniedPayload = await readJson(riskDeniedResponse)
  expect(riskDeniedPayload.status).toBe(403)
  expect(riskDeniedPayload.payload.message).toBe('当前账号不能处理风险动作。')

  const qrDeniedLog = await waitForDeniedLog(
    request,
    platformLogin.token,
    'QR_PUBLISH_DENIED',
    (item) => item.operatorUserId === regulatorLogin.user.id && item.summary === '当前账号不能执行“生成二维码”操作。'
  )
  expect(qrDeniedLog.roleCode).toBe('REGULATOR')
  expect(qrDeniedLog.result).toBe('FAILED')

  const publishDeniedLog = await waitForDeniedLog(
    request,
    platformLogin.token,
    'QR_PUBLISH_DENIED',
    (item) => item.operatorUserId === regulatorLogin.user.id && item.summary === '当前账号不能执行“发布批次”操作。'
  )
  expect(publishDeniedLog.roleCode).toBe('REGULATOR')
  expect(publishDeniedLog.result).toBe('FAILED')

  const riskDeniedLog = await waitForDeniedLog(
    request,
    platformLogin.token,
    'RISK_ACTION_DENIED',
    (item) => item.operatorUserId === regulatorLogin.user.id && item.summary === '当前账号不能处理风险动作。'
  )
  expect(riskDeniedLog.roleCode).toBe('REGULATOR')
  expect(riskDeniedLog.result).toBe('FAILED')
})
