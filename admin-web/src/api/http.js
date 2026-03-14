/**
 * Axios 实例：
 * - 开发环境走 Vite 代理，避免跨域
 * - 请求时自动带 Authorization
 * - 401 时跳回登录页
 */
import axios from 'axios'

const http = axios.create({
    baseURL: '',
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