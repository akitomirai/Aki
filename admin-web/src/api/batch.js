import http from './http'

export function getBatchList() {
  return http.get('/batches')
}

export function getBatchDetail(id) {
  return http.get(`/batches/${id}`)
}
