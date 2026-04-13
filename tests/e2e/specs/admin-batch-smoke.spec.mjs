import { expect, test } from '@playwright/test'
import { createDraftBatch, seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, apiBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

test('admin batch list and workbench support the main smoke path', async ({ page, request }) => {
  const { batchCode, batchId } = await createDraftBatch(request)
  const login = await seedAdminSession(page, request)

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
  await expect(page.getByTestId('workbench-simple-actions')).toBeVisible()
  await expect(page.getByTestId('workbench-trace-chain-panel')).toBeVisible()
  await expect(page.getByTestId('workbench-trace-chain-panel')).toContainText('可信溯源校验')

  const quickActions = page.getByTestId('workbench-simple-actions')
  await quickActions.getByRole('button', { name: '生成二维码' }).click()
  await expect(quickActions.getByRole('button', { name: '二维码已生成' })).toBeVisible()
  await expect(quickActions.getByRole('button', { name: '查看公开页' })).toBeVisible()
  await saveNamedScreenshot(page, 'round8-admin-workbench-after-qr')

  const workbenchResponse = await request.get(`${apiBaseUrl}/batches/${batchId}`, {
    headers: {
      Authorization: `Bearer ${login.token}`
    }
  })
  expect(workbenchResponse.ok()).toBeTruthy()
  const workbenchPayload = await workbenchResponse.json()
  expect(workbenchPayload.data.qr.generated).toBe(true)
})
