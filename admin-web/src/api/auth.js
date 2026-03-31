/**
 * 认证相关接口
 */
import http from './http'

export function loginApi(data) {
  return http.post('/auth/login', data)
}

export function registerApi(data) {
  return http.post('/auth/register', data)
}

export function changePasswordApi(data) {
  return http.post('/auth/change-password', data)
}
