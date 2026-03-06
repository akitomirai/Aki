/**
 * trace-web 的 HTTP 客户端
 */
import axios from 'axios'

const http = axios.create({
    baseURL: '',      // ✅ 同域：走当前站点
    timeout: 10000
})

http.interceptors.response.use(
    (response) => response.data,
    (error) => Promise.reject(error)
)

export default http