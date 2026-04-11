import http from './http'

function withBatchPath(id, path) {
  return `/batches/${id}/${path}`
}

function postBatchPath(id, path, data) {
  return http.post(withBatchPath(id, path), data)
}

function getBatchPath(id, path) {
  return http.get(withBatchPath(id, path))
}

export function getBatchList(params) {
  return http.get('/batches', { params })
}

export function getCompanyOptions(params) {
  return http.get('/batches/lookup/companies', { params })
}

export function getProductOptions(params) {
  return http.get('/batches/lookup/products', { params })
}

export function getOperatorOptions(params) {
  return http.get('/batches/lookup/operators', { params })
}

export function uploadBatchFiles(businessType, files, options = {}) {
  const formData = new FormData()
  formData.append('businessType', businessType)
  for (const file of files) {
    formData.append('files', file)
  }
  return http.post('/batches/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options.onUploadProgress
  })
}

export function cleanupBatchFiles() {
  return http.post('/batches/files/cleanup')
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
  return postBatchPath(id, 'records/quick', data)
}

export function updateBatchAssignment(id, data) {
  return postBatchPath(id, 'assignment', data)
}

export function getFieldDraftList() {
  return http.get('/batches/field-drafts')
}

export function getFieldDraft(id) {
  return getBatchPath(id, 'field-draft')
}

export function saveFieldDraft(id, data) {
  return postBatchPath(id, 'field-draft', data)
}

export function deleteFieldDraft(id) {
  return http.delete(`/batches/${id}/field-draft`)
}

export function createQualityReport(id, data) {
  return postBatchPath(id, 'quality-reports', data)
}

export function createRiskAction(id, data) {
  return postBatchPath(id, 'risk-actions', data)
}

export function generateBatchQr(id) {
  return postBatchPath(id, 'qr')
}

export function changeBatchStatus(id, data) {
  return postBatchPath(id, 'status', data)
}
