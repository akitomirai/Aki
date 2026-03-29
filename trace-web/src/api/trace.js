import http from './http'

export function getTraceDetail(token) {
  return http.get(`/public/traces/${token}`)
}
