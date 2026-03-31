import http from './http'

export function getOperationLogs(params) {
  return http.get('/logs', { params })
}
