import { computed, onMounted, ref, reactive } from 'vue'
import { useRoute } from 'vue-router'
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
  getStatusText
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

  const loading = ref(false)
  const errorMessage = ref('')
  const detail = ref(null)

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
        return
      }

      throw new Error(payload?.message || '查询失败')
    } catch (err) {
      console.error('fetch trace detail failed:', err)
      detail.value = null
      errorMessage.value = `查询失败：${extractErrorMessage(err, '追溯信息加载失败')}`
    } finally {
      loading.value = false
    }
  }

  const riskType = computed(() => {
    const msg = detail.value?.riskMessage || ''
    if (!msg) return 'success'
    if (msg.includes('待整改')) return 'warning'
    return 'error'
  })

  const riskTitle = computed(() => {
    return detail.value?.riskMessage ? '风险提示' : '状态正常'
  })

  const riskText = computed(() => {
    return detail.value?.riskMessage || '当前二维码与批次状态正常，可查看公开溯源信息。'
  })

  const productInfoList = computed(() => {
    const d = detail.value || {}
    return [
      { label: '产品名称', value: d.productName || '-' },
      { label: '企业名称', value: d.companyName || '-' },
      { label: '批次编码', value: d.batchCode || '-' },
      { label: '产地信息', value: d.originPlace || '-' },
      { label: '开始日期', value: d.startDate || '-' }
    ]
  })

  const statusInfoList = computed(() => {
    const d = detail.value || {}
    return [
      {
        label: '批次状态',
        text: getStatusText(d.batchStatus),
        raw: d.batchStatus,
        tagType: getStatusTagType(d.batchStatus)
      },
      {
        label: '监管状态',
        text: getStatusText(d.regulationStatus),
        raw: d.regulationStatus,
        tagType: getStatusTagType(d.regulationStatus)
      },
      {
        label: '二维码状态',
        text: getStatusText(d.qrStatus),
        raw: d.qrStatus,
        tagType: getStatusTagType(d.qrStatus)
      },
      {
        label: '对外说明',
        text: d.publicRemark || '暂无说明',
        raw: '',
        tagType: ''
      }
    ]
  })

  const eventList = computed(() => detail.value?.events || [])
  const participantList = computed(() => detail.value?.participants || [])
  const nodeList = computed(() => detail.value?.nodes || [])
  const qualityReportList = computed(() => detail.value?.qualityReports || [])
  const pesticideRecordList = computed(() => detail.value?.pesticideRecords || [])
  const regulationRecordList = computed(() => detail.value?.regulationRecords || [])

  // 反馈表单
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
      const data = {
        batchId: detail.value.batchId,
        qrId: detail.value.qrId,
        feedbackType: feedbackForm.feedbackType,
        content: feedbackForm.content,
        contactName: feedbackForm.contactName,
        contactPhone: feedbackForm.contactPhone
      }
      
      const res = await createFeedback(data)
      if (res?.code === 0) {
        ElMessage.success('反馈提交成功，感谢您的监督！')
        // 重置表单
        feedbackForm.content = ''
        feedbackForm.contactName = ''
        feedbackForm.contactPhone = ''
        feedbackForm.feedbackType = 'SUGGESTION'
      } else {
        throw new Error(res?.message || '提交失败')
      }
    } catch (err) {
      console.error('submit feedback failed:', err)
      ElMessage.error(err?.response?.data?.message || err?.message || '提交失败，请稍后再试')
    } finally {
      submittingFeedback.value = false
    }
  }

  function getEventFields(item) {
    let fields = item?.content?.fields
    // 兼容双重 fields 嵌套情况
    if (fields?.fields) {
      fields = fields.fields
    }
    
    if (!fields || typeof fields !== 'object') {
      return []
    }

    return Object.keys(fields).map((key) => ({
      key,
      label: eventFieldLabelMap[key] || key,
      value: stringifyValue(fields[key])
    }))
  }

  function openReport(item) {
    if (!item?.reportFileUrl) return
    window.open(item.reportFileUrl, '_blank')
  }

  function formatJson(value) {
    if (value == null) return ''
    try {
      return JSON.stringify(value, null, 2)
    } catch (e) {
      return String(value)
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

  function parseNodeContent(content) {
    if (!content) return '-'
    try {
      // 尝试解析 JSON
      const obj = JSON.parse(content)
      if (obj.fields) {
        return Object.entries(obj.fields)
          .map(([k, v]) => `${eventFieldLabelMap[k] || k}: ${v}`)
          .join('; ')
      }
      return content
    } catch (e) {
      // 非 JSON 字符串直接返回
      return content
    }
  }

  onMounted(() => {
    loadTraceDetail()
  })

  return {
    loading,
    errorMessage,
    detail,
    qrToken,
    riskType,
    riskTitle,
    riskText,
    productInfoList,
    statusInfoList,
    eventList,
    participantList,
    nodeList,
    qualityReportList,
    pesticideRecordList,
    regulationRecordList,
    feedbackForm,
    submittingFeedback,
    submitFeedback,
    loadTraceDetail,
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
    openReport,
    formatJson
  }
}
