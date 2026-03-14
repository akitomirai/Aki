/**
 * 批次管理接口
 */
import http from './http'

/**
 * 新建批次
 */
export function createBatchApi(data, role) {
    return http.post('/api/enterprise/batch/create', data)
}

/**
 * 批次分页
 */
export function pageBatchApi(params, role) {
    let url = '/api/enterprise/batch/page'

    if (role === 'PLATFORM_ADMIN') {
        url = '/api/platform/batch/page'
    } else if (role === 'REGULATOR') {
        url = '/api/regulator/batch/page'
    }

    return http.get(url, { params })
}

/**
 * 详情查询
 */
export function getBatchDetailApi(id, role) {
    let url = `/api/enterprise/batch/detail/${id}`

    if (role === 'PLATFORM_ADMIN') {
        url = `/api/platform/batch/detail/${id}`
    } else if (role === 'REGULATOR') {
        url = `/api/regulator/batch/detail/${id}`
    }

    return http.get(url)
}

/**
 * 编辑批次（企业端）
 */
export function updateBatchApi(data, role) {
    return http.put('/api/enterprise/batch/update', data)
}

export function listBatchParticipantsApi(batchId, role) {
    let url = `/api/enterprise/batch/participant/list/${batchId}`

    if (role === 'PLATFORM_ADMIN') {
        url = `/api/platform/batch/participant/list/${batchId}`
    }

    return http.get(url)
}

export function saveBatchParticipantsApi(data, role) {
    let url = '/api/enterprise/batch/participant/save'

    if (role === 'PLATFORM_ADMIN') {
        url = '/api/platform/batch/participant/save'
    }

    return http.post(url, data)
}
