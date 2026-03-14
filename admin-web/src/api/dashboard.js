import http from './http'

export function getDashboardStatsApi(roleCode) {
  const url = roleCode === 'PLATFORM_ADMIN'
    ? '/api/platform/qr/stats/overview'
    : '/api/admin/qr/stats/overview'
  return http.get(url)
}
