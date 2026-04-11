import fs from 'node:fs/promises'
import { Buffer } from 'node:buffer'
import { expect, test } from '@playwright/test'
import { assignBatchByApi, createDraftBatch, loginByApi } from '../helpers/demo-api.mjs'
import { adminBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

const operatorCredentials = { username: 'operator', password: '123456' }
const platformCredentials = { username: 'platform', password: '123456' }

test('field entry draft can resume and sync to workbench with image evidence', async ({ browser, request }, testInfo) => {
  test.setTimeout(120_000)

  const { batchId } = await createDraftBatch(request)
  await assignBatchByApi(request, batchId, 3)
  const operatorSession = await loginByApi(request, operatorCredentials)
  const platformSession = await loginByApi(request, platformCredentials)

  const mobileContext = await browser.newContext({
    viewport: { width: 390, height: 844 },
    isMobile: true,
    hasTouch: true
  })
  const desktopContext = await browser.newContext({
    viewport: { width: 1440, height: 1080 }
  })

  await Promise.all([
    mobileContext.addInitScript(({ token, user }) => {
      localStorage.setItem('admin_token', token)
      localStorage.setItem('admin_user', JSON.stringify(user))
    }, operatorSession),
    desktopContext.addInitScript(({ token, user }) => {
      localStorage.setItem('admin_token', token)
      localStorage.setItem('admin_user', JSON.stringify(user))
    }, platformSession)
  ])

  const mobilePage = await mobileContext.newPage()
  const desktopPage = await desktopContext.newPage()

  const summaryText = `答辩演示现场补录 ${Date.now()} 已完成装筐与交接记录`
  const sampleImagePath = testInfo.outputPath('field-photo.png')
  await fs.writeFile(
    sampleImagePath,
    Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kL9sAAAAASUVORK5CYII=', 'base64')
  )

  try {
    await mobilePage.goto(`${adminBaseUrl}/field-entry?batchId=${batchId}`, { waitUntil: 'networkidle' })
    await expect(mobilePage.getByTestId('field-entry-page')).toBeVisible()

    await mobilePage.getByRole('button', { name: '运输' }).click()
    await mobilePage.locator('textarea').first().fill(summaryText)
    await mobilePage.locator('summary').click()
    await mobilePage.getByTestId('field-entry-save-draft').click()
    await expect(mobilePage.getByTestId('field-entry-draft-banner')).toBeVisible()

    await mobilePage.reload({ waitUntil: 'networkidle' })
    await expect(mobilePage.getByTestId('field-entry-draft-banner')).toBeVisible()
    await expect(mobilePage.locator('textarea').first()).toHaveValue(summaryText)

    await mobilePage.locator('input[type="file"]').setInputFiles(sampleImagePath)
    await expect(mobilePage.getByTestId('field-entry-image-grid')).toBeVisible()
    await saveNamedScreenshot(mobilePage, 'round12-field-work-draft-restored')

    mobilePage.once('dialog', (dialog) => dialog.accept())
    await mobilePage.getByTestId('field-entry-submit').click()
    await expect(mobilePage.getByTestId('field-entry-success')).toContainText(summaryText)
    await saveNamedScreenshot(mobilePage, 'round12-field-work-submit-success')

    await desktopPage.goto(`${adminBaseUrl}/batches/${batchId}`, { waitUntil: 'networkidle' })
    await expect(desktopPage.getByTestId('batch-workbench-page')).toBeVisible()
    const recentPanel = desktopPage.getByTestId('workbench-recent-records')
    await expect(recentPanel).toContainText(summaryText)
    await expect(recentPanel.locator('img.record-image').first()).toBeVisible()
    await saveNamedScreenshot(desktopPage, 'round12-field-work-workbench')
  } finally {
    await Promise.all([mobileContext.close(), desktopContext.close()])
  }
})
