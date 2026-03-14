/**
 * 消费者反馈接口
 */
import http from './http'

/**
 * 反馈列表
 */
export function listFeedbackApi(role) {
  let url = '/api/admin/feedback/list'
  if (role === 'ENTERPRISE_ADMIN' || role === 'ENTERPRISE_USER' || role === 'ADMIN') {
    url = '/api/enterprise/feedback/list'
  } else if (role === 'REGULATOR') {
    url = '/api/regulator/feedback/list'
  }
  return http.get(url)
}

/**
 * 反馈详情
 */
export function getFeedbackDetailApi(id, role) {
  let url = `/api/admin/feedback/detail/${id}`
  if (role === 'ENTERPRISE_ADMIN' || role === 'ENTERPRISE_USER' || role === 'ADMIN') {
    url = `/api/enterprise/feedback/detail/${id}`
  } else if (role === 'REGULATOR') {
    url = `/api/regulator/feedback/detail/${id}`
  }
  return http.get(url)
}

/**
 * 处理反馈
 */
export function handleFeedbackApi(data, role) {
  let url = '/api/admin/feedback/handle'
  if (role === 'ENTERPRISE_ADMIN' || role === 'ENTERPRISE_USER' || role === 'ADMIN') {
    url = '/api/enterprise/feedback/handle'
  }
  return http.put(url, data)
}
