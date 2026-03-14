import { computed, ref, onMounted, reactive } from 'vue'
import { listFeedbackApi, handleFeedbackApi, getFeedbackDetailApi } from '../api/feedback'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

export function useFeedbackManage() {
  const authStore = useAuthStore()
  const roleCode = computed(() => authStore.user?.roleCode || '')
  const canHandle = computed(() => roleCode.value === 'PLATFORM_ADMIN' || roleCode.value === 'ENTERPRISE_ADMIN' || roleCode.value === 'ENTERPRISE_USER' || roleCode.value === 'ADMIN')

  const loading = ref(false)
  const feedbackList = ref([])

  // 处理表单相关
  const handleVisible = ref(false)
  const handleLoading = ref(false)
  const detailVisible = ref(false)
  const detailLoading = ref(false)
  const currentDetail = ref(null)
  const handleForm = reactive({
    id: null,
    status: 'PROCESSING',
    handleResult: ''
  })

  async function loadFeedbackList() {
    loading.value = true
    try {
      const res = await listFeedbackApi(roleCode.value)
      if (res?.code === 0) {
        feedbackList.value = res.data
      } else {
        ElMessage.error(res?.message || '获取反馈列表失败')
      }
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '获取反馈列表失败')
    } finally {
      loading.value = false
    }
  }

  async function openDetailDialog(row) {
    detailVisible.value = true
    detailLoading.value = true
    currentDetail.value = null
    try {
      const res = await getFeedbackDetailApi(row.id, roleCode.value)
      if (res?.code === 0) {
        currentDetail.value = res.data
      } else {
        ElMessage.error(res?.message || '获取反馈详情失败')
      }
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '获取反馈详情失败')
    } finally {
      detailLoading.value = false
    }
  }

  function openHandleDialog(row) {
    if (!canHandle.value) {
      ElMessage.warning('当前角色无权处理反馈')
      return
    }
    handleForm.id = row.id
    handleForm.status = row.status === 'PENDING' ? 'PROCESSING' : row.status
    handleForm.handleResult = row.handleResult || ''
    handleVisible.value = true
  }

  async function submitHandle() {
    if (!canHandle.value) {
      ElMessage.warning('当前角色无权处理反馈')
      return
    }
    if (!handleForm.status) {
      ElMessage.warning('请选择处理状态')
      return
    }
    
    handleLoading.value = true
    try {
      const res = await handleFeedbackApi({
        id: handleForm.id,
        status: handleForm.status,
        handleResult: handleForm.handleResult
      }, roleCode.value)
      
      if (res?.code === 0) {
        ElMessage.success('反馈处理成功')
        handleVisible.value = false
        loadFeedbackList()
      } else {
        ElMessage.error(res?.message || '处理失败')
      }
    } catch (error) {
      ElMessage.error('请求处理失败')
    } finally {
      handleLoading.value = false
    }
  }

  function getFeedbackTypeText(type) {
    const map = {
      SUGGESTION: '建议',
      COMPLAINT: '投诉',
      REPORT_RISK: '举报风险'
    }
    return map[type] || type
  }

  function getStatusText(status) {
    const map = {
      PENDING: '待处理',
      PROCESSING: '处理中',
      CLOSED: '已关闭',
      REJECTED: '已驳回'
    }
    return map[status] || status
  }

  function getStatusTagType(status) {
    const map = {
      PENDING: 'danger',
      PROCESSING: 'warning',
      CLOSED: 'success',
      REJECTED: 'info'
    }
    return map[status] || 'info'
  }

  onMounted(() => {
    loadFeedbackList()
  })

  return {
    roleCode,
    canHandle,
    loading,
    feedbackList,
    handleVisible,
    handleLoading,
    detailVisible,
    detailLoading,
    currentDetail,
    handleForm,
    loadFeedbackList,
    openDetailDialog,
    openHandleDialog,
    submitHandle,
    getFeedbackTypeText,
    getStatusText,
    getStatusTagType
  }
}
