import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from '../../tests/e2e/node_modules/@playwright/test/index.mjs'

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')
const outputDir = path.join(rootDir, 'output', 'playwright')
await fs.mkdir(outputDir, { recursive: true })

const adminBaseUrl = 'http://127.0.0.1:5174'
const stamp = new Date().toISOString().replace(/[.:TZ-]/g, '').slice(0, 14)
const companyName = `建档回归企业${stamp}`
const productName = `建档回归脐橙${stamp}`
const batchCode = `CHAIN-${stamp.slice(0, 8)}-${stamp.slice(8, 12)}`
const screenshots = []

function screenshotPath(name) {
  const fullPath = path.join(outputDir, name)
  screenshots.push(fullPath)
  return fullPath
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

async function chooseElSelectOption(scope, index, optionText, page) {
  await scope.locator('.el-select').nth(index).click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first()
  await option.waitFor({ state: 'visible' })
  await option.click()
}

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({
  viewport: { width: 1440, height: 960 }
})
const page = await context.newPage()

try {
  await login(page, 'platform')

  await page.goto(`${adminBaseUrl}/companies`, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: '新增企业' }).click()
  const companyDialog = page.getByRole('dialog', { name: '新增企业' })
  await companyDialog.waitFor()
  await page.screenshot({ path: screenshotPath('165-company-create-dialog.png'), fullPage: true })

  await companyDialog.getByPlaceholder('请输入企业名称').fill(companyName)
  await companyDialog.getByPlaceholder('可选，便于备案和回查').fill(`LIC-${stamp}`)
  await companyDialog.getByPlaceholder('请输入联系人姓名').fill('建档联络人')
  await companyDialog.getByPlaceholder('请输入联系电话，至少保留一个可回拨号码').fill('13800138000')
  await companyDialog.getByPlaceholder('请输入联系地址，至少写到园区、仓库或办公地点').fill('江西省赣州市信丰县建档回归示范园 1 号')
  await companyDialog.getByRole('button', { name: '确认新增' }).click()
  await companyDialog.waitFor({ state: 'hidden' })

  await page.locator('input[placeholder="按企业名称、联系人、电话或地址搜索"]').fill(companyName)
  await page.getByRole('button', { name: '查询' }).click()
  await page.locator('.el-table').getByText(companyName).waitFor()
  await page.screenshot({ path: screenshotPath('166-company-created.png'), fullPage: true })

  await page.goto(`${adminBaseUrl}/products`, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: '新增产品' }).click()
  const productDialog = page.getByRole('dialog', { name: '新增产品' })
  await productDialog.waitFor()
  await page.screenshot({ path: screenshotPath('167-product-create-dialog.png'), fullPage: true })

  await chooseElSelectOption(productDialog, 0, companyName, page)
  await productDialog.getByPlaceholder('请输入产品名称').fill(productName)
  await productDialog.getByPlaceholder('可选，便于内部台账和打印标识').fill(`P-${stamp}`)
  await productDialog.getByPlaceholder('如水果、茶叶、粮油').fill('水果')
  await productDialog.getByPlaceholder('请输入主要产地').fill('江西省赣州市信丰县建档回归果园')
  await productDialog.getByPlaceholder('可选，如 5kg / 箱').fill('5kg / 箱')
  await productDialog.getByPlaceholder('可选，如 箱、斤、袋').fill('箱')
  await productDialog.getByRole('button', { name: '确认新增' }).click()
  await productDialog.waitFor({ state: 'hidden' })

  await page.locator('input[placeholder="按产品名称、编码、分类、规格搜索"]').fill(productName)
  await page.getByRole('button', { name: '查询' }).click()
  await page.locator('.el-table').getByText(productName).waitFor()
  await page.screenshot({ path: screenshotPath('168-product-created.png'), fullPage: true })

  await page.goto(`${adminBaseUrl}/batches?mode=ALL`, { waitUntil: 'networkidle' })
  await page.getByTestId('batch-list-page').waitFor()
  await page.getByTestId('batch-create-button').click()
  const batchDialog = page.getByTestId('batch-edit-dialog')
  await batchDialog.waitFor()
  await page.screenshot({ path: screenshotPath('169-batch-create-dialog.png'), fullPage: true })

  const companySelect = batchDialog.locator('select').nth(0)
  await companySelect.selectOption({ label: companyName })
  const productSelect = batchDialog.locator('select').nth(1)
  await productSelect.locator(`option:text-is("${productName}")`).waitFor({ state: 'attached', timeout: 20000 })
  await productSelect.selectOption({ label: productName })

  const batchCodeInput = batchDialog.locator('input[type="text"]').first()
  await batchCodeInput.fill(batchCode)
  const originInput = batchDialog.locator('input[placeholder="例如 江西赣州信丰"]')
  const autoFilledOrigin = await originInput.inputValue()
  if (!autoFilledOrigin) {
    throw new Error('选择产品后未带入产地，批次创建联动未生效。')
  }
  const dateInput = batchDialog.locator('input[type="date"]')
  await dateInput.fill('2026-03-31')
  await batchDialog.getByPlaceholder('填写消费者可见的批次说明，例如产地、工艺特点或本批次情况').fill('建档回归批次，用于验证创建后直达工作台。')
  await batchDialog.getByPlaceholder('用于记录补录计划、处理提醒或内部跟进说明').fill('当前批次刚建档，待补录追溯、上传质检和生成二维码。')

  await page.getByTestId('batch-dialog-submit').click()
  await page.getByTestId('batch-workbench-page').waitFor()
  await page.getByTestId('fresh-batch-banner').waitFor()
  await page.screenshot({ path: screenshotPath('170-batch-workbench-fresh.png'), fullPage: true })

  const title = (await page.locator('.manage-page-title').first().innerText()).trim()
  const meta = (await page.locator('.workbench-meta').innerText()).trim()
  const freshBanner = (await page.getByTestId('fresh-batch-banner').innerText()).trim()
  const todoTexts = await page.locator('.mini-list li').evaluateAll((nodes) => nodes.map((node) => node.textContent?.trim() || ''))
  const statusText = (await page.getByTestId('workbench-next-step-card').innerText()).trim()

  const result = {
    runtime: {
      adminBaseUrl
    },
    created: {
      companyName,
      productName,
      batchCode
    },
    companyCreated: true,
    productCreated: true,
    batchCreated: true,
    workbench: {
      title,
      meta,
      freshBanner,
      todoTexts,
      statusText,
      autoFilledOrigin
    },
    screenshots
  }

  const resultPath = path.join(outputDir, 'create-batch-chain-round.json')
  await fs.writeFile(resultPath, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  console.log(`Create chain round saved to ${resultPath}`)
} finally {
  await context.close()
  await browser.close()
}
