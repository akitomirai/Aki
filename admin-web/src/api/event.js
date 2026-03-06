/**
 * 事件管理接口
 */
import http from './http'

/**
 * 根据批次ID查询事件列表
 */
export function listEventApi(batchId) {
    return http.get('/api/admin/event/list', {
        params: { batchId }
    })
}

/**
 * 新增事件
 */
export function createEventApi(data) {
    return http.post('/api/admin/event', data)
}