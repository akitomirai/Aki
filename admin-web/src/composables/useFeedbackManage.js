import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { handleFeedbackApi, listFeedbackApi } from '../api/feedback'
import { useAuthStore } from '../stores/auth'

export function useFeedbackManage() {
  const authStore = useAuthStore()
  const loading = ref(false)
  const feedbackList = ref([])
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

  const canHandle = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(authStore.user?.roleCode))

  async function loadFeedbackList() {
    loading.value = true
    try {
      const response = await listFeedbackApi()
      feedbackList.value = Array.isArray(response.data) ? response.data : []
    } catch (error) {
      feedbackList.value = []
      ElMessage.error('反馈列表暂时无法加载')
    } finally {
      loading.value = false
    }
  }

  function openDetailDialog(row) {
    currentDetail.value = row
    detailVisible.value = true
  }

  function openHandleDialog(row) {
    handleForm.id = row.id
    handleForm.status = normalizeStatus(row.status) === 'CLOSED' ? 'CLOSED' : 'PROCESSING'
    handleForm.handleResult = row.handleResult || ''
    handleVisible.value = true
  }

  async function submitHandle() {
    if (!handleForm.id) {
      return
    }
    const handleResult = String(handleForm.handleResult || '').trim()
    if (handleForm.status === 'CLOSED' && handleResult.length < 10) {
      ElMessage.warning('关闭反馈时请填写不少于 10 个字的处理结果')
      return
    }
    handleLoading.value = true
    try {
      await handleFeedbackApi(handleForm.id, {
        status: handleForm.status,
        handleResult
      })
      ElMessage.success('反馈处理已保存')
      handleVisible.value = false
      await loadFeedbackList()
    } catch (error) {
      ElMessage.error('反馈处理暂时无法保存')
    } finally {
      handleLoading.value = false
    }
  }

  function normalizeStatus(status) {
    return String(status || 'PENDING').toUpperCase()
  }

  function getFeedbackTypeText(type) {
    return type || '其他'
  }

  function getStatusText(status) {
    return {
      PENDING: '待处理',
      PROCESSING: '处理中',
      CLOSED: '已处理'
    }[normalizeStatus(status)] || '待处理'
  }

  function getStatusTagType(status) {
    return {
      PENDING: 'warning',
      PROCESSING: 'primary',
      CLOSED: 'success'
    }[normalizeStatus(status)] || 'warning'
  }

  onMounted(loadFeedbackList)

  return {
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
