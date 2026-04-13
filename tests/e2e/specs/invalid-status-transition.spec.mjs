import { expect, test } from '@playwright/test'
import { loginByApi, seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, apiBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

const platformCredentials = { username: 'platform', password: '123456' }

async function readJson(response) {
  const payload = await response.json()
  return {
    ok: response.ok(),
    status: response.status(),
    payload
  }
}

test('frozen batch without completed rectification cannot resume publish in UI or API', async ({ page, request }) => {
  await seedAdminSession(page, request, platformCredentials)

  await page.goto(`${adminBaseUrl}/batches/3`, { waitUntil: 'networkidle' })
  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  const riskPanel = page.getByTestId('workbench-risk-panel')
  const quickActions = page.getByTestId('workbench-simple-actions')
  await expect(riskPanel).toBeVisible()
  await expect(riskPanel).toContainText('未满足条件')
  await expect(quickActions.getByRole('button', { name: '发布批次' })).toBeDisabled()
  await saveNamedScreenshot(page, 'round12-invalid-status-frozen')

  const login = await loginByApi(request, platformCredentials)
  const response = await request.post(`${apiBaseUrl}/batches/3/status`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'Content-Type': 'application/json'
    },
    data: {
      targetStatus: 'PUBLISHED',
      reason: '验证冻结批次不能直接恢复发布',
      operatorName: '平台管理员'
    }
  })

  const result = await readJson(response)
  expect(result.status).toBe(400)
  expect([
    '请先补充处理意见并标记整改完成，再恢复发布',
    '检测结果不合格，不能发布'
  ]).toContain(result.payload.message)
})

test('recalled batch cannot be republished through status API', async ({ request }) => {
  const login = await loginByApi(request, platformCredentials)
  const response = await request.post(`${apiBaseUrl}/batches/4/status`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'Content-Type': 'application/json'
    },
    data: {
      targetStatus: 'PUBLISHED',
      reason: '验证召回批次不能重新发布',
      operatorName: '平台管理员'
    }
  })

  const result = await readJson(response)
  expect(result.status).toBe(400)
  expect(result.payload.message).toBe('已召回批次不可再变更状态')
})
