import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBatchDetailApi } from '../api/batch'
import { useAuthStore } from '../stores/auth'
import { extractErrorMessage } from '../utils/feedback'

export function useBatchDetail(batchId) {
    const authStore = useAuthStore()
    const batch = ref(null)
    const loading = ref(false)
    const error = ref('')

    async function loadBatch() {
        loading.value = true
        error.value = ''
        try {
            const res = await getBatchDetailApi(batchId.value, authStore.user?.roleCode)
            if (String(res.code) === '0') {
                batch.value = res.data
            } else {
                error.value = res.message || '批次不存在或已删除'
                ElMessage.error(`查询失败：${error.value}`)
            }
        } catch (err) {
            const msg = extractErrorMessage(err, '批次详情加载失败')
            error.value = msg
            ElMessage.error(`加载失败：${msg}`)
        } finally {
            loading.value = false
        }
    }

    function formatJson(jsonStr) {
        if (!jsonStr) return '-'
        try {
            return JSON.stringify(JSON.parse(jsonStr), null, 2)
        } catch {
            return jsonStr
        }
    }

    return {
        batch,
        loading,
        error,
        loadBatch,
        formatJson
    }
}
