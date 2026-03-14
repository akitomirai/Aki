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

/**
 * 查询批次质检报告列表
 */
export function listQualityReportApi(batchId) {
    return http.get(`/api/admin/quality/list/${batchId}`)
}

/**
 * 查询质检报告详情
 */
export function getQualityReportDetailApi(id) {
    return http.get(`/api/admin/quality/detail/${id}`)
}

/**
 * 修改质检报告
 */
export function updateQualityReportApi(data) {
    return http.put('/api/admin/quality/update', data)
}
