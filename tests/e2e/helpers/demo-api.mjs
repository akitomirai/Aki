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

export async function createTraceRecordByApi(request, batchId, overrides = {}, credentials = { username: 'platform', password: '123456' }) {
  const login = await loginByApi(request, credentials)
  const payload = {
    stage: 'PRODUCE',
    title: '答辩演示现场记录',
    eventTime: new Date().toISOString().slice(0, 16),
    operatorName: '答辩演示员',
    location: '答辩演示基地',
    summary: '已完成现场记录补录，准备继续上传质检与生成二维码。',
    imageUrl: '',
    attachmentIds: [],
    visibleToConsumer: true,
    ...overrides
  }

  const response = await request.post(`${apiBaseUrl}/batches/${batchId}/records/quick`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'Content-Type': 'application/json'
    },
    data: payload
  })

  if (!response.ok()) {
    throw new Error(`Failed to create trace record: ${response.status()} ${response.statusText()}`)
  }

  return response.json()
}

export async function getBatchWorkbenchByApi(request, batchId, credentials = { username: 'platform', password: '123456' }) {
  const login = await loginByApi(request, credentials)
  const response = await request.get(`${apiBaseUrl}/batches/${batchId}`, {
    headers: {
      Authorization: `Bearer ${login.token}`
    }
  })

  if (!response.ok()) {
    throw new Error(`Failed to get batch workbench: ${response.status()} ${response.statusText()}`)
  }

  return response.json()
}

export async function assignBatchByApi(request, batchId, assigneeUserId, options = {}, credentials = { username: 'platform', password: '123456' }) {
  const login = await loginByApi(request, credentials)
  const response = await request.post(`${apiBaseUrl}/batches/${batchId}/assignment`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'Content-Type': 'application/json'
    },
    data: {
      assigneeUserId,
      forceClearDraft: Boolean(options.forceClearDraft)
    }
  })

  if (!response.ok()) {
    throw new Error(`Failed to assign batch: ${response.status()} ${response.statusText()}`)
  }

  return response.json()
}
