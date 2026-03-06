/**
 * 质检相关接口
 */
import http from './http'

/**
 * 查询批次最新质检校验状态
 */
export function verifyQualityApi(batchId) {
    return http.get('/api/admin/quality/verify', {
        params: { batchId }
    })
}

/**
 * 新增质检报告
 */
export function createQualityReportApi(data) {
    return http.post('/api/admin/quality/report', data)
}