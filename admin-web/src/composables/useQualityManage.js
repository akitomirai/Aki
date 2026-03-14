import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFileApi } from '../api/file'
import { createQualityReportApi, getQualityReportDetailApi, listQualityReportApi, updateQualityReportApi } from '../api/quality'
import { useAuthStore } from '../stores/auth'
import { extractErrorMessage } from '../utils/feedback'

const defaultJsonExample = `{
  "items": [
    {
      "name": "农残",
      "value": "合格"
    },
    {
      "name": "重金属",
      "value": "符合限量标准"
    }
  ],
  "remark": "检测结果可公开展示"
}`

const pesticideJsonExample = `{
  "items": [
    {
      "name": "有机磷农药残留",
      "value": "未检出"
    },
    {
      "name": "氯氰菊酯",
      "value": "低于限量值"
    }
  ],
  "remark": "重点农残项目均符合标准"
}`

const microbesJsonExample = `{
  "items": [
    {
      "name": "菌落总数",
      "value": "合格"
    },
    {
      "name": "大肠菌群",
      "value": "未检出"
    }
  ],
  "remark": "微生物检测结果正常"
}`

export const jsonTemplateOptions = [
  { label: '通用模板', value: 'default', desc: '适合常规质检项目录入。' },
  { label: '农残模板', value: 'pesticide', desc: '适合农药残留类质检报告。' },
  { label: '微生物模板', value: 'microbes', desc: '适合菌落总数、大肠菌群等项目。' }
]

function isValidJson(text) {
  try {
    JSON.parse(text)
    return true
  } catch (e) {
    return false
  }
}

function formatJsonText(text) {
  if (!text) return ''
  return JSON.stringify(JSON.parse(text), null, 2)
}

function getTemplateContent(templateKey) {
  const templateMap = {
    default: defaultJsonExample,
    pesticide: pesticideJsonExample,
    microbes: microbesJsonExample
  }
  return templateMap[templateKey] || defaultJsonExample
}

export function useQualityManage() {
  const authStore = useAuthStore()
  const roleCode = computed(() => authStore.user?.roleCode || '')
  const canWrite = computed(() => ['ENTERPRISE_ADMIN', 'ENTERPRISE_USER', 'ADMIN'].includes(roleCode.value))

  const selectedFile = ref(null)
  const uploading = ref(false)
  const submitting = ref(false)
  const uploadedUrl = ref('')

  const form = reactive({
    batchId: '',
    reportNo: '',
    agency: '',
    result: '合格',
    reportFileUrl: '',
    reportJson: defaultJsonExample
  })

  const queryBatchId = ref('')
  const listLoading = ref(false)
  const reportList = ref([])

  const detailDialogVisible = ref(false)
  const detailLoading = ref(false)
  const detailMode = ref('view')
  const currentReport = ref(null)
  const editForm = reactive({
    id: null,
    reportNo: '',
    agency: '',
    result: '',
    reportFileUrl: '',
    reportJson: ''
  })

  function handleFileChange(file) {
    selectedFile.value = file.raw
  }

  async function handleUpload() {
    if (!selectedFile.value) {
      ElMessage.warning('请先选择文件')
      return
    }

    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('biz', 'quality')

    uploading.value = true
    try {
      const res = await uploadFileApi(formData)
      if (String(res.code) === '0') {
        uploadedUrl.value = res.data?.url || ''
        form.reportFileUrl = res.data?.url || ''
        ElMessage.success('文件上传成功')
      } else {
        ElMessage.error(`提交失败：${res.message || '文件上传失败'}`)
      }
    } catch (error) {
      ElMessage.error(`提交失败：${extractErrorMessage(error, '文件上传失败')}`)
    } finally {
      uploading.value = false
    }
  }

  async function handleSubmit() {
    if (!canWrite.value) {
      ElMessage.warning('当前角色无权新增质检报告')
      return
    }
    if (!form.batchId) {
      ElMessage.warning('请输入批次 ID')
      return
    }
    if (!form.result) {
      ElMessage.warning('请选择检测结果')
      return
    }
    if (!form.reportFileUrl) {
      ElMessage.warning('请先上传文件或填写报告 URL')
      return
    }
    if (form.reportJson && !isValidJson(form.reportJson)) {
      ElMessage.error('扩展信息不是合法 JSON，请检查括号、引号和逗号')
      return
    }

    submitting.value = true
    try {
      const res = await createQualityReportApi({
        batchId: form.batchId,
        reportNo: form.reportNo,
        agency: form.agency,
        result: form.result,
        reportFileUrl: form.reportFileUrl,
        reportJson: form.reportJson
      })

      if (String(res.code) === '0') {
        ElMessage.success('质检报告提交成功，已完成哈希存证')
        queryBatchId.value = form.batchId
        await loadReportList()
      } else {
        ElMessage.error(`提交失败：${res.message || '质检报告提交失败'}`)
      }
    } catch (error) {
      ElMessage.error(`提交失败：${extractErrorMessage(error, '质检报告提交失败')}`)
    } finally {
      submitting.value = false
    }
  }

  async function loadReportList() {
    if (!queryBatchId.value) {
      ElMessage.warning('请输入批次 ID')
      return
    }
    listLoading.value = true
    try {
      const res = await listQualityReportApi(queryBatchId.value)
      if (String(res.code) === '0') {
        reportList.value = res.data || []
        if (!reportList.value.length) {
          ElMessage.info('未查询到质检报告')
        }
      } else {
        ElMessage.error(`查询失败：${res.message || '未查询到质检报告'}`)
      }
    } catch (error) {
      ElMessage.error(`加载失败：${extractErrorMessage(error, '质检报告列表加载失败')}`)
    } finally {
      listLoading.value = false
    }
  }

  const fillEditForm = (data) => {
    editForm.id = data?.id || null
    editForm.reportNo = data?.reportNo || ''
    editForm.agency = data?.agency || ''
    editForm.result = data?.result || ''
    editForm.reportFileUrl = data?.reportFileUrl || ''
    editForm.reportJson = data?.reportJson || ''
  }

  async function openDetail(id, mode) {
    detailDialogVisible.value = true
    detailMode.value = mode || 'view'
    detailLoading.value = true
    currentReport.value = null
    try {
      const res = await getQualityReportDetailApi(id)
      if (String(res.code) === '0') {
        currentReport.value = res.data
        fillEditForm(res.data)
      } else {
        ElMessage.error(`加载失败：${res.message || '质检报告详情加载失败'}`)
      }
    } catch (error) {
      ElMessage.error(`加载失败：${extractErrorMessage(error, '质检报告详情加载失败')}`)
    } finally {
      detailLoading.value = false
    }
  }

  async function handleUpdate() {
    if (!canWrite.value) {
      ElMessage.warning('当前角色无权编辑质检报告')
      return
    }
    if (!editForm.id) return
    if (editForm.reportJson && !isValidJson(editForm.reportJson)) {
      ElMessage.error('扩展信息不是合法 JSON，请检查括号、引号和逗号')
      return
    }
    submitting.value = true
    try {
      const res = await updateQualityReportApi({
        id: editForm.id,
        reportNo: editForm.reportNo,
        agency: editForm.agency,
        result: editForm.result,
        reportFileUrl: editForm.reportFileUrl,
        reportJson: editForm.reportJson
      })
      if (String(res.code) === '0') {
        ElMessage.success('修改成功')
        detailDialogVisible.value = false
        await loadReportList()
      } else {
        ElMessage.error(`保存失败：${res.message || '质检报告修改失败'}`)
      }
    } catch (error) {
      ElMessage.error(`保存失败：${extractErrorMessage(error, '质检报告修改失败')}`)
    } finally {
      submitting.value = false
    }
  }

  const formatJsonInput = (targetType, payload) => {
    if (!payload.reportJson) {
      ElMessage.warning('请先输入 JSON 内容')
      return
    }
    try {
      payload.reportJson = formatJsonText(payload.reportJson)
      ElMessage.success(targetType === 'create' ? '新增表单 JSON 已格式化' : '编辑表单 JSON 已格式化')
    } catch (e) {
      ElMessage.error('请输入合法 JSON')
    }
  }

  const fillJsonExample = (targetType, payload) => {
    payload.reportJson = defaultJsonExample
    ElMessage.success(targetType === 'create' ? '已填充新增示例 JSON' : '已填充编辑示例 JSON')
  }

  const applyJsonTemplate = (targetType, payload, templateKey) => {
    payload.reportJson = getTemplateContent(templateKey)
    ElMessage.success(targetType === 'create' ? '已应用模板到新增表单' : '已应用模板到编辑表单')
  }

  const clearJsonInput = (targetType, payload) => {
    payload.reportJson = ''
    ElMessage.success(targetType === 'create' ? '已清空新增 JSON' : '已清空编辑 JSON')
  }

  const isJsonValid = (text) => {
    if (!text) return true
    return isValidJson(text)
  }

  return {
    roleCode,
    canWrite,
    form,
    uploading,
    submitting,
    uploadedUrl,
    queryBatchId,
    listLoading,
    reportList,
    detailDialogVisible,
    detailLoading,
    detailMode,
    currentReport,
    editForm,
    handleFileChange,
    handleUpload,
    handleSubmit,
    jsonTemplateOptions,
    formatJsonInput,
    applyJsonTemplate,
    fillJsonExample,
    clearJsonInput,
    isJsonValid,
    loadReportList,
    openDetail,
    handleUpdate
  }
}
