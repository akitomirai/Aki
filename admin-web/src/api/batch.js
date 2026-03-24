import http from './http'

export function getBatchList(params) {
  return http.get('/batches', { params })
}

export function getBatchDetail(id) {
  return http.get(`/batches/${id}`)
}

export function createBatch(data) {
  return http.post('/batches', data)
}

export function updateBatch(id, data) {
  return http.patch(`/batches/${id}`, data)
}

export function createTraceRecord(id, data) {
  return http.post(`/batches/${id}/records/quick`, data)
}

export function createQualityReport(id, data) {
  return http.post(`/batches/${id}/quality-reports`, data)
}

export function generateBatchQr(id) {
  return http.post(`/batches/${id}/qr`)
}

export function changeBatchStatus(id, data) {
  return http.post(`/batches/${id}/status`, data)
}
