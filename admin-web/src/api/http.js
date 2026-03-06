/**
 * Axios 实例：
 * - 统一 baseURL
 * - 请求时自动带 Authorization
 * - 401 时跳回登录页
 */
import axios from 'axios'

const http = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000
})

http.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('admin_token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

http.interceptors.response.use(
    (response) => response.data,
    (error) => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('admin_token')
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default http