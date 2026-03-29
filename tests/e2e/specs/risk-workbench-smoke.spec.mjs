import { expect, test } from '@playwright/test'
import { adminBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

test('risk workbench shows current status, recent actions and checklist', async ({ page }) => {
  await page.goto(`${adminBaseUrl}/batches/4`)

  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  await expect(page.getByTestId('workbench-risk-panel')).toBeVisible()
  await expect(page.getByTestId('workbench-risk-checklist')).toBeVisible()
  await expect(page.getByTestId('workbench-next-step-card')).toBeVisible()
  await expect(page.getByTestId('workbench-group-status')).toBeVisible()
  await saveNamedScreenshot(page, 'round8-risk-workbench')
})
