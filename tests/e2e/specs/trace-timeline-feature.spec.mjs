import path from 'node:path'
import { expect, test } from '@playwright/test'
import { seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, screenshotDir, saveNamedScreenshot } from '../helpers/paths.mjs'

test('trace timeline feature shows the star batch lifecycle and stays safe on incomplete batches', async ({ page, request }) => {
  await seedAdminSession(page, request)

  await page.goto(`${adminBaseUrl}/batches/2`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toBeVisible()
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('溯源时间轴回放')
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('建档')
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('任务分配')
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('现场追溯')
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('质检')
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('二维码')
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('发布')

  await saveNamedScreenshot(page, 'feature-trace-timeline-workbench')
  await page.getByTestId('workbench-trace-timeline-panel').screenshot({
    path: path.join(screenshotDir, 'feature-trace-timeline-main.png')
  })

  const highlightedItem = page.locator('.timeline-feature-item.is-highlighted').first()
  await expect(highlightedItem).toBeVisible()
  await highlightedItem.screenshot({
    path: path.join(screenshotDir, 'feature-trace-timeline-detail.png')
  })

  await page.goto(`${adminBaseUrl}/batches/5`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByTestId('batch-workbench-page')).toBeVisible()
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toBeVisible()
  await expect(page.getByTestId('workbench-trace-timeline-panel')).toContainText('建档')
})
