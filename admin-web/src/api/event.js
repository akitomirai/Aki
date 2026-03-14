/**
 * 事件管理接口
 */
import http from './http'

/**
 * 根据批次ID查询事件列表（企业端）
 */
export function listEventApi(batchId, role) {
  return http.get('/api/enterprise/trace-event/list', {
    params: { batchId }
  })
}

/**
 * 新增事件（企业端）
 */
export function createEventApi(data, role) {
  return http.post('/api/enterprise/trace-event/create', data)
}
