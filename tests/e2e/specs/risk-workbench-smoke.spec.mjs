import { expect, test } from '@playwright/test'
import { seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

test('risk workbench shows current status, recent actions and checklist', async ({ page, request }) => {
  await seedAdminSession(page, request)
  await page.goto(`${adminBaseUrl}/batches/3`)

  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  await expect(page.getByTestId('workbench-risk-panel')).toBeVisible()
  await expect(page.getByTestId('workbench-risk-panel')).toContainText('当前风险状态')
  await expect(page.getByTestId('workbench-risk-panel')).toContainText('最近动作')
  await expect(page.getByTestId('workbench-risk-panel')).toContainText('整改结果')
  await expect(page.getByTestId('workbench-trace-chain-panel')).toBeVisible()
  await saveNamedScreenshot(page, 'round8-risk-workbench')
})
