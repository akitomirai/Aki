import { apiBaseUrl } from './paths.mjs'

export async function loginByApi(request, credentials = { username: 'platform', password: '123456' }) {
  const response = await request.post(`${apiBaseUrl}/auth/login`, {
    data: credentials
  })

  if (!response.ok()) {
    throw new Error(`Failed to login for smoke test: ${response.status()} ${response.statusText()}`)
  }

  const payload = await response.json()
  if (!payload?.success || !payload?.data?.token) {
    throw new Error(`Login payload is invalid for smoke test: ${JSON.stringify(payload)}`)
  }

  return payload.data
}

export async function seedAdminSession(page, request, credentials = { username: 'platform', password: '123456' }) {
  const login = await loginByApi(request, credentials)
  await page.addInitScript(({ token, user }) => {
    localStorage.setItem('admin_token', token)
    localStorage.setItem('admin_user', JSON.stringify(user))
  }, login)
  return login
}

export async function createDraftBatch(request) {
  const login = await loginByApi(request)
  const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
  const batchCode = `R8AUTO${stamp}`
  const response = await request.post(`${apiBaseUrl}/batches`, {
    headers: {
      Authorization: `Bearer ${login.token}`
    },
    data: {
      batchCode,
      productId: 1,
      companyId: 1,
      originPlace: 'Jiangxi Ganzhou Xinfeng Orchard',
      productionDate: '2026-03-25',
      publicRemark: 'Round 8 smoke batch.',
      internalRemark: 'Created by Playwright smoke test.'
    }
  })

  if (!response.ok()) {
    throw new Error(`Failed to create batch for smoke test: ${response.status()} ${response.statusText()}`)
  }

  const payload = await response.json()
  return {
    batchCode,
    batchId: payload.data.batch.id
  }
}
