<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminListPagination from '../components/AdminListPagination.vue'
import AdminListTemplate from '../components/AdminListTemplate.vue'
import AdminOverviewCards from '../components/AdminOverviewCards.vue'
import BatchWorkbenchDrawerPanel from '../components/BatchWorkbenchDrawerPanel.vue'
import {
  changeBatchStatus,
  createBatch,
  createQualityReport,
  createTraceRecord,
  generateBatchQr,
  getBatchDetail,
  getBatchList,
  getCompanyOptions,
  getOperatorOptions,
  getProductOptions,
  updateBatchAssignment,
  updateBatch,
  uploadBatchFiles
} from '../api/batch'
import { useAuthStore } from '../stores/auth'
import {
  createBatchCode,
  createQualityForm,
  createTraceForm,
  currentDateTime,
  getFriendlyErrorMessage,
  getFriendlyUploadError,
  getTraceStageProfile,
  qualityOptions,
  splitHighlights,
  stageOptions,
  todayString
} from '../utils/batchExperience'
import { buildTraceLink } from '../utils/display'
import { downloadCsvFile } from '../utils/exportTools'
import { resolveQrStatusText, resolveTaskStatusText, resolveTodayStatusText } from '../utils/statusPresentation'
import { canManageAdminBatch, isRegulator } from '../utils/access'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const message = ref('')
const messageType = ref('info')
const batches = ref([])
const allBatches = ref([])
const DEFAULT_PAGE_SIZE = 10
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]

const filters = ref(createFilterState())
const listMode = ref('ACTION')
const dialog = ref(createDialogState())
const detailDrawer = ref(createBatchDetailDrawerState())
const assignmentDialog = ref(createAssignmentDialogState())
const batchSideDrawerSize = 'min(460px, 92vw)'
const batchForm = ref(createBatchForm())
const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const statusForm = ref(createStatusForm())

const formCompanyOptions = ref([])
const formProductOptions = ref([])
const operatorOptions = ref([])
const operatorLoading = ref(false)
const assignmentSaving = ref(false)
const traceUploading = ref(false)
const qualityUploading = ref(false)
const lastProductOriginPrefill = ref('')
const lastTraceStage = ref('PRODUCE')
const lastHandledCopySourceId = ref('')
const traceDialogContext = ref({
  latestRecord: null,
  totalCount: 0
})

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'FROZEN', label: '已冻结' },
  { value: 'RECALLED', label: '已召回' }
]

const listModes = [
  { value: 'ACTION', label: '待处理' },
  { value: 'READY', label: '可发布' },
  { value: 'RISK', label: '风险批次' },
  { value: 'LIVE', label: '已发布' },
  { value: 'ALL', label: '全部批次' }
]

const companyOptions = computed(() => {
  const source = formCompanyOptions.value.length
    ? formCompanyOptions.value.map((item) => item.name)
    : batches.value.map((item) => item.companyName)
  return [...new Set(source.filter(Boolean))]
})

const selectedProductOption = computed(() => {
  return formProductOptions.value.find((item) => String(item.id) === String(batchForm.value.productId ?? '')) ?? null
})
const selectedCompanyOption = computed(() => {
  return formCompanyOptions.value.find((item) => String(item.id) === String(batchForm.value.companyId ?? '')) ?? null
})
const roleCode = computed(() => authStore.user?.roleCode || '')
const canManageBatch = computed(() => canManageAdminBatch(roleCode.value))
const canManageAssignment = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(roleCode.value))
const readOnlyBatchView = computed(() => isRegulator(roleCode.value))
const pageTitle = computed(() => readOnlyBatchView.value ? '批次查看' : '批次管理')
const pageSubtitle = ''
const listModeOptions = computed(() => {
  const source = readOnlyBatchView.value
    ? listModes.filter((item) => item.value !== 'READY')
    : listModes
  if (!readOnlyBatchView.value) {
    return source
  }
  const labelMap = {
    ACTION: '重点关注',
    RISK: '风险批次',
    LIVE: '已发布',
    ALL: '全部批次'
  }
  return source.map((item) => ({
    ...item,
    label: labelMap[item.value] || item.label
  }))
})

const activeModeMeta = computed(() => {
  const mode = String(listMode.value || 'ACTION').toUpperCase()
  const readOnly = readOnlyBatchView.value

  if (readOnly) {
    return {
      ACTION: {
        title: '重点关注批次',
        copy: '优先查看还在风险处理中、资料未齐或需要继续跟进的批次，答辩时更容易直接讲清楚问题与处置进度。'
      },
      RISK: {
        title: '风险批次看板',
        copy: '集中展示冻结、召回和整改中的批次，适合演示监管视角下的风险识别与后续处置。'
      },
      LIVE: {
        title: '已发布批次',
        copy: '用于回查已经公开的批次，重点看公开状态、质检结论和最近风险动作。'
      },
      ALL: {
        title: '全部批次总览',
        copy: '展示所有批次的当前状态与风险摘要，适合讲解系统全流程覆盖范围。'
      }
    }[mode] ?? {
      title: '重点关注批次',
      copy: '优先查看仍需要继续跟进的批次。'
    }
  }

  return {
    ACTION: {
      title: '待处理优先看板',
      copy: '优先看还没补齐追溯、质检或二维码的批次，适合顺着“下一步”直接演示主链路。'
    },
    READY: {
      title: '可发布看板',
      copy: '这里的批次资料已经基本齐全，适合演示质检、二维码到发布的最后一步闭环。'
    },
    RISK: {
      title: '风险处置看板',
      copy: '集中查看冻结和召回批次，方便讲解风险识别、整改和恢复发布。'
    },
    LIVE: {
      title: '已发布看板',
      copy: '用于回查公开页表现、二维码状态和已发布批次的稳定演示效果。'
    },
    ALL: {
      title: '全部批次总览',
      copy: '展示所有批次的阶段、状态和下一步动作，适合从全局快速切入演示。'
    }
  }[mode] ?? {
    title: '待处理优先看板',
    copy: '优先看当前最值得继续处理的批次。'
  }
})
const currentAssignmentAssigneeId = computed(() => assignmentDialog.value.currentAssigneeUserId ? String(assignmentDialog.value.currentAssigneeUserId) : '')
const selectedAssignmentAssigneeId = computed(() => assignmentDialog.value.assigneeUserId ? String(assignmentDialog.value.assigneeUserId) : '')
const assignmentChanged = computed(() => selectedAssignmentAssigneeId.value !== currentAssignmentAssigneeId.value)
const selectedAssignmentOperator = computed(() => operatorOptions.value.find((item) => String(item.id) === selectedAssignmentAssigneeId.value) ?? null)
const assignmentActionLabel = computed(() => {
  if (!selectedAssignmentAssigneeId.value) return '确认清空分配'
  if (!currentAssignmentAssigneeId.value) return '分配操作员'
  if (assignmentChanged.value) return '确认改派'
  return '当前分配未变更'
})
const assignmentHint = computed(() => {
  if (!assignmentDialog.value.batchId) return ''
  if (assignmentDialog.value.draftPending) {
    const assigneeName = assignmentDialog.value.currentAssigneeName || '原分配人'
    return assignmentDialog.value.draftUpdatedAt
      ? `当前分配人 ${assigneeName} 还有未提交草稿，最近保存于 ${assignmentDialog.value.draftUpdatedAt}。`
      : `当前分配人 ${assigneeName} 还有未提交草稿。`
  }
  if (selectedAssignmentOperator.value) {
    return `将由 ${selectedAssignmentOperator.value.realName || selectedAssignmentOperator.value.username} 接管该批次后续现场作业。`
  }
  return '清空后，该批次会从操作员待办中移除。'
})
const assignmentConfirmActionLabel = computed(() => {
  return assignmentDialog.value.confirmMode === 'clear'
    ? '强制清空分配并清除原分配人草稿'
    : '强制改派并清除原分配人草稿'
})

const traceStageProfile = computed(() => getTraceStageProfile(traceForm.value.stage))

const batchCards = computed(() => {
  return batches.value
    .map((item) => ({
      item,
      insight: buildBatchInsight(item)
    }))
    .sort((left, right) => {
      if (right.insight.priority !== left.insight.priority) {
        return right.insight.priority - left.insight.priority
      }
      return (right.item.id ?? 0) - (left.item.id ?? 0)
      })
  })

const allBatchCards = computed(() => {
  return allBatches.value
    .map((item) => ({
      item,
      insight: buildBatchInsight(item)
    }))
    .sort((left, right) => {
      if (right.insight.priority !== left.insight.priority) {
        return right.insight.priority - left.insight.priority
      }
      return (right.item.id ?? 0) - (left.item.id ?? 0)
    })
})

const visibleBatchCards = computed(() => {
  return batchCards.value.filter(({ item, insight }) => {
    switch (listMode.value) {
      case 'ACTION':
        return insight.isActionable
      case 'READY':
        return insight.readyToPublish
      case 'RISK':
        return insight.isRisk
      case 'LIVE':
        return item.status === 'PUBLISHED'
      default:
        return true
    }
  })
})
const pageCount = computed(() => Math.max(1, Math.ceil(Number(visibleBatchCards.value.length || 0) / Number(pageSize.value || DEFAULT_PAGE_SIZE))))
const paginatedBatchCards = computed(() => {
  const fromIndex = (page.value - 1) * pageSize.value
  return visibleBatchCards.value.slice(fromIndex, fromIndex + pageSize.value)
})

const listStats = computed(() => {
  return {
    total: allBatchCards.value.length,
    actionable: allBatchCards.value.filter((card) => card.insight.isActionable).length,
    ready: allBatchCards.value.filter((card) => card.insight.readyToPublish).length,
    risk: allBatchCards.value.filter((card) => card.insight.isRisk).length
  }
})

const modeCounts = computed(() => ({
  ACTION: listStats.value.actionable,
  READY: listStats.value.ready,
  RISK: listStats.value.risk,
  LIVE: allBatchCards.value.filter((card) => card.item.status === 'PUBLISHED').length,
  ALL: listStats.value.total
}))

const batchOverviewCards = computed(() => {
  const detailMap = {
    ACTION: '优先处理资料未齐或仍需推进的批次',
    READY: '已经接近发布收口，可继续完成发布',
    RISK: '需要重点说明冻结、召回和整改进展',
    LIVE: '方便回查公开页和二维码展示效果',
    ALL: '从全局视角快速看完整批次台账'
  }

  return listModeOptions.value.map((item) => ({
    value: item.value,
    label: item.label,
    count: modeCounts.value[item.value] ?? 0,
    detail: detailMap[item.value] || '查看当前批次集合'
  }))
})
const batchBoardCards = computed(() => {
  return batchOverviewCards.value.map((item) => ({
    key: item.value,
    label: item.label,
    value: item.count,
    detail: item.detail
  }))
})
const listSummary = computed(() => {
  if (!visibleBatchCards.value.length) {
    return '暂无批次数据。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(visibleBatchCards.value.length, page.value * pageSize.value)
  return `共 ${visibleBatchCards.value.length} 个批次，当前显示 ${from}-${to} 个。`
})
const paginationSummary = computed(() => `第 ${page.value} / ${pageCount.value} 页`)

const topQueue = computed(() => {
  return batchCards.value.filter((card) => card.insight.isActionable).slice(0, 3)
})

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'create':
      return '新增批次'
    case 'copy':
      return `复制为新批次：${dialog.value.batchName}`
    case 'edit':
      return `编辑批次：${dialog.value.batchName}`
    case 'trace':
      return `补录追溯：${dialog.value.batchName}`
    case 'quality':
      return `上传质检：${dialog.value.batchName}`
    case 'status':
      return `状态处理：${dialog.value.batchName}`
    default:
      return ''
  }
})

onMounted(async () => {
  syncListModeFromRoute()
  await loadCompanyOptions()
  await fetchBatches()
  await maybeOpenCopyDialogFromRoute()
})

watch(
  () => route.query.mode,
  () => {
    syncListModeFromRoute()
  }
)

watch(
  () => route.query.copyFrom,
  async () => {
    await maybeOpenCopyDialogFromRoute()
  }
)

watch([listMode, filters], () => {
  page.value = 1
}, { deep: true })

watch([visibleBatchCards, pageSize], () => {
  if (page.value > pageCount.value) {
    page.value = pageCount.value
  }
}, { deep: true })

async function fetchBatches() {
  loading.value = true
  try {
    const [response, summaryResponse] = await Promise.all([
      getBatchList(cleanObject(filters.value)),
      getBatchList()
    ])
    batches.value = response.data ?? []
    allBatches.value = summaryResponse.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '批次列表加载失败，请稍后再试。'), 'error')
  } finally {
    loading.value = false
  }
}

async function loadCompanyOptions(keyword = '') {
  try {
    const response = await getCompanyOptions(cleanObject({ keyword }))
    formCompanyOptions.value = response.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '企业选项加载失败。'), 'error')
  }
}

async function loadProductOptions(companyId, keyword = '') {
  if (!companyId) {
    formProductOptions.value = []
    return
  }
  try {
    const response = await getProductOptions(cleanObject({ companyId, keyword }))
    formProductOptions.value = response.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '产品选项加载失败。'), 'error')
  }
}

function createFilterState() {
  return {
    batchCode: '',
    productName: '',
    status: '',
    companyName: '',
    dateFrom: '',
    dateTo: ''
  }
}

function createDialogState() {
  return {
    visible: false,
    type: '',
    batchId: null,
    batchName: '',
    sourceBatchCode: '',
    sourceProductName: ''
  }
}

function createBatchDetailDrawerState() {
  return {
    visible: false,
    batchId: null,
    batchCode: '',
    panel: ''
  }
}

function syncAssignmentDialog(item) {
  assignmentDialog.value = {
    visible: true,
    batchId: item?.id ?? null,
    batchCode: item?.batchCode ?? '',
    companyId: item?.companyId ?? null,
    currentAssigneeUserId: item?.assigneeUserId ? String(item.assigneeUserId) : '',
    currentAssigneeName: item?.assigneeName || '',
    assignedAt: item?.assignedAt || '',
    taskStatus: item?.taskStatus || 'PENDING',
    taskStatusLabel: item?.taskStatusLabel || resolveTaskStatusText(item),
    todayCompleted: Boolean(item?.todayCompleted),
    draftPending: Boolean(item?.draftPending),
    draftStatusLabel: item?.draftStatusLabel || (item?.draftPending ? '草稿待续' : '无草稿'),
    draftUpdatedAt: item?.draftUpdatedAt || '',
    assigneeUserId: item?.assigneeUserId ? String(item.assigneeUserId) : '',
    confirmVisible: false,
    confirmMode: 'reassign',
    confirmMessage: ''
  }
}

async function loadAssignableOperators(companyId) {
  if (!canManageAssignment.value) {
    operatorOptions.value = []
    return
  }
  operatorLoading.value = true
  try {
    const response = await getOperatorOptions(cleanObject({ companyId }))
    operatorOptions.value = response.data ?? []
  } catch (error) {
    operatorOptions.value = []
    showMessage(getFriendlyErrorMessage(error, '操作员列表加载失败，请稍后重试。'), 'error')
  } finally {
    operatorLoading.value = false
  }
}

async function openAssignmentDialog(item) {
  if (!canManageAssignment.value) {
    return
  }
  closeBatchDetailDrawer()
  syncAssignmentDialog(item)
  try {
    const response = await getBatchDetail(item.id)
    const detail = response.data ?? {}
    syncAssignmentDialog({
      ...item,
      companyId: detail.company?.id ?? null,
      assigneeUserId: detail.task?.assigneeUserId ?? item.assigneeUserId,
      assigneeName: detail.task?.assigneeName ?? item.assigneeName,
      assignedAt: detail.task?.assignedAt ?? item.assignedAt,
      taskStatus: detail.task?.taskStatus ?? item.taskStatus,
      taskStatusLabel: detail.task?.taskStatusLabel ?? item.taskStatusLabel,
      todayCompleted: detail.task?.todayCompleted ?? item.todayCompleted,
      draftPending: detail.task?.draftPending ?? item.draftPending,
      draftStatusLabel: detail.task?.draftStatusLabel ?? item.draftStatusLabel,
      draftUpdatedAt: detail.task?.draftUpdatedAt ?? item.draftUpdatedAt
    })
    await loadAssignableOperators(detail.company?.id)
  } catch (error) {
    await loadAssignableOperators(null)
    showMessage(getFriendlyErrorMessage(error, '分配信息加载失败，请稍后重试。'), 'error')
  }
}

function closeAssignmentDialog() {
  assignmentDialog.value = createAssignmentDialogState()
}

async function submitAssignment(forceClearDraft = false) {
  if (!assignmentDialog.value.batchId || !canManageAssignment.value) return
  if (!assignmentChanged.value) {
    showMessage('当前分配未发生变化。', 'info')
    return
  }

  assignmentSaving.value = true
  const assigneeUserId = selectedAssignmentAssigneeId.value ? Number(selectedAssignmentAssigneeId.value) : null
  const nextMode = assigneeUserId == null ? 'clear' : (currentAssignmentAssigneeId.value ? 'reassign' : 'assign')

  try {
    await updateBatchAssignment(assignmentDialog.value.batchId, {
      assigneeUserId,
      forceClearDraft
    })
    await fetchBatches()
    if (assigneeUserId == null) {
      showMessage(forceClearDraft ? '已强制清空分配，并清除原分配人的未提交草稿。' : '已清空当前批次分配。', 'success')
    } else if (nextMode === 'assign') {
      showMessage('操作员已分配到当前批次。', 'success')
    } else {
      showMessage(forceClearDraft ? '已强制改派，并清除原分配人的未提交草稿。' : '操作员已改派。', 'success')
    }
    closeAssignmentDialog()
  } catch (error) {
    const nextMessage = getFriendlyErrorMessage(error, '任务分配更新失败，请稍后重试。')
    if (!forceClearDraft && nextMessage.includes('存在未提交草稿')) {
      assignmentDialog.value.confirmVisible = true
      assignmentDialog.value.confirmMode = nextMode === 'clear' ? 'clear' : 'reassign'
      assignmentDialog.value.confirmMessage = nextMessage
      showMessage('该批次当前分配人存在未提交草稿，请确认是否继续强制改派。', 'error')
      return
    }
    showMessage(nextMessage, 'error')
  } finally {
    assignmentSaving.value = false
  }
}

async function clearAssignment() {
  if (!assignmentDialog.value.currentAssigneeUserId) {
    assignmentDialog.value.assigneeUserId = ''
    showMessage('当前批次本来就是未分配状态。', 'info')
    return
  }
  assignmentDialog.value.assigneeUserId = ''
  await submitAssignment(false)
}

function cancelAssignmentConfirm() {
  assignmentDialog.value.confirmVisible = false
  assignmentDialog.value.confirmMode = 'reassign'
  assignmentDialog.value.confirmMessage = ''
  assignmentDialog.value.assigneeUserId = assignmentDialog.value.currentAssigneeUserId
}

async function forceAssignmentChange() {
  assignmentDialog.value.confirmVisible = false
  await submitAssignment(true)
}

function createAssignmentDialogState() {
  return {
    visible: false,
    batchId: null,
    batchCode: '',
    companyId: null,
    currentAssigneeUserId: '',
    currentAssigneeName: '',
    assignedAt: '',
    taskStatus: 'PENDING',
    taskStatusLabel: '待处理',
    todayCompleted: false,
    draftPending: false,
    draftStatusLabel: '无草稿',
    draftUpdatedAt: '',
    assigneeUserId: '',
    confirmVisible: false,
    confirmMode: 'reassign',
    confirmMessage: ''
  }
}

function createBatchForm() {
  return {
    batchCode: createBatchCode(),
    productId: null,
    companyId: null,
    originPlace: '',
    productionDate: todayString(),
    publicRemark: '',
    internalRemark: ''
  }
}

function batchDialogGuideText() {
  if (dialog.value.type === 'copy') {
    return '会继承企业、产品、产地和基础备注，但不会带入旧追溯、质检、二维码、风险、分配和草稿。'
  }
  if (dialog.value.type === 'edit') {
    return '保存后会回到当前批次工作台，继续补录追溯、上传质检和生成二维码。'
  }
  return '先选企业，再选产品，保存后会直接进入新批次工作台继续补录。'
}

function clearCopyQuery() {
  if (!route.query.copyFrom) {
    return
  }
  const nextQuery = { ...route.query }
  delete nextQuery.copyFrom
  router.replace({
    path: route.path,
    query: nextQuery
  })
}

async function maybeOpenCopyDialogFromRoute() {
  const copyFrom = String(route.query.copyFrom || '').trim()
  if (!copyFrom || copyFrom === lastHandledCopySourceId.value) {
    return
  }
  lastHandledCopySourceId.value = copyFrom
  await openCopyDialog({ id: Number(copyFrom) }, { fromRoute: true })
}

function localizeVisibleText(text) {
  const value = String(text || '').trim()
  if (!value) {
    return ''
  }
  return {
    'Xinfeng Orchard Base': '江西省赣州市信丰果园基地',
    'Wuyuan Tea Base': '江西省上饶市婺源县茶园基地'
  }[value] ?? value
}

function createStatusForm(targetStatus = 'PUBLISHED') {
  return {
    targetStatus,
    reason: defaultReason(targetStatus),
    operatorName: '企业管理员'
  }
}

function cleanObject(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== null && value !== undefined && value !== '')
  )
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function exportTimestamp() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

function latestUpdatedText(item) {
  return item.lastUpdatedAt || item.latestTraceTime || item.productionDate || '暂无更新'
}

function riskStatusText(item) {
  return item.riskStatusLabel || '当前无风险'
}

function latestRiskActionText(item) {
  return item.latestRiskActionLabel || '暂无风险动作'
}

function openWorkbenchLabel() {
  return readOnlyBatchView.value ? '查看详情' : '查看工作台'
}

function qualityStatusText(item) {
  return item.qualityStatus || '待上传质检'
}

function qrStatusText(item) {
  return resolveQrStatusText({ statusLabel: item.qrStatusLabel, status: item.qrStatus })
}

function qualityTone(item) {
  const value = String(item.qualityStatus || '').toUpperCase()
  if (/FAIL|不合格/.test(value)) return 'danger'
  if (!value || /PENDING|待上传/.test(value)) return 'pending'
  return 'success'
}

function qrTone(item) {
  return item.qrStatus && item.qrStatus !== 'NOT_GENERATED' ? 'info' : 'pending'
}

function riskTone(item) {
  if (String(item.status || '').toUpperCase() === 'RECALLED') return 'danger'
  if (String(item.status || '').toUpperCase() === 'FROZEN') return 'warning'
  return item.riskStatusLabel && item.riskStatusLabel !== '当前无风险' ? 'warning' : 'normal'
}

function batchStatusChips(item) {
  const chips = [
    {
      label: item.statusLabel || '状态待确认',
      tone: statusClass(item.status)
    },
    {
      label: qualityStatusText(item),
      tone: qualityTone(item)
    }
  ]

  if (item.riskStatusLabel && item.riskStatusLabel !== '当前无风险') {
    chips.push({
      label: item.riskStatusLabel,
      tone: riskTone(item)
    })
  } else {
    chips.push({
      label: qrStatusText(item),
      tone: qrTone(item)
    })
  }

  return chips.slice(0, 3)
}

function normalizeBatchActionCode(code) {
  return String(code || '').trim().toUpperCase()
}

function actionEnabled(item, code) {
  return Boolean(actionOf(item, code).enabled)
}

function canOpenPublicTrace(item) {
  return Boolean(item?.qrToken) && actionEnabled(item, 'VIEW_PUBLIC')
}

function openPublicTrace(item) {
  if (!canOpenPublicTrace(item)) {
    return false
  }
  window.open(buildTraceLink(item.qrToken), '_blank', 'noopener')
  return true
}

function openBatchDetail(item, panel = '') {
  const normalizedPanel = String(panel || '').trim().toLowerCase()
  closeAssignmentDialog()
  detailDrawer.value = {
    visible: true,
    batchId: item?.id ?? null,
    batchCode: item?.batchCode ?? '',
    panel: normalizedPanel
  }
}

function closeBatchDetailDrawer() {
  detailDrawer.value = createBatchDetailDrawerState()
}

function showRecommendedAction(card) {
  return !readOnlyBatchView.value && String(card?.insight?.nextActionCode || '').toUpperCase() !== 'WORKBENCH'
}

function isRiskPanelActionCode(code) {
  return ['RISK_COMMENT', 'RISK_RECTIFICATION', 'RISK_PROCESSING', 'RISK_RECTIFIED'].includes(normalizeBatchActionCode(code))
}

function resolveBackendRecommendedListAction(item, fallbackCode = '') {
  const backendCode = normalizeBatchActionCode(item?.recommendedActionCode)
  if (!backendCode) {
    return fallbackCode
  }
  if (backendCode === 'ADD_TRACE' && actionEnabled(item, 'ADD_TRACE')) return 'ADD_TRACE'
  if (backendCode === 'UPLOAD_QUALITY' && actionEnabled(item, 'UPLOAD_QUALITY')) return 'UPLOAD_QUALITY'
  if (backendCode === 'GENERATE_QR' && actionEnabled(item, 'GENERATE_QR')) return 'GENERATE_QR'
  if (backendCode === 'PUBLISH' && actionEnabled(item, 'PUBLISH')) return 'PUBLISH'
  if (backendCode === 'RESUME' && actionEnabled(item, 'RESUME')) return 'RESUME'
  if (backendCode === 'VIEW_PUBLIC' && canOpenPublicTrace(item)) return 'VIEW_PUBLIC'
  if (isRiskPanelActionCode(backendCode)) return 'RISK_PANEL'
  return fallbackCode
}

function fallbackActionLabel(code) {
  return {
    ADD_TRACE: '补录追溯',
    UPLOAD_QUALITY: '上传质检',
    GENERATE_QR: '生成二维码',
    PUBLISH: '发布批次',
    RESUME: '恢复发布',
    VIEW_PUBLIC: '查看公开页',
    RISK_PANEL: '风险处理',
    WORKBENCH: openWorkbenchLabel()
  }[code] ?? '继续处理'
}

function blockedActionHint(item, code) {
  return actionOf(item, code).hint || '当前批次暂时不能执行这个操作。'
}

function missingSummary(card) {
  if (card.insight.missing.length) {
    return `仍需补齐：${card.insight.missing.map((item) => item.label.replace(/^待/, '')).join('、')}`
  }
  if (String(card.item.status || '').toUpperCase() === 'PUBLISHED') {
    return '资料已公开，可直接讲解扫码查询与回查能力。'
  }
  if (card.insight.readyToPublish) {
    return '关键资料已齐，可以直接进入发布演示。'
  }
  return '当前资料已齐，建议回工作台复核状态。'
}

function recommendedActionClass(card) {
  if (readOnlyBatchView.value) {
    return 'ghost'
  }
  if (card.insight.nextActionCode === 'PUBLISH' || card.insight.nextActionCode === 'RESUME') {
    return 'success'
  }
  if (card.insight.nextActionCode === 'VIEW_PUBLIC' || card.insight.nextActionCode === 'WORKBENCH') {
    return 'ghost'
  }
  if (card.insight.nextActionCode === 'RISK_PANEL' || card.insight.isRisk) {
    return 'warning'
  }
  return 'primary'
}

function exportCurrentLedger() {
  const rows = visibleBatchCards.value.map(({ item }) => ({
    productName: item.productName || '未命名批次',
    batchCode: item.batchCode || '',
    companyName: item.companyName || '',
    statusLabel: item.statusLabel || '草稿',
    qualityStatus: item.qualityStatus || '待上传',
    qrStatus: resolveQrStatusText({ statusLabel: item.qrStatusLabel, status: item.qrStatus }),
    assigneeName: item.assigneeName || '未分配操作员',
    taskStatusLabel: resolveTaskStatusText(item),
    updatedAt: latestUpdatedText(item)
  }))

  if (!rows.length) {
    showMessage('当前筛选结果没有可导出的批次。', 'info')
    return
  }

  downloadCsvFile({
    filename: `批次台账-${exportTimestamp()}.csv`,
    columns: [
      { key: 'productName', label: '批次名称' },
      { key: 'batchCode', label: '批次编号' },
      { key: 'companyName', label: '企业' },
      { key: 'statusLabel', label: '批次状态' },
      { key: 'qualityStatus', label: '质检状态' },
      { key: 'qrStatus', label: '二维码状态' },
      { key: 'assigneeName', label: '分配人' },
      { key: 'taskStatusLabel', label: '任务状态' },
      { key: 'updatedAt', label: '最近更新时间' }
    ],
    rows
  })
  showMessage(`批次台账已导出，共 ${rows.length} 条。`, 'success')
}

function resetFilters() {
  filters.value = createFilterState()
  page.value = 1
  pageSize.value = DEFAULT_PAGE_SIZE
  switchListMode('ACTION')
  fetchBatches()
}

function handlePageSizeChange(value) {
  const nextPageSize = Number(value || DEFAULT_PAGE_SIZE)
  if (nextPageSize === pageSize.value) {
    return
  }
  pageSize.value = nextPageSize
  page.value = 1
}

function goPrevPage() {
  if (loading.value || page.value <= 1) return
  page.value -= 1
}

function goNextPage() {
  if (loading.value || page.value >= pageCount.value) return
  page.value += 1
}

function syncListModeFromRoute() {
  const mode = String(route.query.mode || '').toUpperCase()
  const exists = listModeOptions.value.some((item) => item.value === mode)
  listMode.value = exists ? mode : 'ACTION'
}

function switchListMode(mode) {
  listMode.value = mode
  page.value = 1
  const nextQuery = { ...route.query }
  if (!mode || mode === 'ACTION') {
    delete nextQuery.mode
  } else {
    nextQuery.mode = mode
  }
  router.replace({
    path: route.path,
    query: nextQuery
  })
}

async function openCreateDialog() {
  await loadCompanyOptions()
  batchForm.value = createBatchForm()
  formProductOptions.value = []
  lastProductOriginPrefill.value = ''
  dialog.value = {
    visible: true,
    type: 'create',
    batchId: null,
    batchName: ''
  }
}

async function openCopyDialog(item, options = {}) {
  const { fromRoute = false } = options
  try {
    const response = await getBatchDetail(item.id)
    const detail = response.data
    await loadCompanyOptions()
    await loadProductOptions(detail.company.id)
    batchForm.value = {
      batchCode: createBatchCode(),
      productId: detail.product.id,
      companyId: detail.company.id,
      originPlace: detail.batch.originPlace,
      productionDate: todayString(),
      publicRemark: detail.batch.publicRemark ?? '',
      internalRemark: detail.batch.internalRemark ?? ''
    }
    lastProductOriginPrefill.value = detail.batch.originPlace || detail.product.originPlace || ''
    dialog.value = {
      visible: true,
      type: 'copy',
      batchId: item.id,
      batchName: detail.batch.batchCode,
      sourceBatchCode: detail.batch.batchCode,
      sourceProductName: detail.product.name
    }
    if (fromRoute) {
      clearCopyQuery()
    }
  } catch (error) {
    if (fromRoute) {
      clearCopyQuery()
    }
    showMessage(getFriendlyErrorMessage(error, '复制源批次加载失败，请稍后再试。'), 'error')
  }
}

async function openEditDialog(item) {
  try {
    const response = await getBatchDetail(item.id)
    const detail = response.data
    await loadCompanyOptions()
    await loadProductOptions(detail.company.id)
    batchForm.value = {
      batchCode: detail.batch.batchCode,
      productId: detail.product.id,
      companyId: detail.company.id,
      originPlace: detail.batch.originPlace,
      productionDate: detail.batch.productionDate,
      publicRemark: detail.batch.publicRemark ?? '',
      internalRemark: detail.batch.internalRemark ?? ''
    }
    lastProductOriginPrefill.value = detail.product.originPlace || detail.batch.originPlace || ''
    dialog.value = {
      visible: true,
      type: 'edit',
      batchId: item.id,
      batchName: item.batchCode
    }
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '批次详情加载失败。'), 'error')
  }
}

async function openTraceDialog(item) {
  traceForm.value = createTraceForm()
  traceDialogContext.value = {
    latestRecord: null,
    totalCount: 0
  }
  lastTraceStage.value = traceForm.value.stage
  dialog.value = {
    visible: true,
    type: 'trace',
    batchId: item.id,
    batchName: item.batchCode
  }

  try {
    const response = await getBatchDetail(item.id)
    const latestRecord = response.data?.trace?.recentRecords?.[0] ?? null
    traceDialogContext.value = {
      latestRecord,
      totalCount: response.data?.trace?.totalCount ?? 0
    }
    if (latestRecord) {
      traceForm.value = createTraceForm({
        stage: latestRecord.stageCode,
        operatorName: latestRecord.operatorName,
        location: latestRecord.location
      })
      lastTraceStage.value = traceForm.value.stage
    }
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '已打开补录窗口，但上一条记录未能加载。'), 'info')
  }
}

function openQualityDialog(item) {
  qualityForm.value = createQualityForm()
  dialog.value = {
    visible: true,
    type: 'quality',
    batchId: item.id,
    batchName: item.batchCode
  }
}

function openStatusDialog(item, targetStatus) {
  statusForm.value = createStatusForm(targetStatus)
  dialog.value = {
    visible: true,
    type: 'status',
    batchId: item.id,
    batchName: item.batchCode
  }
}

function closeDialog() {
  if (dialog.value.type === 'copy') {
    clearCopyQuery()
  }
  dialog.value = createDialogState()
}

function validateBatchForm() {
  if (!String(batchForm.value.batchCode || '').trim()) {
    return '请填写批次编号，后续台账、二维码和公开页都会用它关联。'
  }
  if (!batchForm.value.companyId) {
    return '请先选择企业，再继续选择产品和创建批次。'
  }
  if (!batchForm.value.productId) {
    return '请先选择归属产品，批次必须明确挂到具体产品下。'
  }
  if (!String(batchForm.value.productionDate || '').trim()) {
    return '请补充关键日期，至少填写生产日期。'
  }
  if (!String(batchForm.value.originPlace || '').trim()) {
    return '请补充产地，工作台和公开页回查时都会用到。'
  }
  return ''
}

async function submitDialog(options = {}) {
  const { keepOpen = false } = options

  try {
    if (dialog.value.type === 'create' || dialog.value.type === 'copy') {
      const validationMessage = validateBatchForm()
      if (validationMessage) {
        showMessage(validationMessage, 'error')
        return
      }
      const response = await createBatch({
        batchCode: batchForm.value.batchCode,
        productId: batchForm.value.productId,
        companyId: batchForm.value.companyId,
        originPlace: batchForm.value.originPlace,
        productionDate: batchForm.value.productionDate,
        publicRemark: batchForm.value.publicRemark,
        internalRemark: batchForm.value.internalRemark
      })
      const isCopyDialog = dialog.value.type === 'copy'
      const copySourceBatchCode = dialog.value.sourceBatchCode
      const createdBatchCode = response.data?.batch?.batchCode || batchForm.value.batchCode
      if (isCopyDialog) {
        showMessage(`已基于批次 ${copySourceBatchCode} 复制出新批次 ${createdBatchCode}。`, 'success')
      } else {
        showMessage('批次已创建，工作台已打开，可继续补录。', 'success')
      }
      closeDialog()
      await fetchBatches()
      openBatchDetail(response.data.batch)
      return
    }

    if (dialog.value.type === 'edit') {
      const validationMessage = validateBatchForm()
      if (validationMessage) {
        showMessage(validationMessage, 'error')
        return
      }
      const response = await updateBatch(dialog.value.batchId, {
        productId: batchForm.value.productId,
        companyId: batchForm.value.companyId,
        originPlace: batchForm.value.originPlace,
        productionDate: batchForm.value.productionDate,
        publicRemark: batchForm.value.publicRemark,
        internalRemark: batchForm.value.internalRemark
      })
      showMessage('批次资料已更新。', 'success')
      closeDialog()
      await fetchBatches()
      openBatchDetail(response.data.batch)
      return
    }

    if (dialog.value.type === 'trace') {
      const response = await createTraceRecord(dialog.value.batchId, {
        stage: traceForm.value.stage,
        title: traceForm.value.title,
        eventTime: traceForm.value.eventTime,
        operatorName: traceForm.value.operatorName,
        location: traceForm.value.location,
        summary: traceForm.value.summary,
        imageUrl: traceForm.value.imageUrl,
        attachmentIds: traceForm.value.attachmentIds,
        visibleToConsumer: traceForm.value.visibleToConsumer
      })

      await fetchBatches()
      traceDialogContext.value.latestRecord = response.data?.trace?.recentRecords?.[0] ?? null
      traceDialogContext.value.totalCount = response.data?.trace?.totalCount ?? traceDialogContext.value.totalCount

      if (keepOpen) {
        traceForm.value = createTraceForm({
          stage: traceDialogContext.value.latestRecord?.stageCode ?? traceForm.value.stage,
          operatorName: traceDialogContext.value.latestRecord?.operatorName ?? traceForm.value.operatorName,
          location: traceDialogContext.value.latestRecord?.location ?? traceForm.value.location,
          title: traceDialogContext.value.latestRecord?.title ?? getTraceStageProfile(traceForm.value.stage).defaultTitle,
          summary: getTraceStageProfile(traceForm.value.stage).summaryTemplates[0]
        })
        lastTraceStage.value = traceForm.value.stage
        showMessage('这条记录已保存，你可以继续补录下一条。', 'success')
        return
      }

      showMessage('追溯记录已补录。', 'success')
      closeDialog()
      return
    }

    if (dialog.value.type === 'quality') {
      await createQualityReport(dialog.value.batchId, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlights(qualityForm.value.highlightsText),
        attachmentIds: qualityForm.value.attachmentIds
      })
      showMessage('质检摘要已上传。', 'success')
      closeDialog()
      await fetchBatches()
      return
    }

    if (dialog.value.type === 'status') {
      const response = await changeBatchStatus(dialog.value.batchId, statusForm.value)
      showMessage('批次状态已更新。', 'success')
      closeDialog()
      await fetchBatches()
      openBatchDetail(response.data.batch)
    }
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error), 'error')
  }
}

async function handleGenerateQr(item) {
  try {
    await generateBatchQr(item.id)
    showMessage('二维码已就绪。', 'success')
    await fetchBatches()
    openBatchDetail(item, 'qr')
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码生成失败，请稍后再试。'), 'error')
  }
}

async function handleBatchCompanyChange(resetProduct = true) {
  const companyId = batchForm.value.companyId
  if (resetProduct) {
    if (batchForm.value.originPlace === lastProductOriginPrefill.value) {
      batchForm.value.originPlace = ''
    }
    batchForm.value.productId = null
    lastProductOriginPrefill.value = ''
  }
  await loadProductOptions(companyId)
}

function handleBatchProductChange() {
  const productOrigin = String(selectedProductOption.value?.originPlace || '').trim()
  if (productOrigin && (!batchForm.value.originPlace || batchForm.value.originPlace === lastProductOriginPrefill.value)) {
    batchForm.value.originPlace = productOrigin
  }
  lastProductOriginPrefill.value = productOrigin
}

async function handleTraceFilesChange(event) {
  const files = [...(event.target.files ?? [])]
  if (!files.length) {
    return
  }
  traceUploading.value = true
  try {
    const response = await uploadBatchFiles('trace-image', files)
    const uploadedFiles = response.data ?? []
    traceForm.value.uploadedFiles = [...traceForm.value.uploadedFiles, ...uploadedFiles]
    traceForm.value.attachmentIds = traceForm.value.uploadedFiles.map((item) => item.id)
    if (!traceForm.value.imageUrl && uploadedFiles[0]?.fileUrl) {
      traceForm.value.imageUrl = uploadedFiles[0].fileUrl
    }
    showMessage(`图片已上传 ${uploadedFiles.length} 个，保存记录后会一起绑定。`, 'success')
  } catch (error) {
    showMessage(getFriendlyUploadError(error, '补录图片'), 'error')
  } finally {
    traceUploading.value = false
    event.target.value = ''
  }
}

async function handleQualityFilesChange(event) {
  const files = [...(event.target.files ?? [])]
  if (!files.length) {
    return
  }
  qualityUploading.value = true
  try {
    const response = await uploadBatchFiles('quality-attachment', files)
    const uploadedFiles = response.data ?? []
    qualityForm.value.uploadedFiles = [...qualityForm.value.uploadedFiles, ...uploadedFiles]
    qualityForm.value.attachmentIds = qualityForm.value.uploadedFiles.map((item) => item.id)
    showMessage(`质检附件已上传 ${uploadedFiles.length} 个。`, 'success')
  } catch (error) {
    showMessage(getFriendlyUploadError(error, '质检附件'), 'error')
  } finally {
    qualityUploading.value = false
    event.target.value = ''
  }
}

function removeTraceAttachment(fileId) {
  traceForm.value.uploadedFiles = traceForm.value.uploadedFiles.filter((item) => item.id !== fileId)
  traceForm.value.attachmentIds = traceForm.value.uploadedFiles.map((item) => item.id)
  if (traceForm.value.imageUrl && !traceForm.value.uploadedFiles.some((item) => item.fileUrl === traceForm.value.imageUrl)) {
    traceForm.value.imageUrl = traceForm.value.uploadedFiles[0]?.fileUrl ?? ''
  }
}

function removeQualityAttachment(fileId) {
  qualityForm.value.uploadedFiles = qualityForm.value.uploadedFiles.filter((item) => item.id !== fileId)
  qualityForm.value.attachmentIds = qualityForm.value.uploadedFiles.map((item) => item.id)
}

function defaultReason(targetStatus) {
  return {
    PUBLISHED: '质检与二维码已齐，准备对外发布。',
    FROZEN: '发现异常，先冻结批次并进入处理。',
    RECALLED: '风险已确认，立即召回并保留公开提醒。'
  }[targetStatus] ?? ''
}

function actionOf(item, code) {
  return item.actions?.find((action) => action.code === code) ?? {
    code,
    label: code,
    enabled: false,
    hint: ''
  }
}

function buildBatchInsight(item) {
  const qualityText = item.qualityStatus || ''
  const hasQr = item.qrStatus && item.qrStatus !== 'NOT_GENERATED'
  const hasQuality = !/待上传|pending/i.test(qualityText)
  const qualityFailed = /不合格|fail/i.test(qualityText)
  const needsTrace = /待补录/.test(item.currentNode || '')
  const isRisk = ['FROZEN', 'RECALLED'].includes(item.status)
  const readyToPublish = item.status === 'DRAFT' && hasQuality && !qualityFailed && hasQr && !needsTrace

  const missing = []
  if (needsTrace && item.status !== 'RECALLED') {
    missing.push({ key: 'trace', label: '待补追溯', actionCode: 'ADD_TRACE' })
  }
  if (!hasQuality && item.status !== 'RECALLED') {
    missing.push({ key: 'quality', label: '待上传质检', actionCode: 'UPLOAD_QUALITY' })
  }
  if (!hasQr && item.status !== 'RECALLED') {
    missing.push({ key: 'qr', label: '待生成二维码', actionCode: 'GENERATE_QR' })
  }
  if (readyToPublish) {
    missing.push({ key: 'publish', label: '资料已齐，可直接发布', actionCode: 'PUBLISH' })
  }

  let nextActionCode = 'WORKBENCH'
  let nextLabel = openWorkbenchLabel()
  let nextCopy = '打开批次详情继续处理。'
  let priority = 100
  let summaryTone = 'pending'
  let summaryLabel = '资料待补'

  if (item.status === 'RECALLED') {
    nextLabel = '召回处置'
    nextCopy = '先看风险处理记录与公开页风险提示。'
    priority = 520
    summaryTone = 'danger'
    summaryLabel = '已召回'
  } else if (item.status === 'FROZEN') {
    if (actionOf(item, 'RESUME').enabled) {
      nextActionCode = 'RESUME'
      nextLabel = '恢复发布'
      nextCopy = '整改检查项已满足，可以恢复发布。'
      priority = 460
      summaryTone = 'success'
      summaryLabel = '可恢复'
    } else {
      nextLabel = '补风险'
      nextCopy = '先补处理说明、整改记录，再考虑恢复。'
      priority = 430
      summaryTone = 'warning'
      summaryLabel = '处理中'
    }
  } else if (missing.length) {
    nextActionCode = missing[0].actionCode
    nextLabel = {
      ADD_TRACE: '补追溯',
      UPLOAD_QUALITY: '传质检',
      GENERATE_QR: '生成码',
      PUBLISH: '去发布'
    }[nextActionCode] ?? '继续处理'
    nextCopy = {
      ADD_TRACE: '先补关键追溯节点，方便后续查看和扫码查询。',
      UPLOAD_QUALITY: '补上质检摘要后，公开页可信度会明显更高。',
      GENERATE_QR: '二维码就绪后，才能顺畅衔接公开查询。',
      PUBLISH: '资料已经齐全，可以直接完成对外发布。'
    }[nextActionCode] ?? '继续处理当前批次。'
    priority = item.status === 'DRAFT' ? 360 - missing.length * 10 : 220
    summaryTone = qualityFailed ? 'danger' : 'pending'
    summaryLabel = `待补${missing.length}项`
  } else if (item.status === 'PUBLISHED') {
    nextLabel = '查看公开页'
    nextCopy = '已发布，可直接核对公开页、二维码和扫码效果。'
    priority = 180
    summaryTone = 'success'
    summaryLabel = '已发布'
  } else {
    summaryTone = qualityFailed ? 'danger' : 'success'
    summaryLabel = qualityFailed ? '需复核' : '资料齐全'
  }

  const backendRecommendedAction = resolveBackendRecommendedListAction(item, '')
  if (backendRecommendedAction) {
    nextActionCode = backendRecommendedAction
    nextLabel = item.recommendedActionLabel || fallbackActionLabel(backendRecommendedAction)
    nextCopy = item.recommendedActionHint || nextCopy
  }

  return {
    isRisk,
    isActionable: isRisk || item.status === 'DRAFT',
    readyToPublish,
    missing,
    nextActionCode,
    nextLabel,
    nextCopy,
    summaryTone,
    summaryLabel,
    priority,
    compactStatuses: [
      { label: summaryLabel, tone: summaryTone },
      { label: qualityStatusText(item), tone: qualityTone(item) },
      { label: riskStatusText(item), tone: riskTone(item) }
    ]
  }
}

function runRecommendedAction(card) {
  if (readOnlyBatchView.value) {
    openBatchDetail(card.item)
    return
  }
  const { item, insight } = card
  switch (insight.nextActionCode) {
    case 'ADD_TRACE':
      if (!actionEnabled(item, 'ADD_TRACE')) {
        showMessage(blockedActionHint(item, 'ADD_TRACE'), 'info')
        return
      }
      openTraceDialog(item)
      return
    case 'UPLOAD_QUALITY':
      if (!actionEnabled(item, 'UPLOAD_QUALITY')) {
        showMessage(blockedActionHint(item, 'UPLOAD_QUALITY'), 'info')
        return
      }
      openQualityDialog(item)
      return
    case 'GENERATE_QR':
      if (!actionEnabled(item, 'GENERATE_QR')) {
        showMessage(blockedActionHint(item, 'GENERATE_QR'), 'info')
        return
      }
      handleGenerateQr(item)
      return
    case 'PUBLISH':
      if (!actionEnabled(item, 'PUBLISH')) {
        showMessage(blockedActionHint(item, 'PUBLISH'), 'info')
        return
      }
      openStatusDialog(item, 'PUBLISHED')
      return
    case 'RESUME':
      if (!actionEnabled(item, 'RESUME')) {
        showMessage(blockedActionHint(item, 'RESUME'), 'info')
        return
      }
      openStatusDialog(item, 'PUBLISHED')
      return
    case 'VIEW_PUBLIC':
      if (openPublicTrace(item)) {
        return
      }
      openBatchDetail(item)
      return
    case 'RISK_PANEL':
      openBatchDetail(item, 'risk')
      return
    default:
      openBatchDetail(item)
  }
}

function handleRowCommand(card, command) {
  if (readOnlyBatchView.value) {
    return
  }
  if (command === 'assignment') {
    void openAssignmentDialog(card.item)
    return
  }
  if (command === 'edit') {
    openEditDialog(card.item)
    return
  }
  if (command === 'copy') {
    openCopyDialog(card.item)
    return
  }
  if (command === 'trace') {
    if (!actionEnabled(card.item, 'ADD_TRACE')) {
      showMessage(blockedActionHint(card.item, 'ADD_TRACE'), 'info')
      return
    }
    openTraceDialog(card.item)
    return
  }
  if (command === 'quality') {
    if (!actionEnabled(card.item, 'UPLOAD_QUALITY')) {
      showMessage(blockedActionHint(card.item, 'UPLOAD_QUALITY'), 'info')
      return
    }
    openQualityDialog(card.item)
    return
  }
  if (command === 'qr') {
    if (!actionEnabled(card.item, 'GENERATE_QR')) {
      showMessage(blockedActionHint(card.item, 'GENERATE_QR'), 'info')
      return
    }
    handleGenerateQr(card.item)
    return
  }
  if (command === 'public') {
    if (openPublicTrace(card.item)) {
      return
    }
    showMessage(blockedActionHint(card.item, 'VIEW_PUBLIC'), 'info')
    return
  }
  if (command === 'publish') {
    if (!(actionEnabled(card.item, 'PUBLISH') || actionEnabled(card.item, 'RESUME'))) {
      showMessage(blockedActionHint(card.item, actionEnabled(card.item, 'RESUME') ? 'RESUME' : 'PUBLISH'), 'info')
      return
    }
    openStatusDialog(card.item, 'PUBLISHED')
    return
  }
  if (command === 'freeze') {
    if (!actionEnabled(card.item, 'FREEZE')) {
      showMessage(blockedActionHint(card.item, 'FREEZE'), 'info')
      return
    }
    openStatusDialog(card.item, 'FROZEN')
    return
  }
  if (command === 'recall') {
    if (!actionEnabled(card.item, 'RECALL')) {
      showMessage(blockedActionHint(card.item, 'RECALL'), 'info')
      return
    }
    openStatusDialog(card.item, 'RECALLED')
  }
}

function setTraceStage(stage) {
  const previousProfile = getTraceStageProfile(lastTraceStage.value)
  const nextProfile = getTraceStageProfile(stage)
  const shouldResetTitle = !traceForm.value.title || traceForm.value.title === previousProfile.defaultTitle
  const shouldResetLocation = !traceForm.value.location || previousProfile.locationTemplates.includes(traceForm.value.location)
  const shouldResetSummary = !traceForm.value.summary || previousProfile.summaryTemplates.includes(traceForm.value.summary)

  traceForm.value.stage = stage
  if (shouldResetTitle) {
    traceForm.value.title = nextProfile.defaultTitle
  }
  if (shouldResetLocation) {
    traceForm.value.location = nextProfile.locationTemplates[0]
  }
  if (shouldResetSummary) {
    traceForm.value.summary = nextProfile.summaryTemplates[0]
  }
  lastTraceStage.value = stage
}

function copyLatestTraceRecord() {
  if (!traceDialogContext.value.latestRecord) {
    return
  }
  traceForm.value = {
    ...createTraceForm({
      stage: traceDialogContext.value.latestRecord.stageCode,
      title: traceDialogContext.value.latestRecord.title,
      operatorName: traceDialogContext.value.latestRecord.operatorName,
      location: traceDialogContext.value.latestRecord.location,
      summary: traceDialogContext.value.latestRecord.summary,
      visibleToConsumer: traceDialogContext.value.latestRecord.visibleToConsumer
    }),
    eventTime: currentDateTime()
  }
  lastTraceStage.value = traceForm.value.stage
}

function formatFileSize(size) {
  if (!size) {
    return '0 B'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function fileLabel(file) {
  return file.fileName || file.fileUrl || '已上传文件'
}

function taskStatusClass(taskStatus) {
  return {
    PENDING: 'pending',
    DRAFT: 'draft',
    COMPLETED: 'completed'
  }[String(taskStatus || 'PENDING').toUpperCase()] ?? 'pending'
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[status] ?? 'draft'
}
</script>

<template>
  <div class="page-shell" data-testid="batch-list-page">
    <div class="manage-page batch-manage">
    <AdminListTemplate
      template-class="batch-card-stack"
      filter-card-class="batch-filter-panel"
      ledger-card-class="batch-ledger-panel batch-ledger-card"
    >
      <template #summary>
        <AdminOverviewCards
          :items="batchBoardCards"
          :active-key="listMode"
          test-id-prefix="batch-mode"
          @select="switchListMode($event)"
        />
      </template>

      <template #actions>
        <button class="ghost" :disabled="loading" @click="fetchBatches">刷新</button>
        <button class="ghost" data-testid="batch-export-ledger" :disabled="loading || !visibleBatchCards.length" @click="exportCurrentLedger">
          导出台账
        </button>
        <button
          v-if="canManageBatch"
          class="primary"
          data-testid="batch-create-button"
          @click="openCreateDialog"
        >
          新增批次
        </button>
      </template>

      <template #banner>
        <section v-if="readOnlyBatchView" class="panel profile-banner" data-testid="batch-regulator-banner">
            <strong>监管查看模式</strong>
            <span>当前只保留批次状态、质检进度、风险摘要和最近更新，用于核对全链路状态，不展示新增、改派、复制等写入入口。</span>
          </section>
        </template>

    <template #filterPrimary>
      <div class="batch-filter-layout">
        <div class="filter-grid batch-filter-grid">
          <label class="manage-filter-field batch-filter-field batch-filter-field--code">
            <span class="manage-filter-field__label">批次号</span>
            <input v-model.trim="filters.batchCode" data-testid="batch-filter-code" type="text" placeholder="输入批次号">
          </label>

          <label class="manage-filter-field batch-filter-field batch-filter-field--product">
            <span class="manage-filter-field__label">产品名称</span>
            <input v-model.trim="filters.productName" type="text" placeholder="输入产品名称">
          </label>

          <label class="manage-filter-field batch-filter-field batch-filter-field--status">
            <span class="manage-filter-field__label">状态</span>
            <select v-model="filters.status">
              <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>

          <label class="manage-filter-field batch-filter-field batch-filter-field--company">
            <span class="manage-filter-field__label">企业</span>
            <input
              v-model.trim="filters.companyName"
              type="text"
              list="company-options"
              placeholder="输入或选择企业"
            >
            <datalist id="company-options">
              <option v-for="item in companyOptions" :key="item" :value="item" />
            </datalist>
          </label>

          <label class="manage-filter-field batch-filter-field batch-filter-field--page-size">
            <span class="manage-filter-field__label">每页显示</span>
            <select v-model="pageSize" class="page-size-select" @change="handlePageSizeChange($event.target.value)">
              <option v-for="item in pageSizeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
        </div>
      </div>
    </template>

    <template #filterSecondary>
      <div class="batch-filter-toolbar">
        <div class="batch-filter-actions">
          <button class="primary" data-testid="batch-search-button" :disabled="loading" @click="fetchBatches">查询</button>
          <button class="ghost" :disabled="loading" @click="resetFilters">重置</button>
        </div>
        <div class="batch-list-summary">
          <span class="manage-muted">{{ listSummary }}</span>
        </div>
      </div>
    </template>

    <template #message>
    <section v-if="message" class="message-bar" :class="messageType">
        {{ message }}
      </section>
    </template>

    <template #ledger>
        <div class="panel-heading">
          <div>
            <h2 class="panel-heading__title">批次台账</h2>
        </div>
      </div>

      <div v-if="!loading && !visibleBatchCards.length" class="empty-state batch-ledger-empty">
        <div>
          <h3>当前条件下没有批次数据</h3>
          <div class="toolbar-actions">
            <button v-if="canManageBatch" class="primary" @click="openCreateDialog">新增批次</button>
            <button class="ghost" @click="switchListMode('ALL')">查看全部</button>
          </div>
        </div>
      </div>

      <div v-else class="table-scroll-shell ledger-table-shell batch-table-shell" :class="{ 'is-loading': loading }">
        <div class="batch-table-head ledger-table-head">
          <span>批次与产品</span>
          <span>企业 / 更新时间</span>
          <span>状态</span>
          <span>负责人 / 任务</span>
          <span>{{ readOnlyBatchView ? '查看' : '动作' }}</span>
        </div>

        <div v-if="loading" class="batch-row-list ledger-row-list batch-row-list--loading" data-testid="batch-list-loading">
          <article v-for="index in 4" :key="`batch-skeleton-${index}`" class="batch-row ledger-row batch-row--skeleton" aria-hidden="true">
            <div class="batch-skeleton-block batch-skeleton-block--main">
              <span></span>
              <span></span>
            </div>
            <div class="batch-skeleton-block">
              <span></span>
              <span></span>
            </div>
            <div class="batch-skeleton-chips">
              <span></span>
              <span></span>
            </div>
            <div class="batch-skeleton-block">
              <span></span>
              <span></span>
            </div>
            <div class="batch-skeleton-actions">
              <span></span>
              <span></span>
            </div>
          </article>
        </div>

        <div v-else class="batch-row-list ledger-row-list">
        <article
          v-for="card in paginatedBatchCards"
          :key="card.item.id"
          class="batch-row ledger-row"
          :data-testid="`batch-card-${card.item.id}`"
        >
          <div class="row-main">
            <strong>{{ card.item.batchCode }}</strong>
            <span>{{ card.item.productName }}</span>
          </div>

          <div class="row-meta row-meta--flow">
            <strong>{{ card.item.companyName }}</strong>
            <small>最近更新：{{ latestUpdatedText(card.item) }}</small>
          </div>

          <div class="row-status">
            <div class="status-chip-row">
              <span
                v-for="item in batchStatusChips(card.item)"
                :key="item.label"
                class="status-chip"
                :class="item.tone"
              >
                {{ item.label }}
              </span>
            </div>
          </div>

          <div class="row-task" :data-testid="`batch-task-block-${card.item.id}`">
            <strong :data-testid="`batch-task-assignee-${card.item.id}`">{{ card.item.assigneeName || '未分配操作员' }}</strong>
            <div class="task-pill-row">
              <span
                class="task-state-badge"
                :class="taskStatusClass(card.item.taskStatus)"
                :data-testid="`batch-task-status-${card.item.id}`"
              >
                {{ resolveTaskStatusText(card.item) }}
              </span>
              <span
                class="task-flag"
                :class="{ done: card.item.todayCompleted }"
                :data-testid="`batch-task-today-${card.item.id}`"
              >
                {{ resolveTodayStatusText(card.item.todayCompleted) }}
              </span>
              <span
                class="task-flag"
                :class="{ draft: card.item.draftPending }"
                :data-testid="`batch-task-draft-${card.item.id}`"
              >
                {{ card.item.draftStatusLabel || (card.item.draftPending ? '草稿待续' : '无草稿') }}
              </span>
            </div>
          </div>

          <div class="row-actions table-cell--actions">
            <span class="row-next-title" :data-testid="`batch-next-${card.item.id}`">{{ card.insight.nextLabel }}</span>
            <div class="row-actions-scroll">
              <button
                v-if="showRecommendedAction(card)"
                class="action-primary-button"
                :class="recommendedActionClass(card)"
                :data-testid="`batch-recommend-${card.item.id}`"
                @click.stop="runRecommendedAction(card)"
              >
                {{ card.insight.nextLabel }}
              </button>
              <button
                class="text-button primary-text"
                :data-testid="`batch-open-workbench-${card.item.id}`"
                @click.stop="openBatchDetail(card.item)"
              >
                {{ openWorkbenchLabel() }}
              </button>
              <button
                v-if="canManageAssignment"
                class="text-button"
                :data-testid="`batch-assignment-open-${card.item.id}`"
                @click.stop="openAssignmentDialog(card.item)"
              >
                分配
              </button>
              <el-dropdown v-if="!readOnlyBatchView" @command="(command) => handleRowCommand(card, command)">
                <button type="button" class="text-button" @click.stop>更多</button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="copy">复制为新批次</el-dropdown-item>
                    <el-dropdown-item command="edit">编辑资料</el-dropdown-item>
                    <el-dropdown-item command="trace" :disabled="!actionEnabled(card.item, 'ADD_TRACE')">补录追溯</el-dropdown-item>
                    <el-dropdown-item command="quality" :disabled="!actionEnabled(card.item, 'UPLOAD_QUALITY')">上传质检</el-dropdown-item>
                    <el-dropdown-item command="qr" :disabled="!actionEnabled(card.item, 'GENERATE_QR')">生成二维码</el-dropdown-item>
                    <el-dropdown-item v-if="canOpenPublicTrace(card.item)" command="public">查看公开页</el-dropdown-item>
                    <el-dropdown-item
                      command="publish"
                      :disabled="!(actionEnabled(card.item, 'PUBLISH') || actionEnabled(card.item, 'RESUME'))"
                    >
                      {{ actionEnabled(card.item, 'RESUME') ? '恢复发布' : '发布' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="freeze" :disabled="!actionEnabled(card.item, 'FREEZE')">冻结</el-dropdown-item>
                    <el-dropdown-item command="recall" :disabled="!actionEnabled(card.item, 'RECALL')">召回</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </article>
        </div>
      </div>

      <AdminListPagination
        :summary="paginationSummary"
        :prev-disabled="loading || page <= 1"
        :next-disabled="loading || page >= pageCount"
        @prev="goPrevPage"
        @next="goNextPage"
      />
    </template>
    </AdminListTemplate>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost icon-button" @click="closeDialog">关闭</button>
        </div>

        <div v-if="dialog.type === 'create' || dialog.type === 'copy' || dialog.type === 'edit'" class="form-grid" data-testid="batch-edit-dialog">
          <div v-if="false" class="full-width form-intro-banner">
            <strong>
              {{
                dialog.type === 'copy'
                  ? '会基于当前批次带入基础信息，但新批次仍会从干净状态开始。'
                  : (dialog.type === 'create' ? '先完成批次建档，系统会直接打开工作台继续处理。' : '当前正在调整批次建档信息。')
              }}
            </strong>
            <span>{{ batchDialogGuideText() }}</span>
          </div>

          <label v-if="false && dialog.type === 'copy'" class="full-width">
            <span>复制来源</span>
            <div class="field-note field-note--accent" data-testid="batch-copy-source-note">
              <strong>{{ dialog.sourceBatchCode }}</strong>
              <small>
                产品：{{ dialog.sourceProductName || '沿用原产品' }}；
                不会继承旧追溯、质检、二维码、风险、分配、草稿和任务状态。
              </small>
            </div>
          </label>

          <label>
            <span>企业（必填）</span>
            <select v-model="batchForm.companyId" @change="handleBatchCompanyChange()">
              <option :value="null">请选择企业</option>
              <option v-for="item in formCompanyOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span>产品（必填）</span>
            <select v-model="batchForm.productId" :disabled="!batchForm.companyId" @change="handleBatchProductChange">
              <option :value="null">{{ batchForm.companyId ? '请选择产品' : '请先选择企业' }}</option>
              <option v-for="item in formProductOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>

          <label v-if="dialog.type === 'create' || dialog.type === 'copy'">
            <span>批次编号（必填）</span>
            <input v-model.trim="batchForm.batchCode" type="text">
          </label>
          <label v-else>
            <span>批次编号</span>
            <input :value="batchForm.batchCode" type="text" disabled>
          </label>

          <label v-if="false && selectedCompanyOption" class="full-width">
            <span>当前企业</span>
            <div class="field-note">
              <strong>{{ selectedCompanyOption.name }}</strong>
              <small>先在当前企业下选产品，后续批次、分配和质检都会沿用这个企业归属。</small>
            </div>
          </label>
          <label>
            <span>产地（必填）</span>
            <input v-model.trim="batchForm.originPlace" type="text" placeholder="例如 江西赣州信丰">
          </label>
          <label>
            <span>生产日期（必填）</span>
            <input v-model="batchForm.productionDate" type="date">
          </label>
          <label v-if="false && dialog.type === 'copy'" class="full-width">
            <span>复制提示</span>
            <div class="field-note">
              <strong>当前日期已重新带入</strong>
              <small>请重新确认新批次编号和生产日期，复制出的批次会回到“刚创建”的初始状态。</small>
            </div>
          </label>
          <label v-if="false && selectedProductOption" class="full-width">
            <span>当前产品</span>
            <div class="field-note">
              <strong>{{ selectedProductOption.name }}</strong>
              <small>
                分类：{{ selectedProductOption.category || '待补充' }}；
                规格：{{ selectedProductOption.specification || '待补充' }}；
                批次名称将沿用这个产品名称
              </small>
            </div>
          </label>
          <div v-if="false && batchForm.companyId && !formProductOptions.length" class="full-width flow-tip warning-tip">
            <strong>当前企业还没有可建档产品。</strong>
            <span>请先去产品管理新增产品，再回来继续建批次。</span>
          </div>

          <label class="full-width">
            <span>公开说明（选填）</span>
            <textarea
              v-model.trim="batchForm.publicRemark"
              rows="3"
              placeholder="填写消费者可见的批次说明，例如产地、工艺特点或本批次情况"
            />
          </label>
          <label class="full-width">
            <span>内部备注（选填）</span>
            <textarea
              v-model.trim="batchForm.internalRemark"
              rows="3"
              placeholder="用于记录补录计划、处理提醒或内部跟进说明"
            />
          </label>
          <div v-if="false" class="full-width flow-tip">
            <strong>
              {{
                dialog.type === 'copy'
                  ? '复制后将直接进入新批次工作台。'
                  : (dialog.type === 'create' ? '创建后将直接进入批次工作台。' : '保存后将返回批次工作台。')
              }}
            </strong>
            <span>下一步入口会继续保留在工作台中，可补录追溯、上传质检和生成二维码。</span>
          </div>
        </div>

        <div v-else-if="dialog.type === 'trace'" class="form-grid" data-testid="batch-trace-dialog">
          <div class="full-width quick-entry-card quick-entry-card--compact">
            <div>
              <strong>补录追溯</strong>
            </div>
            <button
              v-if="traceDialogContext.latestRecord"
              class="ghost"
              @click="copyLatestTraceRecord"
            >
              复制上一条
            </button>
          </div>

          <div class="full-width chip-row">
            <button
              v-for="item in stageOptions"
              :key="item.value"
              class="chip-button"
              :class="{ active: traceForm.stage === item.value }"
              @click="setTraceStage(item.value)"
            >
              {{ item.label }}
            </button>
          </div>

          <label>
            <span>阶段</span>
            <select v-model="traceForm.stage" @change="setTraceStage(traceForm.stage)">
              <option v-for="item in stageOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>记录时间</span>
            <input v-model="traceForm.eventTime" type="datetime-local">
          </label>
          <label>
            <span>记录人</span>
            <input v-model.trim="traceForm.operatorName" type="text" placeholder="例如 现场录入员">
          </label>
          <label>
            <span>地点</span>
            <input v-model.trim="traceForm.location" type="text" placeholder="例如 一号种植基地">
          </label>
          <label class="full-width">
            <span>记录标题</span>
            <input v-model.trim="traceForm.title" type="text" placeholder="一句话说明这个节点">
          </label>
          <label class="full-width">
            <span>记录说明</span>
            <textarea
              v-model.trim="traceForm.summary"
              rows="4"
              placeholder="建议填写本节点完成了什么、由谁完成、是否进入下一步"
            />
          </label>

          <div class="full-width template-grid">
            <div class="template-card">
              <span>常用地点</span>
              <div class="chip-row">
                <button
                  v-for="item in traceStageProfile.locationTemplates"
                  :key="item"
                  class="chip-button light"
                  @click="traceForm.location = item"
                >
                  {{ item }}
                </button>
              </div>
            </div>
            <div class="template-card">
              <span>说明模板</span>
              <div class="chip-row">
                <button
                  v-for="item in traceStageProfile.summaryTemplates"
                  :key="item"
                  class="chip-button light"
                  @click="traceForm.summary = item"
                >
                  {{ item }}
                </button>
              </div>
            </div>
          </div>

          <label class="full-width">
            <span>现场图片</span>
            <div class="upload-box">
              <input type="file" accept="image/*" multiple @change="handleTraceFilesChange">
              <small>支持多图上传。上传成功后会立刻显示在下面，保存记录时会自动一起绑定。</small>
            </div>
          </label>
          <label class="full-width">
            <span>图片链接兜底</span>
            <input v-model.trim="traceForm.imageUrl" type="url" placeholder="如已在外部图床，可粘贴图片地址">
          </label>

          <div v-if="traceUploading" class="full-width upload-hint">正在上传图片，稍等一下就会显示在当前记录里...</div>

          <div v-if="traceForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in traceForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }} · 已上传，保存记录后生效</small>
              </div>
              <div class="inline-actions">
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看</a>
                <button class="ghost" @click="removeTraceAttachment(item.id)">移除</button>
              </div>
            </article>
          </div>

          <label class="checkbox-field full-width">
            <input v-model="traceForm.visibleToConsumer" type="checkbox">
            <span>这条记录同步展示给消费者</span>
          </label>

          <div v-if="false && traceDialogContext.latestRecord" class="full-width last-record-card">
            <span>上一条记录</span>
            <strong>{{ traceDialogContext.latestRecord.title }}</strong>
            <p>{{ traceDialogContext.latestRecord.location }} 路 {{ traceDialogContext.latestRecord.eventTime }}</p>
            <small>{{ traceDialogContext.latestRecord.summary }}</small>
          </div>
        </div>

        <div v-else-if="dialog.type === 'quality'" class="form-grid" data-testid="batch-quality-dialog">
          <label>
            <span>报告编号</span>
            <input v-model.trim="qualityForm.reportNo" type="text" placeholder="例如 JX-20260325-01">
          </label>
          <label>
            <span>检测机构</span>
            <input v-model.trim="qualityForm.agency" type="text" placeholder="例如 江西省农产品质检中心">
          </label>
          <label>
            <span>结果</span>
            <select v-model="qualityForm.result">
              <option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>检测时间</span>
            <input v-model="qualityForm.reportTime" type="datetime-local">
          </label>
          <label class="full-width">
            <span>质检摘要</span>
            <textarea
              v-model.trim="qualityForm.highlightsText"
              rows="4"
              placeholder="每行一个重点，例如：关键指标合格 / 企业自检正常 / 抽检无异常"
            />
          </label>
          <label class="full-width">
            <span>附件</span>
            <div class="upload-box">
              <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
              <small>可上传 PDF 或图片，公开页会优先展示质检结论，后台同时保留附件备查。</small>
            </div>
          </label>
          <div v-if="qualityUploading" class="full-width upload-hint">正在上传质检附件...</div>
          <div v-if="qualityForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in qualityForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }}</small>
              </div>
              <div class="inline-actions">
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看</a>
                <button class="ghost" @click="removeQualityAttachment(item.id)">移除</button>
              </div>
            </article>
          </div>
        </div>

        <div v-else-if="dialog.type === 'status'" class="form-grid">
          <label>
            <span>目标状态</span>
            <select v-model="statusForm.targetStatus">
              <option value="PUBLISHED">发布</option>
              <option value="FROZEN">冻结</option>
              <option value="RECALLED">召回</option>
            </select>
          </label>
          <label>
            <span>处理人</span>
            <input v-model.trim="statusForm.operatorName" type="text">
          </label>
          <label class="full-width">
            <span>处理原因</span>
            <textarea
              v-model.trim="statusForm.reason"
              rows="4"
              placeholder="建议写清当前批次为什么要发布、冻结或召回"
            />
          </label>
        </div>

        <div class="dialog-actions">
          <button class="ghost" @click="closeDialog">取消</button>
          <button
            v-if="dialog.type === 'trace'"
            class="ghost"
            data-testid="batch-trace-save-continue"
            @click="submitDialog({ keepOpen: true })"
          >
            保存并继续
          </button>
          <button class="primary" data-testid="batch-dialog-submit" @click="submitDialog()">
            {{ dialog.type === 'copy' ? '复制并查看工作台' : (dialog.type === 'create' ? '创建并查看工作台' : '确认保存') }}
          </button>
        </div>
      </section>
    </div>

    <el-drawer
      v-model="detailDrawer.visible"
      direction="rtl"
      :size="batchSideDrawerSize"
      append-to-body
      destroy-on-close
      :with-header="false"
      class="batch-side-drawer batch-detail-drawer"
    >
      <div class="drawer-shell batch-workbench-drawer-shell" data-testid="batch-workbench-drawer">
        <div class="dialog-head">
          <div>
            <h3>批次工作台</h3>
            <p>批次 {{ detailDrawer.batchCode || '详情查看' }}</p>
          </div>
          <button class="ghost icon-button" @click="closeBatchDetailDrawer">关闭</button>
        </div>
        <BatchWorkbenchDrawerPanel
          v-if="detailDrawer.visible && detailDrawer.batchId"
          :batch-id="detailDrawer.batchId"
        />
      </div>
    </el-drawer>

    <el-drawer
      v-model="assignmentDialog.visible"
      direction="rtl"
      :size="batchSideDrawerSize"
      append-to-body
      destroy-on-close
      :with-header="false"
      class="batch-side-drawer assignment-list-drawer"
    >
      <div class="drawer-shell assignment-drawer-shell" data-testid="batch-assignment-drawer">
        <div class="dialog-head">
          <div>
            <h3>负责人安排</h3>
            <p>批次 {{ assignmentDialog.batchCode }}</p>
          </div>
          <button class="ghost icon-button" @click="closeAssignmentDialog">关闭</button>
        </div>

        <div class="assignment-overview">
          <div>
            <span>当前分配人</span>
            <strong>{{ assignmentDialog.currentAssigneeName || '未分配操作员' }}</strong>
          </div>
          <div>
            <span>分配时间</span>
            <strong>{{ assignmentDialog.assignedAt || '暂无记录' }}</strong>
          </div>
          <div>
            <span>任务状态</span>
            <strong>{{ resolveTaskStatusText(assignmentDialog) }}</strong>
          </div>
          <div>
            <span>今日完成</span>
            <strong>{{ resolveTodayStatusText(assignmentDialog.todayCompleted) }}</strong>
          </div>
          <div>
            <span>草稿状态</span>
            <strong>{{ assignmentDialog.draftStatusLabel }}</strong>
          </div>
          <div>
            <span>草稿更新时间</span>
            <strong>{{ assignmentDialog.draftUpdatedAt || '暂无草稿' }}</strong>
          </div>
        </div>

        <div class="assignment-form">
          <label class="full-width">
            <span>指派操作员</span>
            <select
              v-model="assignmentDialog.assigneeUserId"
              data-testid="batch-list-assignment-select"
              :disabled="assignmentSaving || operatorLoading"
            >
              <option value="">未分配操作员</option>
              <option v-for="item in operatorOptions" :key="item.id" :value="String(item.id)">
                {{ item.realName || item.username }}（{{ item.username }} / {{ item.companyName }}）
              </option>
            </select>
          </label>

          <div class="field-note">
            <strong>{{ assignmentHint }}</strong>
            <small>{{ operatorLoading ? '正在加载操作员列表...' : `当前可分配 ${operatorOptions.length} 位操作员。` }}</small>
          </div>

          <div v-if="assignmentDialog.confirmVisible" class="assignment-warning" data-testid="batch-list-assignment-confirm">
            <strong>该批次当前分配人存在未提交草稿</strong>
            <p>{{ assignmentDialog.confirmMessage }}</p>
            <div class="toolbar-actions">
              <button class="ghost" data-testid="batch-list-assignment-cancel" @click="cancelAssignmentConfirm">取消改派</button>
              <button class="warning" data-testid="batch-list-assignment-force" :disabled="assignmentSaving" @click="forceAssignmentChange">
                {{ assignmentConfirmActionLabel }}
              </button>
            </div>
          </div>
        </div>

        <div class="dialog-actions">
          <button class="ghost" :disabled="assignmentSaving" @click="closeAssignmentDialog">取消</button>
          <button
            class="ghost danger"
            data-testid="batch-list-assignment-clear"
            :disabled="assignmentSaving || !assignmentDialog.currentAssigneeUserId"
            @click="clearAssignment"
          >
            清空分配
          </button>
          <button
            class="primary"
            data-testid="batch-list-assignment-submit"
            :disabled="assignmentSaving || operatorLoading || !assignmentChanged"
            @click="submitAssignment(false)"
          >
            {{ assignmentActionLabel }}
          </button>
        </div>
      </div>
    </el-drawer>
    </div>
    </div>
</template>

<style src="../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.page-shell {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: var(--admin-page-shell-padding);
}

.batch-manage {
  font-family: "PingFang SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif;
  --batch-filter-text-inset: 14px;
  --batch-grid-inline-padding: 18px;
  --batch-grid-column-gap: 16px;
  --batch-filter-group-left-shift: 8px;
  --batch-code-filter-width: 184px;
  --batch-product-filter-width: 208px;
  --batch-status-filter-width: 136px;
  --batch-company-filter-width: 196px;
  --batch-page-size-width: 148px;
  --batch-filter-card-border: rgba(56, 134, 217, 0.14);
  --batch-filter-card-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 251, 255, 0.94) 100%);
  --batch-filter-card-shadow: 0 16px 34px rgba(45, 113, 194, 0.1);
  --batch-filter-control-height: 40px;
  --batch-filter-control-radius: 12px;
}

.batch-card-stack {
  display: grid;
  gap: 16px;
}

:deep(.batch-filter-panel),
:deep(.batch-ledger-card) {
  position: relative;
  overflow: hidden;
}

:deep(.batch-filter-panel) {
  padding: 18px 22px 0;
  border-color: var(--batch-filter-card-border) !important;
  background: var(--batch-filter-card-bg) !important;
  box-shadow: var(--batch-filter-card-shadow) !important;
}

:deep(.batch-ledger-card) {
  padding: 20px 22px 18px;
  border: 1px solid rgba(56, 134, 217, 0.14) !important;
  border-radius: 24px !important;
  background: #fff !important;
  box-shadow: 0 18px 42px rgba(45, 113, 194, 0.08) !important;
}

.batch-mode-summary {
  margin-top: 0;
}

.batch-summary-row {
  margin-top: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.batch-summary-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex: 0 0 auto;
}

.batch-mode-summary .manage-summary-chip {
  min-height: 38px;
  padding: 8px 12px;
  border: 1px solid var(--admin-border);
  border-radius: 999px;
  background: var(--admin-surface-soft);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  box-shadow: none;
}

.batch-mode-summary .manage-summary-chip strong {
  color: var(--admin-text-strong);
  font-weight: 700;
}

.batch-mode-summary .manage-summary-chip--interactive:hover {
  border-color: rgba(48, 149, 246, 0.28);
  background: rgba(48, 149, 246, 0.08);
}

.batch-mode-summary .manage-summary-chip.is-active {
  border-color: rgba(48, 149, 246, 0.3);
  background: rgba(48, 149, 246, 0.14);
  box-shadow: 0 10px 20px rgba(48, 149, 246, 0.1);
}

.batch-mode-summary .manage-summary-chip.is-active,
.batch-mode-summary .manage-summary-chip.is-active strong {
  color: var(--admin-primary-deep);
}

.batch-tabs-panel {
  padding: 12px 16px;
}

.batch-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.batch-tab {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid var(--admin-border);
  border-radius: 999px;
  background: var(--admin-surface-soft);
  color: var(--admin-text);
}

.batch-tab strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(56, 134, 217, 0.12);
  color: var(--admin-primary-deep);
  font-size: 12px;
}

.batch-tab.active {
  border-color: rgba(48, 149, 246, 0.22);
  background: rgba(48, 149, 246, 0.12);
  color: var(--admin-primary-deep);
}

.batch-tab.active strong {
  background: var(--admin-primary);
  color: #fff;
}

.hero-card,
.stat-card,
.batch-card,
.dialog-card,
.message-bar {
  border: 1px solid rgba(56, 134, 217, 0.12);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow:
    0 22px 54px rgba(45, 113, 194, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.hero-card {
  display: grid;
  grid-template-columns: 1.5fr auto;
  gap: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(129, 199, 255, 0.2), transparent 30%),
    linear-gradient(145deg, #4aa7ff 0%, #2f8de6 66%, #1f79d6 100%);
  color: #f8fbff;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--admin-text-soft);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-card h1,
.hero-card h2,
.hero-card h3,
.hero-card h4,
.hero-card p {
  margin-top: 0;
}

.hero-card h1 {
  margin-bottom: 12px;
  font-size: 38px;
}

.lead {
  margin-bottom: 0;
  max-width: 760px;
  line-height: 1.8;
  color: rgba(248, 251, 255, 0.92);
}

.hero-actions,
.toolbar,
.action-row,
.dialog-actions,
.inline-actions,
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-actions {
  align-items: flex-end;
  justify-content: flex-end;
}

.queue-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.queue-item {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.16);
}

.queue-item strong,
.queue-item span {
  display: block;
}

.queue-item span {
  margin-top: 6px;
  color: rgba(248, 251, 255, 0.84);
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.stat-card,
.batch-card,
.dialog-card,
.message-bar {
  padding: 24px;
}

.stat-card span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 14px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  color: var(--admin-text);
  font-size: 34px;
}

.stat-card.warning strong {
  color: var(--admin-danger-text);
}

.profile-banner {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
  border-radius: 20px;
  border: 1px solid rgba(29, 111, 161, 0.16);
  background: linear-gradient(135deg, rgba(246, 251, 255, 0.96), rgba(236, 245, 255, 0.92));
}

.profile-banner strong {
  color: var(--admin-text);
  font-size: 15px;
}

.profile-banner span {
  color: var(--admin-text-soft);
  line-height: 1.7;
}

.toolbar {
  align-items: center;
  justify-content: space-between;
}

.batch-filter-layout {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 18px;
  margin-left: calc(-1 * var(--batch-filter-group-left-shift));
  padding: 0 12px 0 calc(var(--batch-grid-inline-padding) - var(--batch-filter-text-inset));
  flex-wrap: wrap;
}

.batch-filter-toolbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  margin-left: calc(-1 * var(--batch-filter-group-left-shift));
  padding: 0 12px 18px var(--batch-grid-inline-padding);
  min-width: 0;
  flex-wrap: wrap;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.list-summary {
  color: var(--admin-text-soft);
  font-size: 13px;
}

.batch-stats-grid {
  margin-top: 18px;
}

.batch-stats-grid .stat-card {
  min-height: 118px;
}

.batch-mode-banner {
  margin-top: 18px;
}

.batch-mode-banner h2 {
  margin: 10px 0 8px;
  font-size: 30px;
  color: #f8fbff;
}

.mode-banner-note {
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-start;
  min-width: 180px;
  color: rgba(248, 251, 255, 0.88);
}

.mode-banner-note span,
.mode-banner-note strong {
  display: block;
}

.mode-banner-note strong {
  margin-top: 6px;
  font-size: 20px;
}

.panel-head {
  margin-bottom: 18px;
}

.mode-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.mode-pill {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-height: 104px;
  padding: 16px;
  border-radius: 22px;
  background: var(--admin-surface-soft);
  color: var(--admin-text);
}

.mode-pill.active {
  background: var(--admin-primary);
  color: #ffffff;
}

.mode-pill strong,
.mode-pill span {
  display: block;
}

.mode-pill span {
  font-size: 13px;
  line-height: 1.6;
  text-align: left;
}

.filter-grid,
.form-grid,
.meta-grid,
.template-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.template-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

label {
  display: block;
}

label span,
.meta-grid span,
.template-card span {
  display: block;
  margin-bottom: 8px;
  color: var(--admin-text-soft);
  font-size: 14px;
}

input,
select,
textarea,
button {
  font: inherit;
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid var(--admin-border);
  border-radius: 16px;
  background: var(--admin-surface-soft);
  color: var(--admin-text);
}

textarea {
  resize: vertical;
}

.field-note,
.upload-box,
.uploaded-file-item,
.form-intro-banner,
.flow-tip,
.quick-entry-card,
.last-record-card,
.template-card {
  border: 1px solid var(--admin-border);
  border-radius: 18px;
  background: var(--admin-surface-soft);
}

.field-note,
.form-intro-banner,
.flow-tip,
.quick-entry-card,
.last-record-card,
.template-card {
  padding: 16px;
}

.flow-tip span,
.form-intro-banner span,
.field-note small,
.quick-entry-card p,
.last-record-card p,
.last-record-card small,
.upload-box small,
.uploaded-file-item small,
.upload-hint {
  color: var(--admin-text-soft);
}

.flow-tip strong,
.form-intro-banner strong,
.last-record-card strong,
.field-note strong,
.uploaded-file-item strong {
  display: block;
  color: var(--admin-text);
}

.form-section-title {
  color: var(--admin-text);
  font-size: 15px;
  font-weight: 700;
  margin-bottom: -4px;
}

.warning-tip {
  border-color: rgba(242, 139, 34, 0.28);
  background: rgba(255, 244, 228, 0.72);
}

.field-note--accent {
  border-color: rgba(48, 149, 246, 0.24);
  background: linear-gradient(135deg, rgba(48, 149, 246, 0.12) 0%, rgba(255, 255, 255, 0.96) 100%);
}

.quick-entry-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.template-card {
  min-height: 100%;
}

.upload-box {
  padding: 14px;
}

.upload-box input {
  border: 0;
  padding: 0;
  background: transparent;
}

.upload-box small {
  display: block;
  margin-top: 8px;
  line-height: 1.7;
}

.upload-hint {
  padding: 4px 2px;
}

.uploaded-file-list {
  display: grid;
  gap: 10px;
}

.uploaded-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
}

.full-width {
  grid-column: 1 / -1;
}

.toolbar {
  margin-top: 18px;
}

button:not(.manage-summary-chip),
.preview-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  text-decoration: none;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

button:not(.manage-summary-chip):hover,
.preview-link:hover {
  transform: translateY(-1px);
}

button:not(.manage-summary-chip):disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.primary {
  background: var(--admin-primary);
  color: #ffffff;
}

.ghost {
  background: #ffffff;
  color: var(--admin-primary-deep);
  border: 1px solid var(--admin-border);
}

.neutral {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-deep);
}

.success {
  background: var(--admin-success-bg);
  color: var(--admin-success-text);
}

.warning {
  background: rgba(214, 137, 58, 0.16);
  color: #8b4c13;
}

.danger {
  background: rgba(190, 70, 58, 0.14);
  color: #8f2f29;
}

.chip-button {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  background: var(--admin-primary-soft);
  color: var(--admin-primary-deep);
}

.chip-button.active {
  background: var(--admin-primary);
  color: #ffffff;
}

.chip-button.light {
  background: #ffffff;
  color: var(--admin-primary-deep);
  border: 1px solid var(--admin-border);
}

.message-bar {
  margin-top: 18px;
}

.message-bar.success {
  background: var(--admin-success-bg);
  color: var(--admin-success-text);
}

.message-bar.error {
  background: rgba(253, 236, 235, 0.94);
  color: #8f2f29;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  text-align: center;
  color: var(--admin-text-soft);
}

.batch-table-shell {
  --ledger-grid-columns:
    minmax(220px, 1.08fr)
    minmax(220px, 0.98fr)
    minmax(260px, 1.02fr)
    minmax(220px, 0.9fr)
    minmax(286px, 1.1fr);
  --ledger-column-gap: var(--batch-grid-column-gap);
  --ledger-inline-padding: var(--batch-grid-inline-padding);
  --ledger-min-width: 1360px;
}

.batch-table-head {
  color: #6f86a4;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  background: #fff;
}

:deep(.batch-ledger-panel .panel-heading) {
  padding-left: 28px;
}

:deep(.batch-ledger-card .table-scroll-shell) {
  background: #fff !important;
}

:deep(.batch-ledger-card .table-scroll-shell .batch-table-head) {
  background: #fff !important;
}

:deep(.batch-ledger-card .admin-list-pagination) {
  background: #fff !important;
}

:deep(.batch-ledger-card .panel-heading),
:deep(.batch-ledger-card .panel-heading > div) {
  background: #fff;
}

.batch-filter-grid {
  grid-template-columns:
    minmax(var(--batch-code-filter-width), max-content)
    minmax(var(--batch-product-filter-width), max-content)
    minmax(var(--batch-status-filter-width), max-content)
    minmax(var(--batch-company-filter-width), max-content)
    minmax(var(--batch-page-size-width), max-content) !important;
  align-items: end;
  column-gap: 12px;
  row-gap: 12px;
  padding: 0;
  flex: 0 1 auto;
  min-width: 0;
  width: max-content;
  max-width: 100%;
}

.batch-filter-grid > * {
  min-width: 0;
}

.batch-filter-field {
  gap: 8px;
  width: 100%;
  justify-self: start;
}

.batch-filter-field--code {
  max-width: var(--batch-code-filter-width);
}

.batch-filter-field--product {
  max-width: var(--batch-product-filter-width);
}

.batch-filter-field--status {
  max-width: var(--batch-status-filter-width);
}

.batch-filter-field--company {
  max-width: var(--batch-company-filter-width);
}

.batch-filter-field--page-size {
  max-width: var(--batch-page-size-width);
}

.batch-filter-grid .manage-filter-field__label {
  padding-inline-start: var(--batch-filter-text-inset);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.batch-filter-grid input,
.batch-filter-grid select {
  width: 100%;
  min-height: var(--batch-filter-control-height);
  box-sizing: border-box;
  padding: 0 14px 0 var(--batch-filter-text-inset);
  border: 0;
  border-radius: var(--batch-filter-control-radius);
  background: #fff;
  color: var(--admin-text);
  box-shadow: 0 0 0 1px rgba(56, 134, 217, 0.14) inset;
}

.batch-filter-grid input::placeholder {
  color: var(--admin-text-faint);
}

.batch-filter-grid select {
  appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #9bb0c8 50%),
    linear-gradient(135deg, #9bb0c8 50%, transparent 50%);
  background-position:
    calc(100% - 18px) calc(50% - 2px),
    calc(100% - 12px) calc(50% - 2px);
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
}

.page-size-select {
  width: 100%;
}

.batch-filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.batch-filter-actions {
  min-width: 0;
}

.batch-filter-actions button {
  min-height: 38px;
  padding-inline: 16px;
  border-radius: 12px;
}

.batch-list-summary .manage-muted {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.batch-row-list {
  display: grid;
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 26px;
  background: #fff;
}

.batch-row-list.ledger-row-list {
  background: #fff;
}

.batch-row-list--loading {
  min-height: 428px;
}

.batch-row {
  align-items: center;
  border-top: 1px solid rgba(56, 134, 217, 0.1);
  background: #fff;
  transition: background-color 0.18s ease;
}

.batch-row.ledger-row {
  background: #fff;
}

.batch-row:first-child {
  border-top: 0;
}

.batch-row:hover {
  background: rgba(48, 149, 246, 0.03);
}

.batch-row--skeleton {
  pointer-events: none;
}

.batch-skeleton-block,
.batch-skeleton-actions,
.batch-skeleton-chips {
  display: grid;
  gap: 10px;
}

.batch-skeleton-block span,
.batch-skeleton-actions span,
.batch-skeleton-chips span {
  display: block;
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(226, 236, 247, 0.9) 0%, rgba(241, 246, 252, 0.98) 50%, rgba(226, 236, 247, 0.9) 100%);
  background-size: 220px 100%;
  animation: batch-skeleton-pulse 1.35s ease-in-out infinite;
}

.batch-skeleton-block--main span:first-child {
  width: 76%;
  height: 18px;
}

.batch-skeleton-block--main span:last-child,
.batch-skeleton-block span:last-child {
  width: 56%;
}

.batch-skeleton-chips {
  grid-auto-flow: column;
  grid-auto-columns: 74px;
  justify-content: start;
}

.batch-skeleton-chips span {
  width: 74px;
  height: 30px;
}

.batch-skeleton-actions {
  grid-auto-flow: column;
  grid-auto-columns: 88px;
  justify-content: start;
}

.batch-skeleton-actions span {
  width: 88px;
  height: 36px;
}

@keyframes batch-skeleton-pulse {
  0% {
    background-position: 220px 0;
  }
  100% {
    background-position: -40px 0;
  }
}

.row-main,
.row-meta,
.row-status,
.row-task {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.row-main strong,
.row-meta strong {
  color: var(--admin-text);
  font-size: 15px;
  font-weight: 700;
}

.row-main strong {
  color: var(--admin-text-strong);
  font-size: 17px;
  font-weight: 800;
}

.row-main span,
.row-main small,
.row-meta span,
.row-meta small,
.row-status small,
.row-task span {
  color: var(--admin-text-soft);
  font-size: 13px;
  line-height: 1.55;
  font-weight: 500;
}

.row-status {
  align-items: flex-start;
}

.row-health {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-chip-row,
.action-link-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  color: #5f7b9e;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-chip.success {
  background: rgba(22, 163, 74, 0.14);
  color: #15803d;
}

.status-chip.info {
  background: rgba(48, 149, 246, 0.12);
  color: var(--admin-primary-deep);
}

.status-chip.warning {
  background: rgba(242, 139, 34, 0.16);
  color: #8b4c13;
}

.status-chip.danger {
  background: rgba(190, 70, 58, 0.14);
  color: #8f2f29;
}

.status-chip.pending,
.status-chip.normal {
  background: rgba(120, 147, 180, 0.14);
  color: #5f7b9e;
}

.compact-progress {
  margin-top: 0;
}

.row-health-scroll,
.row-actions-scroll {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  overflow: visible;
  width: max-content;
}

.row-health-scroll::-webkit-scrollbar,
.row-actions-scroll::-webkit-scrollbar {
  display: none;
}

.row-next-title {
  color: var(--admin-text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-task strong {
  color: var(--admin-text);
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-pill-row {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow: visible;
  width: max-content;
}

.task-state-badge,
.task-flag {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.task-state-badge.pending {
  background: rgba(56, 134, 217, 0.12);
  color: var(--admin-primary-deep);
}

.task-state-badge.draft,
.task-flag.draft {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.task-state-badge.completed,
.task-flag.done {
  background: rgba(22, 163, 74, 0.14);
  color: #15803d;
}

.task-flag {
  background: rgba(148, 163, 184, 0.16);
  color: var(--admin-text-soft);
}

.row-actions {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.action-primary-button {
  min-width: auto;
  flex: 0 0 auto;
  white-space: nowrap;
  min-height: 40px;
  padding: 0 14px;
  font-size: 13px;
}

.text-button {
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(56, 134, 217, 0.16);
  border-radius: 999px;
  background: #fff;
  color: var(--admin-primary-deep);
  box-shadow: none;
  flex: 0 0 auto;
  white-space: nowrap;
  font-size: 13px;
}

.text-button:hover {
  transform: translateY(-1px);
  color: var(--admin-primary-deep);
}

.primary-text {
  color: var(--admin-primary-deep);
  font-weight: 600;
}

.batch-side-drawer :deep(.el-drawer__body) {
  padding: 18px 18px 22px;
  overflow: auto;
}

.assignment-drawer-shell {
  display: grid;
  gap: 20px;
}

.batch-workbench-drawer-shell {
  align-content: start;
}

.assignment-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.assignment-overview div,
.assignment-form {
  padding: 16px 18px;
  border: 1px solid rgba(56, 134, 217, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
}

.assignment-overview span {
  display: block;
  margin-bottom: 6px;
  color: var(--admin-text-soft);
  font-size: 12px;
}

.assignment-overview strong {
  color: var(--admin-text);
  font-size: 14px;
}

.assignment-form {
  display: grid;
  gap: 14px;
}

.assignment-warning {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(244, 63, 94, 0.18);
  background: rgba(255, 241, 242, 0.94);
}

.assignment-warning strong {
  color: #be123c;
}

.assignment-warning p {
  margin: 8px 0 0;
  color: #9f1239;
  line-height: 1.6;
}

.ghost.danger {
  border-color: rgba(244, 63, 94, 0.18);
  color: #be123c;
}

.empty-state h3 {
  margin-bottom: 10px;
}

.batch-list {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.batch-card {
  padding: 22px;
}

.batch-main {
  display: grid;
  grid-template-columns: 132px 1fr;
  gap: 18px;
}

.product-image {
  width: 132px;
  height: 132px;
  object-fit: cover;
  border-radius: 22px;
  background: linear-gradient(160deg, #eef7ff, #dbeeff);
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.batch-code {
  margin-bottom: 6px;
  color: var(--admin-text-soft);
  font-size: 14px;
  letter-spacing: 0.04em;
}

.batch-copy h3 {
  margin-bottom: 8px;
  font-size: 28px;
  color: var(--admin-text);
}

.next-copy {
  margin-bottom: 0;
  color: #4a6b90;
  line-height: 1.7;
}

.title-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.status-badge,
.next-badge,
.progress-pill,
.tag-row span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
}

.status-badge {
  min-height: 36px;
  font-weight: 700;
}

.next-badge {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-deep);
  font-size: 13px;
}

.status-badge.draft {
  background: rgba(120, 147, 180, 0.16);
  color: #59759b;
}

.status-badge.published {
  background: var(--admin-success-bg);
  color: var(--admin-success-text);
}

.status-badge.frozen {
  background: rgba(214, 137, 58, 0.18);
  color: #8b4c13;
}

.status-badge.recalled {
  background: rgba(190, 70, 58, 0.16);
  color: #8f2f29;
}

.meta-grid {
  margin-top: 16px;
}

.meta-grid div {
  padding: 14px;
  border-radius: 18px;
  background: var(--admin-surface-soft);
}

.meta-grid strong {
  display: block;
  margin-top: 8px;
  color: var(--admin-text);
  line-height: 1.5;
}

.progress-strip,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.progress-pill {
  background: rgba(120, 147, 180, 0.12);
  color: #5f7b9e;
  font-size: 13px;
  flex: 0 0 auto;
  white-space: nowrap;
}

.progress-pill.done {
  background: var(--admin-success-bg);
  color: var(--admin-success-text);
}

.progress-pill.success {
  background: var(--admin-success-bg);
  color: var(--admin-success-text);
}

.progress-pill.warning {
  background: rgba(242, 139, 34, 0.14);
  color: #b96b16;
}

.progress-pill.danger {
  background: rgba(221, 74, 74, 0.12);
  color: #b63f3f;
}

.progress-pill.pending {
  background: rgba(120, 147, 180, 0.12);
  color: #5f7b9e;
}

.tag-row span {
  background: var(--admin-primary-soft);
  color: var(--admin-primary-deep);
  font-size: 13px;
}

.tag-row .missing-tag {
  background: rgba(242, 204, 105, 0.16);
  color: #7b5d13;
}

.action-row {
  margin-top: 18px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 24, 20, 0.42);
}

.dialog-card {
  width: min(920px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.icon-button {
  min-width: 72px;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 20px;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.checkbox-field input {
  width: 18px;
  height: 18px;
}

.checkbox-field span {
  margin: 0;
}

@media (max-width: 1080px) {
  .stats-grid,
  .mode-row,
  .meta-grid,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .batch-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .batch-filter-layout,
  .batch-filter-toolbar {
    margin-left: 0;
    padding-left: 0;
    padding-right: 0;
  }

  .batch-filter-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .template-grid {
    grid-template-columns: 1fr;
  }

  .assignment-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .batch-summary-row {
    flex-direction: column;
    align-items: stretch;
  }

  .batch-summary-actions {
    justify-content: flex-start;
  }

  .page-shell {
    padding: var(--admin-page-shell-padding-mobile);
  }

  .hero-card,
  .batch-main {
    grid-template-columns: 1fr;
  }

  .queue-strip {
    grid-template-columns: 1fr;
  }

  .product-image {
    width: 100%;
    height: 220px;
  }

  .title-row,
  .quick-entry-card {
    flex-direction: column;
  }

  .title-side {
    align-items: flex-start;
  }

  .uploaded-file-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .batch-filter-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .mode-banner-note {
    align-items: flex-start;
  }

  .batch-table-head {
    display: none;
  }

  .batch-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .assignment-overview {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero-card h1 {
    font-size: 30px;
  }

  .stats-grid,
  .mode-row,
  .meta-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .batch-filter-grid {
    grid-template-columns: 1fr;
  }

  .batch-filter-actions {
    width: 100%;
  }

  .batch-filter-actions button {
    flex: 1 1 auto;
  }

  .batch-copy h3 {
    font-size: 24px;
  }
}
</style>

<style src="../assets/styles/admin-task-pages.css" scoped></style>
<style src="../assets/styles/admin-ledger-unified.css" scoped></style>

