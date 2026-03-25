import { apiBaseUrl } from './paths.mjs'

export async function createDraftBatch(request) {
  const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
  const batchCode = `R8AUTO${stamp}`
  const response = await request.post(`${apiBaseUrl}/batches`, {
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
