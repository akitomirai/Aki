/**
 * 产品管理接口
 */
import http from './http'

export function listProductApi(role) {
  let url = '/api/platform/product/list'
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = '/api/enterprise/product/list'
  } else if (role === 'REGULATOR') {
    url = '/api/regulator/product/list'
  }
  
  return http.get(url)
}

export function getProductDetailApi(id, role) {
  let url = `/api/platform/product/detail/${id}`
  
  if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    url = `/api/enterprise/product/detail/${id}`
  } else if (role === 'REGULATOR') {
    url = `/api/regulator/product/detail/${id}`
  }
  
  return http.get(url)
}

export function createProductApi(data) {
  return http.post('/api/platform/product', data)
}

export function updateProductApi(id, data) {
  return http.put(`/api/platform/product/${id}`, data)
}

