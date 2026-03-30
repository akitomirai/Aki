/**
 * 认证相关接口
 */
import http from './http'

/**
 * 登录
 */
export function loginApi(data) {
    return http.post('/auth/login', data)
}

/**
 * 注册
 */
export function registerApi(data) {
    return http.post('/auth/register', data)
}
