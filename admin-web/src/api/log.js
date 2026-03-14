/**
 * 操作日志接口
 */
import http from './http'

/**
 * 分页查询日志
 * 自动根据当前角色请求对应端点的接口
 */
export function pageLogApi(params, role) {
  let url = '/api/platform/log/page'
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = '/api/enterprise/log/page'
  } else if (role === 'REGULATOR') {
    url = '/api/regulator/log/page'
  }
  
  return http.get(url, { params })
}

/**
 * 查询日志详情
 */
export function getLogDetailApi(id, role) {
  let url = `/api/platform/log/detail/${id}`
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = `/api/enterprise/log/detail/${id}`
  } else if (role === 'REGULATOR') {
    url = `/api/regulator/log/detail/${id}`
  }
  return http.get(url)
}
