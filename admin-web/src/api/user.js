/**
 * 用户管理接口
 */
import http from './http'

/**
 * 分页查询用户
 */
export function listUserApi(params, role) {
  let url = '/api/platform/user/list'
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = '/api/enterprise/user/list'
  } else if (role === 'REGULATOR') {
    url = '/api/regulator/user/list'
  }
  
  return http.get(url, { params })
}

/**
 * 查询已邀请列表
 */
export function listInviteApi(role) {
  let url = '/api/platform/invite-code/invite/list'
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = '/api/enterprise/user/invite/list'
  } else if (role === 'REGULATOR') {
    url = '/api/regulator/user/invite/list'
  }
  
  return http.get(url)
}

/**
 * 创建邀请码
 */
export function createInviteApi(data, role) {
  let url = '/api/platform/invite-code/create'
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = '/api/enterprise/user/invite/create'
  } else if (role === 'REGULATOR') {
    url = '/api/regulator/user/invite/create'
  }
  
  return http.post(url, data)
}

/**
 * 更新用户状态
 */
export function updateUserStatusApi(id, status, role) {
  let url = `/api/platform/user/${id}/status`
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = `/api/enterprise/user/${id}/status`
  } else if (role === 'REGULATOR') {
    url = `/api/regulator/user/${id}/status`
  }
  
  return http.put(url, { status })
}

/**
 * 删除用户
 */
export function deleteUserApi(id, role) {
  let url = `/api/platform/user/${id}`
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = `/api/enterprise/user/${id}`
  } else if (role === 'REGULATOR') {
    url = `/api/regulator/user/${id}`
  }
  
  return http.delete(url)
}

/**
 * 重置用户密码为系统默认密码
 */
export function resetUserPasswordApi(id, role) {
  let url = `/api/platform/user/${id}/password/reset`
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = `/api/enterprise/user/${id}/password/reset`
  } else if (role === 'REGULATOR') {
    url = `/api/regulator/user/${id}/password/reset`
  }
  
  return http.put(url)
}
