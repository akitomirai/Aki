import http from './http'

export function getCompanyList(params) {
  return http.get('/companies', { params })
}

export function getCompanyDetail(id) {
  return http.get(`/companies/${id}`)
}

export function createCompany(data) {
  return http.post('/companies', data)
}

export function updateCompany(id, data) {
  return http.patch(`/companies/${id}`, data)
}

export function updateCompanyStatus(id, status) {
  return http.post(`/companies/${id}/status`, { status })
}

export function getProductList(params) {
  return http.get('/products', { params })
}

export function getProductDetail(id) {
  return http.get(`/products/${id}`)
}

export function createProduct(data) {
  return http.post('/products', data)
}

export function updateProduct(id, data) {
  return http.patch(`/products/${id}`, data)
}

export function updateProductStatus(id, status) {
  return http.post(`/products/${id}/status`, { status })
}
