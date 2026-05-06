import http from './http'

export function listFeedbackApi() {
  return http.get('/feedback')
}

export function handleFeedbackApi(id, data) {
  return http.patch(`/feedback/${id}`, data)
}
