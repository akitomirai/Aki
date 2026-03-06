/**
 * 溯源查询接口
 */
import http from './http'

/**
 * 按二维码 token 查询
 */
export function getTraceByTokenApi(qrToken) {
    return http.get(`/api/public/trace/${qrToken}`)
}