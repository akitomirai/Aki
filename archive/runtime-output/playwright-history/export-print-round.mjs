import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const traceBaseUrl = 'http://127.0.0.1:5173'
const batchCode = 'ORANGE-202603-D1'
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const screenshotFiles = []

function screenshotPath(name) {
  const fullPath = path.join(outputDir, name)
  screenshotFiles.push(fullPath)
  return fullPath
}

function stripBom(text) {
  return String(text || '').replace(/^\uFEFF/, '')
}

function parseCsvLine(line) {
  const cells = []
  let current = ''
  let quoted = false
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index]
    if (quoted) {
      if (char === '"' && line[index + 1] === '"') {
        current += '"'
        index += 1
      } else if (char === '"') {
        quoted = false
      } else {
        current += char
      }
      continue
    }
    if (char === '"') {
      quoted = true
    } else if (char === ',') {
      cells.push(current)
      current = ''
    } else {
      current += char
    }
  }
  cells.push(current)
  return cells
}

function parseCsv(text) {
  const lines = stripBom(text)
    .split(/\r?\n/)
    .filter(Boolean)
  const headers = parseCsvLine(lines[0] || '')
  const rows = lines.slice(1).map((line) => {
    const values = parseCsvLine(line)
    return Object.fromEntries(headers.map((header, index) => [header, values[index] ?? '']))
  })
  return { headers, rows }
}

async function login(page, username, password = '123456') {
  await page.goto(`${adminBaseUrl}/login`, { waitUntil: 'networkidle' })
  await page.getByTestId('login-page').waitFor()
  await page.locator('input').nth(0).fill(username)
  await page.locator('input').nth(1).fill(password)
  await page.getByTestId('login-submit').click()
  await page.waitForFunction(() => Boolean(localStorage.getItem('admin_token')), { timeout: 20000 })
  await page.waitForFunction(() => !window.location.pathname.includes('/login'), { timeout: 20000 })
  await page.waitForLoadState('networkidle')
}

async function chooseBulkPrintTab(page) {
  for (const tab of ['PUBLISHED', 'BLOCKED', 'READY']) {
    await page.getByTestId(`qr-tab-${tab}`).click()
    await page.waitForTimeout(250)
    const printableCount = await page.locator('[data-testid^="qr-select-row-"]:not([disabled])').count()
    if (printableCount >= 2) {
      return tab
    }
  }
  throw new Error('没有找到至少 2 个已生成二维码的批次可用于批量打印预览。')
}

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({
  acceptDownloads: true,
  viewport: { width: 1440, height: 960 }
})
const page = await context.newPage()

try {
  await login(page, 'platform')

  await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-list-page').waitFor()
  await page.getByTestId('batch-filter-code').fill(batchCode)
  await page.getByTestId('batch-search-button').click()
  await page.getByTestId('batch-card-2').waitFor()
  await page.screenshot({ path: screenshotPath('162-ledger-batches-page.png'), fullPage: true })

  const downloadPromise = page.waitForEvent('download')
  await page.getByTestId('batch-export-ledger').click()
  const download = await downloadPromise
  const ledgerPath = path.join(outputDir, `ledger-export-${stamp}.csv`)
  await download.saveAs(ledgerPath)
  const ledgerText = await fs.readFile(ledgerPath, 'utf8')
  const ledgerCsv = parseCsv(ledgerText)

  const requiredHeaders = ['批次名称', '批次编号', '企业', '批次状态', '质检状态', '二维码状态', '分配人', '任务状态', '最近更新时间']
  for (const header of requiredHeaders) {
    if (!ledgerCsv.headers.includes(header)) {
      throw new Error(`导出台账缺少字段：${header}`)
    }
  }
  const exportedRow = ledgerCsv.rows.find((row) => row['批次编号'] === batchCode)
  if (!exportedRow) {
    throw new Error(`导出台账未找到批次 ${batchCode}`)
  }

  await page.goto(`${adminBaseUrl}/qr`, { waitUntil: 'networkidle' })
  await page.getByTestId('qr-publish-page').waitFor()
  const selectedTab = await chooseBulkPrintTab(page)

  const printableCheckboxes = page.locator('[data-testid^="qr-select-row-"]:not([disabled])')
  const selectedItems = []
  for (let index = 0; index < 2; index += 1) {
    const checkbox = printableCheckboxes.nth(index)
    const testId = await checkbox.getAttribute('data-testid')
    const id = Number(String(testId).replace('qr-select-row-', ''))
    const row = page.getByTestId(`qr-row-${id}`)
    const productName = await row.locator('.row-main strong').innerText()
    const batchCodeText = await row.locator('.row-main small').first().innerText()
    selectedItems.push({
      id,
      productName,
      batchCode: batchCodeText
    })
    await checkbox.check()
  }

  await page.screenshot({ path: screenshotPath('163-qr-bulk-selection.png'), fullPage: true })

  const popupPromise = page.waitForEvent('popup')
  await page.getByTestId('qr-bulk-print').click()
  const popup = await popupPromise
  await popup.waitForLoadState('domcontentloaded')
  await popup.waitForFunction(() => {
    const cards = document.querySelectorAll('.qr-card').length
    const images = Array.from(document.images)
    return cards >= 2 && images.length >= 2 && images.every((item) => item.complete)
  }, null, { timeout: 20000 })

  const popupText = await popup.locator('body').innerText()
  for (const item of selectedItems) {
    if (!popupText.includes(item.batchCode)) {
      throw new Error(`打印预览中未找到批次编号：${item.batchCode}`)
    }
  }

  await popup.screenshot({ path: screenshotPath('164-qr-bulk-print-preview.png'), fullPage: true })

  const printPreview = {
    title: await popup.locator('h1').first().innerText(),
    imageCount: await popup.locator('.qr-card-body img').count(),
    bodyText: popupText
  }

  const result = {
    adminBaseUrl,
    traceBaseUrl,
    ledgerExport: {
      filePath: ledgerPath,
      headers: ledgerCsv.headers,
      rowCount: ledgerCsv.rows.length,
      exportedRow
    },
    bulkPrint: {
      tab: selectedTab,
      selectedItems,
      preview: printPreview
    },
    screenshots: screenshotFiles
  }

  const resultPath = path.join(outputDir, 'export-print-round.json')
  await fs.writeFile(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  console.log(`Export & print round saved to ${resultPath}`)
} finally {
  await context.close()
  await browser.close()
}
