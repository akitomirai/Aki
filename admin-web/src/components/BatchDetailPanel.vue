<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import QRCode from 'qrcode'
import { useRoute, useRouter } from 'vue-router'
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
  formatStageLabel,
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
  },
  panel: {
    type: String,
    default: ''
  },
  embedded: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const resolvedBatchId = computed(() => String(props.batchId ?? route.params.id ?? '').trim())
const panelQuery = computed(() => String(props.panel || route.query.panel || '').trim())
const isEmbedded = computed(() => props.embedded)

const loading = ref(true)
const detail = ref(null)
const message = ref('')
const messageType = ref('info')
const dialog = ref({ visible: false, type: '' })
const assignmentDrawerVisible = ref(false)
const recordsDrawerVisible = ref(false)
const recordDetailVisible = ref(false)
const qrDialogVisible = ref(false)
const riskSectionVisible = ref(false)
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
const qrLoading = ref(false)
const qrPreviewUrl = ref('')
const activeRecord = ref(null)

const roleCode = computed(() => authStore.user?.roleCode || '')
const canManageBatch = computed(() => canManageAdminBatch(roleCode.value))
const readOnlyBatchView = computed(() => isRegulator(roleCode.value))
const canManageAssignment = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(authStore.user?.roleCode))

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
const freezeAction = computed(() => actionOf('FREEZE'))
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
const assignmentHint = computed(() => {
  if (!detail.value) return ''
  if (detail.value.task?.draftPending) {
    const assigneeName = detail.value.task?.assigneeName || '原分配人'
    return detail.value.task?.draftUpdatedAt
      ? `${assigneeName} 仍有未提交草稿，最近保存于 ${formatDateTime(detail.value.task.draftUpdatedAt)}。`
      : `${assigneeName} 仍有未提交草稿。`
  }
  if (!operatorOptions.value.length && canManageAssignment.value) return '当前没有可分配操作员。'
  if (selectedAssignee.value) return `确认后将由 ${selectedAssignee.value.realName || selectedAssignee.value.username} 负责后续现场作业。`
  if (detail.value.task?.assigneeName) return `当前负责人为 ${detail.value.task.assigneeName}。`
  return '当前还没有分配负责人。'
})
const latestRiskActionLabel = computed(() => {
  if (!latestRiskAction.value) return canHandleRisk.value ? '尚未记录处理动作' : '当前无风险动作'
  return latestRiskAction.value.actionLabel || latestRiskAction.value.actionType || '已记录处理动作'
})
const riskSummaryCopy = computed(() => {
  if (!canHandleRisk.value) return '当前没有需要处理的风险事项。'
  return detail.value?.risk?.reason || detail.value?.status?.reason || '当前批次仍在风险处理中。'
})
const statusSummaryCopy = computed(() => {
  return {
    DRAFT: '资料仍在准备中，先补录追溯、质检和二维码。',
    PUBLISHED: '批次已对外公开，可继续回查公开页和最近记录。',
    FROZEN: '批次已冻结，等待风险处理完成。',
    RECALLED: '批次已召回，公开页会持续提示风险。'
  }[String(detail.value?.status?.code || '').toUpperCase()] || '当前批次状态待确认。'
})
const headerNote = computed(() => {
  if (readOnlyBatchView.value) return '当前为查看模式，仅保留状态、任务、风险和最近记录。'
  if (String(route.query.created || '') !== '1') return ''
  const copiedFromCode = String(route.query.copiedFrom || '').trim()
  return copiedFromCode
    ? `已基于批次 ${copiedFromCode} 复制出新批次，接下来先补录追溯、上传质检并生成二维码。`
    : '批次已创建成功，接下来先补录追溯、上传质检并生成二维码。'
})
const readinessDoneCount = computed(() => [hasTraceRecord.value, qualityUploaded.value && qualityAllowsPublish.value, qrGenerated.value].filter(Boolean).length)
const coreStatusCards = computed(() => [
  { key: 'batch-status', title: '批次状态', value: batchStatusText.value, description: statusSummaryCopy.value, tag: String(detail.value?.status?.code || '').toUpperCase() === 'PUBLISHED' ? '公开中' : '' },
  { key: 'publish-ready', title: '发布准备度', value: publishReady.value ? (resumeAction.value.enabled ? '可恢复发布' : '可发布') : '待补齐', description: publishReady.value ? '追溯、质检和二维码已满足主流程要求。' : publishGate.value.reason, tag: `${readinessDoneCount.value}/3` },
  { key: 'task-status', title: '任务执行', value: taskStatusText.value, description: detail.value?.task?.assigneeName ? `${detail.value.task.assigneeName} · ${todayProgressText.value}` : '当前还没有分配负责人。', tag: detail.value?.task?.draftPending ? '有草稿' : '' },
  { key: 'risk-status', title: '风险事项', value: riskStageText.value, description: canHandleRisk.value ? riskSummaryCopy.value : '当前没有异常风险链路。', tag: canHandleRisk.value ? (riskResolved.value ? '待恢复' : '处理中') : '' }
])
const actionButtons = computed(() => {
  if (readOnlyBatchView.value) {
    const buttons = []
    if (qrGenerated.value) buttons.push({ key: 'qr', label: '查看二维码', note: '查看当前公开入口二维码', disabled: false, handler: openQrDialog })
    buttons.push(
      { key: 'records', label: '查看全部记录', note: '进入记录抽屉查看明细', disabled: recentRecords.value.length === 0, handler: openRecordsDrawer },
      { key: 'assignment', label: '任务分配', note: '查看当前负责人和任务状态', disabled: false, handler: openAssignmentDrawer },
      { key: 'risk', label: '风险处理', note: '查看风险动作和时间线', disabled: false, handler: openRiskDrawer }
    )
    return buttons
  }
  return [
    { key: 'trace', label: '补录追溯', note: hasTraceRecord.value ? `最近记录：${latestRecord.value?.title || '已补录'}` : '先补第一条现场记录', disabled: !traceAction.value.enabled, handler: openTraceDialog },
    { key: 'quality', label: '上传质检', note: qualityUploaded.value ? `当前状态：${detail.value?.quality?.label || '已上传'}` : '补齐发布前的质检摘要', disabled: !qualityAction.value.enabled, handler: openQualityDialog },
    { key: 'qr', label: qrGenerated.value ? '查看二维码' : '生成二维码', note: qrGenerated.value ? '二维码已生成，可直接回查' : '生成后即可进入公开页', disabled: qrGenerated.value ? false : !qrAction.value.enabled, handler: qrGenerated.value ? openQrDialog : () => handleGenerateQr(true) },
    { key: 'publish', label: resumeAction.value.enabled ? '恢复发布' : '发布批次', note: publishReady.value ? '当前已满足发布条件' : publishGate.value.reason, disabled: !publishReady.value, handler: () => openStatusDialog('PUBLISHED') },
    { key: 'assignment', label: '任务分配', note: detail.value?.task?.assigneeName ? `当前负责人：${detail.value.task.assigneeName}` : '设置当前负责人', disabled: false, handler: openAssignmentDrawer },
    { key: 'risk', label: '风险处理', note: canHandleRisk.value ? '查看处理进度并补充动作' : '查看风险摘要和状态变更', disabled: false, handler: openRiskDrawer }
  ]
})
const primaryActionButtons = computed(() => {
  if (!actionButtons.value.length) return []
  const availableMap = new Map(actionButtons.value.map((item) => [item.key, item]))
  const preferredKeys = []
  if (publishReady.value) preferredKeys.push('publish')
  if (!hasTraceRecord.value) preferredKeys.push('trace')
  if (!qualityUploaded.value) preferredKeys.push('quality')
  if (!qrGenerated.value) preferredKeys.push('qr')
  const picked = []
  for (const key of preferredKeys) {
    const action = availableMap.get(key)
    if (action && !action.disabled && !picked.some((item) => item.key === action.key)) picked.push(action)
    if (picked.length === 2) break
  }
  return picked.length ? picked : actionButtons.value.filter((item) => !item.disabled).slice(0, 2)
})
const secondaryActionButtons = computed(() => {
  const primaryKeys = new Set(primaryActionButtons.value.map((item) => item.key))
  return actionButtons.value.filter((item) => !primaryKeys.has(item.key))
})
const infoItems = computed(() => [
  { label: '批次号', value: batchCode.value, hint: `批次 ID：${batch.value.id || '--'}` },
  { label: '产品', value: batchName.value, hint: batch.value.originPlace || '未填写产地' },
  { label: '企业', value: companyName.value, hint: company.value.contactName || '暂无联系人信息' },
  { label: '质检状态', value: detail.value?.quality?.label || '待上传', hint: latestQualityReport.value?.reportNo ? `报告编号：${latestQualityReport.value.reportNo}` : '还没有上传质检摘要' },
  { label: '二维码状态', value: qrStatusText.value, hint: detail.value?.qr?.token ? `二维码标识：${detail.value.qr.token}` : '还没有生成二维码' },
  { label: '公开页状态', value: canPreviewPublic.value ? '已开放' : '未开放', hint: canPreviewPublic.value ? '右上角可直接查看公开页' : '先生成二维码后开放' },
  { label: '最近更新时间', value: formatDateTime(resolveLatestActivityTime()), hint: latestRiskAction.value ? `最近风险动作：${latestRiskActionLabel.value}` : (latestRecord.value?.title ? `最近记录：${latestRecord.value.title}` : '暂无最近动作') }
])
const previewRecords = computed(() => recentRecords.value.slice(0, 3))
const allRecords = computed(() => recentRecords.value)
const assignmentSummaryItems = computed(() => [
  { label: '当前负责人', value: detail.value?.task?.assigneeName || '未分配' },
  { label: '任务状态', value: taskStatusText.value },
  { label: '分配时间', value: formatDateTime(detail.value?.task?.assignedAt) }
])
const riskSummaryItems = computed(() => [
  { label: '当前风险状态', value: riskStageText.value, hint: canHandleRisk.value ? riskSummaryCopy.value : '暂无风险链路' },
  { label: '最近处理动作', value: latestRiskActionLabel.value, hint: latestRiskAction.value ? formatDateTime(latestRiskAction.value.createdAt || latestRiskAction.value.operatedAt) : '暂无动作时间' },
  { label: '恢复发布状态', value: detail.value?.riskHandling?.canResume ? '已满足条件' : '暂不可恢复', hint: detail.value?.riskHandling?.canResume ? '可以恢复发布' : '需要继续补充处理动作' }
])
const showInlineRiskSection = computed(() => riskSectionVisible.value || canHandleRisk.value || Boolean(latestRiskAction.value))
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
function recordPreviewImages(record) {
  const attachments = Array.isArray(record?.attachments) ? record.attachments : []
  if (attachments.length) {
    return attachments.map((item, index) => ({
      id: item.id ?? `${record?.id || 'record'}-${index}`,
      fileUrl: item.fileUrl,
      fileName: item.fileName || record?.title || '现场图片'
    }))
  }
  if (!record?.imageUrl) return []
  return [{ id: `cover-${record.id || 'record'}`, fileUrl: record.imageUrl, fileName: record.title || '现场图片' }]
}
function recordTime(record) {
  return formatDateTime(record?.eventTime || record?.createdAt || record?.operatedAt)
}
function recordStage(record) {
  return record?.stageCode ? formatStageLabel(record.stageCode) : '现场记录'
}
function goBackToList() {
  if (isEmbedded.value) {
    emit('close')
    return
  }
  router.push('/batches')
}
function openCopyBatch() {
  if (!detail.value?.batch?.id) return
  router.push({ path: '/batches', query: { mode: 'ALL', copyFrom: String(detail.value.batch.id) } })
}
function openPublicPreview() {
  if (!detail.value?.qr?.publicUrl) return
  window.open(detail.value.qr.publicUrl, '_blank', 'noopener')
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
  assignmentDrawerVisible.value = true
}
async function openRiskDrawer() {
  riskSectionVisible.value = true
  await nextTick()
  riskSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function openRecordsDrawer() {
  recordsDrawerVisible.value = true
}
function openRecordDetail(record) {
  activeRecord.value = record
  recordDetailVisible.value = true
}

function syncPanelFromRoute(panelValue = panelQuery.value) {
  const panel = String(panelValue || '').trim().toLowerCase()
  if (!panel || !detail.value) {
    return
  }
  if (panel === 'assignment') {
    openAssignmentDrawer()
    return
  }
  if (panel === 'risk') {
    void openRiskDrawer()
    return
  }
  if (panel === 'records') {
    openRecordsDrawer()
    return
  }
  if (panel === 'qr') {
    void openQrDialog()
  }
}

async function openQrDialog() {
  if (!detail.value?.qr?.publicUrl) {
    showMessage('当前暂无可查看的二维码。', 'error')
    return
  }
  qrDialogVisible.value = true
  qrLoading.value = true
  qrPreviewUrl.value = ''
  try {
    qrPreviewUrl.value = await QRCode.toDataURL(detail.value.qr.publicUrl, { width: 240, margin: 2 })
  } catch (error) {
    showMessage(error?.message || '二维码预览生成失败。', 'error')
  } finally {
    qrLoading.value = false
  }
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
  qrPreviewUrl.value = ''
  qrDialogVisible.value = false
  activeRecord.value = null
  assignmentDrawerVisible.value = false
  recordsDrawerVisible.value = false
  recordDetailVisible.value = false
  riskSectionVisible.value = false
  try {
    const response = await getBatchDetail(id)
    detail.value = response.data
    syncAssignmentForm()
    await loadAssignableOperators()
    syncPanelFromRoute()
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
async function handleGenerateQr(openPreviewAfterGenerate = false) {
  if (qrLoading.value) return
  qrLoading.value = true
  try {
    const response = await generateBatchQr(resolvedBatchId.value)
    detail.value = response.data
    syncAssignmentForm()
    await loadAssignableOperators()
    showMessage('二维码已生成。', 'success')
    if (openPreviewAfterGenerate) await openQrDialog()
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '二维码生成失败，请稍后再试。', 'error')
  } finally {
    qrLoading.value = false
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
watch(panelQuery, () => {
  syncPanelFromRoute()
})
onMounted(async () => {
  await loadDetail(resolvedBatchId.value)
  if (!isEmbedded.value && String(route.query.created || '') === '1') {
    const copiedFromCode = String(route.query.copiedFrom || '').trim()
    showMessage(copiedFromCode ? `已基于批次 ${copiedFromCode} 复制出新批次，先补录追溯、上传质检和生成二维码。` : '批次已创建成功，先补录追溯、上传质检和生成二维码。', 'success')
  }
})
</script>

<template>
  <div class="page-shell batch-detail-page" :class="{ 'is-embedded': isEmbedded }" data-testid="batch-workbench-page">
    <div v-if="loading" class="loading-card">正在加载批次详情...</div>
    <div v-else-if="!detail" class="state-card">
      <h2>批次详情加载失败</h2>
      <p>{{ message || '请稍后重试。' }}</p>
      <div class="state-actions">
        <button class="ghost" @click="goBackToList">{{ isEmbedded ? '关闭详情' : '返回批次列表' }}</button>
        <button class="primary" @click="loadDetail(resolvedBatchId)">重新加载</button>
      </div>
    </div>
    <template v-else>
      <div v-if="message" class="message-bar" :class="messageType">{{ message }}</div>
      <div class="detail-page">
        <section class="detail-panel header-panel">
          <div class="header-main">
            <div class="header-copy">
              <p class="eyebrow">批次详情主页面</p>
              <h1>{{ batchName }}</h1>
              <div class="header-meta">
                <span>批次号：{{ batchCode }}</span>
                <span>企业：{{ companyName }}</span>
              </div>
              <p v-if="headerNote" class="header-note">{{ headerNote }}</p>
            </div>
            <div class="header-actions">
              <button class="ghost" @click="goBackToList">{{ isEmbedded ? '关闭详情' : '返回批次列表' }}</button>
              <button v-if="canManageBatch" class="ghost" @click="openCopyBatch">复制批次</button>
              <button class="ghost" :disabled="!canPreviewPublic" @click="openPublicPreview">查看公开页</button>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="section-head">
            <div>
              <h2>核心状态总览</h2>
              <p>只保留当前最关键的状态、准备度、任务和风险信息。</p>
            </div>
          </div>
          <div class="overview-grid">
            <article v-for="card in coreStatusCards" :key="card.key" class="status-card">
              <div class="status-card-top">
                <span class="card-label">{{ card.title }}</span>
                <span v-if="card.tag" class="detail-pill">{{ card.tag }}</span>
              </div>
              <strong class="status-card-value">{{ card.value }}</strong>
              <p class="status-card-copy">{{ card.description }}</p>
            </article>
          </div>
        </section>

        <section class="detail-panel" data-testid="workbench-action-groups">
          <div class="section-head">
            <div>
              <h2>主操作区</h2>
              <p>把高频动作收口到这里，低频处理改为抽屉或详情弹窗。</p>
            </div>
          </div>
          <div class="action-layout">
            <div class="action-summary">
              <div class="action-highlight">
                <span class="card-label">当前建议</span>
                <strong>{{ publishReady ? (resumeAction.enabled ? '可恢复发布' : '可直接发布') : (!hasTraceRecord ? '先补录追溯' : (!qualityUploaded ? '先上传质检' : (!qrGenerated ? '先生成二维码' : '查看最近记录'))) }}</strong>
                <p>{{ publishReady ? '关键资料已经收口，当前可以完成发布。' : publishGate.reason }}</p>
              </div>
            </div>
            <div class="action-groups">
              <div v-if="primaryActionButtons.length" class="button-row">
                <button
                  v-for="action in primaryActionButtons"
                  :key="action.key"
                  class="primary"
                  :data-testid="action.key === 'quality' ? 'workbench-open-quality-dialog' : undefined"
                  :disabled="action.disabled"
                  @click="action.handler"
                >
                  {{ action.label }}
                </button>
              </div>
              <div v-if="secondaryActionButtons.length" class="button-row button-row-secondary">
                <button v-for="action in secondaryActionButtons" :key="action.key" class="ghost" :disabled="action.disabled" @click="action.handler">
                  {{ action.label }}
                </button>
              </div>
              <ul class="action-note-list">
                <li v-for="action in actionButtons" :key="`${action.key}-note`">
                  <strong>{{ action.label }}</strong>
                  <span>{{ action.note }}</span>
                </li>
              </ul>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="section-head">
            <div>
              <h2>批次基础信息</h2>
              <p>整合批次概览、质检与二维码状态，避免拆成多个重复卡片。</p>
            </div>
          </div>
          <div class="info-grid">
            <article v-for="item in infoItems" :key="item.label" class="info-card">
              <span class="card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.hint }}</small>
            </article>
          </div>
        </section>
        <section class="detail-panel">
          <div class="section-head">
            <div>
              <h2>最近记录</h2>
              <p>主页面只保留最近 3 条，详情和图片回查看弹窗或抽屉。</p>
            </div>
            <button class="ghost" :disabled="!allRecords.length" @click="openRecordsDrawer">查看全部记录</button>
          </div>
          <div v-if="previewRecords.length" class="record-grid">
            <article v-for="record in previewRecords" :key="record.id" class="record-card">
              <div class="record-card-head">
                <div>
                  <div class="record-title-row">
                    <strong>{{ record.title || recordStage(record) }}</strong>
                    <span class="detail-pill subtle">{{ recordStage(record) }}</span>
                  </div>
                  <div class="record-meta">
                    <span>{{ record.operatorName || '未记录提交人' }}</span>
                    <span>{{ recordTime(record) }}</span>
                  </div>
                </div>
              </div>
              <p class="record-summary">{{ record.summary || '暂无简述' }}</p>
              <div class="record-footer">
                <div v-if="recordPreviewImages(record).length" class="record-thumb">
                  <img :src="recordPreviewImages(record)[0].fileUrl" :alt="recordPreviewImages(record)[0].fileName">
                  <span v-if="recordPreviewImages(record).length > 1" class="thumb-count">+{{ recordPreviewImages(record).length - 1 }}</span>
                </div>
                <div v-else class="record-thumb record-thumb-placeholder"><span>{{ recordStage(record) }}</span></div>
                <button class="ghost" @click="openRecordDetail(record)">查看详情</button>
              </div>
            </article>
          </div>
          <div v-else class="empty-panel">
            <strong>当前还没有追溯记录。</strong>
            <p>先补录一条关键现场记录，再继续质检和发布流程。</p>
            <button v-if="canManageBatch" class="primary" :disabled="!traceAction.enabled" @click="openTraceDialog">补录追溯</button>
          </div>
        </section>
      </div>

      <el-drawer v-model="assignmentDrawerVisible" direction="rtl" size="420px" append-to-body :with-header="false" class="detail-drawer">
        <div class="drawer-shell" data-testid="workbench-assignment-panel">
          <div class="drawer-head">
            <div>
              <p class="eyebrow">任务分配</p>
              <h3>负责人安排</h3>
              <p>只保留负责人、任务状态、分配时间和分配动作。</p>
            </div>
            <button class="ghost icon-button" @click="assignmentDrawerVisible = false">关闭</button>
          </div>
          <div class="drawer-summary-grid">
            <article v-for="item in assignmentSummaryItems" :key="item.label" class="drawer-summary-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
          <div class="drawer-card">
            <span class="card-label">当前说明</span>
            <p class="drawer-copy">{{ assignmentHint }}</p>
          </div>
          <div v-if="canManageAssignment" class="drawer-card assignment-actions-card">
            <label class="form-label">
              <span>选择操作员</span>
              <select v-model="assignmentForm.assigneeUserId" data-testid="assignment-operator-select" :disabled="operatorLoading || assignmentSaving || !operatorOptions.length">
                <option value="">暂不分配</option>
                <option v-for="item in operatorOptions" :key="item.id" :value="String(item.id)">{{ item.realName || item.username }}</option>
              </select>
            </label>
            <p v-if="operatorLoading" class="inline-tip">正在加载可分配操作员...</p>
            <p v-else-if="!operatorOptions.length" class="inline-tip">当前没有可分配操作员。</p>
            <div class="button-row">
              <button class="primary" data-testid="assignment-save-button" :disabled="assignmentSaving || operatorLoading || !assignmentChanged || !operatorOptions.length && selectedAssigneeId" @click="submitAssignment(false)">{{ assignmentActionLabel }}</button>
              <button class="ghost danger" data-testid="assignment-clear-button" :disabled="assignmentSaving || !detail.task?.assigneeUserId" @click="clearAssignment">清空分配</button>
            </div>
            <div v-if="assignmentConfirm.visible" class="warning-box" data-testid="assignment-draft-confirm">
              <strong>发现未提交草稿</strong>
              <p>{{ assignmentConfirm.message }}</p>
              <div class="button-row button-row-secondary">
                <button class="ghost" data-testid="assignment-draft-cancel" @click="cancelAssignmentConfirm">取消改派</button>
                <button class="primary" data-testid="assignment-draft-force" :disabled="assignmentSaving" @click="forceAssignmentChange">{{ assignmentConfirmActionLabel }}</button>
              </div>
            </div>
          </div>
        </div>
      </el-drawer>

      <section
        v-if="showInlineRiskSection"
        ref="riskSectionRef"
        class="detail-panel detail-panel-risk"
        data-testid="workbench-risk-panel"
      >
        <div class="section-head">
          <div>
            <p class="eyebrow">风险处理</p>
            <h2>风险事项详情</h2>
            <p>风险摘要、处理动作和时间线都收在这一块，不再单独弹第二层风险页。</p>
          </div>
        </div>
        <div class="overview-grid risk-inline-grid">
          <article v-for="item in riskSummaryItems" :key="item.label" class="status-card">
            <span class="card-label">{{ item.label }}</span>
            <strong class="status-card-value inline-card-value">{{ item.value }}</strong>
            <p class="status-card-copy">{{ item.hint }}</p>
          </article>
        </div>
        <div v-if="!readOnlyBatchView" class="drawer-card inline-risk-actions">
          <span class="card-label">处理动作</span>
          <div class="button-row">
            <button v-if="detail.riskHandling?.canResume && resumeAction.enabled" class="primary" @click="openStatusDialog('PUBLISHED')">恢复发布</button>
            <button v-else-if="freezeAction.enabled" class="primary" @click="openStatusDialog('FROZEN')">冻结批次</button>
            <button v-else-if="canHandleRisk" class="primary" @click="openRiskDialog('COMMENT')">补处理说明</button>
            <button v-if="canHandleRisk" class="ghost" @click="openRiskDialog('PROCESSING')">标记处理中</button>
            <button v-if="canHandleRisk" class="ghost" @click="openRiskDialog('RECTIFIED')">标记已整改</button>
            <button v-if="canHandleRisk" class="ghost" @click="openRiskDialog('RECTIFICATION')">补整改记录</button>
            <button v-if="recallAction.enabled" class="ghost danger" @click="openStatusDialog('RECALLED')">发起召回</button>
          </div>
        </div>
        <div class="drawer-card">
          <span class="card-label">处理记录时间线</span>
          <div v-if="detail.riskHandling?.history?.length" class="timeline-list">
            <article v-for="item in detail.riskHandling.history" :key="item.id" class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <strong>{{ item.actionLabel || item.actionType }}</strong>
                <span>{{ item.operatorName || '未记录处理人' }} · {{ formatDateTime(item.createdAt || item.operatedAt) }}</span>
                <p>{{ item.comment || item.reason || '暂无补充说明' }}</p>
              </div>
            </article>
          </div>
          <p v-else class="drawer-copy">当前还没有风险处理记录。</p>
        </div>
      </section>

      <el-drawer v-model="recordsDrawerVisible" direction="rtl" size="520px" append-to-body :with-header="false" class="detail-drawer">
        <div class="drawer-shell">
          <div class="drawer-head">
            <div>
              <p class="eyebrow">全部记录</p>
              <h3>最近记录列表</h3>
              <p>点击任意一条记录，可继续查看详情和图片。</p>
            </div>
            <button class="ghost icon-button" @click="recordsDrawerVisible = false">关闭</button>
          </div>
          <div v-if="allRecords.length" class="record-drawer-list">
            <article v-for="record in allRecords" :key="record.id" class="record-drawer-card">
              <div>
                <div class="record-title-row">
                  <strong>{{ record.title || recordStage(record) }}</strong>
                  <span class="detail-pill subtle">{{ recordStage(record) }}</span>
                </div>
                <div class="record-meta">
                  <span>{{ record.operatorName || '未记录提交人' }}</span>
                  <span>{{ recordTime(record) }}</span>
                </div>
                <p class="record-summary">{{ record.summary || '暂无简述' }}</p>
              </div>
              <button class="ghost" @click="openRecordDetail(record)">查看详情</button>
            </article>
          </div>
          <div v-else class="empty-panel compact"><strong>当前还没有追溯记录。</strong></div>
        </div>
      </el-drawer>

      <el-dialog v-model="recordDetailVisible" width="760px" append-to-body class="detail-dialog" title="记录详情">
        <div v-if="activeRecord" class="record-detail-dialog">
          <div class="record-detail-head">
            <div>
              <strong>{{ activeRecord.title || recordStage(activeRecord) }}</strong>
              <div class="record-meta">
                <span>{{ activeRecord.operatorName || '未记录提交人' }}</span>
                <span>{{ recordTime(activeRecord) }}</span>
                <span>{{ recordStage(activeRecord) }}</span>
              </div>
            </div>
          </div>
          <p class="record-detail-summary">{{ activeRecord.summary || '暂无说明' }}</p>
          <div v-if="recordPreviewImages(activeRecord).length" class="record-detail-gallery">
            <article v-for="image in recordPreviewImages(activeRecord)" :key="image.id" class="record-detail-image">
              <img :src="image.fileUrl" :alt="image.fileName">
              <span>{{ image.fileName }}</span>
            </article>
          </div>
          <div v-else class="empty-panel compact"><strong>这条记录没有上传图片。</strong></div>
        </div>
      </el-dialog>

      <el-dialog v-model="qrDialogVisible" width="420px" append-to-body class="detail-dialog" title="二维码">
        <div class="qr-dialog">
          <div v-if="qrLoading" class="empty-panel compact"><strong>正在生成二维码预览...</strong></div>
          <template v-else>
            <div class="qr-preview"><img v-if="qrPreviewUrl" :src="qrPreviewUrl" alt="二维码预览"></div>
            <div class="qr-info">
              <div>
                <span>二维码标识</span>
                <strong>{{ detail.qr?.token || '未生成' }}</strong>
              </div>
              <div>
                <span>公开页地址</span>
                <strong>{{ detail.qr?.publicUrl || '未开放' }}</strong>
              </div>
            </div>
            <div class="button-row button-row-secondary">
              <button class="ghost" :disabled="!canPreviewPublic" @click="openPublicPreview">查看公开页</button>
            </div>
          </template>
        </div>
      </el-dialog>

      <el-dialog
        v-model="dialog.visible"
        width="760px"
        append-to-body
        :close-on-click-modal="!dialogSubmitting"
        class="detail-dialog"
        :title="dialogTitle"
        @close="closeDialog(true)"
      >
        <div class="form-shell">
          <p v-if="dialog.type === 'trace'" class="form-intro">补一条关键现场记录，主页面只展示最近记录摘要。</p>
          <p v-else-if="dialog.type === 'quality'" class="form-intro">上传质检摘要后，主页面会同步更新质检状态和发布准备度。</p>
          <p v-else-if="dialog.type === 'risk'" class="form-intro">风险动作会进入右侧抽屉的时间线，不再在主页面大面积平铺。</p>
          <p v-else-if="dialog.type === 'status'" class="form-intro">状态流转会直接反映到主页面 4 张状态卡中。</p>

          <div v-if="dialog.type === 'trace'" class="form-grid">
            <label class="form-label"><span>记录阶段</span><select v-model="traceForm.stage"><option v-for="item in stageOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
            <label class="form-label"><span>记录标题</span><input v-model.trim="traceForm.title" type="text"></label>
            <label class="form-label"><span>记录时间</span><input v-model="traceForm.eventTime" type="datetime-local"></label>
            <label class="form-label"><span>操作人</span><input v-model.trim="traceForm.operatorName" type="text"></label>
            <label class="form-label"><span>地点</span><input v-model.trim="traceForm.location" type="text"></label>
            <label class="form-label form-label-full"><span>现场说明</span><textarea v-model.trim="traceForm.summary" rows="4"></textarea></label>
            <label class="form-label form-label-full">
              <span>上传图片</span>
              <div class="upload-box">
                <input type="file" accept="image/*" multiple @change="handleTraceFilesChange">
                <small>主页面列表只展示一张缩略图，详情弹窗里可查看全部图片。</small>
              </div>
            </label>
            <div v-if="traceUploading" class="form-label-full inline-tip">正在上传现场图片...</div>
            <div v-if="traceForm.uploadedFiles.length" class="form-label-full uploaded-file-list">
              <article v-for="file in traceForm.uploadedFiles" :key="file.id" class="uploaded-file-item">
                <div>
                  <strong>{{ fileLabel(file) }}</strong>
                  <small>{{ formatFileSize(file.fileSize) }}</small>
                </div>
                <button class="ghost" @click="removeTraceAttachment(file.id)">移除</button>
              </article>
            </div>
            <label class="checkbox-field form-label-full"><input v-model="traceForm.visibleToConsumer" type="checkbox"><span>同步到公开页可见记录</span></label>
          </div>

          <div v-else-if="dialog.type === 'quality'" class="form-grid">
            <label class="form-label"><span>报告编号</span><input v-model.trim="qualityForm.reportNo" type="text"></label>
            <label class="form-label"><span>检测机构</span><input v-model.trim="qualityForm.agency" type="text"></label>
            <label class="form-label"><span>检测时间</span><input v-model="qualityForm.reportTime" type="datetime-local"></label>
            <label class="form-label"><span>检测结果</span><select v-model="qualityForm.result"><option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
            <label class="form-label form-label-full"><span>摘要要点</span><textarea v-model.trim="qualityForm.highlightsText" rows="5"></textarea></label>
            <label class="form-label form-label-full">
              <span>附件上传</span>
              <div class="upload-box">
                <input type="file" multiple @change="handleQualityFilesChange">
                <small>建议上传 PDF 或图片附件，基础信息区只展示状态和编号。</small>
              </div>
            </label>
            <div v-if="qualityUploading" class="form-label-full inline-tip">正在上传质检附件...</div>
            <div v-if="qualityForm.uploadedFiles.length" class="form-label-full uploaded-file-list">
              <article v-for="file in qualityForm.uploadedFiles" :key="file.id" class="uploaded-file-item">
                <div>
                  <strong>{{ fileLabel(file) }}</strong>
                  <small>{{ formatFileSize(file.fileSize) }}</small>
                </div>
                <button class="ghost" @click="removeQualityAttachment(file.id)">移除</button>
              </article>
            </div>
          </div>

          <div v-else-if="dialog.type === 'risk'" class="form-grid" data-testid="workbench-risk-dialog">
            <label class="form-label"><span>处理动作</span><select v-model="riskForm.actionType"><option v-for="item in riskActionOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
            <label class="form-label"><span>处理人</span><input v-model.trim="riskForm.operatorName" type="text"></label>
            <label class="form-label form-label-full"><span>处理原因</span><textarea v-model.trim="riskForm.reason" rows="3"></textarea></label>
            <label class="form-label form-label-full"><span>处理说明</span><textarea v-model.trim="riskForm.comment" rows="4"></textarea></label>
          </div>

          <div v-else-if="dialog.type === 'status'" class="form-grid">
            <label class="form-label">
              <span>目标状态</span>
              <select v-model="statusForm.targetStatus">
                <option v-for="item in statusTargetOptions" :key="item.value" :value="item.value" :disabled="!item.allowed">{{ item.label }}</option>
              </select>
            </label>
            <label class="form-label"><span>处理人</span><input v-model.trim="statusForm.operatorName" type="text"></label>
            <label class="form-label form-label-full"><span>处理原因</span><textarea v-model.trim="statusForm.reason" rows="4"></textarea></label>
            <div class="form-label-full status-option-list">
              <article v-for="item in statusTargetOptions" :key="`${item.value}-hint`" class="status-option-card">
                <strong>{{ item.label }}</strong>
                <small>{{ item.allowed ? '当前状态允许此操作。' : item.hint }}</small>
              </article>
            </div>
          </div>

          <p v-if="dialogValidationError" class="form-error">{{ dialogValidationError }}</p>
          <div class="button-row dialog-actions">
            <button class="ghost" :disabled="dialogSubmitting" @click="closeDialog()">取消</button>
            <button v-if="dialog.type === 'trace'" class="ghost" :disabled="dialogSubmitting" @click="submitDialog({ keepOpen: true })">{{ dialogSubmitting ? '提交中...' : '保存并继续' }}</button>
            <button class="primary" :disabled="dialogSubmitting" @click="submitDialog()">{{ dialogSubmitting ? '提交中...' : '确认保存' }}</button>
          </div>
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped src="../pages/BatchWorkbenchView.css"></style>
