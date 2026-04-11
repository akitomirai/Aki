import http from './http'

export function loginApi(data) {
  return http.post('/auth/login', data)
}

export function getProfileApi() {
  return http.get('/auth/profile')
}

export function updateProfileApi(data) {
  return http.patch('/auth/profile', data)
}

export function registerApi(data) {
  return http.post('/auth/register', data)
}

export function changePasswordApi(data) {
  return http.post('/auth/change-password', data)
}
