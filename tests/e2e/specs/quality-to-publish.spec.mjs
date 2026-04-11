import { expect, test } from '@playwright/test'
import { createDraftBatch, createTraceRecordByApi, getBatchWorkbenchByApi, seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

test('quality to publish path stays stable for demo batches', async ({ page, request }) => {
  test.setTimeout(120_000)

  const { batchCode, batchId } = await createDraftBatch(request)
  await createTraceRecordByApi(request, batchId, {
    title: '答辩演示首条现场记录',
    location: '江西省赣州市信丰果园基地',
    summary: '已补录答辩演示所需的首条现场记录，准备上传质检并继续发布。'
  })
  await seedAdminSession(page, request)

  await page.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  await expect(page.getByTestId('workbench-next-step-card')).toBeVisible()

  const reportNo = `QA-DEMO-${Date.now()}`
  await page.getByTestId('workbench-open-quality-dialog').click()
  const qualityDialog = page.getByTestId('workbench-quality-dialog')
  await expect(qualityDialog).toBeVisible()
  await qualityDialog.locator('label').filter({ hasText: '报告编号' }).locator('input').fill(reportNo)
  await qualityDialog.locator('label').filter({ hasText: '检测机构' }).locator('input').fill('江西省农产品质检中心')
  await qualityDialog.locator('label').filter({ hasText: '检测结果' }).locator('select').selectOption('PASS')
  await qualityDialog.locator('label').filter({ hasText: '质检摘要' }).locator('textarea').fill('答辩演示批次质检合格\n允许进入发布环节')
  await page.getByTestId('workbench-dialog-submit').click()

  await expect(page.getByTestId('workbench-quality-panel')).toContainText(reportNo)
  await expect(page.getByTestId('workbench-quality-panel')).toContainText('合格')

  await page.getByTestId('workbench-qr-action-0').click()
  await expect(page.getByTestId('workbench-qr-status')).toContainText('已生成')
  await expect(page.getByTestId('workbench-public-preview')).toBeVisible()

  await page.getByTestId('workbench-publish-action').click()
  await expect(page.getByTestId('workbench-status-dialog')).toBeVisible()
  page.once('dialog', (dialog) => dialog.accept())
  await page.getByTestId('workbench-dialog-submit').click()

  const statusBadge = page.locator('.status-badge.published').first()
  await expect(statusBadge).toContainText('已发布')
  await expect(page.getByTestId('workbench-public-preview')).toBeVisible()
  await saveNamedScreenshot(page, 'round12-quality-to-publish-workbench')

  const workbenchPayload = await getBatchWorkbenchByApi(request, batchId)
  expect(workbenchPayload.data.status.code).toBe('PUBLISHED')
  expect(workbenchPayload.data.qr.generated).toBe(true)

  const popupPromise = page.waitForEvent('popup')
  await page.getByTestId('workbench-public-preview').click()
  const popup = await popupPromise
  await popup.waitForLoadState('networkidle')
  await expect(popup.getByTestId('public-trace-page')).toBeVisible()
  await expect(popup.getByTestId('public-batch-code')).toContainText(batchCode)
  await expect(popup.getByTestId('public-product-name')).toContainText('赣南脐橙')
  await popup.close()
})
