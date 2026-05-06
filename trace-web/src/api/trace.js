import http from './http'

export function getTraceDetail(token) {
  return http.get(`/public/traces/${token}`)
}

export function submitTraceFeedback(payload) {
  return http.post('/public/feedback', payload)
}
