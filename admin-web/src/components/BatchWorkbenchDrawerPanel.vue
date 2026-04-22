<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  changeBatchStatus,
  createQualityReport,
  createRiskAction,
  createTraceRecord,
  generateBatchQr,
  getBatchDetail,
  getOperatorOptions,
  updateBatchAssignment,
  uploadBatchFiles
} from '../api/batch'
import { useAuthStore } from '../stores/auth'
import {
  createQualityForm,
  createTraceForm,
  qualityOptions,
  riskActionOptions,
  splitHighlightsInput,
  stageOptions
} from '../utils/traceWorkflow'
import { canBatchStatusTransition, resolvePublishBlockState } from '../utils/batchStatusFlow'
import { resolveQrStatusText, resolveRiskStatusText, resolveTaskStatusText, resolveTodayStatusText } from '../utils/statusPresentation'
import { canManageAdminBatch, isRegulator } from '../utils/access'

const props = defineProps({
  batchId: {
    type: [String, Number],
    default: null
  }
})

const router = useRouter()
const authStore = useAuthStore()

const resolvedBatchId = computed(() => String(props.batchId ?? '').trim())
const activeTab = ref('overview')
const loading = ref(true)
const detail = ref(null)
const message = ref('')
const messageType = ref('info')
const dialog = ref({ visible: false, type: '' })
const assignmentSectionRef = ref(null)
const riskSectionRef = ref(null)

const operatorOptions = ref([])
const operatorLoading = ref(false)
const assignmentSaving = ref(false)
const assignmentForm = ref({ assigneeUserId: '' })
const assignmentConfirm = ref({ visible: false, mode: 'reassign', message: '', nextAssigneeUserId: '' })

const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const riskForm = ref(createRiskForm())
const statusForm = ref(createStatusForm())
const dialogSubmitting = ref(false)
const traceUploading = ref(false)
const qualityUploading = ref(false)

const roleCode = computed(() => authStore.user?.roleCode || '')
const canManageBatch = computed(() => canManageAdminBatch(roleCode.value))
const readOnlyBatchView = computed(() => isRegulator(roleCode.value))
const canManageAssignment = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(roleCode.value))

const batch = computed(() => detail.value?.batch ?? {})
const company = computed(() => detail.value?.company ?? {})
const recentRecords = computed(() => detail.value?.trace?.recentRecords ?? [])
const latestRecord = computed(() => recentRecords.value[0] ?? null)
const latestQualityReport = computed(() => detail.value?.quality?.latestReport ?? null)
const latestRiskAction = computed(() => detail.value?.riskHandling?.history?.[0] ?? null)

const batchName = computed(() => batch.value.productName || detail.value?.product?.name || '未命名批次')
const batchCode = computed(() => batch.value.batchCode || '未生成批次号')
const companyName = computed(() => company.value.name || batch.value.companyName || '未关联企业')
const batchStatusText = computed(() => detail.value?.status?.label || '状态待确认')
const taskStatusText = computed(() => resolveTaskStatusText(detail.value?.task))
const todayProgressText = computed(() => resolveTodayStatusText(detail.value?.task?.todayCompleted))
const riskStageText = computed(() => resolveRiskStatusText(detail.value?.risk, detail.value?.riskHandling))
const qualityUploaded = computed(() => Number(detail.value?.quality?.reportCount || 0) > 0)
const qualityResultCode = computed(() => String(latestQualityReport.value?.result || '').toUpperCase())
const qualityAllowsPublish = computed(() => qualityUploaded.value && qualityResultCode.value !== 'FAIL')
const qrGenerated = computed(() => Boolean(detail.value?.qr?.generated))
const qrStatusText = computed(() => resolveQrStatusText(detail.value?.qr))
const canPreviewPublic = computed(() => Boolean(detail.value?.qr?.publicUrl))
const canHandleRisk = computed(() => ['FROZEN', 'RECALLED'].includes(detail.value?.status?.code))
const riskResolved = computed(() => {
  const currentStage = String(detail.value?.riskHandling?.currentStage || detail.value?.risk?.status || '').toUpperCase()
  return currentStage === 'RECTIFIED' || Boolean(detail.value?.riskHandling?.canResume)
})
const publishGate = computed(() => resolvePublishBlockState({
  status: detail.value?.status?.code,
  qualityStatusCode: qualityResultCode.value || 'PENDING',
  qrStatus: qrGenerated.value ? 'READY' : 'NOT_GENERATED',
  canResume: Boolean(detail.value?.riskHandling?.canResume)
}))
const traceAction = computed(() => actionOf('ADD_TRACE'))
const qualityAction = computed(() => actionOf('UPLOAD_QUALITY'))
const qrAction = computed(() => actionOf('GENERATE_QR'))
const publishAction = computed(() => actionOf('PUBLISH'))
const resumeAction = computed(() => actionOf('RESUME'))
const recallAction = computed(() => actionOf('RECALL'))
const publishReady = computed(() => publishAction.value.enabled || resumeAction.value.enabled)
const hasTraceRecord = computed(() => recentRecords.value.length > 0)
const currentAssigneeId = computed(() => detail.value?.task?.assigneeUserId ? String(detail.value.task.assigneeUserId) : '')
const selectedAssigneeId = computed(() => assignmentForm.value.assigneeUserId ? String(assignmentForm.value.assigneeUserId) : '')
const assignmentChanged = computed(() => selectedAssigneeId.value !== currentAssigneeId.value)
const selectedAssignee = computed(() => operatorOptions.value.find((item) => String(item.id) === selectedAssigneeId.value) ?? null)
const assignmentActionLabel = computed(() => {
  if (!selectedAssigneeId.value) return '确认清空分配'
  if (!currentAssigneeId.value) return '确认分配'
  if (assignmentChanged.value) return '确认改派'
  return '当前分配未变更'
})
const assignmentConfirmActionLabel = computed(() => assignmentConfirm.value.mode === 'clear' ? '强制清空分配' : '强制改派')
const hasAssignableOperators = computed(() => operatorOptions.value.length > 0)
const assignmentSelectDisabled = computed(() => operatorLoading.value || assignmentSaving.value)
const assignmentCanClear = computed(() => Boolean(detail.value?.task?.assigneeUserId) && !assignmentSaving.value)
const assignmentSaveDisabled = computed(() => {
  if (assignmentSaving.value || operatorLoading.value || !assignmentChanged.value) {
    return true
  }
  if (!hasAssignableOperators.value && !selectedAssigneeId.value) {
    return !currentAssigneeId.value
  }
  return false
})
const assignmentHelperText = computed(() => {
  if (operatorLoading.value) return '正在加载可分配操作员...'
  if (!canManageAssignment.value) return ''
  if (!hasAssignableOperators.value && currentAssigneeId.value) return '当前没有可分配操作员，可保留当前负责人或清空分配。'
  if (!hasAssignableOperators.value) return '当前没有可分配操作员。'
  if (detail.value?.task?.draftPending) return '当前负责人还有未提交草稿，改派时会先做确认。'
  if (selectedAssignee.value) return `将指派给 ${selectedAssignee.value.realName || selectedAssignee.value.username}。`
  if (currentAssigneeId.value) return '留空表示清空分配。'
  return '请选择操作员。'
})
const draftStatusText = computed(() => detail.value?.task?.draftStatusLabel || (detail.value?.task?.draftPending ? '有草稿' : '无草稿'))
const readinessDoneCount = computed(() => [hasTraceRecord.value, qualityUploaded.value && qualityAllowsPublish.value, qrGenerated.value].filter(Boolean).length)
const topSummaryMeta = computed(() => ([
  { label: '批次号', value: batchCode.value },
  { label: '企业', value: companyName.value }
]))
const overviewActionTiles = computed(() => ([
  { key: 'trace', label: '补录追溯', status: hasTraceRecord.value ? '已补录' : '待补录', disabled: readOnlyBatchView.value || !traceAction.value.enabled, handler: openTraceDialog },
  { key: 'quality', label: '上传质检', status: qualityUploaded.value ? (detail.value?.quality?.label || '已上传') : '待上传', disabled: readOnlyBatchView.value || !qualityAction.value.enabled, handler: openQualityDialog },
  { key: 'publish', label: resumeAction.value.enabled ? '恢复发布' : '发布批次', status: publishReady.value ? '可执行' : '未就绪', disabled: readOnlyBatchView.value || !publishReady.value, handler: () => openStatusDialog('PUBLISHED') },
  { key: 'assignment', label: '任务分配', status: detail.value?.task?.assigneeName || '未分配', disabled: !canManageAssignment.value, handler: openAssignmentDrawer },
  { key: 'risk', label: '风险处理', status: canHandleRisk.value ? riskStageText.value : '查看', disabled: false, handler: openRiskDrawer },
  { key: 'public', label: '查看公开页', status: canPreviewPublic.value ? '已开放' : '未开放', disabled: !canPreviewPublic.value, handler: openPublicPreview }
]))
const coreStatusCards = computed(() => [
  { key: 'batch-status', title: '批次状态', value: batchStatusText.value, tag: String(detail.value?.status?.code || '').toUpperCase() === 'PUBLISHED' ? '公开中' : '' },
  { key: 'publish-ready', title: '发布准备度', value: publishReady.value ? (resumeAction.value.enabled ? '可恢复' : '可发布') : '未就绪', tag: `${readinessDoneCount.value}/3` },
  { key: 'task-status', title: '任务执行', value: taskStatusText.value, tag: detail.value?.task?.draftPending ? '有草稿' : todayProgressText.value },
  { key: 'risk-status', title: '风险事项', value: canHandleRisk.value ? riskStageText.value : '当前无风险', tag: canHandleRisk.value ? (riskResolved.value ? '可恢复' : '处理中') : '' }
])
const traceSummaryRows = computed(() => recentRecords.value.slice(0, 3))
const qualitySummaryRows = computed(() => ([
  { label: '质检状态', value: detail.value?.quality?.label || '待上传' },
  { label: '检测机构', value: latestQualityReport.value?.agency || '未填写' },
  { label: '报告编号', value: latestQualityReport.value?.reportNo || '未生成' },
  { label: '检测时间', value: formatDateTime(latestQualityReport.value?.reportTime) }
]))
const qrSummaryRows = computed(() => ([
  { label: '二维码状态', value: qrStatusText.value },
  { label: '公开状态', value: canPreviewPublic.value ? '已公开' : '未公开' },
  {
    label: '发布校验',
    value: String(detail.value?.status?.code || '').toUpperCase() === 'PUBLISHED'
      ? '已发布'
      : (publishReady.value ? (resumeAction.value.enabled ? '可恢复发布' : '可发布') : '未满足')
  },
  { label: '公开入口', value: detail.value?.qr?.publicUrl || '未生成' }
]))
const riskSummaryRows = computed(() => ([
  { label: '风险状态', value: canHandleRisk.value ? riskStageText.value : '当前无风险' },
  { label: '异常原因', value: detail.value?.risk?.reason || detail.value?.status?.reason || '未记录' },
  { label: '处理措施', value: latestRiskActionLabel.value },
  { label: '是否恢复公开', value: canHandleRisk.value ? (detail.value?.riskHandling?.canResume ? '可恢复' : '未恢复') : '正常公开' }
]))
const ownerInfoRows = computed(() => [
  { label: '当前负责人', value: detail.value?.task?.assigneeName || '未分配' },
  { label: '任务状态', value: taskStatusText.value },
  { label: '分配时间', value: formatDateTime(detail.value?.task?.assignedAt) },
  { label: '草稿状态', value: draftStatusText.value }
])
const baseInfoRows = computed(() => [
  { label: '批次号', value: batchCode.value },
  { label: '产品', value: batchName.value },
  { label: '企业', value: companyName.value },
  { label: '质检状态', value: detail.value?.quality?.label || '待上传', meta: latestQualityReport.value?.reportNo ? `报告编号：${latestQualityReport.value.reportNo}` : '' },
  { label: '二维码状态', value: qrStatusText.value, actionLabel: !readOnlyBatchView.value && qrAction.value.enabled ? '生成二维码' : '', actionDisabled: !qrAction.value.enabled, handler: handleGenerateQr },
  { label: '公开页状态', value: canPreviewPublic.value ? '已开放' : '未开放' },
  { label: '最近更新时间', value: formatDateTime(resolveLatestActivityTime()) }
])
const latestRiskActionLabel = computed(() => {
  if (!latestRiskAction.value) return canHandleRisk.value ? '尚未记录处理动作' : '当前无风险动作'
  return latestRiskAction.value.actionLabel || latestRiskAction.value.actionType || '已记录处理动作'
})
const riskInfoCards = computed(() => [
  { label: '当前风险状态', value: canHandleRisk.value ? riskStageText.value : '当前无风险' },
  { label: '最近动作', value: latestRiskActionLabel.value },
  { label: '整改结果', value: detail.value?.riskHandling?.canResume ? '已满足条件' : (canHandleRisk.value ? '未满足条件' : '无需整改') }
])
const riskActionButtons = computed(() => [
  { key: 'comment', label: '补处理说明', disabled: readOnlyBatchView.value || !canHandleRisk.value, handler: () => openRiskDialog('COMMENT') },
  { key: 'processing', label: '标记处理中', disabled: readOnlyBatchView.value || !canHandleRisk.value, handler: () => openRiskDialog('PROCESSING') },
  { key: 'rectification', label: '补整改记录', disabled: readOnlyBatchView.value || !canHandleRisk.value, handler: () => openRiskDialog('RECTIFICATION') },
  { key: 'rectified', label: '标记已整改', disabled: readOnlyBatchView.value || !canHandleRisk.value, handler: () => openRiskDialog('RECTIFIED') },
  { key: 'resume', label: '恢复发布', disabled: readOnlyBatchView.value || !(detail.value?.riskHandling?.canResume && resumeAction.value.enabled), handler: () => openStatusDialog('PUBLISHED') },
  { key: 'recall', label: '发起召回', disabled: readOnlyBatchView.value || !recallAction.value.enabled, handler: () => openStatusDialog('RECALLED') }
])
const timelineEntries = computed(() => detail.value?.traceTimeline?.items ?? [])
const statusTargetOptions = computed(() => {
  const currentStatus = detail.value?.status?.code
  return [
    { value: 'PUBLISHED', label: '发布 / 恢复发布' },
    { value: 'FROZEN', label: '冻结批次' },
    { value: 'RECALLED', label: '召回批次' }
  ].map((option) => {
    const allowed = canBatchStatusTransition(currentStatus, option.value)
    return { ...option, allowed, hint: allowed ? '' : statusTransitionBlockedHint(currentStatus, option.value) }
  })
})
const dialogTitle = computed(() => ({ trace: '补录追溯', quality: '上传质检', risk: '风险处理', status: '状态处理' }[dialog.value.type] || ''))
const dialogValidationError = computed(() => {
  if (!dialog.value.visible || readOnlyBatchView.value) return ''
  if (dialog.value.type === 'trace') {
    if (!traceForm.value.eventTime) return '请补充记录时间。'
    if (!traceForm.value.operatorName?.trim()) return '请补充操作人。'
    if (!traceForm.value.location?.trim()) return '请补充地点信息。'
    if (!traceForm.value.summary?.trim()) return '请填写现场说明。'
    if (traceUploading.value) return '现场图片仍在上传中，请等待上传完成后再提交。'
  }
  if (dialog.value.type === 'quality') {
    if (!qualityForm.value.reportNo?.trim()) return '请填写报告编号。'
    if (!qualityForm.value.agency?.trim()) return '请填写检测机构。'
    if (!qualityForm.value.reportTime) return '请填写检测时间。'
    if (!splitHighlightsInput(qualityForm.value.highlightsText).length) return '请至少填写一条质检摘要。'
    if (qualityUploading.value) return '质检附件仍在上传中，请等待上传完成后再提交。'
  }
  if (dialog.value.type === 'risk') {
    if (!riskForm.value.operatorName?.trim()) return '请填写处理人。'
    if (['PROCESSING', 'RECTIFIED'].includes(riskForm.value.actionType) && !riskForm.value.reason?.trim()) return '当前处理类型必须填写处理原因。'
    if (['COMMENT', 'RECTIFICATION'].includes(riskForm.value.actionType) && !riskForm.value.comment?.trim()) return '当前处理类型必须填写处理说明。'
  }
  if (dialog.value.type === 'status') {
    if (!statusForm.value.targetStatus) return '请先选择目标状态。'
    if (statusForm.value.targetStatus === 'PUBLISHED' && !publishGate.value.allowed) return publishGate.value.reason
    if (!canBatchStatusTransition(detail.value?.status?.code, statusForm.value.targetStatus)) return statusTransitionBlockedHint(detail.value?.status?.code, statusForm.value.targetStatus)
    if (!statusForm.value.operatorName?.trim()) return '请填写处理人。'
    if (!statusForm.value.reason?.trim()) return '请填写处理原因。'
  }
  return ''
})

function createRiskForm(actionType = 'COMMENT') {
  return { actionType, reason: '', comment: '', operatorName: '企业管理员' }
}

function createStatusForm(targetStatus = 'PUBLISHED') {
  return {
    targetStatus,
    reason: { PUBLISHED: '关键资料已齐，准备对外发布。', FROZEN: '发现异常，先冻结批次并进入处理。', RECALLED: '风险已确认，立即召回并保留公开提醒。' }[targetStatus] ?? '',
    operatorName: '企业管理员'
  }
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function actionOf(code) {
  return detail.value?.actions?.find((item) => item.code === code) ?? { enabled: false, hint: '' }
}

function formatDateTime(value) {
  const raw = String(value || '').trim()
  if (!raw) return '暂无'
  const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return raw.replace('T', ' ')
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function timelineResultTone(item) {
  const resultCode = String(item?.resultCode || '').toUpperCase()
  if (['PASS', 'SUCCESS', 'PUBLISHED', 'ACTIVE'].includes(resultCode)) return 'success'
  if (['FAIL', 'FAILED', 'RECALLED'].includes(resultCode)) return 'danger'
  if (['FROZEN', 'PROCESSING', 'RECTIFIED'].includes(resultCode)) return 'warning'
  return 'subtle'
}

function resolveLatestActivityTime() {
  return detail.value?.batch?.updatedAt || latestRiskAction.value?.createdAt || latestRiskAction.value?.operatedAt || latestRecord.value?.eventTime || latestRecord.value?.createdAt || detail.value?.batch?.createdAt || ''
}

function statusTransitionBlockedHint(currentStatus, targetStatus) {
  const current = String(currentStatus || '').toUpperCase()
  const target = String(targetStatus || '').toUpperCase()
  if (!target) return '目标状态不能为空。'
  if (!current) return '当前状态缺失，暂不允许变更。'
  if (current === target) return '当前状态与目标状态一致，无需重复提交。'
  if (target === 'DRAFT') return '不支持回退到草稿状态。'
  if (current === 'RECALLED') return '召回状态不允许继续变更。'
  if (current === 'DRAFT') return '草稿状态仅支持发布。'
  if (current === 'PUBLISHED') return '已发布状态仅支持冻结或召回。'
  if (current === 'FROZEN') return '冻结状态仅支持恢复发布或召回。'
  return `状态 ${current} 暂不支持变更到 ${target}。`
}

function fileLabel(file) {
  return file.fileName || file.fileUrl || '已上传文件'
}

function formatFileSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function switchTab(tab) {
  activeTab.value = tab
}

function scrollToSection(refEl) {
  nextTick(() => {
    refEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function openPublicPreview() {
  if (!detail.value?.qr?.publicUrl) return
  window.open(detail.value.qr.publicUrl, '_blank', 'noopener')
}

function openCopyBatch() {
  if (!detail.value?.batch?.id) return
  router.push({ path: '/batches', query: { mode: 'ALL', copyFrom: String(detail.value.batch.id) } })
}

function openTraceDialog() {
  traceForm.value = latestRecord.value
    ? createTraceForm({ stage: latestRecord.value.stageCode, operatorName: latestRecord.value.operatorName, location: latestRecord.value.location })
    : createTraceForm()
  dialog.value = { visible: true, type: 'trace' }
}

function openQualityDialog() {
  qualityForm.value = createQualityForm()
  dialog.value = { visible: true, type: 'quality' }
}

function openRiskDialog(actionType = 'COMMENT') {
  riskForm.value = createRiskForm(actionType)
  dialog.value = { visible: true, type: 'risk' }
}

function openStatusDialog(targetStatus = 'PUBLISHED') {
  const nextTargetStatus = canBatchStatusTransition(detail.value?.status?.code, targetStatus)
    ? targetStatus
    : (statusTargetOptions.value.find((item) => item.allowed)?.value ?? targetStatus)
  statusForm.value = createStatusForm(nextTargetStatus)
  dialog.value = { visible: true, type: 'status' }
}

function closeDialog(force = false) {
  if (dialogSubmitting.value && !force) return
  dialog.value = { visible: false, type: '' }
}

function syncAssignmentForm() {
  assignmentForm.value.assigneeUserId = detail.value?.task?.assigneeUserId ? String(detail.value.task.assigneeUserId) : ''
  assignmentConfirm.value = { visible: false, mode: 'reassign', message: '', nextAssigneeUserId: assignmentForm.value.assigneeUserId }
}

function openAssignmentDrawer() {
  switchTab('overview')
  scrollToSection(assignmentSectionRef)
}

function openRiskDrawer() {
  switchTab('timeline')
  scrollToSection(riskSectionRef)
}

function openFieldEntryPage() {
  if (!resolvedBatchId.value) return
  router.push({ path: '/field-entry', query: { batchId: resolvedBatchId.value } })
}

function openQualityPage() {
  router.push('/quality')
}

function openQrPage() {
  router.push('/qr')
}

function openRiskPage() {
  router.push('/risk')
}

async function loadAssignableOperators() {
  if (!canManageAssignment.value) {
    operatorOptions.value = []
    return
  }
  operatorLoading.value = true
  try {
    const response = await getOperatorOptions({ companyId: detail.value?.company?.id || undefined })
    operatorOptions.value = response.data ?? []
  } catch (error) {
    operatorOptions.value = []
    showMessage(error?.response?.data?.message || error?.message || '操作员列表加载失败，请稍后再试。', 'error')
  } finally {
    operatorLoading.value = false
  }
}

async function submitAssignment(forceClearDraft = false) {
  if (!detail.value || !canManageAssignment.value) return
  if (!assignmentChanged.value) {
    showMessage('当前分配未发生变化。', 'info')
    return
  }
  assignmentSaving.value = true
  const assigneeUserId = selectedAssigneeId.value ? Number(selectedAssigneeId.value) : null
  const nextMode = assigneeUserId == null ? 'clear' : (currentAssigneeId.value ? 'reassign' : 'assign')
  try {
    const response = await updateBatchAssignment(resolvedBatchId.value, { assigneeUserId, forceClearDraft })
    detail.value = response.data
    syncAssignmentForm()
    await loadAssignableOperators()
    if (assigneeUserId == null) {
      showMessage(forceClearDraft ? '已强制清空分配，并清除原分配人的未提交草稿。' : '已清空当前批次分配。', 'success')
    } else if (nextMode === 'assign') {
      showMessage('操作员已分配到当前批次。', 'success')
    } else {
      showMessage(forceClearDraft ? '已强制改派，并清除原分配人的未提交草稿。' : '操作员已改派。', 'success')
    }
  } catch (error) {
    const nextMessage = error?.response?.data?.message || error?.message || '任务分配更新失败，请稍后再试。'
    if (!forceClearDraft && nextMessage.includes('存在未提交草稿')) {
      assignmentConfirm.value = { visible: true, mode: nextMode === 'clear' ? 'clear' : 'reassign', message: nextMessage, nextAssigneeUserId: selectedAssigneeId.value }
      showMessage('该批次当前分配人存在未提交草稿，请确认是否继续强制改派。', 'error')
      return
    }
    showMessage(nextMessage, 'error')
  } finally {
    assignmentSaving.value = false
  }
}

async function clearAssignment() {
  if (!detail.value?.task?.assigneeUserId) {
    assignmentForm.value.assigneeUserId = ''
    showMessage('当前批次本来就是未分配状态。', 'info')
    return
  }
  assignmentForm.value.assigneeUserId = ''
  await submitAssignment(false)
}

function cancelAssignmentConfirm() {
  assignmentConfirm.value = { visible: false, mode: 'reassign', message: '', nextAssigneeUserId: currentAssigneeId.value }
  assignmentForm.value.assigneeUserId = currentAssigneeId.value
}

async function forceAssignmentChange() {
  assignmentConfirm.value.visible = false
  await submitAssignment(true)
}

async function loadDetail(id) {
  if (!id) return
  loading.value = true
  detail.value = null
  try {
    const response = await getBatchDetail(id)
    detail.value = response.data
    syncAssignmentForm()
    await loadAssignableOperators()
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '批次详情加载失败，请稍后再试。', 'error')
  } finally {
    loading.value = false
  }
}

async function submitDialog(options = {}) {
  if (dialogSubmitting.value) return
  if (dialogValidationError.value) {
    showMessage(dialogValidationError.value, 'error')
    return
  }
  const { keepOpen = false } = options
  if (dialog.value.type === 'quality' && String(qualityForm.value.result || '').toUpperCase() === 'FAIL' && !window.confirm('当前质检结论为“不合格”，提交后会直接影响发布流程，确认继续吗？')) return
  if (dialog.value.type === 'risk') {
    const riskActionLabel = riskActionOptions.find((item) => item.value === riskForm.value.actionType)?.label || riskForm.value.actionType
    if (!window.confirm(`确认提交“${riskActionLabel}”风险动作吗？`)) return
  }
  if (dialog.value.type === 'status') {
    const actionLabel = statusForm.value.targetStatus === 'PUBLISHED' ? '发布 / 恢复发布' : (statusForm.value.targetStatus === 'FROZEN' ? '冻结' : '召回')
    if (!window.confirm(`确认执行“${actionLabel}”状态操作吗？`)) return
  }
  dialogSubmitting.value = true
  try {
    let response
    if (dialog.value.type === 'trace') {
      response = await createTraceRecord(resolvedBatchId.value, {
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
      detail.value = response.data
      syncAssignmentForm()
      await loadAssignableOperators()
      if (keepOpen) {
        traceForm.value = createTraceForm({ stage: traceForm.value.stage, operatorName: traceForm.value.operatorName, location: traceForm.value.location })
        showMessage('这条记录已保存，可以继续补下一条。', 'success')
        return
      }
      showMessage('追溯记录已保存。', 'success')
    } else if (dialog.value.type === 'quality') {
      response = await createQualityReport(resolvedBatchId.value, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlightsInput(qualityForm.value.highlightsText),
        attachmentIds: qualityForm.value.attachmentIds
      })
      detail.value = response.data
      syncAssignmentForm()
      await loadAssignableOperators()
      showMessage('质检摘要已上传。', 'success')
    } else if (dialog.value.type === 'risk') {
      response = await createRiskAction(resolvedBatchId.value, riskForm.value)
      detail.value = response.data
      syncAssignmentForm()
      await loadAssignableOperators()
      showMessage('风险处理已记录。', 'success')
    } else if (dialog.value.type === 'status') {
      response = await changeBatchStatus(resolvedBatchId.value, statusForm.value)
      detail.value = response.data
      syncAssignmentForm()
      await loadAssignableOperators()
      showMessage('批次状态已更新。', 'success')
    }
    closeDialog(true)
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '操作未完成，请稍后再试。', 'error')
  } finally {
    dialogSubmitting.value = false
  }
}

async function handleGenerateQr() {
  try {
    const response = await generateBatchQr(resolvedBatchId.value)
    detail.value = response.data
    syncAssignmentForm()
    await loadAssignableOperators()
    showMessage('二维码已生成。', 'success')
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '二维码生成失败，请稍后再试。', 'error')
  }
}

async function handleTraceFilesChange(event) {
  const files = [...(event.target.files ?? [])]
  if (!files.length) return
  traceUploading.value = true
  try {
    const response = await uploadBatchFiles('trace-image', files)
    const uploadedFiles = response.data ?? []
    traceForm.value.uploadedFiles = [...traceForm.value.uploadedFiles, ...uploadedFiles]
    traceForm.value.attachmentIds = traceForm.value.uploadedFiles.map((item) => item.id)
    if (!traceForm.value.imageUrl && uploadedFiles[0]?.fileUrl) traceForm.value.imageUrl = uploadedFiles[0].fileUrl
    showMessage(`现场图片已上传 ${uploadedFiles.length} 个。`, 'success')
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '现场图片上传失败。', 'error')
  } finally {
    traceUploading.value = false
    event.target.value = ''
  }
}

async function handleQualityFilesChange(event) {
  const files = [...(event.target.files ?? [])]
  if (!files.length) return
  qualityUploading.value = true
  try {
    const response = await uploadBatchFiles('quality-attachment', files)
    const uploadedFiles = response.data ?? []
    qualityForm.value.uploadedFiles = [...qualityForm.value.uploadedFiles, ...uploadedFiles]
    qualityForm.value.attachmentIds = qualityForm.value.uploadedFiles.map((item) => item.id)
    showMessage(`质检附件已上传 ${uploadedFiles.length} 个。`, 'success')
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '质检附件上传失败。', 'error')
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

watch(resolvedBatchId, async (id) => {
  await loadDetail(id)
})

onMounted(async () => {
  await loadDetail(resolvedBatchId.value)
})
</script>

<template>
  <div class="drawer-workbench" data-testid="batch-workbench-drawer-panel">
    <div v-if="message" class="message-bar" :class="messageType">{{ message }}</div>

    <div v-if="loading" class="state-card">正在加载批次详情...</div>
    <div v-else-if="!detail" class="state-card">
      <h3>批次详情加载失败</h3>
      <p>{{ message || '请稍后重试。' }}</p>
      <button class="primary" @click="loadDetail(resolvedBatchId)">重新加载</button>
    </div>

    <template v-else>
      <article class="drawer-card summary-card">
        <div class="summary-main">
          <div class="summary-copy">
            <h2>{{ batchName }}</h2>
            <div class="summary-meta">
              <div v-for="item in topSummaryMeta" :key="item.label" class="summary-meta-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>
          <div class="summary-side">
            <span class="status-badge" :class="String(detail.status?.code || '').toLowerCase()">{{ batchStatusText }}</span>
            <div class="summary-actions">
              <button v-if="canManageBatch" class="ghost" @click="openCopyBatch">复制批次</button>
              <button class="ghost" :disabled="!canPreviewPublic" @click="openPublicPreview">查看公开页</button>
            </div>
          </div>
        </div>
      </article>

      <section class="drawer-section">
        <div class="section-head"><h3>批次基础信息</h3></div>
        <div class="info-columns">
          <article class="drawer-card info-panel">
            <header>负责人信息</header>
            <div class="info-list">
              <div v-for="item in ownerInfoRows" :key="item.label" class="info-row">
                <div class="info-row-main">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </div>
          </article>

          <article class="drawer-card info-panel">
            <header>批次基础信息</header>
            <div class="info-list">
              <div v-for="item in baseInfoRows" :key="item.label" class="info-row">
                <div class="info-row-main">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                  <small v-if="item.meta">{{ item.meta }}</small>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="drawer-section">
        <div class="section-head"><h3>追溯记录摘要</h3></div>
        <div v-if="traceSummaryRows.length" class="timeline-list">
          <article v-for="item in traceSummaryRows" :key="item.id || item.createdAt || item.eventTime" class="timeline-item">
            <div class="timeline-item-top">
              <div class="timeline-item-copy">
                <div class="timeline-item-heading">
                  <strong>{{ item.title || item.stageLabel || '追溯记录' }}</strong>
                  <span class="phase-chip">{{ item.stageLabel || item.stageCode || '节点' }}</span>
                </div>
                <div class="timeline-meta">
                  <span>{{ formatDateTime(item.eventTime || item.createdAt) }}</span>
                  <span>{{ item.operatorName || '未记录责任人' }}</span>
                </div>
              </div>
            </div>
            <p class="timeline-summary">{{ item.summary || '暂无摘要' }}</p>
          </article>
        </div>
        <div v-else class="state-card compact">当前还没有追溯记录。</div>
        <div class="button-row">
          <button class="ghost" @click="openFieldEntryPage">去现场作业</button>
        </div>
      </section>

      <section class="drawer-section">
        <div class="section-head"><h3>质检区</h3></div>
        <div class="risk-summary-grid">
          <article v-for="item in qualitySummaryRows" :key="item.label" class="drawer-card risk-summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <div class="button-row">
          <button class="ghost" @click="openQualityPage">查看质检页</button>
        </div>
      </section>

      <section class="drawer-section">
        <div class="section-head"><h3>二维码与公开区</h3></div>
        <div class="risk-summary-grid">
          <article v-for="item in qrSummaryRows" :key="item.label" class="drawer-card risk-summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <div class="button-row">
          <button class="ghost" @click="openQrPage">查看二维码与发布页</button>
          <button class="ghost" :disabled="!canPreviewPublic" @click="openPublicPreview">打开公开页</button>
        </div>
      </section>

      <section class="drawer-section">
        <div class="section-head"><h3>风险处理区</h3></div>
        <div class="risk-summary-grid">
          <article v-for="item in riskSummaryRows" :key="item.label" class="drawer-card risk-summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <div class="button-row">
          <button class="ghost" @click="openRiskPage">查看风险处理页</button>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.drawer-workbench {
  display: grid;
  gap: 14px;
}

.message-bar,
.drawer-card,
.drawer-section,
.state-card {
  border: 1px solid rgba(56, 134, 217, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(45, 113, 194, 0.08);
}

.message-bar {
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
}

.message-bar.success {
  background: var(--admin-success-bg);
  color: var(--admin-success-text);
}

.message-bar.error {
  background: var(--admin-danger-bg);
  color: var(--admin-danger-text);
}

.state-card {
  display: grid;
  gap: 10px;
  place-items: center;
  min-height: 140px;
  padding: 18px;
  text-align: center;
}

.state-card.compact {
  min-height: 96px;
}

.state-card h3,
.state-card p {
  margin: 0;
}

.drawer-tab-switcher,
.summary-actions,
.button-row,
.risk-action-row,
.timeline-meta,
.timeline-item-heading,
.status-card-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drawer-tab-switcher {
  padding: 6px;
  border: 1px solid rgba(56, 134, 217, 0.12);
  border-radius: 16px;
  background: rgba(240, 247, 255, 0.92);
}

.tab-chip {
  flex: 1 1 0;
  min-height: 38px;
  border-radius: 12px;
  border: 0;
  background: transparent;
  color: var(--admin-text-mid);
  font-size: 14px;
  font-weight: 700;
}

.tab-chip.active {
  background: linear-gradient(135deg, rgba(48, 149, 246, 0.14) 0%, rgba(255, 255, 255, 0.98) 100%);
  color: var(--admin-primary-deep);
  box-shadow: inset 0 0 0 1px rgba(56, 134, 217, 0.12);
}

.drawer-tab-panel {
  display: grid;
  gap: 14px;
}

.drawer-section,
.summary-card,
.info-panel,
.timeline-item,
.status-card,
.risk-summary-card {
  padding: 14px;
}

.summary-main,
.timeline-item-top,
.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.summary-copy,
.summary-side,
.timeline-item-copy,
.info-row-main {
  min-width: 0;
}

.summary-copy h2,
.section-head h3 {
  margin: 0;
  color: var(--admin-text);
}

.summary-copy h2 {
  font-size: 28px;
  line-height: 1.12;
}

.summary-meta,
.info-list,
.timeline-list,
.form-shell,
.uploaded-file-list {
  display: grid;
  gap: 10px;
}

.summary-meta-item span,
.status-card-top span,
.info-row span,
.timeline-meta span,
.timeline-summary,
.helper-copy,
.form-intro,
.form-label span,
.upload-box small,
.uploaded-file-item small,
.warning-box p {
  color: var(--admin-text-soft);
  font-size: 12px;
  line-height: 1.5;
}

.summary-meta-item strong,
.status-card strong,
.info-row strong,
.risk-summary-card strong,
.timeline-item strong,
.uploaded-file-item strong,
.warning-box strong {
  color: var(--admin-text);
}

.summary-side {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.status-badge,
.phase-chip,
.timeline-result {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-badge {
  background: rgba(120, 147, 180, 0.16);
  color: #59759b;
}

.status-badge.published {
  background: rgba(31, 159, 102, 0.14);
  color: #17784f;
}

.status-badge.frozen {
  background: rgba(242, 154, 52, 0.18);
  color: #a66213;
}

.status-badge.recalled {
  background: rgba(224, 73, 73, 0.14);
  color: #b63f3f;
}

.section-head {
  margin-bottom: 12px;
}

.action-grid,
.status-grid,
.risk-summary-grid,
.info-columns,
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.action-card,
.status-card,
.risk-summary-card,
.info-panel,
.timeline-item {
  border: 1px solid rgba(56, 134, 217, 0.12);
  border-radius: 16px;
  background: rgba(249, 252, 255, 0.96);
}

.action-card {
  display: grid;
  align-content: center;
  justify-items: start;
  min-height: 72px;
  text-align: left;
}

.action-card strong,
.timeline-item strong {
  font-size: 14px;
}

.action-card span {
  color: var(--admin-text-soft);
  font-size: 12px;
}

.action-card.is-disabled {
  opacity: 0.56;
}

.status-card,
.risk-summary-card {
  display: grid;
  gap: 8px;
  min-height: 96px;
}

.status-card strong,
.risk-summary-card strong {
  font-size: 22px;
  line-height: 1.15;
}

.status-card-top {
  justify-content: space-between;
  align-items: flex-start;
}

.status-card-top em {
  font-style: normal;
  color: var(--admin-primary-deep);
  font-size: 11px;
  font-weight: 700;
}

.info-panel header {
  margin-bottom: 12px;
  color: var(--admin-text);
  font-size: 14px;
  font-weight: 700;
}

.info-row {
  padding-top: 10px;
  border-top: 1px solid rgba(56, 134, 217, 0.08);
}

.info-row:first-child {
  padding-top: 0;
  border-top: 0;
}

.info-row small {
  color: var(--admin-text-soft);
  font-size: 12px;
}

.info-inline-action {
  flex: 0 0 auto;
  min-height: 30px;
  padding: 0 10px;
  font-size: 12px;
}

.assignment-controls {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.assignment-action-row,
.risk-action-row,
.dialog-actions {
  flex-wrap: wrap;
}

.helper-copy,
.form-error,
.timeline-summary {
  margin: 0;
}

.warning-box {
  padding: 12px 14px;
  border: 1px solid rgba(214, 137, 58, 0.22);
  border-radius: 14px;
  background: #fff6eb;
}

.warning-box p {
  margin-top: 6px;
}

.phase-chip {
  min-height: 22px;
  padding-inline: 8px;
  background: rgba(48, 149, 246, 0.1);
  color: var(--admin-primary-deep);
  font-size: 11px;
}

.timeline-meta {
  flex-wrap: wrap;
  margin-top: 6px;
}

.timeline-result {
  flex: 0 0 auto;
  font-size: 11px;
}

.timeline-result.is-success {
  background: rgba(31, 159, 102, 0.14);
  color: #17784f;
}

.timeline-result.is-warning {
  background: rgba(242, 154, 52, 0.16);
  color: #a66213;
}

.timeline-result.is-danger {
  background: rgba(224, 73, 73, 0.14);
  color: #b63f3f;
}

.timeline-result.is-subtle {
  background: rgba(120, 147, 180, 0.12);
  color: #59759b;
}

button,
input,
select,
textarea {
  font: inherit;
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: #fff;
  color: var(--admin-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

button.primary {
  background: linear-gradient(135deg, var(--admin-primary) 0%, var(--admin-primary-deep) 100%);
  color: #fff;
}

button.ghost {
  border-color: var(--admin-border);
  color: var(--admin-primary-deep);
}

button.ghost.danger {
  border-color: rgba(190, 70, 58, 0.2);
  color: var(--admin-danger-text);
}

button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

input,
select,
textarea {
  width: 100%;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(56, 134, 217, 0.16);
  border-radius: 12px;
  background: #fff;
  color: var(--admin-text);
}

textarea {
  min-height: 96px;
  padding: 12px;
  resize: vertical;
}

.form-label {
  display: grid;
  gap: 6px;
}

.form-label-full {
  grid-column: 1 / -1;
}

.upload-box {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px dashed rgba(56, 134, 217, 0.26);
  border-radius: 12px;
  background: rgba(246, 251, 255, 0.88);
}

.uploaded-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid rgba(56, 134, 217, 0.12);
  border-radius: 14px;
  background: rgba(249, 252, 255, 0.96);
}

.form-error {
  color: var(--admin-danger-text);
  font-size: 13px;
}

:deep(.detail-dialog .el-dialog__body) {
  padding-top: 8px;
}

@media (max-width: 720px) {
  .action-grid,
  .status-grid,
  .risk-summary-grid,
  .info-columns,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .summary-main,
  .summary-side,
  .summary-actions,
  .info-row,
  .timeline-item-top,
  .uploaded-file-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-side {
    justify-items: start;
  }
}
</style>
