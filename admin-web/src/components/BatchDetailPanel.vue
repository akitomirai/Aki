<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
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
import { generateBrandedQrDataUrl } from '../utils/brandedQr'
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
  },
  showEmbeddedClose: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['close'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const resolvedBatchId = computed(() => String(props.batchId ?? route.params.id ?? '').trim())
const panelQuery = computed(() => String(props.panel || route.query.panel || '').trim())
const isEmbedded = computed(() => props.embedded)
const showEmbeddedClose = computed(() => !isEmbedded.value || props.showEmbeddedClose)

const loading = ref(true)
const detail = ref(null)
const message = ref('')
const messageType = ref('info')
const dialog = ref({ visible: false, type: '' })
const recordsDrawerVisible = ref(false)
const recordDetailVisible = ref(false)
const qrDialogVisible = ref(false)
const assignmentSectionRef = ref(null)
const flowSectionRef = ref(null)

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
const canManageAssignment = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(roleCode.value))

const batch = computed(() => detail.value?.batch ?? {})
const company = computed(() => detail.value?.company ?? {})
const recentRecords = computed(() => detail.value?.trace?.recentRecords ?? [])
const latestRecord = computed(() => recentRecords.value[0] ?? null)
const latestQualityReport = computed(() => detail.value?.quality?.latestReport ?? null)
const latestRiskAction = computed(() => detail.value?.riskHandling?.history?.[0] ?? null)

const productCode = computed(() => batch.value.productCode || detail.value?.product?.productCode || '')
const batchName = computed(() => {
  const name = batch.value.productName || detail.value?.product?.name || '未命名批次'
  if (productCode.value && String(name).includes(`（${productCode.value}）`)) return name
  return productCode.value ? `${name}（${productCode.value}）` : name
})
const batchCode = computed(() => batch.value.batchCode || '未生成批次号')
const companyName = computed(() => company.value.name || batch.value.companyName || '未关联企业')
const batchStatusText = computed(() => detail.value?.status?.label || '状态待确认')
const taskStatusText = computed(() => resolveTaskStatusText(detail.value?.task))
const todayProgressText = computed(() => resolveTodayStatusText(detail.value?.task?.todayCompleted))
const riskStageText = computed(() => resolveRiskStatusText(detail.value?.risk, detail.value?.riskHandling))
const qualityUploaded = computed(() => Number(detail.value?.quality?.reportCount || 0) > 0)
const qualityResultCode = computed(() => String(latestQualityReport.value?.result || '').toUpperCase())
const qualityAllowsPublish = computed(() => qualityResultCode.value === 'PASS')
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
const latestRiskActionLabel = computed(() => {
  if (!latestRiskAction.value) return canHandleRisk.value ? '尚未记录处理动作' : '当前无风险动作'
  return latestRiskAction.value.actionLabel || latestRiskAction.value.actionType || '已记录处理动作'
})
const riskSummaryCopy = computed(() => {
  if (!canHandleRisk.value) return '当前没有需要处理的风险事项。'
  return detail.value?.risk?.reason || detail.value?.status?.reason || '当前批次仍在风险处理中。'
})
const draftStatusText = computed(() => detail.value?.task?.draftStatusLabel || (detail.value?.task?.draftPending ? '草稿待续' : '无草稿'))
const readinessDoneCount = computed(() => [hasTraceRecord.value, qualityUploaded.value && qualityAllowsPublish.value, qrGenerated.value].filter(Boolean).length)
const currentRecommendation = computed(() => {
  if (readOnlyBatchView.value) {
    return {
      label: '当前状态',
      value: batchStatusText.value,
      meta: canPreviewPublic.value ? '公开页可查看' : '后台查看'
    }
  }
  if (resumeAction.value.enabled) {
    return {
      label: '当前建议',
      value: '恢复发布',
      meta: '风险链已完成'
    }
  }
  if (publishAction.value.enabled) {
    return {
      label: '当前建议',
      value: '发布批次',
      meta: '可直接收口'
    }
  }
  if (!hasTraceRecord.value) {
    return {
      label: '当前建议',
      value: '补录追溯',
      meta: '缺首条记录'
    }
  }
  if (!qualityUploaded.value) {
    return {
      label: '当前建议',
      value: '上传质检',
      meta: '待补质检'
    }
  }
  if (!qrGenerated.value) {
    return {
      label: '当前建议',
      value: '生成二维码',
      meta: '公开入口未生成'
    }
  }
  return {
    label: '当前状态',
    value: batchStatusText.value,
    meta: canPreviewPublic.value ? '公开页可回查' : '继续查看'
  }
})
const coreStatusCards = computed(() => [
  { key: 'batch-status', title: '批次状态', value: batchStatusText.value, tag: String(detail.value?.status?.code || '').toUpperCase() === 'PUBLISHED' ? '公开中' : '' },
  { key: 'publish-ready', title: '发布准备度', value: publishReady.value ? (resumeAction.value.enabled ? '可恢复' : '可发布') : '未就绪', tag: `${readinessDoneCount.value}/3` },
  { key: 'task-status', title: '任务执行', value: taskStatusText.value, tag: detail.value?.task?.draftPending ? '有草稿' : todayProgressText.value },
  { key: 'risk-status', title: '风险事项', value: canHandleRisk.value ? riskStageText.value : '当前无风险', tag: canHandleRisk.value ? (riskResolved.value ? '可恢复' : '处理中') : '' }
])
const manageActionTiles = computed(() => [
  { key: 'trace', label: '补录追溯', status: hasTraceRecord.value ? '已补录' : '待补录', disabled: !traceAction.value.enabled, handler: openTraceDialog },
  { key: 'quality', label: '上传质检', status: qualityUploaded.value ? (detail.value?.quality?.label || '已上传') : '待上传', disabled: !qualityAction.value.enabled, handler: openQualityDialog, testid: 'workbench-open-quality-dialog' },
  { key: 'qr', label: qrGenerated.value ? '二维码已生成' : '生成二维码', status: qrGenerated.value ? '可查看' : '待生成', disabled: qrGenerated.value ? false : !qrAction.value.enabled, handler: qrGenerated.value ? openQrDialog : () => handleGenerateQr(false) },
  { key: 'publish', label: resumeAction.value.enabled ? '恢复发布' : '发布批次', status: publishReady.value ? '可执行' : '未就绪', disabled: !publishReady.value, handler: () => openStatusDialog('PUBLISHED') },
  { key: 'assignment', label: '任务分配', status: detail.value?.task?.assigneeName || '未分配', disabled: !canManageAssignment.value, handler: openAssignmentDrawer },
  { key: 'risk', label: '风险处理', status: canHandleRisk.value ? riskStageText.value : '查看', disabled: false, handler: openRiskDrawer },
  { key: 'public', label: '查看公开页', status: canPreviewPublic.value ? '已开放' : '未开放', disabled: !canPreviewPublic.value, handler: openPublicPreview }
])
const readonlyActionTiles = computed(() => [
  { key: 'public', label: '查看公开页', status: canPreviewPublic.value ? '已开放' : '未开放', disabled: !canPreviewPublic.value, handler: openPublicPreview },
  { key: 'qr', label: '查看二维码', status: qrGenerated.value ? '可查看' : '未生成', disabled: !qrGenerated.value, handler: openQrDialog },
  { key: 'risk', label: '风险详情', status: canHandleRisk.value ? riskStageText.value : '当前无风险', disabled: false, handler: openRiskDrawer },
  { key: 'records', label: '全部记录', status: allRecords.value.length ? `${allRecords.value.length} 条` : '暂无', disabled: !allRecords.value.length, handler: openRecordsDrawer }
])
const actionTiles = computed(() => readOnlyBatchView.value ? readonlyActionTiles.value : manageActionTiles.value)
const traceTimelineSection = computed(() => detail.value?.traceTimeline ?? {
  totalCount: 0,
  phaseCount: 0,
  currentPhaseLabel: '暂无关键节点',
  currentStatusLabel: batchStatusText.value,
  items: []
})
const traceTimelineItems = computed(() => traceTimelineSection.value?.items ?? [])
const allRecords = computed(() => recentRecords.value)
const assignmentInfoCards = computed(() => [
  { label: '当前负责人', value: detail.value?.task?.assigneeName || '未分配' },
  { label: '任务状态', value: taskStatusText.value },
  { label: '分配时间', value: formatDateTime(detail.value?.task?.assignedAt) },
  { label: '草稿状态', value: draftStatusText.value }
])
const baseInfoCards = computed(() => [
  { label: '批次号', value: batchCode.value },
  { label: '产品', value: batchName.value },
  { label: '企业', value: companyName.value },
  { label: '质检状态', value: detail.value?.quality?.label || '待上传', meta: latestQualityReport.value?.reportNo ? `报告编号：${latestQualityReport.value.reportNo}` : '' },
  { label: '二维码状态', value: qrStatusText.value, testid: 'workbench-qr-status' },
  { label: '公开页状态', value: canPreviewPublic.value ? '已开放' : '未开放' },
  { label: '最近更新时间', value: formatDateTime(resolveLatestActivityTime()) }
])
const riskInfoCards = computed(() => [
  { label: '当前风险状态', value: canHandleRisk.value ? riskStageText.value : '当前无风险' },
  { label: '最近动作', value: latestRiskActionLabel.value },
  { label: '整改结果', value: detail.value?.riskHandling?.canResume ? '已满足条件' : (canHandleRisk.value ? '未满足条件' : '无需整改') }
])
const riskActionButtons = computed(() => [
  { key: 'comment', label: '补处理说明', disabled: !canHandleRisk.value, handler: () => openRiskDialog('COMMENT') },
  { key: 'processing', label: '标记处理中', disabled: !canHandleRisk.value, handler: () => openRiskDialog('PROCESSING') },
  { key: 'rectification', label: '补整改记录', disabled: !canHandleRisk.value, handler: () => openRiskDialog('RECTIFICATION') },
  { key: 'rectified', label: '标记已整改', disabled: !canHandleRisk.value, handler: () => openRiskDialog('RECTIFIED') },
  { key: 'resume', label: '恢复发布', disabled: !(detail.value?.riskHandling?.canResume && resumeAction.value.enabled), handler: () => openStatusDialog('PUBLISHED') },
  { key: 'recall', label: '发起召回', disabled: !recallAction.value.enabled, handler: () => openStatusDialog('RECALLED') }
])
function compactTimelineGroupMeta(item) {
  const label = String(item?.phaseLabel || item?.phaseCode || '')
  if (/建档|任务/.test(label)) {
    return { key: 'basic', label: '基础建档' }
  }
  if (/现场|追溯|质检|二维码/.test(label)) {
    return { key: 'trace', label: '质检与追溯' }
  }
  return { key: 'publish', label: '发布与风险' }
}
const compactTimelineGroups = computed(() => {
  const groups = []
  for (const item of traceTimelineItems.value) {
    const meta = compactTimelineGroupMeta(item)
    const lastGroup = groups[groups.length - 1]
    if (!lastGroup || lastGroup.key !== meta.key) {
      groups.push({
        ...meta,
        items: [item]
      })
      continue
    }
    lastGroup.items.push(item)
  }
  return groups
})
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
function formatTimelineSummary(value) {
  return String(value || '').replace(/质检与二维码/g, '质检、二维码')
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
  if (!canManageAssignment.value) {
    return
  }
  nextTick(() => {
    assignmentSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}
async function openRiskDrawer() {
  await nextTick()
  flowSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
    if (canManageAssignment.value) {
      openAssignmentDrawer()
    }
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
    qrPreviewUrl.value = await generateBrandedQrDataUrl(detail.value.qr.publicUrl, { width: 240 })
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
  recordsDrawerVisible.value = false
  recordDetailVisible.value = false
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
        <button v-if="showEmbeddedClose" class="ghost" @click="goBackToList">{{ isEmbedded ? '关闭' : '返回批次列表' }}</button>
        <button class="primary" @click="loadDetail(resolvedBatchId)">重新加载</button>
      </div>
    </div>
    <template v-else>
      <div v-if="message" class="message-bar" :class="messageType">{{ message }}</div>
      <div class="detail-page simple-main-grid">
        <section v-if="readOnlyBatchView" class="detail-panel readonly-banner-panel" data-testid="workbench-readonly-banner">
          <strong>监管查看模式</strong>
          <p>当前仅保留查看入口和关键状态。</p>
        </section>

        <section class="detail-panel header-panel compact-panel">
          <div class="header-main">
            <div class="header-copy">
              <h1>{{ batchName }}</h1>
              <div class="header-meta">
                <span>批次号：{{ batchCode }}</span>
                <span>企业：{{ companyName }}</span>
                <span class="status-badge" :class="String(detail.status?.code || '').toLowerCase()">{{ batchStatusText }}</span>
              </div>
            </div>
            <div class="header-actions">
              <button v-if="showEmbeddedClose" class="ghost" @click="goBackToList">{{ isEmbedded ? '关闭' : '返回批次列表' }}</button>
              <button v-if="canManageBatch" class="ghost" data-testid="workbench-copy-batch-button" @click="openCopyBatch">复制批次</button>
              <button class="ghost" :disabled="!canPreviewPublic" @click="openPublicPreview">查看公开页</button>
            </div>
          </div>
        </section>

        <section
          class="detail-panel compact-panel actions-panel"
          :data-testid="readOnlyBatchView ? undefined : 'workbench-simple-actions'"
        >
          <div class="section-head compact-head">
            <h2>主操作区</h2>
          </div>
          <div class="actions-layout" :data-testid="readOnlyBatchView ? undefined : 'workbench-action-groups'">
            <article class="status-card action-focus-card" data-testid="workbench-next-step-card">
              <div class="status-card-top">
                <span class="card-label">{{ currentRecommendation.label }}</span>
                <span class="status-badge" :class="String(detail.status?.code || '').toLowerCase()">{{ batchStatusText }}</span>
              </div>
              <strong class="status-card-value">{{ currentRecommendation.value }}</strong>
              <span class="compact-meta">{{ currentRecommendation.meta }}</span>
            </article>
            <div class="action-grid">
              <button
                v-for="action in actionTiles"
                :key="action.key"
                class="action-tile"
                :class="{ 'is-disabled': action.disabled }"
                :data-testid="action.testid || undefined"
                :disabled="action.disabled"
                @click="action.handler"
              >
                <strong>{{ action.label }}</strong>
                <span>{{ action.status }}</span>
              </button>
            </div>
          </div>
        </section>

        <section class="detail-panel compact-panel">
          <div class="section-head compact-head">
            <h2>核心状态总览</h2>
          </div>
          <div class="status-grid-four">
            <article v-for="card in coreStatusCards" :key="card.key" class="status-card compact-status-card">
              <div class="status-card-top">
                <span class="card-label">{{ card.title }}</span>
                <span v-if="card.tag" class="detail-pill subtle">{{ card.tag }}</span>
              </div>
              <strong class="status-card-value">{{ card.value }}</strong>
            </article>
          </div>
        </section>

        <section ref="assignmentSectionRef" class="detail-panel compact-panel" data-testid="workbench-assignment-panel">
          <div class="section-head compact-head">
            <div class="flow-main-title">
              <h2>基础信息与负责人</h2>
              <span class="detail-pill subtle">任务分配</span>
              <span class="detail-pill subtle">质检与二维码</span>
            </div>
          </div>
          <div class="compact-info-grid compact-info-grid--four">
            <article v-for="item in assignmentInfoCards" :key="item.label" class="info-card compact-info-card">
              <span class="card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
          <div class="compact-info-grid compact-info-grid--seven">
            <article v-for="item in baseInfoCards" :key="item.label" class="info-card compact-info-card">
              <span class="card-label">{{ item.label }}</span>
              <strong :data-testid="item.testid || undefined">{{ item.value }}</strong>
              <small v-if="item.meta">{{ item.meta }}</small>
            </article>
          </div>
          <div v-if="canManageAssignment" class="inline-assignment-bar">
            <select v-model="assignmentForm.assigneeUserId" data-testid="assignment-operator-select" :disabled="assignmentSelectDisabled">
              <option value="">{{ currentAssigneeId ? '清空分配' : '保持未分配' }}</option>
              <option v-if="!hasAssignableOperators && !operatorLoading" value="" disabled>暂无可分配操作员</option>
              <option v-for="item in operatorOptions" :key="item.id" :value="String(item.id)">{{ item.realName || item.username }}</option>
            </select>
            <div class="button-row inline-assignment-actions">
              <button class="primary" data-testid="assignment-save-button" :disabled="assignmentSaveDisabled" @click="submitAssignment(false)">{{ assignmentActionLabel }}</button>
              <button class="ghost danger" data-testid="assignment-clear-button" :disabled="!assignmentCanClear" @click="clearAssignment">清空分配</button>
            </div>
          </div>
          <div v-if="canManageAssignment" class="inline-assignment-feedback">
            <div v-if="assignmentConfirm.visible" class="warning-box" data-testid="assignment-draft-confirm">
              <strong>发现未提交草稿</strong>
              <p>{{ assignmentConfirm.message }}</p>
              <div class="button-row button-row-secondary">
                <button class="ghost" data-testid="assignment-draft-cancel" @click="cancelAssignmentConfirm">取消改派</button>
                <button class="primary" data-testid="assignment-draft-force" :disabled="assignmentSaving" @click="forceAssignmentChange">{{ assignmentConfirmActionLabel }}</button>
              </div>
            </div>
          </div>
        </section>

        <section ref="flowSectionRef" class="detail-panel compact-panel flow-panel">
          <div class="section-head compact-head">
            <h2>时间线 / 风险处理</h2>
          </div>
          <div class="flow-layout">
            <div class="flow-main">
              <div class="flow-summary-bar" data-testid="workbench-risk-panel">
                <div class="compact-info-grid compact-info-grid--three risk-grid">
                  <article v-for="item in riskInfoCards" :key="item.label" class="status-card compact-status-card">
                    <span class="card-label">{{ item.label }}</span>
                    <strong class="status-card-value inline-card-value">{{ item.value }}</strong>
                  </article>
                </div>
                <div v-if="!readOnlyBatchView" class="risk-action-grid risk-action-toolbar">
                  <button
                    v-for="action in riskActionButtons"
                    :key="action.key"
                    class="ghost"
                    :class="{ danger: action.key === 'recall' }"
                    :disabled="action.disabled"
                    @click="action.handler"
                  >
                    {{ action.label }}
                  </button>
                </div>
              </div>

              <div class="flow-main-head" data-testid="workbench-trace-chain-panel">
                <div class="flow-main-title">
                  <h3>溯源时间轴回放</h3>
                  <span class="detail-pill subtle trace-chain-badge">可信溯源校验</span>
                </div>
                <button class="ghost" :disabled="!allRecords.length" @click="openRecordsDrawer">全部记录</button>
              </div>

              <div class="latest-record-strip" data-testid="workbench-recent-records">
                <div v-if="latestRecord" class="latest-record-card" data-testid="workbench-simple-records">
                  <article class="record-card compact-record-card" data-testid="workbench-latest-record">
                    <div class="record-card-head">
                      <div>
                        <div class="record-title-row">
                          <strong>{{ latestRecord.title || recordStage(latestRecord) }}</strong>
                          <span class="detail-pill subtle">{{ recordStage(latestRecord) }}</span>
                        </div>
                        <div class="record-meta">
                          <span>{{ latestRecord.operatorName || '未记录提交人' }}</span>
                          <span>{{ recordTime(latestRecord) }}</span>
                        </div>
                      </div>
                    </div>
                    <p class="record-summary">{{ latestRecord.summary || '暂无简述' }}</p>
                    <div class="record-footer">
                      <div v-if="recordPreviewImages(latestRecord).length" class="record-thumb">
                        <img class="record-image" :src="recordPreviewImages(latestRecord)[0].fileUrl" :alt="recordPreviewImages(latestRecord)[0].fileName">
                        <span v-if="recordPreviewImages(latestRecord).length > 1" class="thumb-count">+{{ recordPreviewImages(latestRecord).length - 1 }}</span>
                      </div>
                      <div v-else class="record-thumb record-thumb-placeholder"><span>{{ recordStage(latestRecord) }}</span></div>
                      <button class="ghost" @click="openRecordDetail(latestRecord)">查看详情</button>
                    </div>
                  </article>
                </div>
                <div v-else class="empty-panel compact">
                  <strong>暂无追溯记录</strong>
                </div>
              </div>

              <div class="timeline-compact" data-testid="workbench-trace-timeline-panel">
                <div class="timeline-compact-head">
                  <span class="card-label">溯源时间轴回放</span>
                </div>
                <div v-if="compactTimelineGroups.length" class="timeline-feature-groups compact-timeline-groups">
                  <section
                    v-for="group in compactTimelineGroups"
                    :key="group.key"
                    class="compact-timeline-group"
                    :data-testid="`trace-timeline-phase-${group.key}`"
                  >
                    <div class="compact-timeline-group-head">
                      <span class="card-label">{{ group.label }}</span>
                    </div>
                    <div class="compact-timeline-list">
                      <article
                        v-for="item in group.items"
                        :key="item.key"
                        class="compact-timeline-item timeline-feature-item"
                        :class="{ 'is-highlighted': item.highlighted }"
                      >
                        <span class="compact-timeline-phase">{{ item.phaseLabel }}</span>
                        <div class="compact-timeline-main">
                          <div class="compact-timeline-top">
                            <strong>{{ item.title }}</strong>
                            <span class="detail-pill" :class="`timeline-pill-${timelineResultTone(item)}`">{{ item.resultLabel }}</span>
                          </div>
                          <div class="compact-timeline-meta">
                            <span>{{ item.eventTime }}</span>
                            <span>{{ item.operatorName }}</span>
                          </div>
                          <p class="compact-timeline-copy">{{ formatTimelineSummary(item.summary) }}</p>
                        </div>
                      </article>
                    </div>
                  </section>
                </div>
                <div v-else class="empty-panel compact">
                  <strong>暂无关键节点</strong>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

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
        <div
          class="form-shell"
          :data-testid="dialog.type === 'quality'
            ? 'workbench-quality-dialog'
            : (dialog.type === 'status' ? 'workbench-status-dialog' : undefined)"
        >
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
            <label class="form-label form-label-full"><span>质检摘要</span><textarea v-model.trim="qualityForm.highlightsText" rows="5"></textarea></label>
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
            <button class="primary" data-testid="workbench-dialog-submit" :disabled="dialogSubmitting" @click="submitDialog()">{{ dialogSubmitting ? '提交中...' : '确认保存' }}</button>
          </div>
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped src="../pages/BatchWorkbenchView.css"></style>
