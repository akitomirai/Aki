<script setup>
import { computed, onMounted, ref, watch } from 'vue'
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

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const detail = ref(null)
const message = ref('')
const messageType = ref('info')
const dialog = ref({ visible: false, type: '' })
const operatorOptions = ref([])
const operatorLoading = ref(false)
const assignmentSaving = ref(false)
const assignmentForm = ref({ assigneeUserId: '' })
const assignmentConfirm = ref({
  visible: false,
  mode: 'reassign',
  message: '',
  nextAssigneeUserId: ''
})

const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const riskForm = ref(createRiskForm())
const statusForm = ref(createStatusForm())
const dialogSubmitting = ref(false)

const traceUploading = ref(false)
const qualityUploading = ref(false)

const recentRecords = computed(() => detail.value?.trace?.recentRecords ?? [])
const latestRecord = computed(() => recentRecords.value[0] ?? null)
const canHandleRisk = computed(() => ['FROZEN', 'RECALLED'].includes(detail.value?.status?.code))
const canPreviewPublic = computed(() => Boolean(detail.value?.qr?.publicUrl))
const isFreshCreated = computed(() => String(route.query.created || '') === '1')
const copiedFromCode = computed(() => String(route.query.copiedFrom || '').trim())
const roleCode = computed(() => authStore.user?.roleCode || '')
const canManageBatch = computed(() => canManageAdminBatch(roleCode.value))
const readOnlyBatchView = computed(() => isRegulator(roleCode.value))
const pageSubtitle = computed(() => {
  if (readOnlyBatchView.value) {
    return '监管查看模式已保留批次状态、质检结果、二维码状态、最近记录、任务分配和风险动作，所有写操作都已收口。'
  }
  return ''
})
const readOnlyBannerText = computed(() => {
  return '当前账号只查看批次详情、质检摘要、二维码状态、任务分配和风险记录，不提供复制、现场录入、上传、发布或风险写入入口。'
})
const canManageAssignment = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(authStore.user?.roleCode))
const currentAssigneeId = computed(() => detail.value?.task?.assigneeUserId ? String(detail.value.task.assigneeUserId) : '')
const selectedAssigneeId = computed(() => assignmentForm.value.assigneeUserId ? String(assignmentForm.value.assigneeUserId) : '')
const assignmentChanged = computed(() => selectedAssigneeId.value !== currentAssigneeId.value)
const selectedAssignee = computed(() => operatorOptions.value.find((item) => String(item.id) === selectedAssigneeId.value) ?? null)
const assignmentActionLabel = computed(() => {
  if (!selectedAssigneeId.value) return '确认清空分配'
  if (!currentAssigneeId.value) return '分配操作员'
  if (assignmentChanged.value) return '确认改派'
  return '当前分配未变更'
})
const assignmentHint = computed(() => {
  if (!detail.value) return ''
  if (detail.value.task?.draftPending) {
    const assigneeName = detail.value.task?.assigneeName || '原分配人'
    return detail.value.task?.draftUpdatedAt
      ? `当前分配人 ${assigneeName} 还有未提交草稿，最近保存于 ${detail.value.task.draftUpdatedAt}。`
      : `当前分配人 ${assigneeName} 还有未提交草稿。`
  }
  if (readOnlyBatchView.value) {
    return detail.value.task?.assigneeName
      ? `当前由 ${detail.value.task.assigneeName} 负责后续现场作业，可继续结合任务状态和最近记录判断执行情况。`
      : '当前还没有分配操作员，可结合任务状态与最近更新时间继续判断后续跟进情况。'
  }
  if (selectedAssignee.value) {
    return `将由 ${selectedAssignee.value.realName || selectedAssignee.value.username} 接管该批次后续现场作业。`
  }
  return '清空后，该批次会从操作员待办中移除。'
})
const assignmentConfirmActionLabel = computed(() => {
  return assignmentConfirm.value.mode === 'clear'
    ? '强制清空分配并清除原分配人草稿'
    : '强制改派并清除原分配人草稿'
})
const latestRiskAction = computed(() => detail.value?.riskHandling?.history?.[0] ?? null)
const latestRecordImages = computed(() => recordPreviewImages(latestRecord.value))
const earlierRecords = computed(() => recentRecords.value.slice(1))
const latestQualityReport = computed(() => detail.value?.quality?.latestReport ?? null)
const traceAction = computed(() => actionOf('ADD_TRACE'))
const qualityAction = computed(() => actionOf('UPLOAD_QUALITY'))
const qrAction = computed(() => actionOf('GENERATE_QR'))
const publicAction = computed(() => actionOf('VIEW_PUBLIC'))
const publishAction = computed(() => actionOf('PUBLISH'))
const resumeAction = computed(() => actionOf('RESUME'))
const freezeAction = computed(() => actionOf('FREEZE'))
const recallAction = computed(() => actionOf('RECALL'))
const hasTraceRecord = computed(() => recentRecords.value.length > 0)
const publishReady = computed(() => publishAction.value.enabled || resumeAction.value.enabled)
const qualityUploaded = computed(() => Number(detail.value?.quality?.reportCount || 0) > 0)
const qualityResultCode = computed(() => String(latestQualityReport.value?.result || '').toUpperCase())
const qualityAllowsPublish = computed(() => qualityUploaded.value && qualityResultCode.value !== 'FAIL')
const publishGate = computed(() => resolvePublishBlockState({
  status: detail.value?.status?.code,
  qualityStatusCode: qualityResultCode.value || 'PENDING',
  qrStatus: detail.value?.qr?.generated ? 'READY' : 'NOT_GENERATED',
  canResume: Boolean(detail.value?.riskHandling?.canResume)
}))
const riskStageCode = computed(() => String(detail.value?.riskHandling?.currentStage || detail.value?.risk?.status || '').toUpperCase())
const riskStageText = computed(() => resolveRiskStatusText(detail.value?.risk, detail.value?.riskHandling))
const batchStatusText = computed(() => detail.value?.status?.label || '状态待确认')
const statusSummaryCopy = computed(() => batchStatusSummary(detail.value?.status?.code))
const todoSectionLabel = computed(() => readOnlyBatchView.value ? '监管关注点' : '待完成事项')
const taskStatusText = computed(() => resolveTaskStatusText(detail.value?.task))
const todayProgressText = computed(() => resolveTodayStatusText(detail.value?.task?.todayCompleted))
const assignmentDraftText = computed(() => detail.value?.task?.draftStatusLabel || '无草稿')
const latestRiskActionLabel = computed(() => {
  if (!latestRiskAction.value) return canHandleRisk.value ? '尚未记录处理动作' : '暂无风险动作'
  return `${latestRiskAction.value.actionLabel || latestRiskAction.value.actionType} · ${latestRiskAction.value.operatorName}`
})
const riskResolved = computed(() => {
  const currentStage = riskStageCode.value
  return currentStage === 'RECTIFIED' || Boolean(detail.value?.riskHandling?.canResume)
})
const riskResolutionText = computed(() => {
  if (!canHandleRisk.value) return '无需整改'
  return riskResolved.value ? '已完成整改' : '待完成整改'
})
const riskSummaryCopy = computed(() => {
  if (!canHandleRisk.value) {
    return readOnlyBatchView.value ? '当前没有处于风险链路中的动作，可继续回查质检、二维码和状态流转。' : '当前没有需要跟进的风险动作。'
  }
  return detail.value?.risk?.reason || detail.value?.status?.reason || (readOnlyBatchView.value ? '当前批次仍在风险处理中，请结合最近动作和整改结果继续判断。' : '当前批次正在风险处理中。')
})
const riskPanelCopy = computed(() => {
  if (!canHandleRisk.value) {
    return readOnlyBatchView.value ? '当前没有需要继续写入的风险动作，可重点查看最近风险记录和状态流转。' : '当前没有需要处理的风险事项。'
  }
  return detail.value?.risk?.tip || detail.value?.risk?.reason || (readOnlyBatchView.value ? '这里保留最近风险动作、整改结果和历史留痕，便于直接回查处理链。' : '先补处理说明、整改记录，再完成整改状态。')
})
const riskPanelCalm = computed(() => !canHandleRisk.value || riskResolved.value)
const riskPanelHeadline = computed(() => readOnlyBatchView.value ? '风险查看' : '风险处理')
const riskPanelIntro = computed(() => {
  if (readOnlyBatchView.value) {
    return riskPanelCalm.value ? '当前只保留风险结论、最近动作和整改结果。' : '当前展示最近风险动作、整改进度和历史留痕，便于监管回查。'
  }
  return riskPanelCalm.value ? '当前只保留风险结论和最近动作。' : '补说明、整改并更新风险状态。'
})
const assignmentPanelCopy = computed(() => {
  return readOnlyBatchView.value ? '查看当前分配结果、草稿状态和最近保存时间。' : '在当前页直接分配、改派或清空。'
})
const recordEmptyText = computed(() => {
  return readOnlyBatchView.value ? '当前还没有追溯记录，可据此判断现场资料仍未补齐。' : '当前还没有追溯记录。'
})
const todoEmptyText = computed(() => {
  return readOnlyBatchView.value ? '当前关键资料已收口，可继续核对公开页、风险和状态流转。' : '当前关键事项已收口，可以继续核对公开页、风险和状态流转。'
})
const riskChecklist = computed(() => {
  if (!canHandleRisk.value) {
    return []
  }
  const history = detail.value?.riskHandling?.history ?? []
  return [
    {
      label: '处理说明',
      done: history.some((item) => String(item.actionType).toUpperCase() === 'COMMENT'),
      hint: '先说明风险原因和当前判断。'
    },
    {
      label: '整改记录',
      done: history.some((item) => String(item.actionType).toUpperCase() === 'RECTIFICATION'),
      hint: '补齐整改动作、责任人和现场情况。'
    },
    {
      label: '整改完成',
      done: history.some((item) => String(item.actionType).toUpperCase() === 'RECTIFIED'),
      hint: '需要标记已整改，恢复发布才会开放。'
    }
  ]
})

const freshBatchGuide = computed(() => {
  if (!isFreshCreated.value) return ''
  if (copiedFromCode.value) {
    return `已基于批次 ${copiedFromCode.value} 带入基础信息。接下来仍需补录追溯、上传质检、生成二维码，再发布。`
  }
  return '这个批次刚完成建档，建议先补录首条追溯，再上传质检、生成二维码，最后发布。'
})

const todoItems = computed(() => {
  if (!detail.value) return []
  const readOnly = readOnlyBatchView.value
  const items = []
  if (!recentRecords.value.length && (readOnly || traceAction.value.enabled)) {
    items.push({
      key: 'trace',
      title: readOnly ? '缺少现场追溯' : '补录首条追溯',
      detail: readOnly ? '当前还没有关键现场记录，可据此判断批次资料仍未补齐。' : '先补一条关键现场记录。'
    })
  }
  if (!qualityUploaded.value && (readOnly || qualityAction.value.enabled)) {
    items.push({
      key: 'quality',
      title: readOnly ? '缺少质检摘要' : '上传质检',
      detail: readOnly ? '当前还没有最新质检结论，建议继续关注是否补齐报告。' : '发布前要先补质检摘要。'
    })
  }
  if (!detail.value.qr?.generated && (readOnly || qrAction.value.enabled)) {
    items.push({
      key: 'qr',
      title: readOnly ? '尚未生成二维码' : '生成二维码',
      detail: readOnly ? '公开页入口尚未就绪，可继续跟进发布准备状态。' : '公开页入口要先有二维码。'
    })
  }
  if (detail.value.status?.code === 'DRAFT') {
    items.push({
      key: 'publish',
      title: readOnly ? (publishReady.value ? '已满足发布条件' : '尚未满足发布条件') : (publishReady.value ? '发布批次' : '满足发布条件'),
      detail: readOnly ? (publishReady.value ? '当前资料已满足发布条件，但监管账号仅保留查看。' : publishGate.value.reason) : (publishReady.value ? '条件已满足，可直接发布。' : publishGate.value.reason)
    })
  }
  if (canHandleRisk.value) {
    items.push({
      key: 'risk',
      title: readOnly ? '风险链路回查' : (riskResolved.value ? '恢复发布' : '继续风险处理'),
      detail: readOnly ? (riskResolved.value ? '整改动作已基本完成，可继续核对是否满足恢复发布条件。' : '当前仍在风险处理阶段，可继续查看最近动作和整改记录。') : (riskResolved.value ? '已完成整改，可恢复发布。' : '先补处理说明、整改记录，再标记已整改。')
    })
  }
  return items
})

const quickActions = computed(() => {
  if (readOnlyBatchView.value) {
    return []
  }
  return [
    {
      key: 'trace',
      title: '补录追溯',
      status: recentRecords.value.length ? `最近记录：${formatStageLabel(latestRecord.value?.stageCode)}` : '当前还没有追溯记录',
      desc: traceAction.value.hint || '使用快速录入补齐关键节点。',
      available: traceAction.value.enabled,
      primaryText: '补录追溯',
      primaryClass: 'primary',
      primaryAction: openTraceDialog,
      disabled: !traceAction.value.enabled,
      secondaryText: '现场录入',
      secondaryAction: openFieldEntry,
      secondaryDisabled: false
    },
    {
      key: 'quality',
      title: '上传质检',
      status: detail.value?.quality?.label || '待上传',
      desc: latestQualityReport.value
        ? `最近质检：${latestQualityReport.value.reportNo} · ${latestQualityReport.value.agency}`
        : (qualityAction.value.hint || '发布前优先补齐质检摘要。'),
      available: qualityAction.value.enabled,
      primaryText: '上传质检',
      primaryClass: 'primary',
      primaryAction: openQualityDialog,
      disabled: !qualityAction.value.enabled
    },
    {
      key: 'qr',
      title: '生成二维码',
      status: `二维码${resolveQrStatusText(detail.value?.qr)}`,
      desc: detail.value?.qr?.generated
        ? `二维码标识：${detail.value.qr.token || '已生成'}`
        : (qrAction.value.hint || '同一批次默认只生成一次二维码。'),
      available: qrAction.value.enabled,
      primaryText: detail.value?.qr?.generated ? '二维码已生成' : '生成二维码',
      primaryClass: detail.value?.qr?.generated ? 'ghost' : 'primary',
      primaryAction: handleGenerateQr,
      disabled: Boolean(detail.value?.qr?.generated) || !qrAction.value.enabled,
      secondaryText: canPreviewPublic.value ? '查看公开页' : '',
      secondaryAction: openPublicPreview,
      secondaryDisabled: !canPreviewPublic.value
    },
    {
      key: 'publish',
      title: resumeAction.value.enabled ? '恢复发布' : '发布批次',
      status: publishReady.value ? '可以发布' : '暂不可发布',
      desc: publishReady.value ? '当前页可直接完成发布。' : publishGate.value.reason,
      available: publishReady.value,
      primaryText: resumeAction.value.enabled ? '恢复发布' : '发布批次',
      primaryClass: publishReady.value ? 'success' : 'ghost',
      primaryAction: () => openStatusDialog('PUBLISHED'),
      disabled: !publishReady.value,
      secondaryText: publicAction.value.enabled ? '查看公开页' : '',
      secondaryAction: openPublicPreview,
      secondaryDisabled: !publicAction.value.enabled
    },
    {
      key: 'risk',
      title: '风险处理',
      status: riskStageText.value,
      desc: canHandleRisk.value
        ? '补说明、整改并更新风险状态。'
        : (freezeAction.value.enabled ? freezeAction.value.hint : (recallAction.value.hint || '当前无需风险处理。')),
      available: canHandleRisk.value || freezeAction.value.enabled || recallAction.value.enabled,
      primaryText: canHandleRisk.value ? '补处理说明' : (freezeAction.value.enabled ? '冻结批次' : '风险处理中'),
      primaryClass: canHandleRisk.value || freezeAction.value.enabled ? 'warning' : 'ghost',
      primaryAction: canHandleRisk.value ? () => openRiskDialog('COMMENT') : () => openStatusDialog('FROZEN'),
      disabled: !(canHandleRisk.value || freezeAction.value.enabled),
      secondaryText: recallAction.value.enabled ? '发起召回' : '',
      secondaryAction: () => openStatusDialog('RECALLED'),
      secondaryDisabled: !recallAction.value.enabled
    }
  ]
})

const primaryQuickAction = computed(() => {
  if (readOnlyBatchView.value || !quickActions.value.length) {
    return null
  }
  return quickActions.value.find((item) => item.available && !item.disabled) ?? quickActions.value[0]
})

const secondaryQuickActions = computed(() => {
  if (!primaryQuickAction.value) {
    return quickActions.value
  }
  return quickActions.value.filter((item) => item.key !== primaryQuickAction.value.key)
})

const readinessItems = computed(() => [
  {
    key: 'trace',
    label: '追溯记录',
    done: hasTraceRecord.value,
    detail: hasTraceRecord.value ? `最近记录：${latestRecord.value?.title || '已补录'}` : '还没有关键追溯记录'
  },
  {
    key: 'quality',
    label: '质检摘要',
    done: qualityUploaded.value && qualityAllowsPublish.value,
    detail: !qualityUploaded.value
      ? '还没有质检摘要'
      : (qualityResultCode.value === 'FAIL' ? '当前质检结论不允许发布' : `当前结论：${detail.value?.quality?.label || '可发布'}`)
  },
  {
    key: 'qr',
    label: '二维码',
    done: Boolean(detail.value?.qr?.generated),
    detail: detail.value?.qr?.generated ? `公开标识：${detail.value?.qr?.token || '已生成'}` : '还没有公开二维码'
  }
])

const readinessSummaryText = computed(() => {
  const total = readinessItems.value.length
  const done = readinessItems.value.filter((item) => item.done).length
  if (publishReady.value) {
    return `发布前资料已齐（${done}/${total}）`
  }
  return `发布前还差 ${Math.max(total - done, 0)} 项`
})

const demoFlowSteps = computed(() => [
  {
    key: 'trace',
    label: hasTraceRecord.value ? '现场记录已齐' : '补现场记录',
    done: hasTraceRecord.value
  },
  {
    key: 'quality',
    label: qualityUploaded.value ? '质检已齐' : '上传质检',
    done: qualityUploaded.value
  },
  {
    key: 'qr',
    label: detail.value?.qr?.generated ? '二维码已齐' : '生成二维码',
    done: Boolean(detail.value?.qr?.generated)
  },
  {
    key: 'public',
    label: canPreviewPublic.value ? '查看公开页' : '核对公开入口',
    done: canPreviewPublic.value
  }
])

const publishChecks = computed(() => [
  {
    key: 'quality-uploaded',
    label: '已上传质检摘要',
    done: qualityUploaded.value,
    detail: qualityUploaded.value ? `当前状态：${detail.value?.quality?.label || '已上传'}` : '还没有质检摘要'
  },
  {
    key: 'quality-result',
    label: '质检结论允许发布',
    done: qualityAllowsPublish.value,
    detail: !qualityUploaded.value
      ? '先上传质检摘要'
      : (qualityResultCode.value === 'FAIL' ? '当前结论不允许发布' : `当前结论：${detail.value?.quality?.label || '可发布'}`)
  },
  {
    key: 'qr-generated',
    label: '已生成二维码',
    done: Boolean(detail.value?.qr?.generated),
    detail: detail.value?.qr?.generated ? `二维码标识：${detail.value?.qr?.token || '已生成'}` : '先生成二维码'
  },
  {
    key: 'risk-cleared',
    label: '风险链路允许发布',
    done: detail.value?.status?.code !== 'FROZEN' || Boolean(detail.value?.riskHandling?.canResume),
    detail: detail.value?.status?.code === 'FROZEN'
      ? (detail.value?.riskHandling?.canResume ? '整改与复核已完成' : '请先补充处理意见并标记整改完成')
      : '当前没有冻结风险阻塞'
  }
])

const statusTargetOptions = computed(() => {
  const currentStatus = detail.value?.status?.code
  return [
    { value: 'PUBLISHED', label: '发布' },
    { value: 'FROZEN', label: '冻结' },
    { value: 'RECALLED', label: '召回' }
  ].map((option) => {
    const allowed = canBatchStatusTransition(currentStatus, option.value)
    return {
      ...option,
      allowed,
      hint: allowed ? '' : statusTransitionBlockedHint(currentStatus, option.value)
    }
  })
})

const dialogValidationError = computed(() => {
  if (!dialog.value.visible || readOnlyBatchView.value) {
    return ''
  }
  if (dialog.value.type === 'trace') {
    if (!traceForm.value.eventTime) return '请补充记录时间。'
    if (!traceForm.value.operatorName?.trim()) return '请补充操作人。'
    if (!traceForm.value.location?.trim()) return '请补充地点信息。'
    if (!traceForm.value.summary?.trim()) return '请填写现场说明。'
    if (traceUploading.value) return '现场图片仍在上传中，请等待上传完成后再提交。'
    return ''
  }
  if (dialog.value.type === 'quality') {
    if (!qualityForm.value.reportNo?.trim()) return '请填写报告编号。'
    if (!qualityForm.value.agency?.trim()) return '请填写检测机构。'
    if (!qualityForm.value.reportTime) return '请填写检测时间。'
    if (!splitHighlightsInput(qualityForm.value.highlightsText).length) return '请至少填写一条质检摘要。'
    if (qualityUploading.value) return '质检附件仍在上传中，请等待上传完成后再提交。'
    return ''
  }
  if (dialog.value.type === 'risk') {
    if (!riskForm.value.operatorName?.trim()) return '请填写处理人。'
    if (['PROCESSING', 'RECTIFIED'].includes(riskForm.value.actionType) && !riskForm.value.reason?.trim()) {
      return '当前处理类型必须填写处理原因。'
    }
    if (['COMMENT', 'RECTIFICATION'].includes(riskForm.value.actionType) && !riskForm.value.comment?.trim()) {
      return '当前处理类型必须填写处理说明。'
    }
    return ''
  }
  if (dialog.value.type === 'status') {
    if (!statusForm.value.targetStatus) return '请先选择目标状态。'
    if (statusForm.value.targetStatus === 'PUBLISHED' && !publishGate.value.allowed) {
      return publishGate.value.reason
    }
    if (!canBatchStatusTransition(detail.value?.status?.code, statusForm.value.targetStatus)) {
      return statusTransitionBlockedHint(detail.value?.status?.code, statusForm.value.targetStatus)
    }
    if (!statusForm.value.operatorName?.trim()) return '请填写处理人。'
    if (!statusForm.value.reason?.trim()) return '请填写处理原因。'
  }
  return ''
})

function createRiskForm(actionType = 'COMMENT') {
  return { actionType, reason: '', comment: '', operatorName: '监管人员' }
}

function createStatusForm(targetStatus = 'PUBLISHED') {
  return {
    targetStatus,
    reason: {
      PUBLISHED: '关键资料已齐，准备对外发布。',
      FROZEN: '发现异常，先冻结批次并进入处理。',
      RECALLED: '风险已确认，立即召回并保留公开提醒。'
    }[targetStatus] ?? '',
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

function statusClass(status) {
  return { DRAFT: 'draft', PUBLISHED: 'published', FROZEN: 'frozen', RECALLED: 'recalled' }[status] ?? 'draft'
}

function statusTransitionBlockedHint(currentStatus, targetStatus) {
  const current = String(currentStatus || '').toUpperCase()
  const target = String(targetStatus || '').toUpperCase()
  if (!target) {
    return '目标状态不能为空。'
  }
  if (!current) {
    return '当前状态缺失，暂不允许变更。'
  }
  if (current === target) {
    return '当前状态与目标状态一致，无需重复提交。'
  }
  if (target === 'DRAFT') {
    return '不支持回退到草稿状态。'
  }
  if (current === 'RECALLED') {
    return '召回状态不允许继续变更。'
  }
  if (current === 'DRAFT') {
    return '草稿状态仅支持发布。'
  }
  if (current === 'PUBLISHED') {
    return '已发布状态仅支持冻结或召回。'
  }
  if (current === 'FROZEN') {
    return '冻结状态仅支持恢复发布或召回。'
  }
  return `状态 ${current} 暂不支持变更到 ${target}。`
}

function batchStatusSummary(status) {
  if (isFreshCreated.value && String(status).toUpperCase() === 'DRAFT') {
    return readOnlyBatchView.value
      ? '这个批次刚完成建档，现场记录、质检和二维码可能仍在补齐中。'
      : '这个批次刚完成建档，先补现场记录、上传质检、生成二维码，再发布。'
  }
  if (readOnlyBatchView.value) {
    return {
      DRAFT: '当前批次仍在准备阶段，可重点核对现场记录、质检和二维码是否齐全。',
      PUBLISHED: '当前批次已对外公开，可继续回查公开页、最近记录和风险状态。',
      FROZEN: '当前批次已冻结，可继续查看最近风险动作和整改结果。',
      RECALLED: '当前批次已召回，公开页会持续展示风险提示。'
    }[String(status).toUpperCase()] ?? '当前批次状态待确认。'
  }
  return {
    DRAFT: '当前批次还在后台准备阶段，先补齐现场、质检和二维码。',
    PUBLISHED: '当前批次已对外公开，可继续回查公开页和最近记录。',
    FROZEN: '当前批次已冻结，需先补处理说明和整改记录。',
    RECALLED: '当前批次已召回，公开页会持续显示风险提示。'
  }[String(status).toUpperCase()] ?? '当前批次状态待确认。'
}

function localizeWorkbenchText(text) {
  const value = String(text || '').trim()
  if (!value) return ''
  return {
    'Public trace page is available for this batch.': '当前批次已开放公开查询，可继续查看关键追溯节点。',
    'Used to verify released-batch linkage with the workbench.': '用于核对已发布批次与工作台、公开页的联动状态。',
    'The batch has been created and still needs field records, QA and QR data.': '当前批次已建档，仍需补录现场记录、质检和二维码。',
    'Used for continuous field-entry verification before publish.': '用于发布前连续补录现场作业与工作台联动验证。',
    'The batch is paused and waiting for follow-up handling.': '当前批次已暂停流转，等待后续风险处理。',
    'Used to review frozen-batch rectification flow.': '用于核对冻结批次的整改处理流程。',
    'Latest QA failed and the batch is waiting for recheck.': '最近一次质检未通过，当前批次等待复检。'
  }[value] ?? value
}

function historyReasonText(item) {
  return localizeWorkbenchText(item?.reason) || batchStatusSummary(item?.status)
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
  if (record?.imageUrl) {
    return [{
      id: `cover-${record.id || 'record'}`,
      fileUrl: record.imageUrl,
      fileName: record.title || '现场图片'
    }]
  }
  return []
}

function openFreshBatchNextStep() {
  if (traceAction.value.enabled) {
    openTraceDialog()
    return
  }
  if (qualityAction.value.enabled) {
    openQualityDialog()
    return
  }
  if (qrAction.value.enabled) {
    handleGenerateQr()
  }
}

function scrollToActionHub() {
  const target = document.querySelector('[data-testid="workbench-action-groups"]')
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openCopyBatch() {
  if (!detail.value?.batch?.id) return
  router.push({
    path: '/batches',
    query: {
      mode: 'ALL',
      copyFrom: String(detail.value.batch.id)
    }
  })
}

function openFieldEntry() {
  router.push({ path: '/field-entry', query: { batchId: detail.value?.batch?.id } })
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
  if (dialogSubmitting.value && !force) {
    return
  }
  dialog.value = { visible: false, type: '' }
}

function syncAssignmentForm() {
  assignmentForm.value.assigneeUserId = detail.value?.task?.assigneeUserId ? String(detail.value.task.assigneeUserId) : ''
  assignmentConfirm.value = {
    visible: false,
    mode: 'reassign',
    message: '',
    nextAssigneeUserId: assignmentForm.value.assigneeUserId
  }
}

function operatorOptionLabel(option) {
  if (!option) return '请选择操作员'
  const displayName = option.realName || option.username || '未命名操作员'
  if (option.companyName) {
    return `${displayName}（${option.username} / ${option.companyName}）`
  }
  if (option.username) {
    return `${displayName}（${option.username}）`
  }
  return displayName
}

async function loadAssignableOperators() {
  if (!canManageAssignment.value) {
    operatorOptions.value = []
    return
  }
  operatorLoading.value = true
  try {
    const response = await getOperatorOptions({
      companyId: detail.value?.company?.id || undefined
    })
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
    const response = await updateBatchAssignment(route.params.id, {
      assigneeUserId,
      forceClearDraft
    })
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
      assignmentConfirm.value = {
        visible: true,
        mode: nextMode === 'clear' ? 'clear' : 'reassign',
        message: nextMessage,
        nextAssigneeUserId: selectedAssigneeId.value
      }
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
  assignmentConfirm.value = {
    visible: false,
    mode: 'reassign',
    message: '',
    nextAssigneeUserId: currentAssigneeId.value
  }
  assignmentForm.value.assigneeUserId = currentAssigneeId.value
}

async function forceAssignmentChange() {
  assignmentConfirm.value.visible = false
  await submitAssignment(true)
}

async function loadDetail(id) {
  if (!id) return
  loading.value = true
  try {
    const response = await getBatchDetail(id)
    detail.value = response.data
    syncAssignmentForm()
    await loadAssignableOperators()
  } catch (error) {
    operatorOptions.value = []
    showMessage(error?.response?.data?.message || error?.message || '批次工作台加载失败，请稍后再试。', 'error')
  } finally {
    loading.value = false
  }
}

async function submitDialog(options = {}) {
  if (dialogSubmitting.value) {
    return
  }
  if (dialogValidationError.value) {
    showMessage(dialogValidationError.value, 'error')
    return
  }
  const { keepOpen = false } = options
  if (dialog.value.type === 'quality' && String(qualityForm.value.result || '').toUpperCase() === 'FAIL') {
    const confirmedFail = window.confirm('当前质检结论为“不合格”，提交后会直接影响发布流程，确认继续吗？')
    if (!confirmedFail) {
      return
    }
  }
  if (dialog.value.type === 'risk') {
    const riskActionLabel = riskActionOptions.find((item) => item.value === riskForm.value.actionType)?.label || riskForm.value.actionType
    const confirmedRisk = window.confirm(`确认提交“${riskActionLabel}”风险动作吗？`)
    if (!confirmedRisk) {
      return
    }
  }
  if (dialog.value.type === 'status') {
    const actionLabel = statusForm.value.targetStatus === 'PUBLISHED'
      ? '发布/恢复发布'
      : (statusForm.value.targetStatus === 'FROZEN' ? '冻结' : '召回')
    const confirmedStatus = window.confirm(`确认执行“${actionLabel}”状态操作吗？`)
    if (!confirmedStatus) {
      return
    }
  }

  dialogSubmitting.value = true
  try {
    let response
    if (dialog.value.type === 'trace') {
      response = await createTraceRecord(route.params.id, {
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
      if (keepOpen) {
        traceForm.value = createTraceForm({ stage: traceForm.value.stage, operatorName: traceForm.value.operatorName, location: traceForm.value.location })
        showMessage('这条记录已保存，可以继续补下一条。', 'success')
        return
      }
      showMessage('追溯记录已保存。', 'success')
    } else if (dialog.value.type === 'quality') {
      response = await createQualityReport(route.params.id, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlightsInput(qualityForm.value.highlightsText),
        attachmentIds: qualityForm.value.attachmentIds
      })
      detail.value = response.data
      syncAssignmentForm()
      showMessage('质检摘要已上传。', 'success')
    } else if (dialog.value.type === 'risk') {
      response = await createRiskAction(route.params.id, riskForm.value)
      detail.value = response.data
      syncAssignmentForm()
      showMessage('风险处理已记录。', 'success')
    } else if (dialog.value.type === 'status') {
      response = await changeBatchStatus(route.params.id, statusForm.value)
      detail.value = response.data
      syncAssignmentForm()
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
    const response = await generateBatchQr(route.params.id)
    detail.value = response.data
    syncAssignmentForm()
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
    if (!traceForm.value.imageUrl && uploadedFiles[0]?.fileUrl) {
      traceForm.value.imageUrl = uploadedFiles[0].fileUrl
    }
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

watch(() => route.params.id, async (id) => { await loadDetail(id) })
onMounted(async () => {
  await loadDetail(route.params.id)
  if (isFreshCreated.value) {
    if (copiedFromCode.value) {
      showMessage(`已基于批次 ${copiedFromCode.value} 复制出新批次，先补录追溯、上传质检和生成二维码。`, 'success')
    } else {
      showMessage('批次已创建成功，先补录追溯、上传质检和生成二维码。', 'success')
    }
  }
})
</script>

<template>
  <div class="page-shell" data-testid="batch-workbench-page">
    <div v-if="loading" class="loading-card">正在加载批次工作台...</div>

    <template v-else-if="detail">
      <section class="manage-page-header">
        <div>
          <h1 class="manage-page-title">{{ detail.product.name }}</h1>
          <p class="workbench-meta">
            {{ detail.batch.batchCode }} · {{ detail.company.name }}
            <template v-if="detail.task?.assigneeName"> · 已分配给 {{ detail.task.assigneeName }}</template>
          </p>
          <p v-if="pageSubtitle" class="manage-page-subtitle">{{ pageSubtitle }}</p>
        </div>
        <div class="manage-page-actions">
          <button class="ghost" @click="router.push('/batches')">返回批次列表</button>
          <button v-if="canManageBatch" class="ghost" data-testid="workbench-copy-batch-button" @click="openCopyBatch">复制为新批次</button>
          <button v-if="canManageBatch" class="ghost" data-testid="workbench-field-entry-button" @click="openFieldEntry">现场录入</button>
          <button class="ghost" :disabled="!canPreviewPublic" @click="openPublicPreview">查看公开页</button>
        </div>
      </section>

      <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

      <section v-if="readOnlyBatchView" class="fresh-batch-banner" data-testid="workbench-readonly-banner">
        <div>
          <span class="card-label">监管查看</span>
          <h2>当前批次为只读监管视图</h2>
          <p>{{ readOnlyBannerText }}</p>
        </div>
      </section>

      <section v-if="isFreshCreated" class="fresh-batch-banner" data-testid="fresh-batch-banner">
        <div>
          <span class="card-label">刚创建完成</span>
          <h2>{{ detail.batch.batchCode }} 已建档</h2>
          <small v-if="copiedFromCode" class="fresh-copy-source" data-testid="fresh-batch-copy-source">复制来源：{{ copiedFromCode }}</small>
          <p>{{ freshBatchGuide }}</p>
        </div>
        <div v-if="canManageBatch" class="banner-actions">
          <button class="primary" data-testid="fresh-batch-trace-button" @click="openFreshBatchNextStep">去补第一条记录</button>
          <button class="ghost" @click="scrollToActionHub">查看待完成事项</button>
        </div>
      </section>

      <section class="top-grid" data-testid="workbench-top-grid">
        <article class="summary-card summary-card--featured" data-testid="workbench-next-step-card">
          <span class="card-label">{{ readOnlyBatchView ? '监管焦点' : '推荐下一步' }}</span>
          <div class="summary-card-headline">
            <div>
              <p class="card-title">
                {{ readOnlyBatchView ? (todoItems[0]?.title || '当前关键资料已齐') : (primaryQuickAction?.title || '当前批次可继续回查') }}
              </p>
              <p class="card-copy">
                {{ readOnlyBatchView ? (todoItems[0]?.detail || todoEmptyText) : (primaryQuickAction?.desc || '当前没有阻塞动作，可直接继续讲解工作台和公开页表现。') }}
              </p>
            </div>
            <span
              class="availability-badge"
              :class="{ ok: readOnlyBatchView || (primaryQuickAction?.available && !primaryQuickAction?.disabled), blocked: !readOnlyBatchView && (!primaryQuickAction?.available || primaryQuickAction?.disabled) }"
            >
              {{ readOnlyBatchView ? '只读查看' : ((primaryQuickAction?.available && !primaryQuickAction?.disabled) ? '可直接处理' : '需先补前置条件') }}
            </span>
          </div>
          <div class="summary-meta-list compact featured-meta">
            <div>
              <span>当前状态</span>
              <strong>{{ batchStatusText }}</strong>
            </div>
            <div>
              <span>任务状态</span>
              <strong>{{ taskStatusText }}</strong>
            </div>
          </div>
          <div v-if="!readOnlyBatchView && primaryQuickAction" class="action-buttons featured-actions">
            <button :class="primaryQuickAction.primaryClass" :disabled="primaryQuickAction.disabled" @click="primaryQuickAction.primaryAction">
              {{ primaryQuickAction.primaryText }}
            </button>
            <button
              v-if="primaryQuickAction.secondaryText"
              class="ghost"
              :disabled="primaryQuickAction.secondaryDisabled"
              @click="primaryQuickAction.secondaryAction"
            >
              {{ primaryQuickAction.secondaryText }}
            </button>
          </div>
          <div v-if="!readOnlyBatchView" class="demo-flow">
            <small>演示顺序</small>
            <div class="demo-flow-list">
              <span v-for="item in demoFlowSteps" :key="item.key" :class="{ done: item.done }">
                {{ item.label }}
              </span>
            </div>
          </div>
        </article>

        <article class="summary-card">
          <span class="card-label">批次状态</span>
          <div class="card-head">
            <strong class="status-badge" :class="statusClass(detail.status.code)">{{ batchStatusText }}</strong>
            <small>{{ detail.status.changedAt || '暂无时间' }}</small>
          </div>
          <p class="card-title">{{ detail.status.currentNode }}</p>
          <p class="card-copy">{{ statusSummaryCopy }}</p>
        </article>

        <article class="summary-card">
          <span class="card-label">发布准备度</span>
          <p class="card-title">{{ readinessSummaryText }}</p>
          <div class="progress-strip readiness-strip">
            <span
              v-for="item in readinessItems"
              :key="item.key"
              class="progress-pill"
              :class="{ done: item.done }"
            >
              {{ item.label }}
            </span>
          </div>
          <p class="card-copy">{{ publishReady ? '当前已经满足发布动作所需的关键资料。' : '建议先按追溯、质检、二维码的顺序补齐后再发布。' }}</p>
        </article>

        <article class="summary-card">
          <span class="card-label">任务执行</span>
          <p class="card-title">{{ detail.task?.assigneeName || '未分配操作员' }}</p>
          <div class="summary-meta-list compact">
            <div>
              <span>任务状态</span>
              <strong>{{ taskStatusText }}</strong>
            </div>
            <div>
              <span>今日进度</span>
              <strong>{{ todayProgressText }}</strong>
            </div>
          </div>
          <p v-if="detail.task?.draftPending" class="task-draft-copy">
            {{ assignmentDraftText }}
            <template v-if="detail.task?.draftUpdatedAt"> · 最近保存 {{ detail.task.draftUpdatedAt }}</template>
          </p>
        </article>

        <article class="summary-card">
          <span class="card-label">风险事项</span>
          <p class="card-title">{{ riskStageText }}</p>
          <p class="card-copy">{{ riskSummaryCopy }}</p>
          <div class="summary-meta-list compact">
            <div>
              <span>最近动作</span>
              <strong>{{ latestRiskActionLabel }}</strong>
            </div>
            <div>
              <span>整改状态</span>
              <strong>{{ riskResolutionText }}</strong>
            </div>
          </div>
        </article>
      </section>

      <section class="action-panel">
        <div class="section-head">
          <div>
            <h2>{{ readOnlyBatchView ? '监管摘要' : '主动作与配套动作' }}</h2>
            <p>{{ readOnlyBatchView ? '保留质检、二维码、发布条件和公开入口，便于直接判断批次是否具备对外展示条件。' : '把主链路动作和辅助入口拆开，讲解时更容易一步一步往下走。' }}</p>
          </div>
        </div>
        <div class="action-hub">
          <div v-if="!readOnlyBatchView" class="action-primary-stack">
            <article v-if="primaryQuickAction" class="recommended-action-card">
              <div class="action-card-head">
                <span class="card-label">主动作</span>
                <span class="availability-badge" :class="{ ok: primaryQuickAction.available && !primaryQuickAction.disabled, blocked: primaryQuickAction.disabled || !primaryQuickAction.available }">
                  {{ primaryQuickAction.available && !primaryQuickAction.disabled ? '建议优先演示' : '需要先补条件' }}
                </span>
              </div>
              <h3>{{ primaryQuickAction.title }}</h3>
              <strong class="action-status">{{ primaryQuickAction.status }}</strong>
              <p>{{ primaryQuickAction.desc }}</p>
              <div class="action-buttons">
                <button :class="primaryQuickAction.primaryClass" :disabled="primaryQuickAction.disabled" @click="primaryQuickAction.primaryAction">
                  {{ primaryQuickAction.primaryText }}
                </button>
                <button
                  v-if="primaryQuickAction.secondaryText"
                  class="ghost"
                  :disabled="primaryQuickAction.secondaryDisabled"
                  @click="primaryQuickAction.secondaryAction"
                >
                  {{ primaryQuickAction.secondaryText }}
                </button>
              </div>
            </article>

            <div class="action-grid" data-testid="workbench-action-groups">
              <article
                v-for="item in secondaryQuickActions"
                :key="item.key"
                class="action-card"
                :class="{ unavailable: !item.available && item.disabled !== false }"
              >
                <div class="action-card-head">
                  <span class="card-label">{{ item.title }}</span>
                  <span class="availability-badge" :class="{ ok: item.available && !item.disabled, blocked: item.disabled || !item.available }">
                    {{ item.available && !item.disabled ? '当前可做' : '暂不可做' }}
                  </span>
                </div>
                <h3>{{ item.title }}</h3>
                <strong class="action-status">{{ item.status }}</strong>
                <p>{{ item.disabled && item.desc ? `原因：${item.desc}` : item.desc }}</p>
                <div class="action-buttons">
                  <button :class="item.primaryClass" :disabled="item.disabled" @click="item.primaryAction">{{ item.primaryText }}</button>
                  <button
                    v-if="item.secondaryText"
                    class="ghost"
                    :disabled="item.secondaryDisabled"
                    @click="item.secondaryAction"
                  >
                    {{ item.secondaryText }}
                  </button>
                </div>
              </article>
            </div>
          </div>

          <div v-else class="summary-card release-readonly-card" data-testid="workbench-release-readonly">
            <span class="card-label">监管提示</span>
            <p class="card-title">{{ publishReady ? '已满足发布条件' : '仍需继续补齐资料' }}</p>
            <p class="card-copy">
              {{ publishReady ? '当前批次已经具备发布条件，但监管账号仅保留查看。' : publishGate.reason }}
            </p>
            <div class="summary-meta-list compact">
              <div>
                <span>质检状态</span>
                <strong>{{ detail.quality.label }}</strong>
              </div>
              <div>
                <span>二维码状态</span>
                <strong>{{ resolveQrStatusText(detail.qr) }}</strong>
              </div>
            </div>
          </div>

          <article class="panel release-panel">
            <div class="section-head">
              <div>
                <h2>质检 / 二维码 / 发布</h2>
                <p>发布前的关键条件和公开入口都集中在这里，适合答辩时顺着闭环直接讲。</p>
              </div>
            </div>

            <div class="release-summary-grid">
              <article class="release-summary-card" data-testid="workbench-quality-panel">
                <div class="release-card-head">
                  <div>
                    <span class="card-label">质检</span>
                    <strong>{{ detail.quality.label }}</strong>
                  </div>
                  <button v-if="canManageBatch" class="ghost" data-testid="workbench-open-quality-dialog" @click="openQualityDialog">上传质检</button>
                </div>
                <p class="panel-copy">{{ latestQualityReport ? `最近质检：${latestQualityReport.reportNo} · ${latestQualityReport.agency}` : '当前还没有质检摘要。' }}</p>
                <div class="compact-grid">
                  <div>
                    <span>是否已有质检</span>
                    <strong>{{ qualityUploaded ? '已有' : '暂无' }}</strong>
                  </div>
                  <div>
                    <span>质检结论</span>
                    <strong>{{ latestQualityReport?.resultLabel || detail.quality.label }}</strong>
                  </div>
                  <div>
                    <span>报告编号</span>
                    <strong>{{ latestQualityReport?.reportNo || '暂无报告' }}</strong>
                  </div>
                  <div>
                    <span>检测时间</span>
                    <strong>{{ latestQualityReport?.reportTime || '暂无时间' }}</strong>
                  </div>
                </div>
                <ul
                  v-if="latestQualityReport?.highlights?.length"
                  class="mini-list quality-highlight-list"
                  data-testid="workbench-quality-highlights"
                >
                  <li v-for="item in latestQualityReport.highlights" :key="item">
                    <strong>{{ item }}</strong>
                  </li>
                </ul>
                <div
                  v-if="latestQualityReport?.attachments?.length"
                  class="inline-actions quality-attachment-links"
                  data-testid="workbench-quality-attachments"
                >
                  <a
                    v-for="item in latestQualityReport.attachments"
                    :key="item.id || item.fileUrl"
                    class="preview-link"
                    :href="item.fileUrl"
                    target="_blank"
                    rel="noreferrer"
                  >
                    查看{{ item.fileName || '质检附件' }}
                  </a>
                </div>
              </article>

              <article class="release-summary-card" data-testid="workbench-qr-panel">
                <div class="release-card-head">
                  <div>
                    <span class="card-label">二维码</span>
                    <strong data-testid="workbench-qr-status">{{ resolveQrStatusText(detail.qr) }}</strong>
                  </div>
                  <button
                    v-if="canManageBatch"
                    class="ghost"
                    data-testid="workbench-qr-action-0"
                    :disabled="detail.qr.generated || !qrAction.enabled"
                    @click="handleGenerateQr"
                  >
                    {{ detail.qr.generated ? '二维码已生成' : '生成二维码' }}
                  </button>
                </div>
                <p class="panel-copy">{{ detail.qr.generated ? `公开访问标识：${detail.qr.token || '已生成'}` : (qrAction.hint || '先生成二维码，再核对公开页入口。') }}</p>
                <a
                  v-if="detail.qr.publicUrl"
                  class="public-link"
                  data-testid="workbench-public-preview"
                  :href="detail.qr.publicUrl"
                  target="_blank"
                  rel="noreferrer"
                >
                  查看公开页
                </a>
              </article>
            </div>

            <div class="publish-checklist">
              <article v-for="item in publishChecks" :key="item.key" class="publish-check-card">
                <strong>{{ item.label }}</strong>
                <span :class="{ done: item.done }">{{ item.done ? '已满足' : '未满足' }}</span>
                <small>{{ item.detail }}</small>
              </article>
            </div>

            <div v-if="canManageBatch" class="release-actions">
              <button class="success" data-testid="workbench-publish-action" :disabled="!publishReady" @click="openStatusDialog('PUBLISHED')">
                {{ resumeAction.enabled ? '恢复发布' : '发布批次' }}
              </button>
              <span class="release-tip">
                {{ publishReady ? '条件已满足。' : publishGate.reason }}
              </span>
            </div>
          </article>
        </div>
      </section>

      <section class="content-grid">
        <article class="panel" data-testid="workbench-recent-records">
          <div class="section-head">
            <div>
              <h2>最近记录与图片回查</h2>
              <p>先看最新一条，再回查更早记录，确认环节、提交人、提交时间和图片顺序。</p>
            </div>
            <div class="inline-actions">
              <button v-if="!readOnlyBatchView" class="primary" @click="openTraceDialog">补录追溯</button>
            </div>
          </div>

          <article v-if="latestRecord" class="latest-record-card" data-testid="workbench-latest-record">
            <div class="record-head">
              <div>
                <strong>{{ latestRecord.title }}</strong>
                <span>{{ formatStageLabel(latestRecord.stageCode) }} · {{ latestRecord.operatorName }}</span>
              </div>
              <span class="record-tag">{{ latestRecord.eventTime }}</span>
            </div>
            <div class="record-meta-row">
              <small>提交地点：{{ latestRecord.location || '暂无地点' }}</small>
              <small>消费者可见：{{ latestRecord.visibleToConsumer ? '是' : '否' }}</small>
            </div>
            <p>{{ latestRecord.summary }}</p>
            <div v-if="latestRecordImages.length" class="record-image-grid">
              <article v-for="asset in latestRecordImages" :key="asset.id" class="record-image-frame">
                <img class="record-image" :src="asset.fileUrl" :alt="asset.fileName">
              </article>
            </div>
          </article>
          <p v-else class="empty-copy">{{ recordEmptyText }}</p>

          <div v-if="earlierRecords.length" class="record-list compact-record-list">
            <article v-for="item in earlierRecords" :key="item.id" class="record-card">
              <div class="record-head">
                <div>
                  <strong>{{ item.title }}</strong>
                  <span>{{ formatStageLabel(item.stageCode) }} · {{ item.operatorName }}</span>
                </div>
                <small>{{ item.eventTime }}</small>
              </div>
              <p>{{ item.summary }}</p>
              <small>{{ item.location || '暂无地点' }}</small>
              <div v-if="recordPreviewImages(item).length" class="record-image-grid compact-images">
                <article v-for="asset in recordPreviewImages(item)" :key="asset.id" class="record-image-frame">
                  <img class="record-image" :src="asset.fileUrl" :alt="asset.fileName">
                </article>
              </div>
            </article>
          </div>
        </article>

        <article class="panel assignment-panel" data-testid="workbench-assignment-panel">
          <div class="section-head">
            <div>
              <h2>任务分配</h2>
              <p>{{ assignmentPanelCopy }}</p>
            </div>
          </div>
          <div class="assignment-grid">
            <div class="assignment-overview">
              <div class="compact-grid">
                <div>
                  <span>分配时间</span>
                  <strong>{{ detail.task?.assignedAt || '暂无记录' }}</strong>
                </div>
                <div>
                  <span>草稿状态</span>
                  <strong>{{ assignmentDraftText }}</strong>
                </div>
                <div>
                  <span>草稿更新时间</span>
                  <strong>{{ detail.task?.draftUpdatedAt || '暂无草稿' }}</strong>
                </div>
              </div>
              <p class="panel-copy assignment-note">{{ assignmentHint }}</p>
            </div>

            <div v-if="canManageAssignment" class="assignment-actions">
              <label>
                <span>指派操作员</span>
                <select v-model="assignmentForm.assigneeUserId" data-testid="assignment-operator-select" :disabled="operatorLoading || assignmentSaving">
                  <option value="">未分配操作员</option>
                  <option v-for="item in operatorOptions" :key="item.id" :value="String(item.id)">{{ operatorOptionLabel(item) }}</option>
                </select>
              </label>
              <p class="assignment-helper">
                <template v-if="operatorLoading">正在加载可分配操作员...</template>
                <template v-else-if="operatorOptions.length">当前企业可分配 {{ operatorOptions.length }} 位操作员。</template>
                <template v-else>当前没有可分配的操作员，请先检查企业账号与角色。</template>
              </p>
              <div class="action-buttons assignment-buttons">
                <button
                  class="primary"
                  data-testid="assignment-save-button"
                  :disabled="assignmentSaving || operatorLoading || !assignmentChanged"
                  @click="submitAssignment(false)"
                >
                  {{ assignmentActionLabel }}
                </button>
                <button
                  class="ghost danger"
                  data-testid="assignment-clear-button"
                  :disabled="assignmentSaving || !detail.task?.assigneeUserId"
                  @click="clearAssignment"
                >
                  清空分配
                </button>
              </div>

              <div v-if="assignmentConfirm.visible" class="assignment-warning" data-testid="assignment-draft-confirm">
                <strong>该批次存在未提交草稿</strong>
                <p>{{ assignmentConfirm.message }}</p>
                <div class="inline-actions">
                  <button class="ghost" data-testid="assignment-draft-cancel" @click="cancelAssignmentConfirm">取消改派</button>
                  <button class="warning" data-testid="assignment-draft-force" :disabled="assignmentSaving" @click="forceAssignmentChange">
                    {{ assignmentConfirmActionLabel }}
                  </button>
                </div>
              </div>
            </div>

            <div v-else class="assignment-readonly">
              <p class="empty-copy">当前账号只查看分配结果，分配、改派和清空操作仅对管理员开放。</p>
            </div>
          </div>
        </article>
      </section>

      <section class="risk-panel panel" :class="{ calm: riskPanelCalm }" data-testid="workbench-risk-panel">
        <div class="section-head">
          <div>
            <h2>{{ riskPanelHeadline }}</h2>
            <p>{{ riskPanelIntro }}</p>
          </div>
          <div v-if="canManageBatch" class="inline-actions" data-testid="workbench-group-status">
            <button class="ghost" :disabled="!canHandleRisk" @click="openRiskDialog('COMMENT')">补处理说明</button>
            <button class="ghost" :disabled="!canHandleRisk" @click="openRiskDialog('RECTIFICATION')">补整改记录</button>
            <button class="warning" :disabled="!canHandleRisk" @click="openRiskDialog('PROCESSING')">标记处理中</button>
            <button class="success" :disabled="!canHandleRisk" @click="openRiskDialog('RECTIFIED')">标记已整改</button>
          </div>
        </div>

        <div class="risk-summary-grid">
          <div class="summary-card slim">
            <span class="card-label">当前风险状态</span>
            <p class="card-title">{{ riskStageText }}</p>
            <p class="card-copy">{{ detail.risk?.title || '当前无风险标题' }}</p>
          </div>
          <div class="summary-card slim">
            <span class="card-label">最近风险动作</span>
            <p class="card-title">{{ latestRiskActionLabel }}</p>
            <p class="card-copy">{{ latestRiskAction?.createdAt || latestRiskAction?.operatedAt || '暂无动作时间' }}</p>
          </div>
          <div class="summary-card slim">
            <span class="card-label">整改结果</span>
            <p class="card-title">{{ riskResolutionText }}</p>
            <p class="card-copy">{{ detail.riskHandling?.canResume ? '已满足恢复发布条件' : '当前还不能恢复发布' }}</p>
          </div>
        </div>

        <div class="risk-grid">
          <div>
            <p class="panel-copy">{{ riskPanelCopy }}</p>
            <div v-if="riskChecklist.length && !riskPanelCalm" class="check-grid" data-testid="workbench-risk-checklist">
              <article v-for="item in riskChecklist" :key="item.label" class="check-card">
                <strong>{{ item.label }}</strong>
                <span :class="{ done: item.done }">{{ item.done ? '已完成' : '待补齐' }}</span>
                <small>{{ item.hint }}</small>
              </article>
            </div>
          </div>
          <div>
            <div v-if="detail.riskHandling.history.length" class="history-list">
              <article v-for="item in detail.riskHandling.history" :key="item.id" class="history-card">
                <strong>{{ item.actionLabel || item.actionType }}</strong>
                <p v-if="item.reason">{{ item.reason }}</p>
                <p v-if="item.comment">{{ item.comment }}</p>
                <small>{{ item.operatorName }} · {{ item.createdAt || item.operatedAt }}</small>
              </article>
            </div>
            <p v-else class="empty-copy">当前还没有风险处理记录。</p>
          </div>
        </div>
      </section>

      <section class="panel" data-testid="workbench-status-history">
        <div class="section-head">
          <div>
            <h2>状态流转</h2>
            <p>只保留关键状态变化。</p>
          </div>
        </div>
        <ul class="timeline-list">
          <li v-for="item in detail.statusHistory" :key="`${item.status}-${item.operatedAt}`">
            <span class="timeline-dot" :class="statusClass(item.status)" />
            <div>
              <strong>{{ item.statusLabel || item.status }}</strong>
              <p>{{ historyReasonText(item) }}</p>
              <small>{{ item.operatorName }} · {{ item.operatedAt }}</small>
            </div>
          </li>
        </ul>
      </section>

      <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
        <section class="dialog-card">
          <div class="section-head dialog-head">
            <div>
              <h2 v-if="dialog.type === 'trace'">补录追溯</h2>
              <h2 v-else-if="dialog.type === 'quality'">上传质检</h2>
              <h2 v-else-if="dialog.type === 'risk'">风险处理</h2>
              <h2 v-else-if="dialog.type === 'status'">状态处理</h2>
              <p>{{ detail.batch.batchCode }} · {{ detail.product.name }}</p>
            </div>
            <button class="ghost" :disabled="dialogSubmitting" @click="closeDialog">关闭</button>
          </div>
          <div v-if="dialog.type === 'trace'" class="form-grid" data-testid="workbench-trace-dialog">
            <label>
              <span>环节</span>
              <select v-model="traceForm.stage">
                <option v-for="item in stageOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label><span>时间</span><input v-model="traceForm.eventTime" type="datetime-local"></label>
            <label><span>操作人</span><input v-model.trim="traceForm.operatorName" type="text"></label>
            <label class="full-width"><span>标题</span><input v-model.trim="traceForm.title" type="text"></label>
            <label><span>地点</span><input v-model.trim="traceForm.location" type="text"></label>
            <label class="full-width"><span>说明</span><textarea v-model.trim="traceForm.summary" rows="4"></textarea></label>
            <label class="full-width">
              <span>现场图片</span>
              <div class="upload-box">
                <input type="file" accept="image/*" multiple @change="handleTraceFilesChange">
                <small>支持上传多张图片，保存记录后会自动绑定到本条追溯记录。</small>
              </div>
            </label>
            <div v-if="traceUploading" class="full-width upload-hint">正在上传现场图片...</div>
            <div v-if="traceForm.uploadedFiles.length" class="full-width uploaded-file-list">
              <article v-for="item in traceForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
                <div><strong>{{ fileLabel(item) }}</strong><small>{{ formatFileSize(item.size) }}</small></div>
                <div class="inline-actions">
                  <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看</a>
                  <button class="ghost" @click="removeTraceAttachment(item.id)">移除</button>
                </div>
              </article>
            </div>
            <label class="checkbox-field full-width"><input v-model="traceForm.visibleToConsumer" type="checkbox"><span>同步展示给消费者</span></label>
          </div>

          <div v-else-if="dialog.type === 'quality'" class="form-grid" data-testid="workbench-quality-dialog">
            <label><span>报告编号</span><input v-model.trim="qualityForm.reportNo" type="text"></label>
            <label><span>检测机构</span><input v-model.trim="qualityForm.agency" type="text"></label>
            <label>
              <span>检测结果</span>
              <select v-model="qualityForm.result">
                <option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label><span>检测时间</span><input v-model="qualityForm.reportTime" type="datetime-local"></label>
            <label class="full-width"><span>质检摘要</span><textarea v-model.trim="qualityForm.highlightsText" rows="4"></textarea></label>
            <label class="full-width">
              <span>质检附件</span>
              <div class="upload-box">
                <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
                <small>支持上传 PDF 或图片，后台会保留附件备查。</small>
              </div>
            </label>
            <div v-if="qualityUploading" class="full-width upload-hint">正在上传质检附件...</div>
            <div v-if="qualityForm.uploadedFiles.length" class="full-width uploaded-file-list">
              <article v-for="item in qualityForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
                <div><strong>{{ fileLabel(item) }}</strong><small>{{ formatFileSize(item.size) }}</small></div>
                <div class="inline-actions">
                  <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看</a>
                  <button class="ghost" @click="removeQualityAttachment(item.id)">移除</button>
                </div>
              </article>
            </div>
          </div>

          <div v-else-if="dialog.type === 'risk'" class="form-grid" data-testid="workbench-risk-dialog">
            <label>
              <span>处理类型</span>
              <select v-model="riskForm.actionType">
                <option v-for="item in riskActionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label><span>处理人</span><input v-model.trim="riskForm.operatorName" type="text"></label>
            <label class="full-width"><span>处理原因</span><textarea v-model.trim="riskForm.reason" rows="3"></textarea></label>
            <label class="full-width"><span>处理说明</span><textarea v-model.trim="riskForm.comment" rows="4"></textarea></label>
          </div>

          <div v-else-if="dialog.type === 'status'" class="form-grid" data-testid="workbench-status-dialog">
            <label>
              <span>目标状态</span>
              <select v-model="statusForm.targetStatus">
                <option
                  v-for="item in statusTargetOptions"
                  :key="item.value"
                  :value="item.value"
                  :disabled="!item.allowed"
                >
                  {{ item.allowed ? item.label : `${item.label}（当前不可用）` }}
                </option>
              </select>
              <small v-if="statusForm.targetStatus === 'PUBLISHED' && !publishGate.allowed" class="field-inline-hint">
                {{ publishGate.reason }}
              </small>
              <small v-if="!canBatchStatusTransition(detail?.status?.code, statusForm.targetStatus)" class="field-inline-hint">
                {{ statusTransitionBlockedHint(detail?.status?.code, statusForm.targetStatus) }}
              </small>
            </label>
            <label><span>处理人</span><input v-model.trim="statusForm.operatorName" type="text"></label>
            <label class="full-width"><span>处理原因</span><textarea v-model.trim="statusForm.reason" rows="4"></textarea></label>
          </div>

          <p v-if="dialogValidationError" class="form-error">{{ dialogValidationError }}</p>

          <div class="dialog-actions">
            <button class="ghost" :disabled="dialogSubmitting" @click="closeDialog">取消</button>
            <button
              v-if="dialog.type === 'trace' && !readOnlyBatchView"
              class="ghost"
              data-testid="workbench-dialog-save-continue"
              :disabled="dialogSubmitting || Boolean(dialogValidationError)"
              @click="submitDialog({ keepOpen: true })"
            >
              {{ dialogSubmitting ? '正在保存...' : '保存并继续' }}
            </button>
            <button
              v-if="!readOnlyBatchView"
              class="primary"
              data-testid="workbench-dialog-submit"
              :disabled="dialogSubmitting || Boolean(dialogValidationError)"
              @click="submitDialog()"
            >
              {{ dialogSubmitting ? '正在提交...' : '确认保存' }}
            </button>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-shell { max-width: 1240px; margin: 0 auto; padding: 24px 20px 44px; }
.workbench-meta { margin: 8px 0 0; color: var(--admin-text-soft); font-size: 14px; }
.message-bar,.summary-card,.action-panel,.panel,.dialog-card,.loading-card { border: 1px solid var(--admin-border); border-radius: 20px; background: var(--admin-surface); box-shadow: var(--admin-shadow); }
.message-bar,.action-panel,.panel,.loading-card { margin-top: 18px; padding: 22px; }
.message-bar.success { background: var(--admin-success-bg); color: var(--admin-success-text); }
.message-bar.error { background: rgba(253,236,235,.94); color: #8f2f29; }
.fresh-batch-banner { margin-top: 18px; padding: 20px 22px; border: 1px solid rgba(48,149,246,.18); border-radius: 20px; background: linear-gradient(135deg, rgba(48,149,246,.12) 0%, rgba(255,255,255,.96) 100%); box-shadow: var(--admin-shadow); display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.fresh-batch-banner h2 { margin: 8px 0 6px; color: var(--admin-text); font-size: 22px; }
.fresh-copy-source { display: inline-block; margin-bottom: 8px; color: #2d5f95; font-weight: 600; }
.fresh-batch-banner p { margin: 0; color: var(--admin-text-soft); line-height: 1.7; }
.banner-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.loading-card { display: flex; align-items: center; justify-content: center; min-height: 220px; color: var(--admin-text-soft); }
.top-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-top: 18px; }
.summary-card { padding: 20px; }
.summary-card--featured {
  background: linear-gradient(145deg, #f7fbff 0%, #eef6ff 100%);
  border-color: rgba(48, 149, 246, 0.22);
}
.card-label { display: block; color: var(--admin-text-soft); font-size: 12px; letter-spacing: .08em; text-transform: uppercase; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; }
.card-head small,.summary-card small { color: var(--admin-text-soft); font-size: 12px; }
.card-title { margin: 14px 0 8px; color: var(--admin-text); font-size: 18px; font-weight: 700; }
.card-copy,.panel-copy,.action-card p,.history-card p { margin: 0; color: #4a6b90; line-height: 1.7; }
.summary-card-headline { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-top: 12px; }
.featured-meta { margin-top: 16px; }
.featured-actions { margin-top: 18px; }
.demo-flow { margin-top: 16px; }
.demo-flow small { display: block; color: var(--admin-text-soft); font-size: 12px; }
.demo-flow-list { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; }
.demo-flow-list span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(48, 149, 246, 0.1);
  color: var(--admin-primary-deep);
  font-size: 12px;
  font-weight: 700;
}
.demo-flow-list span.done {
  background: rgba(46, 166, 106, 0.12);
  color: #1e7d50;
}
.readiness-strip { margin-top: 14px; }
.task-draft-copy { margin: 10px 0 0; color: #a35f17; font-size: 13px; font-weight: 600; }
.empty-copy { margin: 14px 0 0; color: var(--admin-text-soft); line-height: 1.7; }
.mini-list { margin: 12px 0 0; padding: 0; list-style: none; display: grid; gap: 10px; }
.mini-list li { padding: 12px 14px; border-radius: 14px; background: var(--admin-surface-soft); }
.mini-list strong { display: block; color: var(--admin-text); }
.action-panel .section-head,.panel .section-head,.dialog-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.section-head h2 { margin: 0; color: var(--admin-text); font-size: 18px; }
.section-head p { margin: 6px 0 0; color: var(--admin-text-soft); font-size: 13px; line-height: 1.7; }
.action-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-top: 16px; }
.action-card,.record-card,.report-card,.history-card,.check-card,.uploaded-file-item { padding: 16px; border: 1px solid rgba(56,134,217,.1); border-radius: 16px; background: var(--admin-surface-soft); }
.action-card h3 { margin: 10px 0 8px; color: var(--admin-text); font-size: 18px; }
.action-buttons,.inline-actions,.dialog-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.action-buttons { margin-top: 16px; }
.action-primary-stack { display: grid; gap: 16px; }
.recommended-action-card {
  padding: 20px;
  border: 1px solid rgba(48, 149, 246, 0.2);
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(48, 149, 246, 0.12), rgba(255, 255, 255, 0.98));
}
.assignment-panel { margin-top: 18px; }
.assignment-grid { display: grid; grid-template-columns: 1.2fr .9fr; gap: 16px; margin-top: 16px; }
.assignment-overview,
.assignment-actions,
.assignment-readonly {
  padding: 18px;
  border: 1px solid rgba(56,134,217,.1);
  border-radius: 16px;
  background: var(--admin-surface-soft);
}
.assignment-info-grid { margin-top: 0; }
.assignment-note { margin-top: 14px; }
.assignment-helper { margin: 10px 0 0; color: var(--admin-text-soft); line-height: 1.7; }
.assignment-buttons { margin-top: 14px; }
.assignment-warning {
  margin-top: 14px;
  padding: 16px;
  border: 1px solid rgba(240, 139, 51, 0.24);
  border-radius: 16px;
  background: rgba(255, 245, 232, 0.96);
}
.assignment-warning strong { display: block; color: #9a6512; }
.assignment-warning p { margin: 8px 0 0; color: #7a5a35; line-height: 1.7; }
.content-grid,.risk-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; margin-top: 18px; }
.side-stack,.record-list,.history-list,.timeline-list,.uploaded-file-list { display: grid; gap: 12px; }
.record-head { display: flex; justify-content: space-between; gap: 12px; }
.record-head strong,.report-card strong,.history-card strong,.check-card strong,.timeline-list strong { display: block; color: var(--admin-text); }
.record-head span,.report-card span,.check-card small,.history-card small,.timeline-list small,.record-card small { color: var(--admin-text-soft); font-size: 13px; }
.record-tag { display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px; border-radius: 999px; background: rgba(48,149,246,.12); color: var(--admin-primary-deep) !important; }
.record-card p { margin: 10px 0 8px; color: #4a6b90; }
.record-image-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 10px; margin-top: 12px; }
.record-image-frame { overflow: hidden; border-radius: 14px; background: rgba(255,255,255,.88); }
.record-image { width: 100%; height: 148px; display: block; object-fit: cover; }
.info-grid,.form-grid,.check-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.info-grid { margin-top: 14px; }
.info-grid div { padding: 14px; border-radius: 14px; background: var(--admin-surface-soft); }
.info-grid span,label span { display: block; margin-bottom: 8px; color: var(--admin-text-soft); font-size: 13px; }
.info-grid strong { display: block; color: var(--admin-text); }
.public-link { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; margin-top: 14px; padding: 0 16px; border-radius: 999px; background: var(--admin-primary-soft); color: var(--admin-primary-deep); }
.check-grid { margin-top: 16px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.check-card span { display: block; margin-top: 8px; color: #9a6512; font-size: 12px; font-weight: 700; }
.check-card span.done { color: var(--admin-success-text); }
.timeline-list { list-style: none; padding: 0; margin: 18px 0 0; }
.timeline-list li { display: grid; grid-template-columns: 18px 1fr; gap: 12px; }
.timeline-dot { width: 14px; height: 14px; margin-top: 6px; border-radius: 999px; }
.status-badge { display: inline-flex; align-items: center; justify-content: center; min-height: 34px; padding: 0 14px; border-radius: 999px; font-weight: 700; }
.status-badge.draft { background: rgba(248, 193, 73, 0.16); color: #946200; }
.status-badge.published { background: rgba(33, 170, 110, 0.14); color: #17784f; }
.status-badge.frozen { background: rgba(255, 132, 59, 0.16); color: #a54d12; }
.status-badge.recalled { background: rgba(224, 73, 73, 0.14); color: #a33030; }
.timeline-dot.draft { background: #f1c85a; }
.timeline-dot.published { background: #21aa6e; }
.timeline-dot.frozen { background: #ff843b; }
.timeline-dot.recalled { background: #e04949; }
.dialog-mask { position: fixed; inset: 0; z-index: 60; display: flex; align-items: center; justify-content: center; padding: 28px 18px; background: rgba(14, 28, 52, 0.38); }
.dialog-card { width: min(760px, 100%); padding: 24px; }
.dialog-card h2 { margin: 0; color: var(--admin-text); font-size: 20px; }
.dialog-card p { margin: 8px 0 0; color: var(--admin-text-soft); line-height: 1.7; }
.dialog-actions { justify-content: flex-end; margin-top: 18px; }
label { display: block; }
input,
select,
textarea {
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 14px;
  background: #fff;
  color: var(--admin-text);
}
textarea { min-height: 120px; padding: 14px; resize: vertical; }
button,
.public-link,
.preview-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease, color 0.16s ease;
}
button:hover,
.public-link:hover,
.preview-link:hover { transform: translateY(-1px); }
button:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }
button.primary { background: var(--admin-primary); color: #fff; box-shadow: 0 10px 24px rgba(56, 134, 217, 0.18); }
button.success { background: #1f9f66; color: #fff; box-shadow: 0 10px 24px rgba(31, 159, 102, 0.18); }
button.warning { background: #f08b33; color: #fff; box-shadow: 0 10px 24px rgba(240, 139, 51, 0.2); }
button.danger { border-color: rgba(224, 73, 73, 0.2); color: #a33030; }
button.ghost,
.preview-link { border-color: rgba(56, 134, 217, 0.18); background: #fff; color: var(--admin-primary-deep); }
button.ghost.danger { border-color: rgba(224, 73, 73, 0.2); color: #a33030; }
.upload-box {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px dashed rgba(56, 134, 217, 0.24);
  border-radius: 14px;
  background: rgba(245, 250, 255, 0.9);
}
.upload-box small,
.upload-hint { color: var(--admin-text-soft); }
.uploaded-file-item { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.checkbox-field {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 12px 14px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 14px;
  background: #fff;
}
.checkbox-field input { width: 18px; min-height: 18px; margin: 0; }
.field-inline-hint {
  display: block;
  margin-top: 8px;
  color: #b96b16;
  font-size: 12px;
  line-height: 1.6;
}
.form-error {
  margin: 12px 0 0;
  color: #b63f3f;
  line-height: 1.7;
}
.full-width { grid-column: 1 / -1; }
.top-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); }
.summary-card.slim { padding: 18px; }
.summary-meta-list,
.compact-grid,
.publish-checklist,
.risk-summary-grid { display: grid; gap: 12px; }
.risk-panel.calm { border-color: rgba(56, 134, 217, 0.08); background: rgba(250, 252, 255, 0.96); }
.summary-meta-list { margin-top: 14px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.summary-meta-list.compact { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.summary-meta-list div,
.compact-grid div,
.risk-summary-grid .summary-card { border: 1px solid rgba(56, 134, 217, 0.1); background: var(--admin-surface-soft); }
.risk-panel.calm .risk-summary-grid .summary-card { border-color: rgba(56, 134, 217, 0.06); background: rgba(248, 250, 253, 0.96); }
.summary-meta-list div,
.compact-grid div { padding: 12px 14px; border-radius: 14px; }
.summary-meta-list span,
.compact-grid span { display: block; margin-bottom: 8px; color: var(--admin-text-soft); font-size: 13px; }
.summary-meta-list strong,
.compact-grid strong { display: block; color: var(--admin-text); }
.summary-record-head,
.action-card-head,
.release-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.summary-image-strip { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-top: 14px; }
.summary-image-frame { overflow: hidden; border-radius: 14px; background: rgba(255, 255, 255, 0.88); }
.summary-image { width: 100%; height: 96px; display: block; object-fit: cover; }
.action-hub { display: grid; grid-template-columns: 1.55fr 1fr; gap: 16px; margin-top: 16px; }
.action-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.action-card.unavailable { background: linear-gradient(180deg, rgba(245, 248, 252, 0.96), rgba(239, 244, 250, 0.96)); }
.release-readonly-card { padding: 18px; border: 1px solid rgba(56, 134, 217, 0.1); border-radius: 16px; background: var(--admin-surface-soft); }
.quality-highlight-list { margin-top: 16px; }
.quality-attachment-links { margin-top: 16px; }
.availability-badge { display: inline-flex; align-items: center; justify-content: center; min-height: 30px; padding: 0 12px; border-radius: 999px; background: rgba(224, 232, 243, 0.9); color: #5b7190; font-size: 12px; font-weight: 700; }
.availability-badge.ok { background: rgba(33, 170, 110, 0.14); color: #17784f; }
.availability-badge.blocked { background: rgba(248, 193, 73, 0.18); color: #946200; }
.action-status { display: block; margin: 0 0 10px; color: var(--admin-text); font-size: 15px; }
.action-reason { margin-top: 10px; color: #7a5a35; font-size: 13px; }
.release-panel { margin-top: 0; }
.release-summary-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 16px; }
.release-summary-card,
.publish-check-card,
.latest-record-card { padding: 16px; border: 1px solid rgba(56,134,217,.1); border-radius: 16px; background: var(--admin-surface-soft); }
.release-card-head strong { display: block; margin-top: 8px; color: var(--admin-text); font-size: 17px; }
.publish-checklist { margin-top: 16px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.publish-check-card span { display: inline-flex; margin-top: 8px; color: #9a6512; font-size: 12px; font-weight: 700; }
.publish-check-card span.done { color: var(--admin-success-text); }
.publish-check-card small { display: block; margin-top: 8px; color: var(--admin-text-soft); font-size: 13px; }
.release-actions { margin-top: 16px; align-items: center; }
.release-tip { color: var(--admin-text-soft); line-height: 1.7; }
.content-grid { grid-template-columns: 1.45fr 1fr; }
.record-meta-row { display: flex; flex-wrap: wrap; gap: 16px; margin: 10px 0; }
.compact-record-list { margin-top: 16px; }
.record-image-grid.compact-images { grid-template-columns: repeat(auto-fit, minmax(96px, 1fr)); }
.assignment-panel { margin-top: 0; }
.assignment-grid { grid-template-columns: 1.15fr .95fr; }
.compact-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.risk-summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); margin-top: 16px; }
@media (max-width: 1160px) {
  .top-grid,
  .action-grid,
  .summary-meta-list,
  .publish-checklist,
  .risk-summary-grid,
  .release-summary-grid,
  .info-grid,
  .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .action-hub,
  .assignment-grid,
  .content-grid,
  .risk-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .page-shell { padding: 18px 14px 36px; }
  .fresh-batch-banner,
  .top-grid,
  .action-grid,
  .summary-meta-list,
  .summary-meta-list.compact,
  .release-summary-grid,
  .publish-checklist,
  .risk-summary-grid,
  .compact-grid,
  .info-grid,
  .form-grid,
  .check-grid,
  .summary-image-strip { grid-template-columns: 1fr; }
  .action-panel .section-head,
  .panel .section-head,
  .fresh-batch-banner,
  .summary-card-headline,
  .record-head,
  .card-head,
  .summary-record-head,
  .action-card-head,
  .release-card-head,
  .dialog-head,
  .uploaded-file-item { flex-direction: column; align-items: flex-start; }
  .record-meta-row { flex-direction: column; gap: 8px; }
  .dialog-mask { padding: 16px; }
  .dialog-card { padding: 18px; }
  .dialog-actions { width: 100%; }
  .dialog-actions button { flex: 1; }
}
</style>
