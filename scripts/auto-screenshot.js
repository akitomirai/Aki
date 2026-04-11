const fs = require('node:fs');
const path = require('node:path');

const rootDir = path.resolve(__dirname, '..');
const screenshotDir = path.join(rootDir, 'screenshots');
const playwrightModulePath = path.join(rootDir, 'tests', 'e2e', 'node_modules', 'playwright');

const adminBaseUrl = 'http://127.0.0.1:5174';
const traceBaseUrl = 'http://127.0.0.1:5173';
const defaultAccount = {
  username: 'platform',
  password: '123456'
};
const traceRecordBatchId = 2;

function ensureDirectory(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

function loadPlaywright() {
  try {
    return require(playwrightModulePath);
  } catch (error) {
    const message = [
      'Unable to load Playwright from tests/e2e/node_modules/playwright.',
      `Expected module path: ${playwrightModulePath}`,
      'Run `npm install` in tests/e2e if the dependency is missing.',
      `Original error: ${error.message}`
    ].join('\n');
    throw new Error(message);
  }
}

async function waitForStablePage(page, selector) {
  await page.waitForLoadState('domcontentloaded');
  try {
    await page.waitForLoadState('networkidle', { timeout: 10000 });
  } catch (error) {
    console.warn(`Network idle wait timed out for ${page.url()}, continuing with selector wait.`);
  }
  if (selector) {
    await page.waitForSelector(selector, { state: 'visible', timeout: 15000 });
  }
  await page.waitForTimeout(800);
}

async function capturePage(page, fileName) {
  const targetPath = path.join(screenshotDir, fileName);
  await page.screenshot({
    path: targetPath,
    fullPage: true
  });
  console.log(`Saved ${targetPath}`);
}

async function gotoAndCapture(page, url, selector, fileName) {
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await waitForStablePage(page, selector);
  await capturePage(page, fileName);
}

async function gotoAndWait(page, url, selector) {
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await waitForStablePage(page, selector);
}

async function clickTabIfVisible(page, selector) {
  const tab = page.locator(selector);
  if (await tab.count()) {
    await tab.first().click();
    await page.waitForTimeout(800);
  }
}

async function loginAndCapture(page) {
  await page.goto(`${adminBaseUrl}/login`, { waitUntil: 'domcontentloaded' });
  await waitForStablePage(page, '[data-testid="login-page"]');
  await capturePage(page, '01-login.png');

  const usernameInput = page.locator('input').nth(0);
  const passwordInput = page.locator('input[type="password"]').first();

  await usernameInput.fill(defaultAccount.username);
  await passwordInput.fill(defaultAccount.password);

  await Promise.all([
    page.waitForURL('**/dashboard', { timeout: 15000 }),
    page.locator('[data-testid="login-submit"]').click()
  ]);

  await waitForStablePage(page, '[data-testid="dashboard-page"]');
  await capturePage(page, '02-dashboard.png');
}

async function main() {
  ensureDirectory(screenshotDir);

  const { chromium } = loadPlaywright();
  const browser = await chromium.launch({
    headless: true
  });

  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 }
  });
  const page = await context.newPage();

  try {
    await loginAndCapture(page);

    await gotoAndCapture(
      page,
      `${adminBaseUrl}/products`,
      '[data-testid="products-page"]',
      '03-product.png'
    );

    await gotoAndCapture(
      page,
      `${adminBaseUrl}/batches?mode=ALL`,
      '[data-testid="batch-list-page"]',
      '04-batch.png'
    );

    await gotoAndWait(page, `${adminBaseUrl}/batches/${traceRecordBatchId}`, '[data-testid="batch-workbench-page"]');
    await page.waitForSelector('[data-testid="workbench-recent-records"]', {
      state: 'visible',
      timeout: 15000
    });
    await page.waitForTimeout(800);
    await capturePage(page, '05-trace-record.png');

    await gotoAndWait(page, `${adminBaseUrl}/quality`, '[data-testid="quality-page"]');
    await clickTabIfVisible(page, '[data-testid="quality-tab-PASS"]');
    await waitForStablePage(page, '[data-testid="quality-page"]');
    await capturePage(page, '06-quality.png');

    await gotoAndWait(page, `${adminBaseUrl}/qr`, '[data-testid="qr-publish-page"]');
    await clickTabIfVisible(page, '[data-testid="qr-tab-PUBLISHED"]');
    await waitForStablePage(page, '[data-testid="qr-publish-page"]');
    await capturePage(page, '07-qrcode.png');

    await gotoAndCapture(
      page,
      `${traceBaseUrl}/t/orange-202603-d1`,
      '[data-testid="public-trace-page"]',
      '08-trace-view.png'
    );
  } finally {
    await context.close();
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
