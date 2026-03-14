import axios from 'axios'

const http = axios.create({
    baseURL: '',
    timeout: 15000
})

http.interceptors.request.use(
    (config) => {
        return config
    },
    (error) => Promise.reject(error)
)

http.interceptors.response.use(
    (response) => {
        const payload = response?.data

        // 兼容后端 code: 0 和 code: "0"
        if (String(payload?.code) === '0') {
            return payload
        }

        return Promise.reject(
            new Error(payload?.message || '请求失败')
        )
    },
    (error) => {
        return Promise.reject(error)
    }
)

export default http