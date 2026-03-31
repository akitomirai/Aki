<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
import { resolveQrStatusText, resolveTaskStatusText, resolveTodayStatusText } from '../utils/statusPresentation'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const message = ref('')
const messageType = ref('info')
const batches = ref([])

const filters = ref(createFilterState())
const listMode = ref('ACTION')
const dialog = ref(createDialogState())
const assignmentDialog = ref(createAssignmentDialogState())
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
const lastTraceStage = ref('PRODUCE')
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
  return formProductOptions.value.find((item) => item.id === batchForm.value.productId) ?? null
})
const canManageAssignment = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(authStore.user?.roleCode))
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

const listStats = computed(() => {
  return {
    total: batchCards.value.length,
    actionable: batchCards.value.filter((card) => card.insight.isActionable).length,
    ready: batchCards.value.filter((card) => card.insight.readyToPublish).length,
    risk: batchCards.value.filter((card) => card.insight.isRisk).length
  }
})

const modeCounts = computed(() => ({
  ACTION: listStats.value.actionable,
  READY: listStats.value.ready,
  RISK: listStats.value.risk,
  LIVE: batchCards.value.filter((card) => card.item.status === 'PUBLISHED').length,
  ALL: listStats.value.total
}))

const topQueue = computed(() => {
  return batchCards.value.filter((card) => card.insight.isActionable).slice(0, 3)
})

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'create':
      return '新增批次'
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
})

watch(
  () => route.query.mode,
  () => {
    syncListModeFromRoute()
  }
)

async function fetchBatches() {
  loading.value = true
  try {
    const response = await getBatchList(cleanObject(filters.value))
    batches.value = response.data ?? []
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
    batchName: ''
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

function resetFilters() {
  filters.value = createFilterState()
  switchListMode('ACTION')
  fetchBatches()
}

function syncListModeFromRoute() {
  const mode = String(route.query.mode || '').toUpperCase()
  const exists = listModes.some((item) => item.value === mode)
  listMode.value = exists ? mode : 'ACTION'
}

function switchListMode(mode) {
  listMode.value = mode
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
  dialog.value = {
    visible: true,
    type: 'create',
    batchId: null,
    batchName: ''
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
  dialog.value = createDialogState()
}

async function submitDialog(options = {}) {
  const { keepOpen = false } = options

  try {
    if (dialog.value.type === 'create') {
      const response = await createBatch({
        batchCode: batchForm.value.batchCode,
        productId: batchForm.value.productId,
        companyId: batchForm.value.companyId,
        originPlace: batchForm.value.originPlace,
        productionDate: batchForm.value.productionDate,
        publicRemark: batchForm.value.publicRemark,
        internalRemark: batchForm.value.internalRemark
      })
      showMessage('批次已创建，已直接带你进入工作台继续补录。', 'success')
      closeDialog()
      await fetchBatches()
      router.push({
        path: `/batches/${response.data.batch.id}`,
        query: {
          created: '1',
          focus: 'trace'
        }
      })
      return
    }

    if (dialog.value.type === 'edit') {
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
      router.push(`/batches/${response.data.batch.id}`)
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
      router.push(`/batches/${response.data.batch.id}`)
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
    router.push(`/batches/${item.id}`)
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码生成失败，请稍后再试。'), 'error')
  }
}

async function handleBatchCompanyChange(resetProduct = true) {
  const companyId = batchForm.value.companyId
  if (resetProduct) {
    batchForm.value.productId = null
  }
  await loadProductOptions(companyId)
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
  let nextLabel = '进入工作台'
  let nextCopy = '打开批次工作台继续处理。'
  let priority = 100

  if (item.status === 'RECALLED') {
    nextLabel = '查看召回处置'
    nextCopy = '先看风险处理记录与公开页风险提示。'
    priority = 520
  } else if (item.status === 'FROZEN') {
    if (actionOf(item, 'RESUME').enabled) {
      nextActionCode = 'RESUME'
      nextLabel = '恢复发布'
      nextCopy = '整改检查项已满足，可以恢复发布。'
      priority = 460
    } else {
      nextLabel = '补风险处理'
      nextCopy = '先补处理说明、整改记录，再考虑恢复。'
      priority = 430
    }
  } else if (missing.length) {
    nextActionCode = missing[0].actionCode
    nextLabel = {
      ADD_TRACE: '去补追溯',
      UPLOAD_QUALITY: '去传质检',
      GENERATE_QR: '去生成二维码',
      PUBLISH: '去发布'
    }[nextActionCode] ?? '继续处理'
    nextCopy = {
      ADD_TRACE: '先补关键追溯节点，方便后续查看和扫码查询。',
      UPLOAD_QUALITY: '补上质检摘要后，公开页可信度会明显更高。',
      GENERATE_QR: '二维码就绪后，才能顺畅衔接公开查询。',
      PUBLISH: '资料已经齐全，可以直接完成对外发布。'
    }[nextActionCode] ?? '继续处理当前批次。'
    priority = item.status === 'DRAFT' ? 360 - missing.length * 10 : 220
  } else if (item.status === 'PUBLISHED') {
    nextLabel = '查看公开页表现'
    nextCopy = '已发布，可回工作台查看二维码与扫码情况。'
    priority = 180
  }

  return {
    isRisk,
    isActionable: isRisk || item.status === 'DRAFT',
    readyToPublish,
    missing,
    nextActionCode,
    nextLabel,
    nextCopy,
    priority,
    progress: [
      { label: '杩芥函', done: !needsTrace },
      { label: '璐ㄦ', done: hasQuality },
      { label: '二维码', done: hasQr },
      { label: '鍙戝竷', done: item.status === 'PUBLISHED' }
    ]
  }
}

function runRecommendedAction(card) {
  const { item, insight } = card
  switch (insight.nextActionCode) {
    case 'ADD_TRACE':
      openTraceDialog(item)
      return
    case 'UPLOAD_QUALITY':
      openQualityDialog(item)
      return
    case 'GENERATE_QR':
      handleGenerateQr(item)
      return
    case 'PUBLISH':
      openStatusDialog(item, 'PUBLISHED')
      return
    case 'RESUME':
      openStatusDialog(item, 'PUBLISHED')
      return
    default:
      router.push(`/batches/${item.id}`)
  }
}

function handleRowCommand(card, command) {
  if (command === 'assignment') {
    openAssignmentDialog(card.item)
    return
  }
  if (command === 'edit') {
    openEditDialog(card.item)
    return
  }
  if (command === 'trace') {
    openTraceDialog(card.item)
    return
  }
  if (command === 'quality') {
    openQualityDialog(card.item)
    return
  }
  if (command === 'qr') {
    handleGenerateQr(card.item)
    return
  }
  if (command === 'publish') {
    openStatusDialog(card.item, 'PUBLISHED')
    return
  }
  if (command === 'freeze') {
    openStatusDialog(card.item, 'FROZEN')
    return
  }
  if (command === 'recall') {
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
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">批次管理</h1>
      </div>
      <div class="manage-page-actions">
        <el-button :loading="loading" data-testid="batch-search-button" @click="fetchBatches">刷新</el-button>
        <el-button type="primary" data-testid="batch-create-button" @click="openCreateDialog">新增批次</el-button>
      </div>
    </section>

    <section class="panel batch-tabs-panel">
      <div class="batch-tabs">
        <button
          v-for="item in listModes"
          :key="item.value"
          type="button"
          class="batch-tab"
          :class="{ active: listMode === item.value }"
          @click="switchListMode(item.value)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ modeCounts[item.value] ?? 0 }}</strong>
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="filter-grid">
        <label>
          <span>批次号</span>
          <input v-model.trim="filters.batchCode" data-testid="batch-filter-code" type="text" placeholder="输入批次号">
        </label>
        <label>
          <span>产品名称</span>
          <input v-model.trim="filters.productName" type="text" placeholder="输入产品名称">
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label>
          <span>企业</span>
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
        <label>
          <span>生产日期起</span>
          <input v-model="filters.dateFrom" type="date">
        </label>
        <label>
          <span>生产日期止</span>
          <input v-model="filters.dateTo" type="date">
        </label>
      </div>

      <div class="toolbar">
        <span class="list-summary">共 {{ listStats.total }} 个批次，当前显示 {{ visibleBatchCards.length }} 个。</span>
        <div class="toolbar-actions">
          <button class="primary" :disabled="loading" @click="fetchBatches">查询</button>
          <button class="ghost" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">
      {{ message }}
    </section>

    <section v-if="loading" class="panel empty-state">
      正在加载批次列表...
    </section>

    <section v-else-if="!visibleBatchCards.length" class="panel empty-state">
      <div>
        <h3>当前条件下没有批次数据</h3>
        <p>请调整筛选条件，或直接新增批次。</p>
        <div class="toolbar-actions">
          <button class="primary" @click="openCreateDialog">新增批次</button>
          <button class="ghost" @click="switchListMode('ALL')">查看全部</button>
        </div>
      </div>
    </section>

    <section v-else class="panel">
      <div class="batch-table-head">
        <span>批次信息</span>
        <span>企业 / 节点</span>
        <span>质量 / 二维码</span>
        <span>状态 / 下一步</span>
        <span>任务分配</span>
        <span>操作</span>
      </div>

      <div class="batch-row-list">
        <article
          v-for="card in visibleBatchCards"
          :key="card.item.id"
          class="batch-row"
          :data-testid="`batch-card-${card.item.id}`"
        >
          <div class="row-main">
            <strong>{{ card.item.batchCode }}</strong>
            <span>{{ card.item.productName }}</span>
            <small>{{ card.item.productionDate }} / {{ localizeVisibleText(card.item.originPlace) }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ card.item.companyName }}</strong>
            <span>{{ card.item.currentNode }}</span>
          </div>

          <div class="row-meta">
            <strong>{{ card.item.qualityStatus }}</strong>
            <span>{{ resolveQrStatusText({ statusLabel: card.item.qrStatusLabel, status: card.item.qrStatus }) }}</span>
          </div>

          <div class="row-status">
            <span class="status-badge" :class="statusClass(card.item.status)">{{ card.item.statusLabel }}</span>
            <span class="next-badge" :data-testid="`batch-next-${card.item.id}`">{{ card.insight.nextLabel }}</span>
          </div>

          <div class="row-task" :data-testid="`batch-task-block-${card.item.id}`">
            <strong :data-testid="`batch-task-assignee-${card.item.id}`">{{ card.item.assigneeName || '未分配操作员' }}</strong>
            <span :data-testid="`batch-task-assigned-at-${card.item.id}`">{{ card.item.assignedAt || '暂无分配时间' }}</span>
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

          <div class="row-actions">
            <button
              v-if="canManageAssignment"
              class="text-button"
              :data-testid="`batch-assignment-open-${card.item.id}`"
              @click="openAssignmentDialog(card.item)"
            >
              任务分配
            </button>
            <button
              class="text-button primary-text"
              :data-testid="`batch-open-workbench-${card.item.id}`"
              @click="router.push(`/batches/${card.item.id}`)"
            >
              工作台
            </button>
            <button
              class="text-button"
              :data-testid="`batch-recommend-${card.item.id}`"
              @click="runRecommendedAction(card)"
            >
              {{ card.insight.nextLabel }}
            </button>
            <el-dropdown @command="(command) => handleRowCommand(card, command)">
              <button type="button" class="text-button">更多操作</button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canManageAssignment" command="assignment">任务分配</el-dropdown-item>
                  <el-dropdown-item command="edit">编辑资料</el-dropdown-item>
                  <el-dropdown-item command="trace">补录追溯</el-dropdown-item>
                  <el-dropdown-item command="quality">上传质检</el-dropdown-item>
                  <el-dropdown-item command="qr">生成二维码</el-dropdown-item>
                  <el-dropdown-item
                    command="publish"
                    :disabled="!(actionOf(card.item, 'PUBLISH').enabled || actionOf(card.item, 'RESUME').enabled)"
                  >
                    {{ actionOf(card.item, 'RESUME').enabled ? '恢复发布' : '发布' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="freeze" :disabled="!actionOf(card.item, 'FREEZE').enabled">冻结</el-dropdown-item>
                  <el-dropdown-item command="recall" :disabled="!actionOf(card.item, 'RECALL').enabled">召回</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </article>
      </div>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">批次操作</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost icon-button" @click="closeDialog">关闭</button>
        </div>

        <div v-if="dialog.type === 'create' || dialog.type === 'edit'" class="form-grid" data-testid="batch-edit-dialog">
          <label v-if="dialog.type === 'create'">
            <span>批次号</span>
            <input v-model.trim="batchForm.batchCode" type="text">
          </label>
          <label v-else>
            <span>批次号</span>
            <input :value="batchForm.batchCode" type="text" disabled>
          </label>
          <label>
            <span>企业</span>
            <select v-model="batchForm.companyId" @change="handleBatchCompanyChange()">
              <option :value="null">请选择企业</option>
              <option v-for="item in formCompanyOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span>产品</span>
            <select v-model="batchForm.productId" :disabled="!batchForm.companyId">
              <option :value="null">{{ batchForm.companyId ? '请选择产品' : '请先选择企业' }}</option>
              <option v-for="item in formProductOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span>产地</span>
            <input v-model.trim="batchForm.originPlace" type="text" placeholder="例如 江西赣州信丰">
          </label>
          <label>
            <span>生产日期</span>
            <input v-model="batchForm.productionDate" type="date">
          </label>
          <label v-if="selectedProductOption" class="full-width">
            <span>当前产品</span>
            <div class="field-note">
              <strong>{{ selectedProductOption.name }}</strong>
              <small>
                分类：{{ selectedProductOption.category || '待补充' }}；
                规格：{{ selectedProductOption.specification || '待补充' }}
              </small>
            </div>
          </label>
          <label class="full-width">
            <span>公开说明</span>
            <textarea
              v-model.trim="batchForm.publicRemark"
              rows="3"
              placeholder="填写消费者可见的批次说明，例如产地、工艺特点或本批次情况"
            />
          </label>
          <label class="full-width">
            <span>内部备注</span>
            <textarea
              v-model.trim="batchForm.internalRemark"
              rows="3"
              placeholder="用于记录补录计划、处理提醒或内部跟进说明"
            />
          </label>
          <div class="full-width flow-tip">
            <strong>{{ dialog.type === 'create' ? '创建后将直接进入批次工作台。' : '保存后将返回批次工作台。' }}</strong>
            <span>下一步入口会继续保留在工作台中，可补录追溯、上传质检和生成二维码。</span>
          </div>
        </div>

        <div v-else-if="dialog.type === 'trace'" class="form-grid" data-testid="batch-trace-dialog">
          <div class="full-width quick-entry-card">
            <div>
              <p class="eyebrow">现场快速补录</p>
              <h4>当前表单已按少录入、少跳转整理</h4>
              <p>
                默认带入当前时间，阶段、地点和说明可以直接点选。
                {{ traceDialogContext.totalCount ? `当前批次已有 ${traceDialogContext.totalCount} 条记录。` : '当前还没有记录，可先补第一条关键节点。' }}
              </p>
            </div>
            <button
              v-if="traceDialogContext.latestRecord"
              class="ghost"
              @click="copyLatestTraceRecord"
            >
              澶嶅埗涓婁竴鏉″苟寰皟
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
            <span>闃舵</span>
            <select v-model="traceForm.stage" @change="setTraceStage(traceForm.stage)">
              <option v-for="item in stageOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>璁板綍鏃堕棿</span>
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
            <input v-model.trim="traceForm.title" type="text" placeholder="涓€鍙ヨ瘽璇存槑杩欎釜鑺傜偣">
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
              <span>甯哥敤鍦扮偣</span>
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
              <span>璇存槑妯℃澘</span>
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
            <span>鐜板満鍥剧墖</span>
            <div class="upload-box">
              <input type="file" accept="image/*" multiple @change="handleTraceFilesChange">
              <small>鏀寔澶氬浘涓婁紶銆備笂浼犳垚鍔熷悗浼氱珛鍒绘樉绀哄湪涓嬮潰锛屼繚瀛樿褰曟椂鑷姩涓€璧风粦瀹氥€</small>
            </div>
          </label>
          <label class="full-width">
            <span>鍥剧墖閾炬帴鍏滃簳</span>
            <input v-model.trim="traceForm.imageUrl" type="url" placeholder="濡傚凡鍦ㄥ閮ㄥ浘搴婏紝鍙矘璐村浘鐗囧湴鍧€">
          </label>

          <div v-if="traceUploading" class="full-width upload-hint">姝ｅ湪涓婁紶鍥剧墖锛岀◢绛変竴涓嬪氨浼氭樉绀哄湪褰撳墠璁板綍閲?..</div>

          <div v-if="traceForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in traceForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }} 路 宸蹭笂浼狅紝淇濆瓨璁板綍鍚庣敓鏁</small>
              </div>
              <div class="inline-actions">
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">鏌ョ湅</a>
                <button class="ghost" @click="removeTraceAttachment(item.id)">绉婚櫎</button>
              </div>
            </article>
          </div>

          <label class="checkbox-field full-width">
            <input v-model="traceForm.visibleToConsumer" type="checkbox">
            <span>杩欐潯璁板綍鍚屾灞曠ず缁欐秷璐硅€</span>
          </label>

          <div v-if="traceDialogContext.latestRecord" class="full-width last-record-card">
            <span>涓婁竴鏉¤褰</span>
            <strong>{{ traceDialogContext.latestRecord.title }}</strong>
            <p>{{ traceDialogContext.latestRecord.location }} 路 {{ traceDialogContext.latestRecord.eventTime }}</p>
            <small>{{ traceDialogContext.latestRecord.summary }}</small>
          </div>
        </div>

        <div v-else-if="dialog.type === 'quality'" class="form-grid" data-testid="batch-quality-dialog">
          <label>
            <span>鎶ュ憡缂栧彿</span>
            <input v-model.trim="qualityForm.reportNo" type="text" placeholder="渚嬪 JX-20260325-01">
          </label>
          <label>
            <span>检测机构</span>
            <input v-model.trim="qualityForm.agency" type="text" placeholder="例如 江西省农产品质检中心">
          </label>
          <label>
            <span>缁撴灉</span>
            <select v-model="qualityForm.result">
              <option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>检测时间</span>
            <input v-model="qualityForm.reportTime" type="datetime-local">
          </label>
          <label class="full-width">
            <span>璐ㄦ鎽樿</span>
            <textarea
              v-model.trim="qualityForm.highlightsText"
              rows="4"
              placeholder="每行一个重点，例如：关键指标合格 / 企业自检正常 / 抽检无异常"
            />
          </label>
          <label class="full-width">
            <span>闄勪欢</span>
            <div class="upload-box">
              <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
              <small>鍙笂浼?PDF 鎴栧浘鐗囷紝鍏紑椤典細浼樺厛灞曠ず璐ㄦ缁撹锛屽悗鍙板悓鏃朵繚鐣欓檮浠跺鏌ャ€</small>
            </div>
          </label>
          <div v-if="qualityUploading" class="full-width upload-hint">姝ｅ湪涓婁紶璐ㄦ闄勪欢...</div>
          <div v-if="qualityForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in qualityForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }}</small>
              </div>
              <div class="inline-actions">
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">鏌ョ湅</a>
                <button class="ghost" @click="removeQualityAttachment(item.id)">绉婚櫎</button>
              </div>
            </article>
          </div>
        </div>

        <div v-else-if="dialog.type === 'status'" class="form-grid">
          <label>
            <span>鐩爣鐘舵€</span>
            <select v-model="statusForm.targetStatus">
              <option value="PUBLISHED">鍙戝竷</option>
              <option value="FROZEN">鍐荤粨</option>
              <option value="RECALLED">鍙洖</option>
            </select>
          </label>
          <label>
            <span>澶勭悊浜</span>
            <input v-model.trim="statusForm.operatorName" type="text">
          </label>
          <label class="full-width">
            <span>澶勭悊鍘熷洜</span>
            <textarea
              v-model.trim="statusForm.reason"
              rows="4"
              placeholder="寤鸿鍐欐竻妤氬綋鍓嶆壒娆′负浠€涔堣鍙戝竷銆佸喕缁撴垨鍙洖"
            />
          </label>
        </div>

        <div class="dialog-actions">
          <button class="ghost" @click="closeDialog">鍙栨秷</button>
          <button
            v-if="dialog.type === 'trace'"
            class="ghost"
            data-testid="batch-trace-save-continue"
            @click="submitDialog({ keepOpen: true })"
          >
            淇濆瓨骞剁户缁?
          </button>
          <button class="primary" data-testid="batch-dialog-submit" @click="submitDialog()">
            {{ dialog.type === 'create' ? '鍒涘缓骞惰繘鍏ュ伐浣滃彴' : '纭淇濆瓨' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="assignmentDialog.visible" class="dialog-mask" @click.self="closeAssignmentDialog">
      <section class="dialog-card assignment-dialog-card" data-testid="batch-assignment-dialog">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">任务分配</p>
            <h3>批次 {{ assignmentDialog.batchCode }}</h3>
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
      </section>
    </div>
  </div>
</template>

<style scoped>
.page-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 28px 20px 48px;
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
.panel,
.stat-card,
.batch-card,
.dialog-card,
.message-bar {
  border-radius: 28px;
  background: var(--admin-surface);
  box-shadow: var(--admin-shadow);
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

h1,
h2,
h3,
h4,
p {
  margin-top: 0;
}

h1 {
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
.panel,
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

.panel {
  margin-top: 18px;
}

.toolbar {
  align-items: center;
  justify-content: space-between;
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
.flow-tip,
.quick-entry-card,
.last-record-card,
.template-card {
  border: 1px solid var(--admin-border);
  border-radius: 18px;
  background: var(--admin-surface-soft);
}

.field-note,
.flow-tip,
.quick-entry-card,
.last-record-card,
.template-card {
  padding: 16px;
}

.flow-tip span,
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
.last-record-card strong,
.field-note strong,
.uploaded-file-item strong {
  display: block;
  color: var(--admin-text);
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

button,
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

button:hover,
.preview-link:hover {
  transform: translateY(-1px);
}

button:disabled {
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

.batch-table-head {
  display: grid;
  grid-template-columns: 1.1fr 0.95fr 0.85fr 0.85fr 1.15fr 1.25fr;
  gap: 16px;
  padding: 0 0 14px;
  border-bottom: 1px solid rgba(56, 134, 217, 0.12);
  color: var(--admin-text-soft);
  font-size: 13px;
}

.batch-row-list {
  display: grid;
}

.batch-row {
  display: grid;
  grid-template-columns: 1.1fr 0.95fr 0.85fr 0.85fr 1.15fr 1.25fr;
  gap: 16px;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid rgba(56, 134, 217, 0.1);
}

.batch-row:last-child {
  border-bottom: 0;
}

.row-main,
.row-meta,
.row-status,
.row-task {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-main strong,
.row-meta strong {
  color: var(--admin-text);
  font-size: 14px;
}

.row-main span,
.row-main small,
.row-meta span,
.row-task span {
  color: var(--admin-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.row-status {
  align-items: flex-start;
}

.row-task strong {
  color: var(--admin-text);
  font-size: 14px;
}

.task-pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.task-state-badge,
.task-flag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
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
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.text-button {
  min-height: auto;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--admin-text-soft);
  box-shadow: none;
}

.text-button:hover {
  transform: none;
  color: var(--admin-primary-deep);
}

.primary-text {
  color: var(--admin-primary-deep);
  font-weight: 600;
}

.assignment-dialog-card {
  max-width: 760px;
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
}

.progress-pill.done {
  background: var(--admin-success-bg);
  color: var(--admin-success-text);
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
  .filter-grid,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .template-grid {
    grid-template-columns: 1fr;
  }

  .batch-table-head,
  .batch-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .assignment-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .page-shell {
    padding-inline: 14px;
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
  h1 {
    font-size: 30px;
  }

  .stats-grid,
  .mode-row,
  .meta-grid,
  .filter-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .batch-copy h3 {
    font-size: 24px;
  }
}
</style>

