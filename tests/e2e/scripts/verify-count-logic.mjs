import fs from 'node:fs/promises'
import path from 'node:path'
import assert from 'node:assert/strict'
import { chromium } from 'playwright'

const adminBaseUrl = 'http://127.0.0.1:5174'
const apiBaseUrl = 'http://127.0.0.1:8080/api'
const screenshotDir = path.resolve('..', '..', 'docs', 'ui-check')
await fs.mkdir(screenshotDir, { recursive: true })

const loginResponse = await fetch(`${apiBaseUrl}/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'platform', password: '123456' })
})
if (!loginResponse.ok) throw new Error(`登录失败: ${loginResponse.status}`)
const loginPayload = await loginResponse.json()
const login = loginPayload.data

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({
  viewport: { width: 1600, height: 1300 },
  locale: 'zh-CN',
  timezoneId: 'Asia/Shanghai'
})
await context.addInitScript((seed) => {
  localStorage.setItem('admin_token', seed.token)
  localStorage.setItem('admin_user', JSON.stringify(seed.user))
}, login)
const page = await context.newPage()

function parseCount(text) {
  const match = String(text || '').match(/(\d+)\s*$/)
  return match ? Number(match[1]) : null
}

async function openRoute(pathname, testId) {
  await page.goto(`${adminBaseUrl}${pathname}`, { waitUntil: 'networkidle' })
  await page.waitForURL((url) => url.pathname === pathname, { timeout: 30000 })
  await page.waitForSelector(`[data-testid="${testId}"]`, { timeout: 30000 })
  await page.waitForTimeout(700)
}

async function collectChipMap(selectors) {
  const result = {}
  for (const selector of selectors) {
    const text = await page.locator(selector).textContent()
    result[selector] = {
      text: String(text || '').replace(/\s+/g, ' ').trim(),
      count: parseCount(text)
    }
  }
  return result
}

async function captureBlock(fileName, topSelector, bottomSelector) {
  const clip = await page.evaluate(({ topSelector: topRef, bottomSelector: bottomRef }) => {
    const shell = document.querySelector('.page-shell')
    const topNode = document.querySelector(topRef)
    const bottomNode = document.querySelector(bottomRef)
    if (!shell || !topNode || !bottomNode) return null
    const shellRect = shell.getBoundingClientRect()
    const topRect = topNode.getBoundingClientRect()
    const bottomRect = bottomNode.getBoundingClientRect()
    const top = Math.max(0, Math.floor(Math.min(topRect.top, bottomRect.top) - 4))
    const left = Math.max(0, Math.floor(shellRect.left))
    const width = Math.ceil(shellRect.width)
    const bottom = Math.ceil(Math.max(topRect.bottom, bottomRect.bottom) + 18)
    return { x: left, y: top, width, height: Math.ceil(bottom - top) }
  }, { topSelector, bottomSelector })
  if (!clip) throw new Error(`无法生成截图区域: ${fileName}`)
  await page.screenshot({ path: path.join(screenshotDir, fileName), clip })
}

async function verifyCompanies() {
  await openRoute('/companies', 'companies-page')
  const chipSelectors = [
    '[data-testid="companies-summary-all"]',
    '[data-testid="companies-summary-enabled"]',
    '[data-testid="companies-summary-disabled"]',
    '[data-testid="companies-summary-archived"]'
  ]
  const defaultCounts = await collectChipMap(chipSelectors)
  const defaultSummary = await page.locator('.manage-filter-card .manage-muted').textContent()
  await captureBlock('count-logic-companies-default.png', '.manage-summary-row', '.manage-filter-card')

  const chosen = { selector: '[data-testid="companies-summary-disabled"]', count: defaultCounts['[data-testid="companies-summary-disabled"]'].count ?? 0 }

  await page.locator(chosen.selector).click()
  await page.waitForTimeout(900)

  const filteredCounts = await collectChipMap(chipSelectors)
  const filteredSummary = await page.locator('.manage-filter-card .manage-muted').textContent()
  await captureBlock('count-logic-companies-filtered.png', '.manage-summary-row', '.manage-filter-card')

  for (const selector of chipSelectors) {
    assert.equal(filteredCounts[selector].count, defaultCounts[selector].count, `companies ${selector} 计数不应变化`)
  }
  assert.notEqual(String(filteredSummary || '').trim(), String(defaultSummary || '').trim(), 'companies 当前结果提示应变化')

  return {
    chosenSelector: chosen.selector,
    defaultCounts,
    filteredCounts,
    defaultSummary: String(defaultSummary || '').trim(),
    filteredSummary: String(filteredSummary || '').trim()
  }
}

async function verifyStatusPage({ pathname, testId, chipSelectors, summarySelector, screenshotName, preferredSelector }) {
  await openRoute(pathname, testId)
  const defaultCounts = await collectChipMap(chipSelectors)
  const defaultSummary = await page.locator(summarySelector).textContent()
  const chosen = { selector: preferredSelector, count: defaultCounts[preferredSelector].count ?? 0 }

  await page.locator(chosen.selector).click()
  await page.waitForTimeout(900)

  const filteredCounts = await collectChipMap(chipSelectors)
  const filteredSummary = await page.locator(summarySelector).textContent()
  const filterPanelSelector = summarySelector.includes('batch-filter-panel') ? '.batch-filter-panel' : '.manage-filter-card'
  await captureBlock(screenshotName, '.manage-summary-row, .batch-summary-row', filterPanelSelector)

  for (const selector of chipSelectors) {
    assert.equal(filteredCounts[selector].count, defaultCounts[selector].count, `${pathname} ${selector} 计数不应变化`)
  }
  assert.notEqual(String(filteredSummary || '').trim(), String(defaultSummary || '').trim(), `${pathname} 当前结果提示应变化`)

  return {
    chosenSelector: chosen.selector,
    defaultCounts,
    filteredCounts,
    defaultSummary: String(defaultSummary || '').trim(),
    filteredSummary: String(filteredSummary || '').trim()
  }
}

async function verifyTabPage({ pathname, testId, chipPrefix, summarySelector, screenshotName }) {
  await openRoute(pathname, testId)
  const chipSelectors = await page
    .locator(`[data-testid^="${chipPrefix}"]`)
    .evaluateAll((els) => els.map((el) => `[data-testid="${el.getAttribute('data-testid')}"]`))
  const defaultCounts = await collectChipMap(chipSelectors)
  const defaultSummary = await page.locator(summarySelector).textContent()
  const preferredSelector = chipPrefix === 'batch-mode-'
    ? '[data-testid="batch-mode-RISK"]'
    : chipPrefix === 'qr-tab-'
      ? '[data-testid="qr-tab-PUBLISHED"]'
      : chipPrefix === 'quality-tab-'
        ? '[data-testid="quality-tab-FAIL"]'
        : '[data-testid="risk-tab-PROCESSING"]'
  const chosen = { selector: preferredSelector, count: defaultCounts[preferredSelector].count ?? 0 }

  await page.locator(chosen.selector).click()
  await page.waitForTimeout(900)

  const filteredCounts = await collectChipMap(chipSelectors)
  const filteredSummary = await page.locator(summarySelector).textContent()
  const filterPanelSelector = summarySelector.includes('qr-filter-panel')
    ? '.qr-filter-panel'
    : summarySelector.includes('batch-filter-panel')
      ? '.batch-filter-panel'
    : summarySelector.includes('quality-filter-panel')
      ? '.quality-filter-panel'
      : '.risk-filter-panel'
  await captureBlock(screenshotName, '.manage-summary-row', filterPanelSelector)

  for (const selector of chipSelectors) {
    assert.equal(filteredCounts[selector].count, defaultCounts[selector].count, `${pathname} ${selector} 计数不应变化`)
  }

  return {
    chosenSelector: chosen.selector,
    defaultCounts,
    filteredCounts,
    defaultSummary: String(defaultSummary || '').trim(),
    filteredSummary: String(filteredSummary || '').trim()
  }
}

async function verifyLogs() {
  await openRoute('/logs', 'logs-page')
  const chipSelectors = ['[data-testid="logs-summary-success"]', '[data-testid="logs-summary-failed"]']
  const defaultCounts = await collectChipMap(chipSelectors)
  const chosen = { selector: '[data-testid="logs-summary-failed"]', count: defaultCounts['[data-testid="logs-summary-failed"]'].count ?? 0 }
  const defaultSummary = await page.locator('.manage-summary .manage-summary-chip').first().textContent()
  await page.locator(chosen.selector).click()
  await page.waitForTimeout(900)
  const filteredCounts = await collectChipMap(chipSelectors)
  const filteredSummary = await page.locator('.manage-summary .manage-summary-chip').first().textContent()
  for (const selector of chipSelectors) {
    assert.equal(filteredCounts[selector].count, defaultCounts[selector].count, `logs ${selector} 计数不应变化`)
  }
  return {
    skipped: false,
    chosenSelector: chosen.selector,
    defaultCounts,
    filteredCounts,
    defaultSummary: String(defaultSummary || '').trim(),
    filteredSummary: String(filteredSummary || '').trim()
  }
}

const results = {
  companies: await verifyCompanies(),
  products: await verifyStatusPage({
    pathname: '/products',
    testId: 'products-page',
    chipSelectors: [
      '[data-testid="products-summary-enabled"]',
      '[data-testid="products-summary-disabled"]',
      '[data-testid="products-summary-archived"]'
    ],
    summarySelector: '.manage-filter-card .manage-muted',
    screenshotName: 'count-logic-products.png',
    preferredSelector: '[data-testid="products-summary-disabled"]'
  }),
  users: await verifyStatusPage({
    pathname: '/users',
    testId: 'users-page',
    chipSelectors: [
      '[data-testid="users-summary-enabled"]',
      '[data-testid="users-summary-disabled"]'
    ],
    summarySelector: '.manage-filter-card .manage-muted',
    screenshotName: 'count-logic-users.png',
    preferredSelector: '[data-testid="users-summary-disabled"]'
  }),
  batches: await verifyTabPage({
    pathname: '/batches',
    testId: 'batch-list-page',
    chipPrefix: 'batch-mode-',
    summarySelector: '.batch-filter-panel .list-summary',
    screenshotName: 'count-logic-batches.png'
  }),
  qr: await verifyTabPage({
    pathname: '/qr',
    testId: 'qr-publish-page',
    chipPrefix: 'qr-tab-',
    summarySelector: '.qr-filter-panel .list-summary',
    screenshotName: 'count-logic-qr.png'
  }),
  quality: await verifyTabPage({
    pathname: '/quality',
    testId: 'quality-page',
    chipPrefix: 'quality-tab-',
    summarySelector: '.quality-filter-panel .list-summary',
    screenshotName: 'count-logic-quality.png'
  }),
  risk: await verifyTabPage({
    pathname: '/risk',
    testId: 'risk-page',
    chipPrefix: 'risk-tab-',
    summarySelector: '.risk-filter-panel .list-summary',
    screenshotName: 'count-logic-risk.png'
  }),
  logs: await verifyLogs()
}

console.log(JSON.stringify(results, null, 2))
await browser.close()
