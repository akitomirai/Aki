import http from './http'

export function getDashboardOverview() {
  return http.get('/dashboard/overview')
}

export function getDashboardStatistics() {
  return http.get('/dashboard/statistics')
}

export function createDashboardBackup() {
  return http.post('/dashboard/backup')
}

export function getDashboardStatsApi() {
  return getDashboardStatistics()
}
