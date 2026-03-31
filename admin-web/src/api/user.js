import http from './http'

export function getUserList(params) {
  return http.get('/users', { params })
}

export function createUser(data) {
  return http.post('/users', data)
}

export function updateUser(id, data) {
  return http.patch(`/users/${id}`, data)
}

export function updateUserStatus(id, status) {
  return http.post(`/users/${id}/status`, { status })
}

export function resetUserPassword(id, data) {
  return http.post(`/users/${id}/reset-password`, data)
}
