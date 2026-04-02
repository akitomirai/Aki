import { expect, test } from '@playwright/test'
import { loginByApi, seedAdminSession } from '../helpers/demo-api.mjs'
import { adminBaseUrl, apiBaseUrl, saveNamedScreenshot } from '../helpers/paths.mjs'

const platformCredentials = { username: 'platform', password: '123456' }
const enterpriseCredentials = { username: 'enterprise_admin', password: '123456' }

test.describe.configure({ mode: 'serial' })

function uniqueStamp(prefix = 'E2E') {
  return `${prefix}${Date.now()}`
}

async function readJson(response) {
  return {
    ok: response.ok(),
    status: response.status(),
    payload: await response.json()
  }
}

async function apiGet(request, token, path) {
  const response = await request.get(`${apiBaseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
  return readJson(response)
}

async function apiPost(request, token, path, data) {
  const response = await request.post(`${apiBaseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    data
  })
  return readJson(response)
}

async function apiPatch(request, token, path, data) {
  const response = await request.patch(`${apiBaseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    data
  })
  return readJson(response)
}

async function createForeignCompany(request, token, name, licenseNo) {
  const result = await apiPost(request, token, '/companies', {
    name,
    licenseNo,
    contactPerson: 'Scope Seeder',
    contactPhone: '13966660001',
    address: 'Scope Seed Address',
    status: 'ENABLED'
  })

  if (!result.ok || !result.payload?.data?.id) {
    throw new Error(`Failed to create foreign company for scope smoke: ${JSON.stringify(result.payload)}`)
  }

  return result.payload.data.id
}

async function fetchProductByCode(request, token, productCode) {
  const result = await apiGet(request, token, `/products?keyword=${encodeURIComponent(productCode)}`)
  const item = result.payload?.data?.find((entry) => entry.productCode === productCode)
  if (!item) {
    throw new Error(`Failed to find product by code ${productCode}: ${JSON.stringify(result.payload)}`)
  }
  return item
}

async function expectToast(page, text) {
  await expect(page.locator('.el-message__content').filter({ hasText: text }).last()).toBeVisible()
}

test('enterprise admin can open /companies, edit only own company, and cannot create another company', async ({ page, request }) => {
  const enterpriseLogin = await seedAdminSession(page, request, enterpriseCredentials)

  await page.goto(`${adminBaseUrl}/companies`)
  await expect(page.getByTestId('companies-page')).toBeVisible()
  await expect(page.getByTestId('companies-self-mode')).toBeVisible()
  await expect(page.getByTestId('companies-open-create')).toHaveCount(0)
  await expect(page.locator('[data-testid^="company-edit-"]')).toHaveCount(1)

  const stamp = uniqueStamp('COMPANY')
  await page.locator('[data-testid^="company-edit-"]').first().click()

  const dialog = page.getByTestId('company-form-dialog')
  await expect(dialog).toBeVisible()
  await dialog.getByRole('textbox', { name: /联系人/ }).fill(`企业资料回归${stamp}`)
  await dialog.getByRole('textbox', { name: /联系电话/ }).fill('13912345678')
  await dialog.getByRole('textbox', { name: /联系地址/ }).fill(`赣州企业资料回归地址 ${stamp}`)
  await page.getByTestId('company-form-submit').click()

  await expect(dialog).toBeHidden()
  await expectToast(page, '企业资料已更新')

  const ownCompany = await apiGet(request, enterpriseLogin.token, '/companies/1')
  expect(ownCompany.status).toBe(200)
  expect(ownCompany.payload.data.id).toBe(1)
  expect(ownCompany.payload.data.contactPerson).toBe(`企业资料回归${stamp}`)
  expect(ownCompany.payload.data.contactPhone).toBe('13912345678')
  expect(ownCompany.payload.data.address).toBe(`赣州企业资料回归地址 ${stamp}`)

  const createDenied = await apiPost(request, enterpriseLogin.token, '/companies', {
    name: `Enterprise Forbidden Company ${stamp}`,
    licenseNo: `LIC-E2E-COMPANY-${stamp}`,
    contactPerson: 'Mallory',
    contactPhone: '13955550002',
    address: 'Forbidden Address',
    status: 'ENABLED'
  })
  expect(createDenied.status).toBe(403)
  expect(createDenied.payload.message).toBe('企业管理员不能新建企业资料。')

  await saveNamedScreenshot(page, 'round10-companies-enterprise-self-mode')
})

test('enterprise admin can create own product on /products and backend rejects cross-company reassignment with a Chinese message', async ({ page, request }) => {
  const platformLogin = await loginByApi(request, platformCredentials)
  const enterpriseLogin = await seedAdminSession(page, request, enterpriseCredentials)
  const stamp = uniqueStamp('PRODUCT')
  const foreignCompanyId = await createForeignCompany(request, platformLogin.token, `Scope Foreign Company ${stamp}`, `LIC-SCOPE-${stamp}`)

  await page.goto(`${adminBaseUrl}/products`)
  await expect(page.getByTestId('products-page')).toBeVisible()
  await expect(page.getByTestId('products-self-mode')).toBeVisible()
  await expect(page.getByTestId('products-open-create')).toBeVisible()

  await page.getByTestId('products-open-create').click()
  const dialog = page.getByTestId('products-form-dialog')
  await expect(dialog).toBeVisible()
  await dialog.getByPlaceholder('请输入产品名称').fill(`企业自有产品 ${stamp}`)
  await dialog.getByPlaceholder('可选，便于内部台账和打印标识').fill(`ENT-${stamp}`)
  await dialog.getByPlaceholder('如水果、茶叶、粮油').fill('水果')
  await dialog.getByPlaceholder('请输入主要产地').fill(`赣州企业产品产地 ${stamp}`)
  await dialog.getByPlaceholder('可选，如 5kg / 箱').fill('8kg/箱')
  await dialog.getByPlaceholder('可选，如 箱、斤、袋').fill('箱')
  await page.getByTestId('products-form-submit').click()

  await expect(dialog).toBeHidden()
  await expectToast(page, '产品已创建')
  await expect(page.getByTestId('products-page')).toContainText(`企业自有产品 ${stamp}`)

  const ownProduct = await fetchProductByCode(request, enterpriseLogin.token, `ENT-${stamp}`)
  expect(ownProduct.companyId).toBe(1)

  const reassignDenied = await apiPatch(request, enterpriseLogin.token, `/products/${ownProduct.id}`, {
    companyId: foreignCompanyId,
    productName: `企业自有产品 ${stamp}`,
    productCode: `ENT-${stamp}`,
    category: '水果',
    originPlace: `赣州企业产品产地 ${stamp}`,
    coverImage: '/images/products/orange-batch.svg',
    specification: '8kg/箱',
    unit: '箱',
    status: 'ENABLED'
  })
  expect(reassignDenied.status).toBe(403)
  expect(reassignDenied.payload.message).toBe('你只能维护本企业产品，不能改挂到其他企业。')

  const deniedLogs = await apiGet(request, platformLogin.token, '/logs?actionType=PRODUCT_ACCESS_DENIED&result=FAILED&pageSize=50')
  const deniedLog = deniedLogs.payload?.data?.items?.find((item) => item.summary === '你只能维护本企业产品，不能改挂到其他企业。')
  expect(deniedLog?.roleCode).toBe('ENTERPRISE_ADMIN')
  expect(deniedLog?.companyId).toBe(1)

  await saveNamedScreenshot(page, 'round10-products-enterprise-self-mode')
})

test('platform admin still sees full company and product management scope', async ({ page, request }) => {
  const platformLogin = await seedAdminSession(page, request, platformCredentials)

  await page.goto(`${adminBaseUrl}/companies`)
  await expect(page.getByTestId('companies-page')).toBeVisible()
  await expect(page.getByTestId('companies-open-create')).toBeVisible()
  const companyEditCount = await page.locator('[data-testid^="company-edit-"]').count()
  expect(companyEditCount).toBeGreaterThan(1)

  await page.goto(`${adminBaseUrl}/products`)
  await expect(page.getByTestId('products-page')).toBeVisible()
  await expect(page.getByTestId('products-open-create')).toBeVisible()

  const allCompanies = await apiGet(request, platformLogin.token, '/companies')
  const allProducts = await apiGet(request, platformLogin.token, '/products')
  expect((allCompanies.payload?.data ?? []).length).toBeGreaterThan(1)
  expect((allProducts.payload?.data ?? []).length).toBeGreaterThan(1)

  await saveNamedScreenshot(page, 'round10-master-data-platform-scope')
})
