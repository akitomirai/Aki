import { expect, test } from '@playwright/test'
import { createDraftBatch } from '../helpers/demo-api.mjs'
import { adminBaseUrl, apiBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

test('admin batch list and workbench support the main smoke path', async ({ page, request }) => {
  const { batchCode, batchId } = await createDraftBatch(request)

  await page.goto(`${adminBaseUrl}/batches`)
  await expect(page.getByTestId('batch-list-page')).toBeVisible()

  await page.getByTestId('batch-filter-code').fill(batchCode)
  await page.getByTestId('batch-search-button').click()

  await expect(page.getByTestId(`batch-card-${batchId}`)).toBeVisible()
  await expect(page.getByTestId(`batch-next-${batchId}`)).toBeVisible()
  await saveNamedScreenshot(page, 'round8-admin-batch-list')

  await page.getByTestId(`batch-open-workbench-${batchId}`).click()
  await expect(page).toHaveURL(new RegExp(`/batches/${batchId}$`))
  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  await expect(page.getByTestId('workbench-next-step-card')).toBeVisible()
  await expect(page.getByTestId('workbench-action-groups')).toBeVisible()

  await page.getByTestId('workbench-qr-action-0').click()
  await expect(page.getByTestId('workbench-qr-status')).toContainText('Generated')
  await expect(page.getByTestId('workbench-public-preview')).toBeVisible()
  await saveNamedScreenshot(page, 'round8-admin-workbench-after-qr')

  const workbenchResponse = await request.get(`${apiBaseUrl}/batches/${batchId}`)
  expect(workbenchResponse.ok()).toBeTruthy()
  const workbenchPayload = await workbenchResponse.json()
  expect(workbenchPayload.data.qr.generated).toBe(true)
})
