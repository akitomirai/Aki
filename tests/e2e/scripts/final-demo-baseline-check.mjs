import { chromium } from 'playwright'
import { adminBaseUrl, traceBaseUrl } from '../helpers/paths.mjs'

const accounts = [
  {
    username: 'platform',
    password: '123456',
    expectedPath: /\/dashboard(?:\?|$)/,
    pageTestId: 'dashboard-page'
  },
  {
    username: 'enterprise_admin',
    password: '123456',
    expectedPath: /\/batches(?:\?|$)/,
    pageTestId: 'batch-list-page'
  },
  {
    username: 'operator',
    password: '123456',
    expectedPath: /\/field-entry(?:\?|$)/,
    pageTestId: 'field-entry-page'
  },
  {
    username: 'regulator',
    password: '123456',
    expectedPath: /\/risk(?:\?|$)/,
    pageTestId: 'risk-page'
  }
]

async function loginViaUi(page, account) {
  await page.goto(`${adminBaseUrl}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('[data-testid="login-page"]', { state: 'visible', timeout: 20000 })

  await page.locator('.login-form input').nth(0).fill(account.username)
  await page.locator('input[type="password"]').first().fill(account.password)

  await page.getByTestId('login-submit').click()
  await page.waitForLoadState('domcontentloaded')
  await page.waitForURL((url) => account.expectedPath.test(url.toString()), { timeout: 20000 })

  await page.waitForSelector(`[data-testid="${account.pageTestId}"]`, { state: 'visible', timeout: 20000 })
}

async function verifyPlatformFlow(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()

  try {
    await loginViaUi(page, accounts[0])

    await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="batch-list-page"]', { state: 'visible', timeout: 20000 })
    await page.locator('[data-testid="batch-list-page"]').getByText('ORANGE-202603-D1').waitFor({ state: 'visible', timeout: 20000 })
    await page.locator('[data-testid="batch-list-page"]').getByText('ORANGE-202604-Q1').waitFor({ state: 'visible', timeout: 20000 })

    await page.goto(`${adminBaseUrl}/quality`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="quality-page"]', { state: 'visible', timeout: 20000 })

    await page.goto(`${adminBaseUrl}/risk`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="risk-page"]', { state: 'visible', timeout: 20000 })

    await page.goto(`${adminBaseUrl}/qr`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="qr-publish-page"]', { state: 'visible', timeout: 20000 })

    await page.goto(`${adminBaseUrl}/batches/2`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="batch-workbench-page"]', { state: 'visible', timeout: 20000 })
    await page.locator('[data-testid="batch-workbench-page"]').getByText('ORANGE-202603-D1', { exact: true }).first().waitFor({ state: 'visible', timeout: 20000 })
  }
  finally {
    await context.close()
  }
}

async function verifyEnterpriseFlow(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()

  try {
    await loginViaUi(page, accounts[1])
    await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="batch-list-page"]', { state: 'visible', timeout: 20000 })
    await page.locator('[data-testid="batch-list-page"]').getByText('ORANGE-202603-D1').waitFor({ state: 'visible', timeout: 20000 })
  }
  finally {
    await context.close()
  }
}

async function verifyOperatorFlow(browser) {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })
  const page = await context.newPage()

  try {
    await loginViaUi(page, accounts[2])
    await page.locator('[data-testid="field-entry-page"]').getByText('ORANGE-202604-Q1', { exact: true }).first().waitFor({ state: 'visible', timeout: 20000 })
  }
  finally {
    await context.close()
  }
}

async function verifyRegulatorFlow(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()

  try {
    await loginViaUi(page, accounts[3])
    await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="batch-list-page"]', { state: 'visible', timeout: 20000 })
    await page.locator('[data-testid="batch-list-page"]').getByText('ORANGE-202603-D1').waitFor({ state: 'visible', timeout: 20000 })
  }
  finally {
    await context.close()
  }
}

async function verifyPublicTrace(browser) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()

  try {
    await page.goto(`${traceBaseUrl}/t/orange-202603-d1`, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('[data-testid="public-trace-page"]', { state: 'visible', timeout: 20000 })
    await page.locator('[data-testid="public-batch-code"]').getByText('ORANGE-202603-D1').waitFor({ state: 'visible', timeout: 20000 })
  }
  finally {
    await context.close()
  }
}

const browser = await chromium.launch({ headless: true })

try {
  await verifyPlatformFlow(browser)
  await verifyEnterpriseFlow(browser)
  await verifyOperatorFlow(browser)
  await verifyRegulatorFlow(browser)
  await verifyPublicTrace(browser)
  console.log('Final demo baseline browser verification passed.')
}
finally {
  await browser.close()
}
