import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
      const currentPath = `${window.location.pathname}${window.location.search}`
      const loginPath = window.location.pathname === '/field-entry' || window.location.pathname.startsWith('/mobile-login')
        ? '/mobile-login'
        : '/login'
      if (!['/login', '/mobile-login'].includes(window.location.pathname)) {
        window.location.href = `${loginPath}?redirect=${encodeURIComponent(currentPath)}`
      }
    }
    return Promise.reject(error)
  }
)

export default http
