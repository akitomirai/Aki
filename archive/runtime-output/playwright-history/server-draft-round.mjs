import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, request } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const samplePng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kL9sAAAAASUVORK5CYII=',
  'base64'
)
const imageOne = path.join(outputDir, 'server-draft-photo-1.png')
const imageTwo = path.join(outputDir, 'server-draft-photo-2.png')
await fs.writeFile(imageOne, samplePng)
await fs.writeFile(imageTwo, samplePng)

const adminBaseUrl = 'http://127.0.0.1:5174'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)

const browser = await chromium.launch({ headless: true })
const mobileContext = await browser.newContext({
  viewport: { width: 390, height: 844 },
  isMobile: true,
  hasTouch: true
})
const page = await mobileContext.newPage()
const consoleErrors = []
const pageErrors = []

page.on('console', (message) => {
  if (message.type() === 'error') consoleErrors.push(message.text())
})
page.on('pageerror', (error) => pageErrors.push(String(error)))

async function readJson(responsePromise) {
  const response = await responsePromise
  const body = await response.json()
  return { status: response.status(), body }
}

async function waitFor(check, timeoutMs = 15000, intervalMs = 500) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    const value = await check()
    if (value) return value
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  throw new Error('Timed out while waiting for condition')
}

await page.goto(`${adminBaseUrl}/login`, { waitUntil: 'networkidle' })
await page.getByTestId('login-page').waitFor()
await page.locator('input').nth(0).fill('operator')
await page.locator('input').nth(1).fill('123456')
await page.getByTestId('login-submit').click()
await page.waitForURL(/field-entry/, { timeout: 20000 })
await page.getByTestId('field-entry-page').waitFor()

const session = await page.evaluate(() => ({
  token: localStorage.getItem('admin_token'),
  user: JSON.parse(localStorage.getItem('admin_user') || '{}')
}))
if (!session.token) {
  throw new Error('Login succeeded visually, but no token was stored in localStorage.')
}

const apiContext = await request.newContext({
  extraHTTPHeaders: {
    Authorization: `Bearer ${session.token}`
  }
})

try {
  await apiContext.delete(`${apiBaseUrl}/batches/2/field-draft`)
} catch (error) {
  // Ignore cleanup failures and continue with the fresh demo state when possible.
}
await page.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await page.getByTestId('field-entry-page').waitFor()

const beforeMine = await readJson(apiContext.get(`${apiBaseUrl}/batches?mineOnly=true`))
await page.screenshot({ path: path.join(outputDir, '18-service-todo.png'), fullPage: true })

await page.getByTestId('field-todo-open-button').first().click()
await page.getByTestId('field-entry-page').waitFor()

const draftTitle = `服务端草稿回归 ${stamp}`
const draftSummary = `服务端草稿回归 ${stamp}，先保存一条草稿再刷新验证。`
await page.locator('input[type="text"]').nth(0).fill(draftTitle)
await page.locator('textarea').fill(draftSummary)
await page.locator('input[type="text"]').nth(1).fill('冷链发运区 A 口')
await page.locator('input[type="text"]').nth(2).fill('Field Operator')
await page.locator('input[type="file"]').setInputFiles(imageOne)
await page.getByTestId('field-image-queue').waitFor()
await page.waitForFunction(() => {
  const states = Array.from(document.querySelectorAll('.image-state')).map((node) => node.textContent || '')
  return states.length > 0 && states.every((text) => !text.includes('上传中'))
}, { timeout: 20000 })
await page.getByTestId('field-entry-save-draft').click()

const draftAfterSave = await waitFor(async () => {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/field-drafts`))
  return payload.body?.data?.length ? payload : null
}, 20000)

await page.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await page.evaluate(() => localStorage.removeItem('field_entry_drafts_v1'))
await page.reload({ waitUntil: 'networkidle' })
await page.getByTestId('field-list-mode-drafts').click()
await page.getByTestId('field-draft-row').first().waitFor()
await page.screenshot({ path: path.join(outputDir, '19-service-draft-refresh.png'), fullPage: true })

const draftRowText = (await page.getByTestId('field-draft-row').first().textContent()) || ''
if (!draftRowText.includes('ORANGE-202603-D1') || !draftRowText.includes('图片数1')) {
  throw new Error(`Draft row did not show the expected batch and image count after refresh: ${draftRowText}`)
}

await page.getByTestId('field-draft-open').first().click()
await page.getByTestId('field-entry-draft-banner').waitFor()
const submitSummary = `${draftSummary} 刷新后继续补第二张图片并提交。`
await page.locator('textarea').fill(submitSummary)
await page.locator('input[type="file"]').setInputFiles(imageTwo)
await page.waitForFunction(() => {
  const rows = document.querySelectorAll('[data-testid="field-image-queue"] .image-row').length
  const states = Array.from(document.querySelectorAll('.image-state')).map((node) => node.textContent || '')
  return rows === 2 && states.length >= 2 && states.every((text) => !text.includes('上传中'))
}, { timeout: 20000 })
await page.screenshot({ path: path.join(outputDir, '20-service-draft-resume.png'), fullPage: true })

await page.getByTestId('field-entry-submit').click()
await page.getByTestId('field-entry-success').waitFor()
await page.screenshot({ path: path.join(outputDir, '21-service-submit-success.png'), fullPage: true })

const draftAfterSubmit = await waitFor(async () => {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches/field-drafts`))
  return payload.body?.data?.length === 0 ? payload : null
}, 20000)
const afterMine = await waitFor(async () => {
  const payload = await readJson(apiContext.get(`${apiBaseUrl}/batches?mineOnly=true`))
  const item = payload.body?.data?.[0]
  return item?.todayCompleted ? payload : null
}, 20000)
const workbenchPayload = await readJson(apiContext.get(`${apiBaseUrl}/batches/2`))

await page.getByTestId('field-entry-success').getByRole('button', { name: '查看批次工作台' }).click()
await page.waitForURL(/\/batches\/2/, { timeout: 20000 })
await page.getByTestId('batch-workbench-page').waitFor()
await page.getByTestId('workbench-recent-records').getByText(submitSummary).first().waitFor()
await page.screenshot({ path: path.join(outputDir, '22-service-workbench-sync.png'), fullPage: true })

await page.goto(`${adminBaseUrl}/field-entry`, { waitUntil: 'networkidle' })
await page.getByTestId('field-task-filter-done_today').click()
await page.getByTestId('field-task-row').first().waitFor()
await page.screenshot({ path: path.join(outputDir, '23-service-done-today.png'), fullPage: true })

const result = {
  verifiedAt: new Date().toISOString(),
  loginUser: session.user,
  apiSnapshots: {
    beforeMine: beforeMine.body?.data ?? [],
    draftAfterSave: draftAfterSave.body?.data ?? [],
    draftAfterSubmit: draftAfterSubmit.body?.data ?? [],
    afterMine: afterMine.body?.data ?? [],
    workbenchTask: workbenchPayload.body?.data?.task ?? null,
    workbenchRecentRecord: workbenchPayload.body?.data?.trace?.recentRecords?.[0] ?? null
  },
  uiChecks: {
    draftVisibleAfterRefresh: true,
    draftRowText,
    consoleErrors,
    pageErrors
  },
  screenshots: [
    '18-service-todo.png',
    '19-service-draft-refresh.png',
    '20-service-draft-resume.png',
    '21-service-submit-success.png',
    '22-service-workbench-sync.png',
    '23-service-done-today.png'
  ].map((name) => path.join(outputDir, name))
}

await fs.writeFile(path.join(outputDir, 'server-draft-round.json'), JSON.stringify(result, null, 2), 'utf8')
await apiContext.dispose()
await mobileContext.close()
await browser.close()
console.log(JSON.stringify(result, null, 2))
