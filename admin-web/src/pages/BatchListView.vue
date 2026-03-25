<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  changeBatchStatus,
  createBatch,
  createQualityReport,
  createTraceRecord,
  generateBatchQr,
  getBatchDetail,
  getBatchList,
  getCompanyOptions,
  getProductOptions,
  updateBatch,
  uploadBatchFiles
} from '../api/batch'
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

const router = useRouter()

const loading = ref(false)
const message = ref('')
const messageType = ref('info')
const batches = ref([])

const filters = ref(createFilterState())
const listMode = ref('ACTION')
const dialog = ref(createDialogState())
const batchForm = ref(createBatchForm())
const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const statusForm = ref(createStatusForm())

const formCompanyOptions = ref([])
const formProductOptions = ref([])
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
  { value: 'ACTION', label: '待处理', copy: '默认优先看还没做完的批次' },
  { value: 'READY', label: '可发布', copy: '资料已齐，只差发布动作' },
  { value: 'RISK', label: '风险批次', copy: '优先看冻结、召回与整改中的批次' },
  { value: 'LIVE', label: '已发布', copy: '已上线可扫码查看的批次' },
  { value: 'ALL', label: '全部批次', copy: '查看完整批次列表' }
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

const topQueue = computed(() => {
  return batchCards.value.filter((card) => card.insight.isActionable).slice(0, 3)
})

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'create':
      return '新建批次'
    case 'edit':
      return `编辑批次：${dialog.value.batchName}`
    case 'trace':
      return `快速补录：${dialog.value.batchName}`
    case 'quality':
      return `上传质检：${dialog.value.batchName}`
    case 'status':
      return `状态处理：${dialog.value.batchName}`
    default:
      return ''
  }
})

onMounted(async () => {
  await loadCompanyOptions()
  await fetchBatches()
})

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
  listMode.value = 'ACTION'
  fetchBatches()
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
      nextCopy = '整改检查项已满足，可恢复发布。'
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
      ADD_TRACE: '先补关键追溯节点，方便后续演示和扫码查看。',
      UPLOAD_QUALITY: '补上质检摘要后，公开页可信度会明显更高。',
      GENERATE_QR: '二维码就绪后，才能顺畅衔接扫码演示。',
      PUBLISH: '资料已经齐，可以直接完成对外发布。'
    }[nextActionCode] ?? '继续处理当前批次。'
    priority = item.status === 'DRAFT' ? 360 - missing.length * 10 : 220
  } else if (item.status === 'PUBLISHED') {
    nextLabel = '查看公开页表现'
    nextCopy = '已发布，可回工作台查看二维码与扫码数据。'
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
      { label: '追溯', done: !needsTrace },
      { label: '质检', done: hasQuality },
      { label: '二维码', done: hasQr },
      { label: '发布', done: item.status === 'PUBLISHED' }
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

function qrStatusLabel(status) {
  return status && status !== 'NOT_GENERATED' ? '已生成' : '待生成'
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
    <header class="hero-card" data-testid="batch-list-hero">
      <div>
        <p class="eyebrow">批次主入口</p>
        <h1>先盯住批次，再把下一步做完</h1>
        <p class="lead">
          默认已经按“待处理优先”整理批次。企业管理员可以从这里直接完成建档后的主流程：
          补追溯、传质检、生成二维码、发布或处理风险。
        </p>

        <div v-if="topQueue.length" class="queue-strip">
          <article v-for="card in topQueue" :key="card.item.id" class="queue-item">
            <strong>{{ card.item.batchCode }}</strong>
            <span>{{ card.insight.nextLabel }}</span>
          </article>
        </div>
      </div>

      <div class="hero-actions">
        <button class="primary" data-testid="batch-create-button" @click="openCreateDialog">新建批次</button>
        <button class="ghost" @click="router.push('/companies')">企业主数据</button>
        <button class="ghost" @click="router.push('/products')">产品主数据</button>
        <button class="ghost" @click="router.push('/dashboard')">返回总览</button>
      </div>
    </header>

    <section class="stats-grid">
      <article class="stat-card">
        <span>批次总数</span>
        <strong>{{ listStats.total }}</strong>
      </article>
      <article class="stat-card">
        <span>待处理</span>
        <strong>{{ listStats.actionable }}</strong>
      </article>
      <article class="stat-card">
        <span>可直接发布</span>
        <strong>{{ listStats.ready }}</strong>
      </article>
      <article class="stat-card warning">
        <span>风险批次</span>
        <strong>{{ listStats.risk }}</strong>
      </article>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">默认视角</p>
          <h2>先把“下一步最明确”的批次找出来</h2>
        </div>
      </div>

      <div class="mode-row">
        <button
          v-for="item in listModes"
          :key="item.value"
          class="mode-pill"
          :class="{ active: listMode === item.value }"
          @click="listMode = item.value"
        >
          <strong>{{ item.label }}</strong>
          <span>{{ item.copy }}</span>
        </button>
      </div>

      <div class="filter-grid">
        <label>
          <span>批次号</span>
          <input v-model.trim="filters.batchCode" data-testid="batch-filter-code" type="text" placeholder="例如 BATCH202603250930">
        </label>
        <label>
          <span>产品名</span>
          <input v-model.trim="filters.productName" type="text" placeholder="输入产品关键字">
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
        <button class="primary" data-testid="batch-search-button" :disabled="loading" @click="fetchBatches">刷新列表</button>
        <button class="ghost" :disabled="loading" @click="resetFilters">恢复默认</button>
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
        <h3>当前视角下没有批次</h3>
        <p>可以切换上方视角，或直接新建一个批次开始演示主流程。</p>
        <div class="toolbar">
          <button class="primary" @click="openCreateDialog">新建批次</button>
          <button class="ghost" @click="listMode = 'ALL'">查看全部批次</button>
        </div>
      </div>
    </section>

    <section v-else class="batch-list">
      <article
        v-for="card in visibleBatchCards"
        :key="card.item.id"
        class="batch-card"
        :data-testid="`batch-card-${card.item.id}`"
      >
        <div class="batch-main">
          <img class="product-image" :src="card.item.productImageUrl" :alt="card.item.productName">

          <div class="batch-copy">
            <div class="title-row">
              <div>
                <p class="batch-code">{{ card.item.batchCode }}</p>
                <h3>{{ card.item.productName }}</h3>
                <p class="next-copy">下一步：{{ card.insight.nextCopy }}</p>
              </div>
              <div class="title-side">
                <span class="status-badge" :class="statusClass(card.item.status)">{{ card.item.statusLabel }}</span>
                <span class="next-badge" :data-testid="`batch-next-${card.item.id}`">{{ card.insight.nextLabel }}</span>
              </div>
            </div>

            <div class="meta-grid">
              <div>
                <span>所属企业</span>
                <strong>{{ card.item.companyName }}</strong>
              </div>
              <div>
                <span>当前节点</span>
                <strong>{{ card.item.currentNode }}</strong>
              </div>
              <div>
                <span>产地</span>
                <strong>{{ card.item.originPlace }}</strong>
              </div>
              <div>
                <span>生产日期</span>
                <strong>{{ card.item.productionDate }}</strong>
              </div>
              <div>
                <span>质检</span>
                <strong>{{ card.item.qualityStatus }}</strong>
              </div>
              <div>
                <span>二维码</span>
                <strong>{{ qrStatusLabel(card.item.qrStatus) }}</strong>
              </div>
            </div>

            <div class="progress-strip">
              <span
                v-for="step in card.insight.progress"
                :key="step.label"
                class="progress-pill"
                :class="{ done: step.done }"
              >
                {{ step.label }}
              </span>
            </div>

            <div class="tag-row">
              <span v-for="tag in card.item.quickTags" :key="tag">{{ tag }}</span>
              <span v-for="tag in card.insight.missing" :key="tag.key" class="missing-tag">{{ tag.label }}</span>
            </div>
          </div>
        </div>

        <div class="action-row">
          <button class="primary" :data-testid="`batch-recommend-${card.item.id}`" @click="runRecommendedAction(card)">{{ card.insight.nextLabel }}</button>
          <button class="ghost" :data-testid="`batch-open-workbench-${card.item.id}`" @click="router.push(`/batches/${card.item.id}`)">进入工作台</button>
          <button class="neutral" @click="openTraceDialog(card.item)">补追溯</button>
          <button class="success" @click="openQualityDialog(card.item)">上传质检</button>
          <button class="ghost" @click="handleGenerateQr(card.item)">二维码</button>
          <button
            v-if="actionOf(card.item, 'PUBLISH').enabled"
            class="success"
            @click="openStatusDialog(card.item, 'PUBLISHED')"
          >
            发布
          </button>
          <button
            v-if="actionOf(card.item, 'RESUME').enabled"
            class="success"
            @click="openStatusDialog(card.item, 'PUBLISHED')"
          >
            恢复发布
          </button>
          <button
            class="warning"
            :disabled="!actionOf(card.item, 'FREEZE').enabled"
            :title="actionOf(card.item, 'FREEZE').hint"
            @click="openStatusDialog(card.item, 'FROZEN')"
          >
            冻结
          </button>
          <button
            class="danger"
            :disabled="!actionOf(card.item, 'RECALL').enabled"
            :title="actionOf(card.item, 'RECALL').hint"
            @click="openStatusDialog(card.item, 'RECALLED')"
          >
            召回
          </button>
          <button class="ghost" @click="openEditDialog(card.item)">编辑资料</button>
        </div>
      </article>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">批次快捷操作</p>
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
            <input v-model.trim="batchForm.originPlace" type="text" placeholder="例如 赣州信丰">
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
                分类：{{ selectedProductOption.category || '待补充' }}，
                规格：{{ selectedProductOption.specification || '待补充' }}
              </small>
            </div>
          </label>
          <label class="full-width">
            <span>公开页说明</span>
            <textarea
              v-model.trim="batchForm.publicRemark"
              rows="3"
              placeholder="创建后会直接进入工作台，建议写一句面向消费者的批次说明"
            />
          </label>
          <label class="full-width">
            <span>内部备注</span>
            <textarea
              v-model.trim="batchForm.internalRemark"
              rows="3"
              placeholder="可写本轮演示提醒、补录计划或内控备注"
            />
          </label>
          <div class="full-width flow-tip">
            <strong>{{ dialog.type === 'create' ? '创建后会直接进入工作台。' : '更新后会回到工作台。' }}</strong>
            <span>下一步入口已经准备好：补追溯、上传质检、生成二维码。</span>
          </div>
        </div>

        <div v-else-if="dialog.type === 'trace'" class="form-grid" data-testid="batch-trace-dialog">
          <div class="full-width quick-entry-card">
            <div>
              <p class="eyebrow">现场快速补录</p>
              <h4>当前按“少录入、少跳转”设计</h4>
              <p>
                默认已经带入当前时间，阶段、地点和说明可直接点选。
                {{ traceDialogContext.totalCount ? `当前批次已有 ${traceDialogContext.totalCount} 条记录。` : '当前还没有记录，可先补第一条关键节点。' }}
              </p>
            </div>
            <button
              v-if="traceDialogContext.latestRecord"
              class="ghost"
              @click="copyLatestTraceRecord"
            >
              复制上一条并微调
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
            <input v-model.trim="traceForm.operatorName" type="text" placeholder="例如 现场补录员">
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
              placeholder="建议写本节点做了什么、由谁完成、是否可继续下一步"
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
              <small>支持多图上传。上传成功后会立刻显示在下面，保存记录时自动一起绑定。</small>
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

          <div v-if="traceDialogContext.latestRecord" class="full-width last-record-card">
            <span>上一条记录</span>
            <strong>{{ traceDialogContext.latestRecord.title }}</strong>
            <p>{{ traceDialogContext.latestRecord.location }} · {{ traceDialogContext.latestRecord.eventTime }}</p>
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
            <input v-model.trim="qualityForm.agency" type="text" placeholder="例如 江西某检测中心">
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
              placeholder="一行一个重点，例如：关键指标合格 / 企业自检正常 / 抽检无异常"
            />
          </label>
          <label class="full-width">
            <span>附件</span>
            <div class="upload-box">
              <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
              <small>可上传 PDF 或图片，公开页会优先展示质检结论，后台保留附件供答辩说明。</small>
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
              placeholder="建议写清楚当前批次为什么要发布、冻结或召回"
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
            {{ dialog.type === 'create' ? '创建并进入工作台' : '确认保存' }}
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

.hero-card,
.panel,
.stat-card,
.batch-card,
.dialog-card,
.message-bar {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(31, 55, 47, 0.08);
}

.hero-card {
  display: grid;
  grid-template-columns: 1.5fr auto;
  gap: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(242, 204, 105, 0.26), transparent 28%),
    linear-gradient(145deg, #16362d, #285143 72%, #edf5eb 170%);
  color: #f7f2e7;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f2cc69;
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
  color: rgba(247, 242, 231, 0.92);
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
  background: rgba(255, 255, 255, 0.1);
}

.queue-item strong,
.queue-item span {
  display: block;
}

.queue-item span {
  margin-top: 6px;
  color: rgba(247, 242, 231, 0.84);
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
  color: #60786d;
  font-size: 14px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  color: #17362d;
  font-size: 34px;
}

.stat-card.warning strong {
  color: #b24b2d;
}

.panel {
  margin-top: 18px;
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
  background: rgba(244, 248, 245, 0.96);
  color: #2b4f43;
}

.mode-pill.active {
  background: #245846;
  color: #fff7ea;
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
  color: #4b6358;
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
.uploaded-file-item,
.flow-tip,
.quick-entry-card,
.last-record-card,
.template-card {
  border: 1px solid rgba(45, 85, 71, 0.12);
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
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
  color: #60786d;
}

.flow-tip strong,
.last-record-card strong,
.field-note strong,
.uploaded-file-item strong {
  display: block;
  color: #17362d;
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

.chip-button {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

.chip-button.active {
  background: #245846;
  color: #fff7ea;
}

.chip-button.light {
  background: rgba(242, 204, 105, 0.14);
  color: #7b5d13;
}

.message-bar {
  margin-top: 18px;
}

.message-bar.success {
  background: rgba(226, 244, 233, 0.94);
  color: #245846;
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
  color: #546b61;
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
  background: linear-gradient(160deg, #eef5ed, #dce9df);
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.batch-code {
  margin-bottom: 6px;
  color: #60786d;
  font-size: 14px;
  letter-spacing: 0.04em;
}

.batch-copy h3 {
  margin-bottom: 8px;
  font-size: 28px;
  color: #17362d;
}

.next-copy {
  margin-bottom: 0;
  color: #51675d;
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
  background: rgba(242, 204, 105, 0.16);
  color: #7b5d13;
  font-size: 13px;
}

.status-badge.draft {
  background: rgba(93, 120, 110, 0.16);
  color: #556b63;
}

.status-badge.published {
  background: rgba(40, 110, 74, 0.14);
  color: #245846;
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
  background: rgba(245, 248, 244, 0.96);
}

.meta-grid strong {
  display: block;
  margin-top: 8px;
  color: #17362d;
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
  background: rgba(93, 120, 110, 0.12);
  color: #5d786e;
  font-size: 13px;
}

.progress-pill.done {
  background: rgba(40, 110, 74, 0.12);
  color: #245846;
}

.tag-row span {
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
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

  .hero-actions {
    justify-content: flex-start;
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
