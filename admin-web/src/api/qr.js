/**
 * 二维码相关接口
 */
import http from './http'

/**
 * 按批次查询二维码列表（含 PV/UV）
 */
export function listQrApi(batchId) {
    return http.get(`/api/admin/qr/list/${batchId}`)
}

/**
 * 生成二维码
 */
export function generateQrApi(data) {
    return http.post('/api/admin/qr/generate', data)
}

/**
 * 查询二维码近N天趋势
 */
export function getQrTrendApi(qrId, days = 7) {
    return http.get('/api/admin/qr/trend', {
        params: { qrId, days }
    })
}
