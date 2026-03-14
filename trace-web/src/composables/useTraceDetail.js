import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTraceDetail, createFeedback } from '../api/trace'
import { ElMessage } from 'element-plus'
import { extractErrorMessage } from '../utils/feedback'
import {
  formatShortDate,
  getBizRoleText,
  getNodeTypeText,
  getQualityResultTagType,
  getQualityResultText,
  getRegulationResultTagType,
  getStageText,
  getStatusTagType,
  getStatusText,
  normalizeDisplayText
} from '../utils/display'

const eventFieldLabelMap = {
  operationName: '作业名称',
  operatorNote: '说明',
  fromAddress: '始发地',
  toAddress: '目的地',
  transportNote: '运输说明',
  processName: '加工环节',
  processResult: '加工结果',
  warehouseName: '仓库名称',
  warehouseNote: '仓储说明',
  saleChannel: '销售渠道',
  saleNote: '销售说明',
  inspectAgency: '检测机构',
  inspectConclusion: '检测结论',
  workType: '作业类型',
  amount: '用量',
  operator: '执行人',
  remark: '备注',
  device: '设备',
  result: '结果',
  location: '地点',
  method: '方式'
}

export function useTraceDetail() {
  const route = useRoute()
  const router = useRouter()

  const loading = ref(false)
  const errorMessage = ref('')
  const detail = ref(null)
  const retryToken = ref('')
  const nodeExpandedMap = ref({})

  const qrToken = computed(() => route.params.qrToken || route.query.token || '')

  async function loadTraceDetail() {
    if (!qrToken.value) {
      errorMessage.value = '追溯码不能为空'
      detail.value = null
      return
    }

    loading.value = true
    errorMessage.value = ''

    try {
      const payload = await getTraceDetail(qrToken.value)
      if (String(payload?.code) === '0') {
        detail.value = payload.data
        nodeExpandedMap.value = {}
        retryToken.value = qrToken.value
        return
      }
      throw new Error(payload?.message || '查询失败')
    } catch (err) {
      detail.value = null
      errorMessage.value = extractErrorMessage(err, '追溯信息加载失败')
    } finally {
      loading.value = false
    }
  }

  const normalizeText = (value, fallback = '-') => normalizeDisplayText(value, fallback)
  const truncateText = (value, length = 52) => {
    const text = normalizeText(value)
    return text.length > length ? `${text.slice(0, length)}...` : text
  }

  const riskType = computed(() => {
    const msg = detail.value?.riskMessage || ''
    if (!msg) return 'success'
    if (msg.includes('整改')) return 'warning'
    return 'error'
  })

  const riskTitle = computed(() => detail.value?.riskMessage ? '风险提示' : '状态正常')
  const riskText = computed(() => detail.value?.riskMessage || '当前追溯档案状态正常，可继续查看公开信息。')

  const pageTitle = computed(() => {
    const productName = normalizeText(detail.value?.productName, '该批次产品')
    return `${productName}追溯档案`
  })

  const pageDescription = computed(() => {
    const batchCode = normalizeText(detail.value?.batchCode, '未提供批次编码')
    return `这是该产品/批次的公开追溯档案，当前可查看批次、关键节点、质检和监管信息。批次编码：${batchCode}`
  })

  const productInfoList = computed(() => {
    const d = detail.value || {}
    return [
      { label: '产品名称', value: normalizeText(d.productName, '未命名产品') },
      { label: '企业名称', value: normalizeText(d.companyName, '暂无企业信息') },
      { label: '批次编码', value: normalizeText(d.batchCode, '未提供') },
      { label: '产地信息', value: normalizeText(d.originPlace, '未填写') },
      { label: '开始日期', value: normalizeText(d.startDate, '未填写') }
    ]
  })

  const statusInfoList = computed(() => {
    const d = detail.value || {}
    return [
      { label: '批次状态', text: getStatusText(d.batchStatus), raw: d.batchStatus, tagType: getStatusTagType(d.batchStatus) },
      { label: '监管状态', text: getStatusText(d.regulationStatus), raw: d.regulationStatus, tagType: getStatusTagType(d.regulationStatus) },
      { label: '追溯码状态', text: getStatusText(d.qrStatus), raw: d.qrStatus, tagType: getStatusTagType(d.qrStatus) },
      { label: '对外说明', text: normalizeText(d.publicRemark, '暂无补充说明'), raw: '', tagType: '' }
    ]
  })

  const eventList = computed(() => detail.value?.events || [])
  const participantList = computed(() => detail.value?.participants || [])
  const nodeList = computed(() => detail.value?.nodes || [])
  const qualityReportList = computed(() => detail.value?.qualityReports || [])
  const regulationRecordList = computed(() => detail.value?.regulationRecords || [])

  const feedbackForm = reactive({
    feedbackType: 'SUGGESTION',
    content: '',
    contactName: '',
    contactPhone: ''
  })
  const submittingFeedback = ref(false)

  async function submitFeedback() {
    if (!detail.value?.batchId) {
      ElMessage.warning('缺少批次信息，无法提交反馈')
      return
    }
    if (!feedbackForm.content.trim()) {
      ElMessage.warning('请输入反馈内容')
      return
    }

    submittingFeedback.value = true
    try {
      const res = await createFeedback({
        batchId: detail.value.batchId,
        qrId: detail.value.qrId,
        feedbackType: feedbackForm.feedbackType,
        content: feedbackForm.content,
        contactName: feedbackForm.contactName,
        contactPhone: feedbackForm.contactPhone
      })
      if (res?.code === 0) {
        ElMessage.success('反馈提交成功，感谢您的监督')
        feedbackForm.content = ''
        feedbackForm.contactName = ''
        feedbackForm.contactPhone = ''
        feedbackForm.feedbackType = 'SUGGESTION'
      } else {
        throw new Error(res?.message || '提交失败')
      }
    } catch (err) {
      ElMessage.error(err?.response?.data?.message || err?.message || '提交失败，请稍后再试')
    } finally {
      submittingFeedback.value = false
    }
  }

  function stringifyValue(value) {
    if (value == null || value === '') return '-'
    if (typeof value === 'object') {
      try {
        return JSON.stringify(value)
      } catch (e) {
        return String(value)
      }
    }
    return String(value)
  }

  function getEventFields(item) {
    let fields = item?.content?.fields
    if (fields?.fields) fields = fields.fields
    if (!fields || typeof fields !== 'object') return []

    return Object.keys(fields).map((key) => ({
      key,
      label: eventFieldLabelMap[key] || key,
      value: stringifyValue(fields[key])
    }))
  }

  function parseNodeContent(content) {
    if (!content) return []
    try {
      const obj = JSON.parse(content)
      const fields = obj.fields || obj
      return Object.entries(fields).map(([key, value], index) => ({
        key: `${key}-${index}`,
        label: eventFieldLabelMap[key] || key,
        fullValue: normalizeText(value),
        shortValue: truncateText(value, 48)
      }))
    } catch (e) {
      return [{
        key: 'raw',
        label: '补充说明',
        fullValue: normalizeText(content),
        shortValue: truncateText(content, 48)
      }]
    }
  }

  function getNodeKey(item) {
    return String(item.id || `${item.title}-${item.eventTime}`)
  }

  function isNodeExpanded(item) {
    return Boolean(nodeExpandedMap.value[getNodeKey(item)])
  }

  function toggleNodeExpanded(item) {
    const key = getNodeKey(item)
    nodeExpandedMap.value = { ...nodeExpandedMap.value, [key]: !nodeExpandedMap.value[key] }
  }

  function openReport(item) {
    if (!item?.reportFileUrl) return
    window.open(item.reportFileUrl, '_blank')
  }

  function goHome() {
    router.push('/')
  }

  function retryWithToken() {
    const token = retryToken.value.trim()
    if (!token) {
      ElMessage.warning('请输入追溯码')
      return
    }
    router.push(`/t/${token}`)
  }

  onMounted(() => {
    retryToken.value = String(qrToken.value || '')
    loadTraceDetail()
  })

  return {
    loading,
    errorMessage,
    detail,
    qrToken,
    retryToken,
    riskType,
    riskTitle,
    riskText,
    pageTitle,
    pageDescription,
    productInfoList,
    statusInfoList,
    eventList,
    participantList,
    nodeList,
    qualityReportList,
    regulationRecordList,
    feedbackForm,
    submittingFeedback,
    submitFeedback,
    loadTraceDetail,
    goHome,
    retryWithToken,
    getStatusText,
    getStatusTagType,
    getQualityResultText,
    getQualityResultTagType,
    getRegulationResultTagType,
    formatShortDate,
    getStageText,
    getBizRoleText,
    getNodeTypeText,
    getEventFields,
    parseNodeContent,
    isNodeExpanded,
    toggleNodeExpanded,
    openReport,
    normalizeText
  }
}
