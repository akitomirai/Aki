import http from './http'

export function getDashboardOverview() {
  return http.get('/dashboard/overview')
}
