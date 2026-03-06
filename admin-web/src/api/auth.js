/**
 * 认证相关接口
 */
import http from './http'

/**
 * 登录
 */
export function loginApi(data) {
    return http.post('/api/auth/login', data)
}