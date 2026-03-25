<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  changeBatchStatus,
  cleanupBatchFiles,
  createQualityReport,
  createRiskAction,
  createTraceRecord,
  generateBatchQr,
  getBatchDetail,
  getCompanyOptions,
  getProductOptions,
  updateBatch,
  uploadBatchFiles
} from '../api/batch'
import {
  createQualityForm as createQualityDraft,
  createTraceForm as createTraceDraft,
  currentDateTime,
  getFriendlyErrorMessage,
  getFriendlyUploadError,
  getTraceStageProfile
} from '../utils/batchExperience'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref(null)
const message = ref('')
const messageType = ref('info')

const dialog = ref({
  visible: false,
  type: ''
})

const batchForm = ref(createBatchForm())
const traceForm = ref(createTraceDraft())
const qualityForm = ref(createQualityDraft())
const riskForm = ref(createRiskForm())
const statusForm = ref(createStatusForm())

const formCompanyOptions = ref([])
const formProductOptions = ref([])
const traceUploading = ref(false)
const qualityUploading = ref(false)
const lastTraceStage = ref('PRODUCE')
const guideDismissed = ref(false)

const stageOptions = [
  { value: 'ARCHIVE', label: '建档' },
  { value: 'PRODUCE', label: '生产' },
  { value: 'QUALITY', label: '质检' },
  { value: 'TRANSPORT', label: '运输' },
  { value: 'WAREHOUSE', label: '仓储' },
  { value: 'DELIVERY', label: '发运' },
  { value: 'MARKET', label: '上市' },
  { value: 'REGULATION', label: '监管' }
]

const qualityOptions = [
  { value: 'PASS', label: '合格' },
  { value: 'FAIL', label: '不合格' },
  { value: 'REVIEW', label: '待复核' }
]

const riskActionOptions = [
  { value: 'COMMENT', label: '补处理说明' },
  { value: 'RECTIFICATION', label: '补整改记录' },
  { value: 'PROCESSING', label: '标记处理中' },
  { value: 'RECTIFIED', label: '标记已整改' }
]

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'edit':
      return '编辑批次'
    case 'trace':
      return '快速补录追溯'
    case 'quality':
      return '上传质检'
    case 'status':
      return '处理批次状态'
    case 'risk':
      return '补充风险处理'
    default:
      return ''
  }
})

const canPreviewPublic = computed(() => detail.value?.qr?.generated && detail.value?.qr?.publicUrl)

const selectedProductOption = computed(() => {
  return formProductOptions.value.find((item) => item.id === batchForm.value.productId) ?? null
})

const traceStageProfile = computed(() => getTraceStageProfile(traceForm.value.stage))

const latestTraceRecord = computed(() => detail.value?.trace?.recentRecords?.[0] ?? null)

const createdGuideVisible = computed(() => route.query.created === '1' && !guideDismissed.value)

const readinessSteps = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    { key: 'trace', label: '补追溯', done: hasTraceRecords(), hint: hasTraceRecords() ? '已有关键追溯记录' : '还没有补录记录' },
    { key: 'quality', label: '上传质检', done: hasQualityReport(), hint: hasQualityReport() ? detail.value.quality.label : '还没有质检摘要' },
    { key: 'qr', label: '生成二维码', done: hasQrReady(), hint: hasQrReady() ? '二维码已就绪' : '还没生成二维码' },
    { key: 'publish', label: '发布批次', done: detail.value.status.code === 'PUBLISHED', hint: detail.value.status.label }
  ]
})

const nextStepCard = computed(() => {
  if (!detail.value) {
    return {
      title: '准备中',
      copy: '正在加载批次工作台。',
      action: '打开工作台',
      code: 'NONE'
    }
  }

  if (detail.value.status.code === 'RECALLED') {
    return {
      title: '先看召回处理进度',
      copy: '这个批次已经召回，演示时先说明异常原因、当前处理阶段和最近处理动作。',
      action: '查看风险区',
      code: 'RISK'
    }
  }

  if (detail.value.status.code === 'FROZEN') {
    if (detail.value.riskHandling.canResume) {
      return {
        title: '可以恢复发布',
        copy: '整改检查项已经满足，确认原因后可直接恢复发布。',
        action: '恢复发布',
        code: 'RESUME'
      }
    }
    return {
      title: '先补风险处理',
      copy: '先补处理说明、整改记录和整改完成标记，再考虑恢复发布。',
      action: '去风险处理',
      code: 'RISK'
    }
  }

  if (!hasTraceRecords()) {
    return {
      title: '先补一条关键追溯',
      copy: '优先录入生产、运输或仓储中的关键节点，让工作台和公开页都能立刻可看。',
      action: '补追溯',
      code: 'TRACE'
    }
  }

  if (!hasQualityReport()) {
    return {
      title: '继续上传质检摘要',
      copy: '质检结论会直接影响公开页首屏可信度，也是发布前的关键前置项。',
      action: '上传质检',
      code: 'QUALITY'
    }
  }

  if (!hasQrReady()) {
    return {
      title: '生成二维码',
      copy: '二维码准备好后，才能顺畅衔接扫码查看与答辩演示。',
      action: '生成二维码',
      code: 'QR'
    }
  }

  if (detail.value.status.code !== 'PUBLISHED') {
    return {
      title: '资料已齐，可以发布',
      copy: '追溯、质检和二维码都已经准备好，现在只差最后一步发布。',
      action: '发布批次',
      code: 'PUBLISH'
    }
  }

  return {
    title: '公开页已可演示',
    copy: '现在适合打开公开页，确认首屏结论、时间线和风险表达是否清楚。',
    action: '预览公开页',
    code: 'PUBLIC'
  }
})

const actionGroups = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    {
      key: 'trace',
      title: '补追溯',
      copy: hasTraceRecords() ? `最近一条：${latestTraceRecord.value?.title || '已补录'}` : '先补第一条关键节点，减少后面来回找入口。',
      actions: [
        { label: '快速补录', onClick: openTraceDialog, variant: 'primary' },
        { label: latestTraceRecord.value ? '复制上一条' : '查看记录', onClick: latestTraceRecord.value ? copyLatestTraceRecord : openTraceDialog, variant: 'ghost' }
      ]
    },
    {
      key: 'quality',
      title: '上传质检',
      copy: hasQualityReport() ? `当前结论：${detail.value.quality.label}` : '补完质检后，工作台和公开页都会更像成品。',
      actions: [
        { label: '上传质检', onClick: openQualityDialog, variant: 'success' }
      ]
    },
    {
      key: 'qr',
      title: '生成二维码',
      copy: hasQrReady() ? '二维码已生成，可直接去公开页核验首屏。' : '先生成二维码，再衔接扫码查看与答辩演示。',
      actions: [
        { label: hasQrReady() ? '查看二维码' : '生成二维码', onClick: handleQrAction, variant: 'ghost' },
        { label: '公开页预览', onClick: openPublicPreview, variant: 'ghost', disabled: !canPreviewPublic.value }
      ]
    },
    {
      key: 'status',
      title: '状态处理',
      copy: detail.value.status.code === 'PUBLISHED' ? '已发布批次可冻结或召回。' : '根据资料完整度或风险状态，决定发布、恢复、冻结或召回。',
      actions: [
        { label: '发布', onClick: () => openStatusDialog('PUBLISHED'), variant: 'success', disabled: !actionOf('PUBLISH').enabled },
        { label: '恢复发布', onClick: () => openStatusDialog('PUBLISHED'), variant: 'success', disabled: !actionOf('RESUME').enabled },
        { label: '冻结', onClick: () => openStatusDialog('FROZEN'), variant: 'warning', disabled: !actionOf('FREEZE').enabled },
        { label: '召回', onClick: () => openStatusDialog('RECALLED'), variant: 'danger', disabled: !actionOf('RECALL').enabled }
      ]
    }
  ]
})

const riskLatestAction = computed(() => detail.value?.riskHandling?.history?.[0] ?? null)

const riskChecklist = computed(() => {
  if (!detail.value || !['FROZEN', 'RECALLED'].includes(detail.value.status.code)) {
    return []
  }
  const history = detail.value.riskHandling.history || []
  const hasComment = history.some((item) => item.actionType === 'COMMENT')
  const hasRectification = history.some((item) => item.actionType === 'RECTIFICATION')
  const hasProcessing = history.some((item) => item.actionType === 'PROCESSING')
  const hasRectified = history.some((item) => item.actionType === 'RECTIFIED')
  return [
    { label: '处理说明', done: hasComment, copy: hasComment ? '已补充说明' : '建议先写明异常原因与当前判断' },
    { label: '整改记录', done: hasRectification, copy: hasRectification ? '已补充整改动作' : '补充已采取的整改动作' },
    { label: '处理中标记', done: hasProcessing, copy: hasProcessing ? '当前阶段清楚可见' : '可补一个“处理中”节点说明进度' },
    { label: '已整改标记', done: hasRectified, copy: hasRectified ? '已记录整改完成' : '恢复发布前需标记整改完成' }
  ]
})

onMounted(async () => {
  await loadCompanyOptions()
  await loadDetail(route.params.id)
})

watch(
  () => route.params.id,
  async (id) => {
    await loadDetail(id)
  }
)

async function loadDetail(id) {
  if (!id) {
    return
  }
  loading.value = true
  try {
    const response = await getBatchDetail(id)
    detail.value = response.data
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '批次工作台加载失败。'), 'error')
  } finally {
    loading.value = false
  }
}

async function loadCompanyOptions(keyword = '') {
  const params = keyword ? { keyword } : {}
  const response = await getCompanyOptions(params)
  formCompanyOptions.value = response.data ?? []
}

async function loadProductOptions(companyId, keyword = '') {
  if (!companyId) {
    formProductOptions.value = []
    return
  }
  const params = { companyId }
  if (keyword) {
    params.keyword = keyword
  }
  const response = await getProductOptions(params)
  formProductOptions.value = response.data ?? []
}

function createBatchForm() {
  return {
    batchCode: '',
    productId: null,
    companyId: null,
    originPlace: '',
    productionDate: '',
    publicRemark: '',
    internalRemark: ''
  }
}

function createTraceForm() {
  return createTraceDraft()
}

function createQualityForm() {
  return createQualityDraft()
}

function createRiskForm(actionType = 'COMMENT') {
  return {
    actionType,
    reason: detail.value?.risk?.reason ?? '',
    comment: '',
    operatorName: 'Admin'
  }
}

function createStatusForm(targetStatus = 'PUBLISHED') {
  return {
    targetStatus,
    reason: defaultReason(targetStatus),
    operatorName: 'Admin'
  }
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function dismissGuide() {
  guideDismissed.value = true
  const query = { ...route.query }
  delete query.created
  delete query.focus
  router.replace({ query })
}

async function openEditDialog() {
  await loadCompanyOptions()
  await loadProductOptions(detail.value.company.id)
  batchForm.value = {
    batchCode: detail.value.batch.batchCode,
    productId: detail.value.product.id,
    companyId: detail.value.company.id,
    originPlace: detail.value.batch.originPlace,
    productionDate: detail.value.batch.productionDate,
    publicRemark: detail.value.batch.publicRemark ?? '',
    internalRemark: detail.value.batch.internalRemark ?? ''
  }
  dialog.value = {
    visible: true,
    type: 'edit'
  }
}

function openTraceDialog() {
  traceForm.value = createTraceForm({
    stage: latestTraceRecord.value?.stageCode ?? 'PRODUCE',
    operatorName: latestTraceRecord.value?.operatorName,
    location: latestTraceRecord.value?.location
  })
  lastTraceStage.value = traceForm.value.stage
  dialog.value = {
    visible: true,
    type: 'trace'
  }
}

function openQualityDialog() {
  qualityForm.value = createQualityForm()
  dialog.value = {
    visible: true,
    type: 'quality'
  }
}

function openStatusDialog(targetStatus) {
  statusForm.value = createStatusForm(targetStatus)
  dialog.value = {
    visible: true,
    type: 'status'
  }
}

function openRiskDialog(actionType) {
  riskForm.value = createRiskForm(actionType)
  dialog.value = {
    visible: true,
    type: 'risk'
  }
}

function closeDialog() {
  dialog.value = {
    visible: false,
    type: ''
  }
}

async function submitDialog(options = {}) {
  const { keepOpen = false } = options
  try {
    let response

    if (dialog.value.type === 'edit') {
      response = await updateBatch(route.params.id, {
        productId: batchForm.value.productId,
        companyId: batchForm.value.companyId,
        originPlace: batchForm.value.originPlace,
        productionDate: batchForm.value.productionDate,
        publicRemark: batchForm.value.publicRemark,
        internalRemark: batchForm.value.internalRemark
      })
    } else if (dialog.value.type === 'trace') {
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
      if (keepOpen) {
        traceForm.value = createTraceForm({
          stage: latestTraceRecord.value?.stageCode ?? traceForm.value.stage,
          operatorName: latestTraceRecord.value?.operatorName ?? traceForm.value.operatorName,
          location: latestTraceRecord.value?.location ?? traceForm.value.location,
          title: latestTraceRecord.value?.title ?? getTraceStageProfile(traceForm.value.stage).defaultTitle,
          summary: getTraceStageProfile(traceForm.value.stage).summaryTemplates[0]
        })
        lastTraceStage.value = traceForm.value.stage
        showMessage('这条记录已保存，可以继续补录下一条。', 'success')
        return
      }
    } else if (dialog.value.type === 'quality') {
      response = await createQualityReport(route.params.id, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlightsInput(qualityForm.value.highlightsText),
        attachmentIds: qualityForm.value.attachmentIds
      })
    } else if (dialog.value.type === 'status') {
      response = await changeBatchStatus(route.params.id, statusForm.value)
    } else if (dialog.value.type === 'risk') {
      response = await createRiskAction(route.params.id, riskForm.value)
    } else {
      return
    }

    detail.value = response.data
    showMessage(response.message || '操作已完成。', 'success')
    closeDialog()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error), 'error')
  }
}

async function handleQrAction() {
  try {
    const response = await generateBatchQr(route.params.id)
    detail.value = response.data
    showMessage('二维码已准备好。', 'success')
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码生成失败，请稍后重试。'), 'error')
  }
}

async function handleAttachmentCleanup() {
  try {
    const response = await cleanupBatchFiles()
    const result = response.data
    showMessage(
      `Cleanup finished. Cleaned ${result.cleanedCount} attachment(s), failed ${result.failedCount}.`,
      'success'
    )
    await loadDetail(route.params.id)
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error), 'error')
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

function actionOf(code) {
  return detail.value?.actions?.find((action) => action.code === code) ?? {
    code,
    label: code,
    enabled: true,
    hint: '',
    variant: 'neutral'
  }
}

function actionClass(code) {
  return actionOf(code).variant || 'neutral'
}

function splitHighlights(text) {
  return text
    .split(/[\n,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function defaultReason(targetStatus) {
  return {
    PUBLISHED: '质检和二维码已准备好，可以恢复对外展示。',
    FROZEN: '发现异常，先冻结批次并进入处理。',
    RECALLED: '风险已确认，需立即召回并保留公开提示。'
  }[targetStatus] ?? ''
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[status] ?? 'draft'
}

function riskToneClass(risk) {
  return {
    normal: 'risk-normal',
    pending: 'risk-pending',
    warning: 'risk-warning',
    danger: 'risk-danger'
  }[risk?.riskLevel] ?? 'risk-normal'
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
  return file.fileName || file.fileUrl || 'Uploaded file'
}

function splitHighlightsInput(text) {
  return text
    .split(/[\n,;，；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function uploadStateLabel(file) {
  return file?.businessId ? '已绑定' : '待绑定'
}

function uploadStateClass(file) {
  return file?.businessId ? 'bound' : 'pending'
}

function canHandleRisk() {
  return ['FROZEN', 'RECALLED'].includes(detail.value?.status?.code)
}

function hasTraceRecords() {
  return (detail.value?.trace?.totalCount ?? 0) > 0
}

function hasQualityReport() {
  return (detail.value?.quality?.reportCount ?? 0) > 0
}

function hasQrReady() {
  return Boolean(detail.value?.qr?.generated)
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
  if (!latestTraceRecord.value) {
    return
  }
  if (dialog.value.type !== 'trace') {
    dialog.value = {
      visible: true,
      type: 'trace'
    }
  }
  traceForm.value = createTraceForm({
    stage: latestTraceRecord.value.stageCode,
    title: latestTraceRecord.value.title,
    operatorName: latestTraceRecord.value.operatorName,
    location: latestTraceRecord.value.location,
    summary: latestTraceRecord.value.summary,
    visibleToConsumer: latestTraceRecord.value.visibleToConsumer
  })
  traceForm.value.eventTime = currentDateTime()
  lastTraceStage.value = traceForm.value.stage
}

function openPublicPreview() {
  if (canPreviewPublic.value) {
    window.open(detail.value.qr.publicUrl, '_blank', 'noopener,noreferrer')
  }
}

function triggerNextStep() {
  switch (nextStepCard.value.code) {
    case 'TRACE':
      openTraceDialog()
      break
    case 'QUALITY':
      openQualityDialog()
      break
    case 'QR':
      handleQrAction()
      break
    case 'PUBLISH':
    case 'RESUME':
      openStatusDialog('PUBLISHED')
      break
    case 'RISK':
      if (canHandleRisk()) {
        openRiskDialog('COMMENT')
      }
      break
    case 'PUBLIC':
      openPublicPreview()
      break
    default:
      break
  }
}

function buttonClass(variant) {
  return variant || 'ghost'
}

function riskStageLabel(status) {
  return {
    FROZEN: '已冻结，等待处理',
    RECALLED: '已召回，暂停销售与使用',
    PROCESSING: '处理中',
    RECTIFIED: '已整改，待确认恢复'
  }[status] ?? '正常'
}

function riskActionLabel(type) {
  return {
    COMMENT: '处理说明',
    RECTIFICATION: '整改记录',
    PROCESSING: '处理中',
    RECTIFIED: '已整改'
  }[type] ?? '处理记录'
}

function statusText(status) {
  return {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    FROZEN: '已冻结',
    RECALLED: '已召回'
  }[status] ?? status
}
</script>

<template>
  <div class="page-shell">
    <div v-if="loading" class="loading-card">Loading batch workbench...</div>

    <template v-else-if="detail">
      <header class="hero-card">
        <div class="hero-main">
          <img class="hero-image" :src="detail.product.imageUrl || detail.batch.coverImageUrl" :alt="detail.product.name">
          <div class="hero-copy">
            <p class="eyebrow">批次工作台</p>
            <h1>{{ detail.product.name }}</h1>
            <p class="batch-code">{{ detail.batch.batchCode }}</p>
            <p class="hero-intro">{{ detail.batch.publicRemark || '这里是当前批次的业务中心，适合继续补追溯、传质检、生成二维码和展示风险闭环。' }}</p>

            <div v-if="createdGuideVisible" class="created-guide">
              <div>
                <strong>批次已创建</strong>
                <p>建议按这个顺序继续：补追溯 -> 上传质检 -> 生成二维码 -> 发布批次。</p>
              </div>
              <div class="guide-actions">
                <button class="primary" @click="openTraceDialog">先补追溯</button>
                <button class="ghost" @click="openQualityDialog">上传质检</button>
                <button class="ghost" @click="handleQrAction">生成二维码</button>
                <button class="ghost" @click="dismissGuide">收起引导</button>
              </div>
            </div>

            <div class="hero-grid">
              <div>
                <span>企业</span>
                <strong>{{ detail.company.name }}</strong>
              </div>
              <div>
                <span>产地</span>
                <strong>{{ detail.batch.originPlace }}</strong>
              </div>
              <div>
                <span>生产日期</span>
                <strong>{{ detail.batch.productionDate }}</strong>
              </div>
              <div>
                <span>当前状态</span>
                <strong>{{ detail.status.label }}</strong>
              </div>
              <div>
                <span>质检结论</span>
                <strong>{{ detail.quality.label }}</strong>
              </div>
              <div>
                <span>当前节点</span>
                <strong>{{ detail.status.currentNode }}</strong>
              </div>
            </div>
          </div>
        </div>

        <aside class="hero-side">
          <span class="status-badge" :class="statusClass(detail.status.code)">{{ detail.status.label }}</span>
          <div class="task-card">
            <p class="task-label">下一步推荐</p>
            <h3>{{ nextStepCard.title }}</h3>
            <p>{{ nextStepCard.copy }}</p>
            <button class="primary" @click="triggerNextStep">{{ nextStepCard.action }}</button>
          </div>
          <div class="task-checklist">
            <div v-for="item in readinessSteps" :key="item.key" class="task-check">
              <strong>{{ item.label }}</strong>
              <span :class="{ done: item.done }">{{ item.done ? '已完成' : '待处理' }}</span>
              <small>{{ item.hint }}</small>
            </div>
          </div>
        </aside>
      </header>

      <section v-if="message" class="message-bar" :class="messageType">
        {{ message }}
      </section>

      <section
        v-if="detail.risk && detail.risk.status !== 'NORMAL'"
        class="risk-panel"
        :class="riskToneClass(detail.risk)"
      >
        <div>
          <p class="eyebrow">风险状态</p>
          <h2>{{ detail.risk.title }}</h2>
          <p class="risk-copy">{{ detail.risk.reason }}</p>
          <div v-if="riskLatestAction" class="latest-risk-action">
            <strong>最近处理动作：{{ riskActionLabel(riskLatestAction.actionType) }}</strong>
            <p>{{ riskLatestAction.reason || riskLatestAction.comment || '已记录最新处理动作。' }}</p>
          </div>
        </div>
        <div class="risk-meta">
          <p><strong>当前阶段：</strong> {{ riskStageLabel(detail.risk.status) }}</p>
          <p><strong>最近更新时间：</strong> {{ detail.risk.updatedAt || detail.status.changedAt || '暂无' }}</p>
          <p><strong>提醒：</strong> {{ detail.risk.tip }}</p>
        </div>
      </section>

      <section class="quick-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">快捷操作</p>
            <h2>按场景分组，不再把动作堆成一排</h2>
          </div>
          <button class="ghost" @click="router.push('/batches')">返回批次列表</button>
        </div>

        <div class="action-groups">
          <article v-for="group in actionGroups" :key="group.key" class="action-group-card">
            <p class="eyebrow">{{ group.title }}</p>
            <h3>{{ group.title }}</h3>
            <p>{{ group.copy }}</p>
            <div class="quick-actions">
              <button
                v-for="action in group.actions"
                :key="action.label"
                :class="buttonClass(action.variant)"
                :disabled="action.disabled"
                @click="action.onClick"
              >
                {{ action.label }}
              </button>
            </div>
          </article>
        </div>

        <div class="support-actions">
          <button class="ghost" @click="openEditDialog">编辑批次资料</button>
          <button class="ghost" @click="handleAttachmentCleanup">清理未绑定附件</button>
        </div>
      </section>

      <section class="grid">
        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">补追溯</p>
              <h2>快速补录，不再像填后台表单</h2>
            </div>
            <div class="inline-actions">
              <button class="ghost" @click="openTraceDialog">快速补录</button>
              <button v-if="latestTraceRecord" class="ghost" @click="copyLatestTraceRecord">复制上一条</button>
            </div>
          </div>

          <div class="trace-summary">
            <div>
              <span>记录总数</span>
              <strong>{{ detail.trace.totalCount }}</strong>
            </div>
            <div>
              <span>最近更新</span>
              <strong>{{ detail.trace.latestRecordedAt || '还没有记录' }}</strong>
            </div>
            <div>
              <span>补录建议</span>
              <strong>{{ detail.trace.quickEntryHint }}</strong>
            </div>
          </div>

          <div v-if="latestTraceRecord" class="trace-reuse-card">
            <div>
              <span>上一条记录</span>
              <strong>{{ latestTraceRecord.title }}</strong>
              <p>{{ latestTraceRecord.location }} · {{ latestTraceRecord.eventTime }}</p>
            </div>
            <button class="ghost" @click="copyLatestTraceRecord">沿用上一条并微调</button>
          </div>

          <div class="trace-list">
            <article v-for="item in detail.trace.recentRecords" :key="item.id" class="trace-card">
              <div class="trace-head">
                <div>
                  <p>{{ item.stageName }}</p>
                  <h3>{{ item.title }}</h3>
                </div>
                <span>{{ item.eventTime }}</span>
              </div>
              <p class="trace-meta">{{ item.location }} | {{ item.operatorName }}</p>
              <p class="trace-summary-text">{{ item.summary }}</p>
              <img v-if="item.imageUrl" class="trace-image" :src="item.imageUrl" :alt="item.title">
              <div v-if="item.attachments?.length" class="attachment-list">
                <a
                  v-for="attachment in item.attachments"
                  :key="attachment.id"
                  class="preview-link"
                  :href="attachment.fileUrl"
                  target="_blank"
                  rel="noreferrer"
                >
                  {{ fileLabel(attachment) }} · {{ uploadStateLabel(attachment) }}
                </a>
              </div>
              <small>{{ item.visibleToConsumer ? '公开页可见' : '仅后台可见' }}</small>
            </article>
          </div>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">质检</p>
              <h2>只保留高价值质检信息</h2>
            </div>
            <button class="ghost" @click="openQualityDialog">上传新质检</button>
          </div>

          <div class="quality-hero">
            <span class="quality-chip">{{ detail.quality.label }}</span>
            <p>{{ detail.quality.summary }}</p>
          </div>

          <article v-if="detail.quality.latestReport" class="quality-card">
            <div class="quality-top">
              <div>
                <p>{{ detail.quality.latestReport.resultLabel }}</p>
                <h3>{{ detail.quality.latestReport.reportNo }}</h3>
              </div>
              <span>{{ detail.quality.latestReport.reportTime }}</span>
            </div>
            <p>{{ detail.quality.latestReport.agency }}</p>
            <div class="pill-row">
              <span v-for="item in detail.quality.latestReport.highlights" :key="item">{{ item }}</span>
            </div>
            <div v-if="detail.quality.latestReport.attachments?.length" class="attachment-list">
              <a
                v-for="attachment in detail.quality.latestReport.attachments"
                :key="attachment.id"
                class="preview-link"
                :href="attachment.fileUrl"
                target="_blank"
                rel="noreferrer"
              >
                {{ fileLabel(attachment) }} · {{ uploadStateLabel(attachment) }}
              </a>
            </div>
          </article>

          <ul class="quality-history">
            <li v-for="item in detail.quality.reports" :key="item.id">
              <strong>{{ item.reportNo }}</strong>
              <span>{{ item.resultLabel }}</span>
              <small>{{ item.reportTime }}</small>
            </li>
          </ul>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">二维码</p>
              <h2>工作台和公开页共用同一个二维码入口</h2>
            </div>
            <button
              class="ghost"
              :disabled="!actionOf('GENERATE_QR').enabled"
              @click="handleQrAction"
            >
              {{ hasQrReady() ? '刷新二维码' : '生成二维码' }}
            </button>
          </div>

          <div class="qr-grid">
            <div>
              <span>QR status</span>
              <strong>{{ detail.qr.generated ? 'Generated' : 'Pending' }}</strong>
            </div>
            <div>
              <span>Public token</span>
              <strong>{{ detail.qr.token || 'N/A' }}</strong>
            </div>
            <div>
              <span>Generated at</span>
              <strong>{{ detail.qr.generatedAt || 'N/A' }}</strong>
            </div>
            <div>
              <span>Last scan</span>
              <strong>{{ detail.qr.lastScanAt || 'N/A' }}</strong>
            </div>
            <div>
              <span>PV</span>
              <strong>{{ detail.qr.pv }}</strong>
            </div>
            <div>
              <span>UV</span>
              <strong>{{ detail.qr.uv }}</strong>
            </div>
          </div>

          <div v-if="detail.qr.generated && detail.qr.imageUrl" class="qr-preview">
            <img :src="detail.qr.imageUrl" :alt="`${detail.batch.batchCode} QR`">
            <a class="preview-link" :href="detail.qr.imageUrl" target="_blank" rel="noreferrer" download>
              Download QR
            </a>
          </div>

          <a v-if="canPreviewPublic" class="preview-block" :href="detail.qr.publicUrl" target="_blank" rel="noreferrer">
            打开公开页，确认首屏是否能在 3 秒内看懂当前结论。
          </a>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Scan stats</p>
              <h2>Readable scan summary for the workbench</h2>
            </div>
          </div>

          <div class="scan-grid">
            <div>
              <span>PV</span>
              <strong>{{ detail.scanStats.pv }}</strong>
            </div>
            <div>
              <span>UV</span>
              <strong>{{ detail.scanStats.uv }}</strong>
            </div>
            <div>
              <span>Last scan</span>
              <strong>{{ detail.scanStats.lastScanAt || 'N/A' }}</strong>
            </div>
          </div>

          <div class="scan-section">
            <h3>Recent scan records</h3>
            <ul class="scan-record-list">
              <li v-for="item in detail.scanStats.recentRecords" :key="`${item.scanTime}-${item.ip}-${item.device}`">
                <strong>{{ item.scanTime }}</strong>
                <span>{{ item.device }}</span>
                <small>{{ item.ip }} {{ item.referer ? `| ${item.referer}` : '' }}</small>
              </li>
            </ul>
          </div>

          <div class="scan-section">
            <h3>Last 7 days</h3>
            <div class="trend-table">
              <div class="trend-row trend-head">
                <strong>Day</strong>
                <strong>PV</strong>
                <strong>UV</strong>
              </div>
              <div v-for="item in detail.scanStats.trend" :key="item.day" class="trend-row">
                <span>{{ item.day }}</span>
                <span>{{ item.pv }}</span>
                <span>{{ item.uv }}</span>
              </div>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">风险处理</p>
              <h2>先看异常原因，再看整改进度和恢复条件</h2>
            </div>
          </div>

          <div class="scan-grid">
            <div>
              <span>当前阶段</span>
              <strong>{{ detail.riskHandling.currentStageLabel }}</strong>
            </div>
            <div>
              <span>可恢复发布</span>
              <strong>{{ detail.riskHandling.canResume ? '可以' : '还不行' }}</strong>
            </div>
            <div>
              <span>当前风险</span>
              <strong>{{ detail.risk.title }}</strong>
            </div>
          </div>

          <div class="risk-checklist">
            <article v-for="item in riskChecklist" :key="item.label" class="risk-check-item">
              <strong>{{ item.label }}</strong>
              <span :class="{ done: item.done }">{{ item.done ? '已完成' : '待补齐' }}</span>
              <small>{{ item.copy }}</small>
            </article>
          </div>

          <div class="quick-actions risk-actions">
            <button class="ghost" :disabled="!canHandleRisk()" @click="openRiskDialog('COMMENT')">补处理说明</button>
            <button class="ghost" :disabled="!canHandleRisk()" @click="openRiskDialog('RECTIFICATION')">补整改记录</button>
            <button class="warning" :disabled="!canHandleRisk()" @click="openRiskDialog('PROCESSING')">标记处理中</button>
            <button class="success" :disabled="!canHandleRisk()" @click="openRiskDialog('RECTIFIED')">标记已整改</button>
          </div>

          <ul class="risk-history">
            <li v-if="!detail.riskHandling.history.length" class="risk-history-empty">
              还没有处理记录。建议先补处理说明，再补整改记录，最后标记整改完成。
            </li>
            <li v-for="item in detail.riskHandling.history" :key="item.id">
              <div>
                <strong>{{ riskActionLabel(item.actionType) }}</strong>
                <p v-if="item.reason">{{ item.reason }}</p>
                <p v-if="item.comment">{{ item.comment }}</p>
              </div>
              <small>{{ item.operatorName }} | {{ item.createdAt }}</small>
            </li>
          </ul>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">状态记录</p>
              <h2>把“目前怎样、为什么变成这样”讲清楚</h2>
            </div>
          </div>

          <ol class="status-timeline">
            <li v-for="item in detail.statusHistory" :key="`${item.status}-${item.operatedAt}`">
              <span class="status-dot" :class="statusClass(item.status)" />
              <div>
                <h3>{{ statusText(item.status) }}</h3>
                <p>{{ item.reason }}</p>
                <small>{{ item.operatorName }} | {{ item.operatedAt }}</small>
              </div>
            </li>
          </ol>
        </article>
      </section>
    </template>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">Workbench</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost icon-button" @click="closeDialog">Close</button>
        </div>

        <div v-if="dialog.type === 'edit'" class="form-grid">
          <label>
            <span>Batch code</span>
            <input :value="batchForm.batchCode" type="text" disabled>
          </label>
          <label>
            <span>Company</span>
            <select v-model="batchForm.companyId" @change="handleBatchCompanyChange()">
              <option :value="null">Select company</option>
              <option v-for="item in formCompanyOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span>Product</span>
            <select v-model="batchForm.productId" :disabled="!batchForm.companyId">
              <option :value="null">{{ batchForm.companyId ? 'Select product' : 'Select company first' }}</option>
              <option v-for="item in formProductOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span>Origin</span>
            <input v-model.trim="batchForm.originPlace" type="text">
          </label>
          <label>
            <span>Production date</span>
            <input v-model="batchForm.productionDate" type="date">
          </label>
          <label v-if="selectedProductOption" class="full-width">
            <span>Product summary</span>
            <div class="field-note">
              <strong>{{ selectedProductOption.name }}</strong>
              <small>Category: {{ selectedProductOption.category || 'N/A' }}, Spec: {{ selectedProductOption.specification || 'N/A' }}</small>
            </div>
          </label>
          <label class="full-width">
            <span>Public remark</span>
            <textarea v-model.trim="batchForm.publicRemark" rows="3"></textarea>
          </label>
          <label class="full-width">
            <span>Internal remark</span>
            <textarea v-model.trim="batchForm.internalRemark" rows="3"></textarea>
          </label>
        </div>

        <div v-else-if="dialog.type === 'trace'" class="form-grid">
          <div class="full-width quick-entry-card">
            <div>
              <p class="eyebrow">现场快速补录</p>
              <h4>默认带当前时间，也支持复制上一条</h4>
              <p>阶段、地点和说明模板都可直接点选，保存后还能继续补下一条。</p>
            </div>
            <button v-if="latestTraceRecord" class="ghost" @click="copyLatestTraceRecord">复制上一条并微调</button>
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
            <input v-model.trim="traceForm.operatorName" type="text">
          </label>
          <label>
            <span>地点</span>
            <input v-model.trim="traceForm.location" type="text">
          </label>
          <label class="full-width">
            <span>标题</span>
            <input v-model.trim="traceForm.title" type="text">
          </label>
          <label class="full-width">
            <span>说明</span>
            <textarea v-model.trim="traceForm.summary" rows="4"></textarea>
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
              <small>图片上传成功后会立刻显示在下面，保存记录时一起绑定。</small>
            </div>
          </label>
          <label class="full-width">
            <span>图片链接兜底</span>
            <input v-model.trim="traceForm.imageUrl" type="url" placeholder="可选的外部图片地址">
          </label>
          <div v-if="traceUploading" class="full-width upload-hint">正在上传图片，马上就能在当前记录里看到...</div>
          <div v-if="traceForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in traceForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }} · 已上传，保存记录后生效</small>
              </div>
              <div class="inline-actions">
                <span class="upload-state" :class="uploadStateClass(item)">{{ uploadStateLabel(item) }}</span>
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看</a>
                <button class="ghost" @click="removeTraceAttachment(item.id)">移除</button>
              </div>
            </article>
          </div>
          <label class="checkbox-field full-width">
            <input v-model="traceForm.visibleToConsumer" type="checkbox">
            <span>这条记录同步展示到公开页</span>
          </label>
        </div>

        <div v-else-if="dialog.type === 'quality'" class="form-grid">
          <label>
            <span>Report no</span>
            <input v-model.trim="qualityForm.reportNo" type="text">
          </label>
          <label>
            <span>Agency</span>
            <input v-model.trim="qualityForm.agency" type="text">
          </label>
          <label>
            <span>Result</span>
            <select v-model="qualityForm.result">
              <option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>Report time</span>
            <input v-model="qualityForm.reportTime" type="datetime-local">
          </label>
          <label class="full-width">
            <span>Highlights</span>
            <textarea v-model.trim="qualityForm.highlightsText" rows="4"></textarea>
          </label>
          <label class="full-width">
            <span>Quality attachments</span>
            <div class="upload-box">
              <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
              <small>Upload PDF or image files for the quality report.</small>
            </div>
          </label>
          <div v-if="qualityUploading" class="full-width upload-hint">Uploading quality attachments...</div>
          <div v-if="qualityForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in qualityForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }} · {{ uploadStateLabel(item) }}</small>
              </div>
              <div class="inline-actions">
                <span class="upload-state" :class="uploadStateClass(item)">{{ uploadStateLabel(item) }}</span>
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">Open</a>
                <button class="ghost" @click="removeQualityAttachment(item.id)">Remove</button>
              </div>
            </article>
          </div>
        </div>

        <div v-else-if="dialog.type === 'risk'" class="form-grid">
          <label>
            <span>Action type</span>
            <select v-model="riskForm.actionType">
              <option v-for="item in riskActionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>Operator</span>
            <input v-model.trim="riskForm.operatorName" type="text">
          </label>
          <label class="full-width">
            <span>Reason</span>
            <textarea v-model.trim="riskForm.reason" rows="3" placeholder="Short reason or current stage"></textarea>
          </label>
          <label class="full-width">
            <span>Comment</span>
            <textarea v-model.trim="riskForm.comment" rows="4" placeholder="Handling opinion or rectification record"></textarea>
          </label>
        </div>

        <div v-else-if="dialog.type === 'status'" class="form-grid">
          <label>
            <span>Target status</span>
            <select v-model="statusForm.targetStatus">
              <option value="PUBLISHED">Publish</option>
              <option value="FROZEN">Freeze</option>
              <option value="RECALLED">Recall</option>
            </select>
          </label>
          <label>
            <span>Operator</span>
            <input v-model.trim="statusForm.operatorName" type="text">
          </label>
          <label class="full-width">
            <span>Reason</span>
            <textarea v-model.trim="statusForm.reason" rows="4"></textarea>
          </label>
        </div>

        <div class="dialog-actions">
          <button class="ghost" @click="closeDialog">取消</button>
          <button
            v-if="dialog.type === 'trace'"
            class="ghost"
            @click="submitDialog({ keepOpen: true })"
          >
            保存并继续
          </button>
          <button class="primary" @click="submitDialog()">确认保存</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 28px 20px 44px;
}

.hero-card,
.quick-panel,
.panel,
.dialog-card,
.message-bar,
.loading-card {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(31, 55, 47, 0.08);
}

.hero-card {
  display: grid;
  grid-template-columns: 1.6fr 0.7fr;
  gap: 18px;
  padding: 24px;
}

.hero-main {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 18px;
}

.hero-image {
  width: 240px;
  height: 240px;
  object-fit: cover;
  border-radius: 28px;
  background: linear-gradient(160deg, #eef5ed, #dce9df);
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;
  color: #a67d2a;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 10px;
  font-size: 36px;
  color: #17362d;
}

.batch-code {
  margin-bottom: 10px;
  color: #60786d;
  font-size: 15px;
  letter-spacing: 0.04em;
}

.hero-intro {
  margin-bottom: 18px;
  color: #42594f;
  line-height: 1.8;
}

.hero-grid,
.trace-summary,
.qr-grid,
.form-grid,
.scan-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-grid div,
.trace-summary div,
.qr-grid div,
.scan-grid div {
  padding: 14px;
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.hero-grid span,
.trace-summary span,
.qr-grid span,
.scan-grid span,
label span {
  display: block;
  color: #688176;
  font-size: 12px;
}

.hero-grid strong,
.trace-summary strong,
.qr-grid strong,
.scan-grid strong {
  display: block;
  margin-top: 8px;
  color: #17362d;
  line-height: 1.5;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(242, 204, 105, 0.2), transparent 30%),
    linear-gradient(160deg, #17362d, #24483c 78%, #edf5eb 160%);
  color: #f7f2e7;
}

.hero-side p {
  margin-bottom: 0;
  line-height: 1.7;
}

.created-guide,
.task-card,
.task-check,
.action-group-card,
.trace-reuse-card,
.risk-check-item,
.latest-risk-action,
.template-card {
  border: 1px solid rgba(45, 85, 71, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
}

.created-guide {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px;
}

.created-guide strong,
.task-card h3,
.task-check strong,
.action-group-card h3,
.trace-reuse-card strong,
.risk-check-item strong,
.latest-risk-action strong {
  display: block;
}

.guide-actions,
.support-actions,
.action-groups,
.risk-checklist {
  display: grid;
  gap: 12px;
}

.guide-actions {
  align-content: start;
}

.task-card {
  padding: 16px;
}

.task-label {
  margin-bottom: 8px;
  color: rgba(247, 242, 231, 0.76);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.task-card p {
  color: rgba(247, 242, 231, 0.9);
}

.task-checklist {
  display: grid;
  gap: 10px;
}

.task-check {
  padding: 14px;
}

.task-check span,
.task-check small {
  display: block;
  margin-top: 6px;
  color: rgba(247, 242, 231, 0.82);
}

.task-check span.done {
  color: #f2cc69;
}

.action-groups {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.action-group-card {
  padding: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.action-group-card .eyebrow {
  color: #a67d2a;
}

.action-group-card h3,
.trace-reuse-card strong,
.risk-check-item strong {
  margin-bottom: 8px;
  color: #17362d;
}

.action-group-card p,
.trace-reuse-card p,
.risk-check-item small,
.latest-risk-action p {
  color: #42594f;
  line-height: 1.7;
}

.support-actions {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14px;
}

.trace-reuse-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
  background: rgba(245, 248, 244, 0.96);
}

.latest-risk-action {
  margin-top: 16px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.52);
}

.risk-checklist {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.risk-check-item {
  padding: 16px;
  background: rgba(245, 248, 244, 0.96);
}

.risk-check-item span {
  display: block;
  margin-top: 6px;
  color: #8b4c13;
}

.risk-check-item span.done {
  color: #245846;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.template-card {
  padding: 14px;
  background: rgba(245, 248, 244, 0.96);
}

.template-card span {
  display: block;
  color: #688176;
  font-size: 12px;
}

.chip-button {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

.chip-button.active {
  background: #245846;
  color: #fff7ea;
}

.chip-button.light {
  background: rgba(242, 204, 105, 0.16);
  color: #7b5d13;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  font-weight: 700;
}

.status-badge.draft,
.status-dot.draft {
  background: rgba(141, 162, 152, 0.16);
  color: #5b6f67;
}

.status-badge.published,
.status-dot.published {
  background: rgba(64, 167, 117, 0.18);
  color: #e3f6ea;
}

.status-badge.frozen,
.status-dot.frozen {
  background: rgba(242, 198, 119, 0.22);
  color: #7b5308;
}

.status-badge.recalled,
.status-dot.recalled {
  background: rgba(213, 111, 100, 0.22);
  color: #8f2f29;
}

.quick-panel,
.panel,
.message-bar,
.risk-panel,
.loading-card {
  margin-top: 18px;
  padding: 24px;
}

.message-bar.info {
  color: #245846;
}

.message-bar.success {
  background: rgba(226, 244, 233, 0.94);
  color: #245846;
}

.message-bar.error {
  background: rgba(253, 236, 235, 0.94);
  color: #8f2f29;
}

.risk-panel {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
}

.risk-panel h2 {
  margin-bottom: 8px;
  color: #17362d;
}

.risk-copy,
.risk-meta p {
  color: #42594f;
  line-height: 1.7;
}

.risk-meta {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.48);
}

.risk-warning {
  background: rgba(255, 245, 223, 0.94);
}

.risk-danger {
  background: rgba(253, 236, 235, 0.94);
}

.risk-pending,
.risk-normal {
  background: rgba(245, 248, 244, 0.96);
}

.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #546b61;
}

.section-head,
.dialog-head,
.quality-top,
.trace-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.quick-actions,
.dialog-actions,
.pill-row,
.attachment-list,
.inline-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.trace-list,
.quality-history {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.trace-card,
.quality-card,
.preview-block {
  border-radius: 22px;
  background: rgba(245, 248, 244, 0.96);
}

.trace-card,
.quality-card {
  padding: 18px;
}

.trace-head p,
.quality-top p {
  margin-bottom: 6px;
  color: #688176;
}

.trace-head h3,
.quality-top h3 {
  margin-bottom: 0;
  color: #17362d;
}

.trace-head span,
.quality-top span,
.quality-history small {
  color: #688176;
  font-size: 13px;
}

.trace-meta,
.trace-summary-text,
.quality-card p,
.preview-block {
  color: #42594f;
  line-height: 1.7;
}

.trace-image {
  width: 100%;
  margin-top: 12px;
  border-radius: 18px;
  object-fit: cover;
  max-height: 220px;
}

.quality-hero {
  margin-top: 18px;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(160deg, rgba(36, 88, 70, 0.08), rgba(242, 204, 105, 0.12));
}

.quality-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.14);
  color: #245846;
  font-weight: 700;
}

.pill-row span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
  font-size: 13px;
}

.quality-history {
  list-style: none;
  padding: 0;
  margin-bottom: 0;
}

.quality-history li {
  display: grid;
  grid-template-columns: 1.4fr auto auto;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(36, 88, 70, 0.08);
}

.preview-block {
  display: block;
  margin-top: 16px;
  padding: 18px;
  text-decoration: none;
}

.qr-preview {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 16px;
  margin-top: 16px;
  padding: 16px;
  border-radius: 22px;
  background: rgba(245, 248, 244, 0.96);
}

.qr-preview img {
  width: 180px;
  height: 180px;
  border-radius: 18px;
  background: #ffffff;
}

.scan-section {
  margin-top: 18px;
}

.scan-section h3 {
  margin-bottom: 12px;
  color: #17362d;
}

.scan-record-list {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.scan-record-list li,
.trend-row {
  display: grid;
  grid-template-columns: 1.3fr 0.8fr 1fr;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.scan-record-list strong,
.trend-row strong {
  color: #17362d;
}

.scan-record-list span,
.scan-record-list small,
.trend-row span {
  color: #60786d;
}

.risk-actions {
  margin-top: 18px;
}

.risk-history {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 18px 0 0;
  list-style: none;
}

.risk-history li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.risk-history strong {
  color: #17362d;
}

.risk-history p,
.risk-history small {
  margin-bottom: 0;
  color: #60786d;
  line-height: 1.6;
}

.risk-history-empty {
  color: #60786d;
}

.trend-table {
  display: grid;
  gap: 10px;
}

.trend-head {
  background: rgba(36, 88, 70, 0.08);
}

.status-timeline {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
}

.status-timeline li {
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 12px;
  padding-bottom: 16px;
}

.status-dot {
  width: 14px;
  height: 14px;
  margin-top: 4px;
  border-radius: 999px;
}

.status-timeline h3 {
  margin-bottom: 6px;
  color: #17362d;
}

.status-timeline p,
.status-timeline small {
  color: #42594f;
  line-height: 1.7;
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
  width: min(860px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 24px;
}

.icon-button {
  min-width: 72px;
}

label {
  display: block;
}

input,
select,
textarea,
button,
.preview-link {
  font: inherit;
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid rgba(45, 85, 71, 0.16);
  border-radius: 16px;
  background: rgba(247, 249, 246, 0.95);
  color: #17362d;
}

textarea {
  resize: vertical;
}

.field-note,
.upload-box,
.uploaded-file-item {
  border: 1px solid rgba(45, 85, 71, 0.12);
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.field-note {
  padding: 14px;
}

.field-note strong,
.uploaded-file-item strong {
  display: block;
  color: #17362d;
}

.field-note small,
.upload-box small,
.uploaded-file-item small,
.upload-hint {
  color: #60786d;
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

.upload-state {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.upload-state.pending {
  background: rgba(242, 204, 105, 0.18);
  color: #7b5d13;
}

.upload-state.bound {
  background: rgba(40, 110, 74, 0.12);
  color: #245846;
}

.upload-hint {
  padding: 4px 2px;
}

.full-width {
  grid-column: 1 / -1;
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
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

button:hover,
.preview-link:hover,
.preview-block:hover {
  transform: translateY(-1px);
}

button:disabled {
  opacity: 0.46;
  cursor: not-allowed;
  transform: none;
}

.primary {
  background: #245846;
  color: #fff7ea;
}

.ghost {
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

.neutral {
  background: rgba(242, 204, 105, 0.18);
  color: #7b5d13;
}

.success {
  background: rgba(40, 110, 74, 0.12);
  color: #245846;
}

.warning {
  background: rgba(214, 137, 58, 0.16);
  color: #8b4c13;
}

.danger {
  background: rgba(190, 70, 58, 0.14);
  color: #8f2f29;
}

.preview-link {
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 1080px) {
  .hero-card,
  .grid,
  .risk-panel {
    grid-template-columns: 1fr;
  }

  .action-groups,
  .risk-checklist,
  .hero-grid,
  .trace-summary,
  .qr-grid,
  .scan-grid,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .page-shell {
    padding-inline: 14px;
  }

  .hero-main {
    grid-template-columns: 1fr;
  }

  .hero-image {
    width: 100%;
    height: 240px;
  }

  .qr-preview {
    grid-template-columns: 1fr;
  }

  .qr-preview img {
    width: 100%;
    height: auto;
    max-width: 260px;
  }

  .uploaded-file-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .created-guide,
  .trace-reuse-card {
    flex-direction: column;
  }

  .risk-history li {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 30px;
  }

  .support-actions,
  .hero-grid,
  .action-groups,
  .risk-checklist,
  .trace-summary,
  .qr-grid,
  .scan-grid,
  .form-grid,
  .quality-history li,
  .scan-record-list li,
  .trend-row {
    grid-template-columns: 1fr;
  }
}
</style>
