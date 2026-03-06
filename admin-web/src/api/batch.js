/**
 * 批次管理接口
 */
import http from './http'

/**
 * 新建批次
 */
export function createBatchApi(data) {
    return http.post('/api/admin/batch', data)
}

/**
 * 批次分页
 */
export function pageBatchApi(params) {
    return http.get('/api/admin/batch/page', { params })
}

/**
 * 详情查询
 */
export function getBatchDetailApi(id) {
    return http.get(`/api/admin/batch/${id}`)
}