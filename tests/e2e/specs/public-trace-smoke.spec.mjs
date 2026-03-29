import { expect, test } from '@playwright/test'
import { saveNamedScreenshot, traceBaseUrl } from '../helpers/paths.mjs'

test('public trace page shows the first-screen summary and simplified timeline', async ({ page }) => {
  await page.goto(`${traceBaseUrl}/t/demo-normal-2026`)

  await expect(page.getByTestId('public-trace-page')).toBeVisible()
  await expect(page.getByTestId('public-summary')).toBeVisible()
  await expect(page.getByTestId('public-product-name')).toContainText('Navel Orange')
  await expect(page.getByTestId('public-status')).not.toBeEmpty()
  await expect(page.getByTestId('public-quality')).not.toBeEmpty()
  await expect(page.getByTestId('public-company')).toContainText('Demo Orchard Company')
  await expect(page.getByTestId('public-batch-code')).toContainText('DEMO-ORANGE-202603-A1')
  await expect(page.getByTestId('public-origin')).toContainText('Jiangxi Ganzhou Xinfeng Orchard')
  await expect(page.getByTestId('public-timeline')).toBeVisible()
  await expect(page.getByTestId('public-timeline-item-0')).toBeVisible()
  await saveNamedScreenshot(page, 'round8-public-trace-normal')
})

test('public trace page shows risk state for recall demo data', async ({ page }) => {
  await page.goto(`${traceBaseUrl}/t/demo-recall-2026`)

  await expect(page.getByTestId('public-risk-banner')).toBeVisible()
  await expect(page.getByTestId('public-summary')).toBeVisible()
  await expect(page.getByTestId('public-product-name')).toContainText('Premium Rice')
  await saveNamedScreenshot(page, 'round8-public-trace-recall')
})
