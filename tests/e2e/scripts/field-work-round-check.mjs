import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '@playwright/test'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..', '..', '..')
const outputDir = path.join(rootDir, 'tests', 'e2e', 'artifacts', 'field-work-round')
const runtimeDir = path.join(outputDir, 'runtime')
const screenshotDir = path.join(outputDir, 'screenshots')
const adminBaseUrl = process.env.ADMIN_BASE_URL || 'http://127.0.0.1:5174'
const traceBaseUrl = process.env.TRACE_BASE_URL || 'http://127.0.0.1:5173'
const apiBaseUrl = process.env.API_BASE_URL || 'http://127.0.0.1:8080/api'

await fs.mkdir(runtimeDir, { recursive: true })
await fs.mkdir(screenshotDir, { recursive: true })

const sampleImagePath = path.join(runtimeDir, 'field-photo.png')
await fs.writeFile(
  sampleImagePath,
  Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kL9sAAAAASUVORK5CYII=', 'base64')
)

const apiContext = await request.newContext()
const loginResponse = await apiContext.post(`${apiBaseUrl}/auth/login`, {
  data: { username: 'operator', password: '123456' }
})

if (!loginResponse.ok()) {
  throw new Error(`operator login failed: ${loginResponse.status()} ${loginResponse.statusText()}`)
}

const loginPayload = await loginResponse.json()
if (!loginPayload?.success || !loginPayload?.data?.token) {
  throw new Error(`operator login payload invalid: ${JSON.stringify(loginPayload)}`)
}

const browser = await chromium.launch({ headless: true })

const mobileContext = await browser.newContext({
  viewport: { width: 390, height: 844 },
  isMobile: true,
  hasTouch: true
})
const desktopContext = await browser.newContext({
  viewport: { width: 1440, height: 1080 }
})

const session = loginPayload.data
await Promise.all([
  mobileContext.addInitScript(({ token, user }) => {
    localStorage.setItem('admin_token', token)
    localStorage.setItem('admin_user', JSON.stringify(user))
  }, session),
  desktopContext.addInitScript(({ token, user }) => {
    localStorage.setItem('admin_token', token)
    localStorage.setItem('admin_user', JSON.stringify(user))
  }, session)
])

const mobilePage = await mobileContext.newPage()
const desktopPage = await desktopContext.newPage()
const tracePage = await desktopContext.newPage()

const mobileConsoleErrors = []
const desktopConsoleErrors = []
const traceConsoleErrors = []
const pageErrors = []

mobilePage.on('console', (message) => {
  if (message.type() === 'error') mobileConsoleErrors.push(message.text())
})
desktopPage.on('console', (message) => {
  if (message.type() === 'error') desktopConsoleErrors.push(message.text())
})
tracePage.on('console', (message) => {
  if (message.type() === 'error') traceConsoleErrors.push(message.text())
})

for (const page of [mobilePage, desktopPage, tracePage]) {
  page.on('pageerror', (error) => pageErrors.push(String(error)))
}

const summaryText = `现场作业图片回归 ${new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)} 已完成拍照与交接记录`

await mobilePage.goto(`${adminBaseUrl}/field-entry?batchId=2`, { waitUntil: 'networkidle' })
await mobilePage.getByTestId('field-entry-page').waitFor()
await mobilePage.getByRole('button', { name: '运输' }).click()
await mobilePage.locator('textarea').fill(summaryText)
await mobilePage.locator('input[type="text"]').nth(1).fill('冷链发运区 A 口')
await mobilePage.locator('input[type="text"]').nth(2).fill('现场操作员-赵强')
await mobilePage.locator('input[type="file"]').setInputFiles(sampleImagePath)
await mobilePage.getByTestId('field-entry-image-grid').waitFor()
await mobilePage.screenshot({
  path: path.join(screenshotDir, 'field-entry-before-submit.png'),
  fullPage: true
})

await mobilePage.getByTestId('field-entry-submit').click()
await mobilePage.getByTestId('field-entry-success').waitFor()
await mobilePage.getByTestId('field-entry-success').getByText(summaryText).waitFor()
await mobilePage.screenshot({
  path: path.join(screenshotDir, 'field-entry-after-submit.png'),
  fullPage: true
})

await desktopPage.goto(`${adminBaseUrl}/batches/2`, { waitUntil: 'networkidle' })
await desktopPage.getByTestId('batch-workbench-page').waitFor()
const recentPanel = desktopPage.getByTestId('workbench-recent-records')
await recentPanel.getByText(summaryText).waitFor()
const recordImageVisible = await recentPanel.locator('img.record-image').first().isVisible()
await desktopPage.screenshot({
  path: path.join(screenshotDir, 'batch-workbench-after-upload.png'),
  fullPage: true
})

await tracePage.goto(`${traceBaseUrl}/t/demo-normal-2026`, { waitUntil: 'networkidle' })
await tracePage.screenshot({
  path: path.join(screenshotDir, 'trace-page-check.png'),
  fullPage: true
})

const batchResponse = await apiContext.get(`${apiBaseUrl}/batches/2`, {
  headers: {
    Authorization: `Bearer ${session.token}`
  }
})
const batchPayload = await batchResponse.json()

const result = {
  loginUser: session.user,
  pages: {
    fieldEntry: `${adminBaseUrl}/field-entry?batchId=2`,
    batchWorkbench: `${adminBaseUrl}/batches/2`,
    tracePage: `${traceBaseUrl}/t/demo-normal-2026`
  },
  submission: {
    summaryText,
    imageUploaded: true,
    workbenchImageVisible: recordImageVisible,
    apiLatestRecordTitle: batchPayload?.data?.trace?.recentRecords?.[0]?.title,
    apiLatestRecordSummary: batchPayload?.data?.trace?.recentRecords?.[0]?.summary,
    apiLatestRecordImageUrl: batchPayload?.data?.trace?.recentRecords?.[0]?.imageUrl || '',
    apiLatestRecordOperator: batchPayload?.data?.trace?.recentRecords?.[0]?.operatorName
  },
  consoleErrors: {
    mobile: mobileConsoleErrors,
    desktop: desktopConsoleErrors,
    trace: traceConsoleErrors
  },
  pageErrors
}

await fs.writeFile(path.join(runtimeDir, 'field-work-check.json'), JSON.stringify(result, null, 2), 'utf8')

await Promise.all([mobileContext.close(), desktopContext.close(), apiContext.dispose()])
await browser.close()
