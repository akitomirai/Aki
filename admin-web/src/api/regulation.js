/**
 * 监管管理接口
 */
import http from './http'

/**
 * 查询批次监管记录列表
 */
export function listRegulationRecordApi(batchId) {
    return http.get(`/api/admin/quality/regulation-record/list/${batchId}`)
}

/**
 * 更新批次监管状态 (监管者/平台管理员使用)
 */
export function updateRegulationStatusApi(data) {
    return http.put('/api/admin/quality/regulation-status/update', data)
}
