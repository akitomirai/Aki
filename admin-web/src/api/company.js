import http from './http'

export function listCompanyApi() {
  return http.get('/api/platform/company/list')
}

export function updateCompanyBizRolesApi(companyId, bizRoles) {
  return http.put(`/api/platform/company/${companyId}/biz-roles`, {
    bizRoles
  })
}
