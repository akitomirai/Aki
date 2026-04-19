import fs from 'node:fs/promises'
import { Buffer } from 'node:buffer'
import { expect, test } from '@playwright/test'
import { assignBatchByApi, createDraftBatch, loginByApi } from '../helpers/demo-api.mjs'
import { adminBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

const operatorCredentials = { username: 'operator', password: '123456' }
const platformCredentials = { username: 'platform', password: '123456' }

test('supplemental full-link smoke keeps the demo flow stable from assignment to public trace', async ({ browser, request }, testInfo) => {
  test.setTimeout(180_000)

  const { batchCode, batchId } = await createDraftBatch(request)
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

  const summaryText = `答辩全链路补录 ${Date.now()} 已完成装筐、转运与交接确认`
  const reportNo = `QA-FULL-${Date.now()}`
  const sampleImagePath = testInfo.outputPath('full-link-field-photo.png')

  await fs.writeFile(
    sampleImagePath,
    Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kL9sAAAAASUVORK5CYII=', 'base64')
  )

  try {
    await desktopPage.goto(`${adminBaseUrl}/batches`, { waitUntil: 'networkidle' })
    await expect(desktopPage.getByTestId('batch-list-page')).toBeVisible()
    await desktopPage.getByTestId('batch-filter-code').fill(batchCode)
    await desktopPage.getByTestId('batch-search-button').click()
    await expect(desktopPage.getByTestId(`batch-card-${batchId}`)).toBeVisible()
    await expect(desktopPage.getByTestId(`batch-next-${batchId}`)).toBeVisible()

    await desktopPage.getByTestId(`batch-open-workbench-${batchId}`).click()
    await expect(desktopPage).toHaveURL(new RegExp(`/batches/${batchId}$`))
    await expect(desktopPage.getByTestId('batch-workbench-page')).toBeVisible()
    await expect(desktopPage.getByTestId('workbench-assignment-panel')).toContainText('任务分配')

    await mobilePage.goto(`${adminBaseUrl}/field-entry?batchId=${batchId}`, { waitUntil: 'networkidle' })
    await expect(mobilePage.getByTestId('field-entry-page')).toBeVisible()
    await mobilePage.getByRole('button', { name: '运输' }).click()
    await mobilePage.locator('textarea').first().fill(summaryText)
    await mobilePage.locator('input[type="file"]').setInputFiles(sampleImagePath)
    mobilePage.once('dialog', (dialog) => dialog.accept())
    await mobilePage.getByTestId('field-entry-submit').click()
    await expect(mobilePage.getByTestId('field-entry-success')).toContainText(summaryText)

    await desktopPage.reload({ waitUntil: 'networkidle' })
    await expect(desktopPage.getByTestId('workbench-recent-records')).toContainText(summaryText)

    const quickActions = desktopPage.getByTestId('workbench-simple-actions')
    await quickActions.getByRole('button', { name: '上传质检' }).click()
    const qualityDialog = desktopPage.getByTestId('workbench-quality-dialog')
    await expect(qualityDialog).toBeVisible()
    await qualityDialog.locator('label').filter({ hasText: '报告编号' }).locator('input').fill(reportNo)
    await qualityDialog.locator('label').filter({ hasText: '检测机构' }).locator('input').fill('江西省农产品质检中心')
    await qualityDialog.locator('label').filter({ hasText: '检测结果' }).locator('select').selectOption('PASS')
    await qualityDialog.locator('label').filter({ hasText: '质检摘要' }).locator('textarea').fill('答辩全链路 smoke 质检合格\n允许继续生成二维码并完成发布')
    await desktopPage.getByTestId('workbench-dialog-submit').click()

    await expect(desktopPage.locator('.simple-main-grid')).toContainText(reportNo)
    await quickActions.getByRole('button', { name: '生成二维码' }).click()
    await expect(quickActions.getByRole('button', { name: '二维码已生成' })).toBeVisible()
    await expect(quickActions.getByRole('button', { name: '查看公开页' })).toBeVisible()

    await quickActions.getByRole('button', { name: '发布批次' }).click()
    await expect(desktopPage.getByTestId('workbench-status-dialog')).toBeVisible()
    desktopPage.once('dialog', (dialog) => dialog.accept())
    await desktopPage.getByTestId('workbench-dialog-submit').click()

    await expect(desktopPage.locator('.status-badge.published').first()).toContainText('已发布')
    await saveNamedScreenshot(desktopPage, 'round14-full-link-workbench-published')

    const popupPromise = desktopPage.waitForEvent('popup')
    await quickActions.getByRole('button', { name: '查看公开页' }).click()
    const popup = await popupPromise
    await popup.waitForLoadState('networkidle')
    await expect(popup.getByTestId('public-trace-page')).toBeVisible()
    await expect(popup.getByTestId('public-batch-code')).toContainText(batchCode)
    await expect(popup.getByTestId('public-timeline')).toBeVisible()
    await saveNamedScreenshot(popup, 'round14-full-link-public-trace')
    await popup.close()
  } finally {
    await Promise.all([mobileContext.close(), desktopContext.close()])
  }
})
