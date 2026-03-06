/**
 * 文件上传接口
 */
import http from './http'

/**
 * 上传文件
 * @param {FormData} formData
 */
export function uploadFileApi(formData) {
    return http.post('/api/admin/file/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}