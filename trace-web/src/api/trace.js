import http from './http'

/**
 * 查询公开溯源详情
 * @param {string} qrToken
 */
export function getTraceDetail(qrToken) {
  return http.get(`/api/public/trace/detail/${qrToken}`)
}

/**
 * 提交消费者反馈
 * @param {object} data
 */
export function createFeedback(data) {
  return http.post('/api/public/feedback/create', data)
}