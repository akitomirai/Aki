<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PrimaryActionGroup from '../components/PrimaryActionGroup.vue'
import {
  createTraceRecord,
  deleteFieldDraft as deleteFieldDraftRequest,
  getBatchDetail,
  getBatchList,
  getFieldDraft as getFieldDraftRequest,
  getFieldDraftList,
  saveFieldDraft as saveFieldDraftRequest,
  uploadBatchFiles
} from '../api/batch'
import { useAuthStore } from '../stores/auth'
import { createTraceForm, currentDateTime, formatStageLabel, getStageProfile, stageOptions } from '../utils/traceWorkflow'
import {
  getFieldDraft as getLocalFieldDraft,
  listFieldDrafts as listLocalFieldDrafts,
  removeFieldDraft as removeLocalFieldDraft,
  saveFieldDraft as saveLocalFieldDraft
} from '../utils/fieldDrafts'
import { resolveTaskStatusText, resolveTodayStatusText, taskFilterOptions } from '../utils/statusPresentation'

const MAX_IMAGE_COUNT = 9
const IMAGE_BUSINESS_TYPE = 'trace-image'

const listModeOptions = [
  { value: 'todo', label: '待办批次' },
  { value: 'drafts', label: '我的草稿' }
]

let imageSeed = 0

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const detailLoading = ref(false)
const saving = ref(false)
const draftSaving = ref(false)
const uploading = ref(false)
const batches = ref([])
const batchDetail = ref(null)
const selectedBatchId = ref('')
const listViewMode = ref('todo')
const taskFilter = ref('PENDING')
const traceForm = ref(createEntryForm())
const imageItems = ref([])
const draftList = ref([])
const draftMeta = ref(null)
const lastSuccess = ref(null)
const formSectionRef = ref(null)
const showAdvancedFields = ref(false)

const draftMap = computed(() => Object.fromEntries(draftList.value.map((item) => [String(item.batchId), item])))
const batchMap = computed(() => Object.fromEntries(batches.value.map((item) => [String(item.id), item])))
const actionableBatches = computed(() => batches.value.filter((item) => batchActionEnabled(item, 'ADD_TRACE')))
const todoBatches = computed(() => {
  return actionableBatches.value
    .map((item) => {
      const draft = draftMap.value[String(item.id)] ?? null
      return {
        ...item,
        productName: productLabel(item.productName, item.productCode, '待补现场记录'),
        draft,
        hasDraft: Boolean(draft),
        recentDisplayTime: draft?.updatedAt || item.lastUpdatedAt || item.latestTraceTime || item.marketDate || ''
      }
    })
    .sort(compareTodoBatch)
})

const filteredTodoBatches = computed(() => {
  if (taskFilter.value === 'DRAFT') {
    return todoBatches.value.filter((item) => item.hasDraft)
  }
  if (taskFilter.value === 'DONE_TODAY') {
    return todoBatches.value.filter((item) => item.todayCompleted)
  }
  return todoBatches.value.filter((item) => !item.hasDraft && !item.todayCompleted)
})

const draftSummaries = computed(() => {
  return draftList.value.map((draft) => {
    const batch = batchMap.value[String(draft.batchId)] ?? {}
    const uploadedFiles = sanitizeUploadedFiles(draft.uploadedFiles)
    return {
      ...draft,
      batchCode: draft.batchCode || batch.batchCode || `批次 ${draft.batchId}`,
      productName: productLabel(
        draft.productName || batch.productName,
        draft.productCode || batch.productCode,
        '待补现场记录'
      ),
      currentNode: draft.currentNode || batch.currentNode || formatStageLabel(draft.stage || 'PRODUCE'),
      stageLabel: formatStageLabel(draft.stage || 'PRODUCE'),
      imageCount: draft.imageCount ?? uploadedFiles.length
    }
  })
})

const selectedBatch = computed(() => {
  return todoBatches.value.find((item) => String(item.id) === String(selectedBatchId.value))
    ?? batches.value.find((item) => String(item.id) === String(selectedBatchId.value))
    ?? null
})

const currentBatchCode = computed(() => selectedBatch.value?.batchCode || batchDetail.value?.batch?.batchCode || '')
const currentProductName = computed(() => productLabel(
  selectedBatch.value?.productName || batchDetail.value?.product?.name,
  selectedBatch.value?.productCode || batchDetail.value?.product?.productCode,
  '现场作业'
))
const currentCompanyName = computed(() => selectedBatch.value?.companyName || batchDetail.value?.company?.name || '')
const currentStatusLabel = computed(() => selectedBatch.value?.statusLabel || batchDetail.value?.status?.label || '待处理')
const currentTaskStatusLabel = computed(() => resolveTaskStatusLabel({
  hasDraft: Boolean(draftMeta.value),
  draftStatusLabel: batchDetail.value?.task?.draftStatusLabel || selectedBatch.value?.draftStatusLabel,
  todayCompleted: selectedBatch.value?.todayCompleted,
  taskStatus: selectedBatch.value?.taskStatus || batchDetail.value?.task?.taskStatus,
  taskStatusLabel: selectedBatch.value?.taskStatusLabel || batchDetail.value?.task?.taskStatusLabel
}))
const currentAssignedAt = computed(() => selectedBatch.value?.assignedAt || batchDetail.value?.task?.assignedAt || '')
const currentAssigneeName = computed(() => selectedBatch.value?.assigneeName || batchDetail.value?.task?.assigneeName || authStore.user?.realName || '')
const currentNode = computed(() => batchDetail.value?.status?.currentNode || selectedBatch.value?.currentNode || '待补现场记录')
const currentProfile = computed(() => getStageProfile(traceForm.value.stage))
const latestRecord = computed(() => batchDetail.value?.trace?.recentRecords?.[0] ?? null)
const failedImageCount = computed(() => imageItems.value.filter((item) => item.status === 'failed').length)
const hasFailedImages = computed(() => failedImageCount.value > 0)
const uploadStatusText = computed(() => {
  if (uploading.value) {
    return '图片上传中，请稍等上传完成后再保存或提交。'
  }
  if (hasFailedImages.value) {
    return `${failedImageCount.value} 张图片上传失败，可重试或删除后继续。`
  }
  if (imageItems.value.length) {
    return `已准备 ${imageItems.value.length} 张图片，可继续排序、补录或提交。`
  }
  return `支持一次选择多张或连续追加，最多保留 ${MAX_IMAGE_COUNT} 张。`
})

const submitBlockReason = computed(() => entryValidationError('submit'))
const draftBlockReason = computed(() => entryValidationError('draft'))
const submitSecondaryActions = computed(() => ([
  {
    key: 'save-draft',
    label: draftSaving.value ? '正在保存草稿...' : '保存草稿',
    testId: 'field-entry-save-draft',
    disabled: Boolean(draftBlockReason.value) || draftSaving.value || saving.value || detailLoading.value
  }
]))
const submitPrimaryHint = computed(() => {
  if (submitBlockReason.value) {
    return submitBlockReason.value
  }
  return '提交后会直接同步到批次工作台。'
})

const summaryStats = computed(() => {
  const pendingCount = todoBatches.value.filter((item) => !item.hasDraft && !item.todayCompleted).length
  const draftCount = todoBatches.value.filter((item) => item.hasDraft).length
  const doneTodayCount = todoBatches.value.filter((item) => item.todayCompleted).length
  return { pendingCount, draftCount, doneTodayCount }
})

const entryHints = computed(() => {
  if (!selectedBatchId.value) return []

  const items = []
  if (draftMeta.value) items.push(`宸叉帴缁崏绋匡紝鏈€杩戜繚瀛樹簬 ${formatDraftTime(draftMeta.value.updatedAt)}`)
  if (!batchDetail.value?.trace?.recentRecords?.length) items.push('建议先补第一条现场记录，提交后工作台会立刻同步显示。')
  if (batchDetail.value?.quality?.status === 'PENDING') items.push('现场记录补完后，记得回工作台继续补质量摘要。')
  if (!batchDetail.value?.qr?.generated) items.push('图片和说明提交后，可继续回工作台生成二维码。')
  return items.slice(0, 3)
})

const entryGuideCards = computed(() => [
  {
    key: 'stage',
    label: '当前要录',
    value: formatStageLabel(traceForm.value.stage),
    detail: currentNode.value || '待补现场记录'
  },
  {
    key: 'summary',
    label: '先说一句',
    value: traceForm.value.summary || currentProfile.value.summaries[0],
    detail: '把现场完成情况讲清即可，不必写成后台字段。'
  },
  {
    key: 'image',
    label: '建议图片',
    value: imageItems.value.length ? `已准备 ${imageItems.value.length} 张` : '建议带 1-3 张',
    detail: hasFailedImages.value ? '有上传失败图片，请重试或删除。' : '优先拍关键操作、标签和环境。'
  }
])

const advancedFieldSummary = computed(() => {
  const timeText = traceForm.value.eventTime ? formatDraftTime(traceForm.value.eventTime) : '沿用当前时间'
  const locationText = traceForm.value.location || currentProfile.value.locations[0] || '地点待补充'
  const operatorText = traceForm.value.operatorName || defaultOperatorName(currentProfile.value.defaultOperator)
  return `${timeText} · ${locationText} · ${operatorText} · ${traceForm.value.visibleToConsumer ? '会同步到追溯页' : '仅后台可见'}`
})
const fieldEntryAccountLabel = computed(() => authStore.user?.realName || authStore.user?.username || '当前账号')

const demoFlowSteps = computed(() => {
  const steps = ['补现场记录']
  steps.push(batchDetail.value?.quality?.status === 'PENDING' ? '回工作台上传质检' : '回工作台核对质检')
  steps.push(batchDetail.value?.qr?.generated ? '查看公开页' : '回工作台生成二维码')
  return steps
})

onMounted(async () => {
  await loadBatches()
})

onBeforeUnmount(() => {
  imageItems.value.forEach(releaseImageItem)
})

watch(
  () => route.query.batchId,
  async (value) => {
    const nextId = value ? String(value) : ''
    if (!nextId) {
      closeBatch(false)
      return
    }
    if (nextId !== selectedBatchId.value && batches.value.length) {
      await openBatch(nextId, { syncRoute: false })
    }
  }
)

watch(
  () => traceForm.value.stage,
  (stage, previousStage) => {
    const profile = getStageProfile(stage)
    const previousProfile = getStageProfile(previousStage)

    if (!previousStage || traceForm.value.title === previousProfile.defaultTitle) {
      traceForm.value.title = profile.defaultTitle
    }
    if (!traceForm.value.location || previousProfile.locations.includes(traceForm.value.location)) {
      traceForm.value.location = profile.locations[0]
    }
    if (!traceForm.value.summary || previousProfile.summaries.includes(traceForm.value.summary)) {
      traceForm.value.summary = profile.summaries[0]
    }
    if (!traceForm.value.operatorName || traceForm.value.operatorName === previousProfile.defaultOperator) {
      traceForm.value.operatorName = defaultOperatorName(profile.defaultOperator)
    }
  }
)

function defaultOperatorName(fallback = '现场操作员') {
  return authStore.user?.realName || authStore.user?.username || fallback
}

function sanitizeUploadedFiles(files = []) {
  if (!Array.isArray(files)) return []
  return files.map((item) => ({
    id: item.id,
    fileName: item.fileName,
    filePath: item.filePath,
    fileUrl: item.fileUrl,
    contentType: item.contentType,
    size: item.size,
    businessType: item.businessType,
    businessId: item.businessId
  }))
}

function parseDateValue(value) {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const normalized = typeof value === 'string' ? value.replace(' ', 'T') : value
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function toTimestamp(value) {
  const date = parseDateValue(value)
  return date ? date.getTime() : 0
}

function buildImageClientId(prefix = 'image') {
  imageSeed += 1
  return `${prefix}-${Date.now()}-${imageSeed}`
}

function createImageItemFromUpload(file) {
  return {
    clientId: buildImageClientId('uploaded'),
    id: file.id,
    fileName: file.fileName,
    filePath: file.filePath,
    fileUrl: file.fileUrl,
    previewUrl: file.fileUrl,
    contentType: file.contentType,
    size: file.size,
    businessType: file.businessType,
    businessId: file.businessId,
    progress: 100,
    status: 'uploaded',
    errorMessage: '',
    localFile: null,
    previewIsObjectUrl: false
  }
}

function createLocalImageItem(file) {
  return {
    clientId: buildImageClientId('local'),
    id: null,
    fileName: file.name,
    filePath: '',
    fileUrl: '',
    previewUrl: URL.createObjectURL(file),
    contentType: file.type,
    size: file.size,
    businessType: IMAGE_BUSINESS_TYPE,
    businessId: null,
    progress: 0,
    status: 'queued',
    errorMessage: '',
    localFile: file,
    previewIsObjectUrl: true
  }
}

function releaseImageItem(item) {
  if (item?.previewIsObjectUrl && item.previewUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(item.previewUrl)
  }
}

function syncAttachmentState() {
  const uploadedFiles = imageItems.value
    .filter((item) => item.status === 'uploaded' && item.id)
    .map((item) => ({
      id: item.id,
      fileName: item.fileName,
      filePath: item.filePath,
      fileUrl: item.fileUrl,
      contentType: item.contentType,
      size: item.size,
      businessType: item.businessType,
      businessId: item.businessId
    }))

  traceForm.value.uploadedFiles = uploadedFiles
  traceForm.value.attachmentIds = uploadedFiles.map((item) => item.id)
  if (!uploadedFiles.some((item) => item.fileUrl === traceForm.value.imageUrl)) {
    traceForm.value.imageUrl = uploadedFiles[0]?.fileUrl ?? ''
  }
}

function resetImageItems(files = []) {
  imageItems.value.forEach(releaseImageItem)
  imageItems.value = sanitizeUploadedFiles(files).map(createImageItemFromUpload)
  syncAttachmentState()
}

function createEntryForm(overrides = {}) {
  const stage = overrides.stage ?? 'PRODUCE'
  const profile = getStageProfile(stage)
  return createTraceForm({
    stage,
    title: overrides.title,
    eventTime: overrides.eventTime ?? currentDateTime(),
    operatorName: overrides.operatorName ?? defaultOperatorName(profile.defaultOperator),
    location: overrides.location ?? profile.locations[0],
    summary: overrides.summary ?? profile.summaries[0],
    imageUrl: overrides.imageUrl ?? '',
    attachmentIds: Array.isArray(overrides.attachmentIds) ? [...overrides.attachmentIds] : [],
    uploadedFiles: sanitizeUploadedFiles(overrides.uploadedFiles),
    visibleToConsumer: overrides.visibleToConsumer ?? true
  })
}

function buildDraftBatchSnapshot(batchId = selectedBatchId.value) {
  const batch = batchMap.value[String(batchId)] ?? selectedBatch.value ?? {}
  return {
    id: batch.id ?? Number(batchId),
    batchCode: batch.batchCode || currentBatchCode.value,
    productName: batch.productName || currentProductName.value,
    productCode: batch.productCode || batchDetail.value?.product?.productCode || '',
    currentNode: batch.currentNode || currentNode.value
  }
}

function normalizeLocalDraft(draft) {
  if (!draft) return null
  const form = draft.data?.form ?? {}
  const uploadedFiles = sanitizeUploadedFiles(form.uploadedFiles)
  return {
    id: draft.id,
    batchId: String(draft.batchId),
    batchCode: draft.data?.batch?.batchCode || '',
    productName: draft.data?.batch?.productName || '',
    productCode: draft.data?.batch?.productCode || '',
    currentNode: draft.data?.batch?.currentNode || '',
    stage: form.stage || 'PRODUCE',
    title: form.title || '',
    eventTime: form.eventTime || '',
    operatorName: form.operatorName || '',
    location: form.location || '',
    summary: form.summary || '',
    imageUrl: form.imageUrl || '',
    attachmentIds: Array.isArray(form.attachmentIds) ? [...form.attachmentIds] : [],
    uploadedFiles,
    visibleToConsumer: form.visibleToConsumer !== false,
    imageCount: uploadedFiles.length,
    updatedAt: draft.updatedAt
  }
}

function normalizeServerDraft(draft) {
  if (!draft) return null
  return {
    ...draft,
    batchId: String(draft.batchId),
    attachmentIds: Array.isArray(draft.attachmentIds) ? [...draft.attachmentIds] : [],
    uploadedFiles: sanitizeUploadedFiles(draft.uploadedFiles),
    visibleToConsumer: draft.visibleToConsumer !== false,
    imageCount: Number(draft.imageCount ?? draft.uploadedFiles?.length ?? 0)
  }
}

function cacheLocalDraft(draft) {
  if (!draft?.batchId) return null
  return saveLocalFieldDraft(authStore.user, draft.batchId, {
    form: {
      stage: draft.stage,
      title: draft.title,
      eventTime: draft.eventTime,
      operatorName: draft.operatorName,
      location: draft.location,
      summary: draft.summary,
      imageUrl: draft.imageUrl,
      attachmentIds: draft.attachmentIds,
      uploadedFiles: draft.uploadedFiles,
      visibleToConsumer: draft.visibleToConsumer
    },
    batch: buildDraftBatchSnapshot(draft.batchId)
  })
}

async function refreshDraftList() {
  try {
    const response = await getFieldDraftList()
    draftList.value = (response.data ?? []).map(normalizeServerDraft).filter(Boolean)
  } catch (error) {
    draftList.value = listLocalFieldDrafts(authStore.user).map(normalizeLocalDraft).filter(Boolean)
  }
}

async function loadCurrentDraft(batchId) {
  if (!batchId) return null
  try {
    const response = await getFieldDraftRequest(batchId)
    const nextDraft = normalizeServerDraft(response.data)
    if (nextDraft) {
      cacheLocalDraft(nextDraft)
      return nextDraft
    }
    removeLocalFieldDraft(authStore.user, batchId)
    return null
  } catch (error) {
    if (error?.response?.status === 401 || error?.response?.status === 403 || error?.response?.status === 404) {
      removeLocalFieldDraft(authStore.user, batchId)
      return null
    }
    // Fall back to local cache when the server draft endpoint is temporarily unavailable.
  }
  return normalizeLocalDraft(getLocalFieldDraft(authStore.user, batchId))
}

function batchActionEnabled(batch, code) {
  return batch.actions?.some((item) => item.code === code && item.enabled)
}

function todoPriority(batch) {
  if (batch.hasDraft) return 0
  if (!batch.todayCompleted) return 1
  return 2
}

function compareTodoBatch(left, right) {
  const priorityGap = todoPriority(left) - todoPriority(right)
  if (priorityGap) return priorityGap

  const updateGap = toTimestamp(right.recentDisplayTime) - toTimestamp(left.recentDisplayTime)
  if (updateGap) return updateGap

  return Number(right.id) - Number(left.id)
}

function productLabel(name, code, fallback = '未命名产品') {
  const safeName = String(name || '').trim() || fallback
  const safeCode = String(code || '').trim()
  if (safeCode && safeName.includes(`（${safeCode}）`)) {
    return safeName
  }
  return safeCode ? `${safeName}（${safeCode}）` : safeName
}

function getTaskLabel(batch) {
  if (batch.draft) return '草稿待续'
  if (batch.status === 'FROZEN') return '已冻结'
  return '待处理'
}

function getTaskCopy(batch) {
  if (batch.draft) return `上次已保存到 ${formatDraftTime(batch.draft.updatedAt)}，可以继续补图、补说明再提交。`
  if (batch.status === 'DRAFT') return '当前批次还在草稿阶段，补录后会直接同步到工作台。'
  if (batch.status === 'FROZEN') return '当前批次已冻结，补录现场信息前请先核对风险处理进度。'
  return '批次已发布，仍可继续补录新的现场记录和图片。'
}

function entryButtonText(batch) {
  if (batch.hasDraft) return '继续录入'
  if (batch.todayCompleted) return '继续补录'
  return '开始录入'
}

function taskToneClass(batch) {
  if (batch.hasDraft) return 'draft'
  if (batch.todayCompleted) return 'done'
  return 'pending'
}

function resolveTaskStatusLabel(batch) {
  return resolveTaskStatusText(batch)
}

function formatDraftTime(value) {
  if (!value) return '刚刚'
  const date = parseDateValue(value)
  if (!date) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadBatches(options = {}) {
  const { preserveSelection = false } = options
  loading.value = true
  try {
    const batchResponse = await getBatchList({
      mineOnly: Boolean(authStore.user?.companyId)
    })
    batches.value = batchResponse.data ?? []
    await refreshDraftList()

    if (!preserveSelection) {
      const routeBatchId = route.query.batchId ? String(route.query.batchId) : ''
      if (routeBatchId) {
        await openBatch(routeBatchId, { syncRoute: false })
      }
    } else if (selectedBatchId.value) {
      draftMeta.value = draftMap.value[String(selectedBatchId.value)] ?? null
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '待办批次加载失败，请稍后再试。')
  } finally {
    loading.value = false
  }
}

async function loadBatchDetail(batchId = selectedBatchId.value) {
  if (!batchId) {
    batchDetail.value = null
    return
  }

  detailLoading.value = true
  try {
    const response = await getBatchDetail(batchId)
    batchDetail.value = response.data ?? null
  } catch (error) {
    batchDetail.value = null
    ElMessage.error(error?.response?.data?.message || '批次详情加载失败，请稍后再试。')
  } finally {
    detailLoading.value = false
  }
}

async function hydrateEntryForm(batchId, options = {}) {
  const { ignoreDraft = false } = options
  const savedDraft = ignoreDraft ? null : await loadCurrentDraft(batchId)
  if (savedDraft) {
    traceForm.value = createEntryForm({
      stage: savedDraft.stage,
      title: savedDraft.title,
      eventTime: savedDraft.eventTime,
      operatorName: savedDraft.operatorName,
      location: savedDraft.location,
      summary: savedDraft.summary,
      imageUrl: savedDraft.imageUrl,
      attachmentIds: savedDraft.attachmentIds,
      uploadedFiles: savedDraft.uploadedFiles,
      visibleToConsumer: savedDraft.visibleToConsumer
    })
    resetImageItems(savedDraft.uploadedFiles)
    draftMeta.value = savedDraft
    return
  }

  draftMeta.value = null
  if (latestRecord.value) {
    const stage = latestRecord.value.stageCode || 'PRODUCE'
    const profile = getStageProfile(stage)
    traceForm.value = createEntryForm({
      stage,
      operatorName: defaultOperatorName(latestRecord.value.operatorName || profile.defaultOperator),
      location: latestRecord.value.location || profile.locations[0]
    })
    resetImageItems()
    return
  }

  traceForm.value = createEntryForm()
  resetImageItems()
}

async function openBatch(batchId, options = {}) {
  const { syncRoute = true } = options
  const nextId = String(batchId || '')
  if (!nextId) return

  selectedBatchId.value = nextId
  lastSuccess.value = null
  showAdvancedFields.value = false
  await loadBatchDetail(nextId)
  await hydrateEntryForm(nextId)

  if (syncRoute && route.query.batchId !== nextId) {
    router.push({
      path: '/field-entry',
      query: { batchId: nextId }
    })
  }
}

function closeBatch(syncRoute = true) {
  selectedBatchId.value = ''
  batchDetail.value = null
  traceForm.value = createEntryForm()
  resetImageItems()
  draftMeta.value = null
  lastSuccess.value = null
  showAdvancedFields.value = false

  if (syncRoute && route.query.batchId) {
    router.push('/field-entry')
  }
}

function openBatchWorkbench(batchId = selectedBatchId.value) {
  if (!batchId) return
  router.push(`/batches/${batchId}`)
}

function handleLogout() {
  const hasActiveEntry = Boolean(selectedBatchId.value)
  if (hasActiveEntry) {
    const confirmed = window.confirm('退出登录后会返回登录页，当前未提交内容请先保存草稿。确认退出吗？')
    if (!confirmed) {
      return
    }
  }
  authStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

function setTaskFilter(value) {
  taskFilter.value = value
}

function setStage(stage) {
  traceForm.value.stage = stage
}

function useLocation(location) {
  traceForm.value.location = location
}

function serializeDraftForm() {
  syncAttachmentState()
  return {
    stage: traceForm.value.stage,
    title: traceForm.value.title,
    eventTime: traceForm.value.eventTime,
    operatorName: traceForm.value.operatorName,
    location: traceForm.value.location,
    summary: traceForm.value.summary,
    imageUrl: traceForm.value.imageUrl,
    attachmentIds: [...traceForm.value.attachmentIds],
    uploadedFiles: sanitizeUploadedFiles(traceForm.value.uploadedFiles),
    visibleToConsumer: traceForm.value.visibleToConsumer
  }
}

function entryValidationError(mode = 'submit') {
  if (!selectedBatchId.value) {
    return '请先从待办批次进入录入。'
  }
  if (!traceForm.value.eventTime) {
    return '请补充记录时间。'
  }
  if (!traceForm.value.operatorName?.trim()) {
    return '请补充操作人。'
  }
  if (!traceForm.value.location?.trim()) {
    return '请补充地点信息。'
  }
  if (mode === 'submit' && !traceForm.value.summary?.trim()) {
    return '请填写现场说明。'
  }
  if (uploading.value) {
    return mode === 'submit'
      ? '图片仍在上传中，请等待上传完成后再提交。'
      : '图片仍在上传中，请稍等上传完成后再保存草稿。'
  }
  if (mode === 'submit' && hasFailedImages.value) {
    return '仍有上传失败的图片，请重试或删除后再提交。'
  }
  return ''
}

async function persistDraft(options = {}) {
  const { silent = false } = options
  const payload = serializeDraftForm()
  try {
    const response = await saveFieldDraftRequest(selectedBatchId.value, payload)
    const nextDraft = normalizeServerDraft(response.data)
    if (nextDraft) {
      cacheLocalDraft(nextDraft)
      draftMeta.value = nextDraft
    }
    await refreshDraftList()
    return nextDraft
  } catch (error) {
    const localDraft = saveLocalFieldDraft(authStore.user, selectedBatchId.value, {
      form: payload,
      batch: buildDraftBatchSnapshot()
    })
    draftMeta.value = normalizeLocalDraft(localDraft)
    await refreshDraftList()
    if (!silent) {
      ElMessage.warning(error?.response?.data?.message || '服务端草稿保存失败，已暂存到本机。')
    }
    return draftMeta.value
  }
}

async function saveDraftRecord() {
  if (draftSaving.value || saving.value) {
    return
  }
  const error = entryValidationError('draft')
  if (error) {
    ElMessage.warning(error)
    return
  }

  draftSaving.value = true
  try {
    await persistDraft({ silent: true })
    if (hasFailedImages.value) {
      ElMessage.warning('草稿已保存，上传失败的图片不会进入草稿，请稍后重试或重新选择。')
    } else {
      ElMessage.success('草稿已保存，可稍后继续编辑和提交。')
    }
  } finally {
    draftSaving.value = false
  }
}

async function discardDraft() {
  if (!draftMeta.value || !selectedBatchId.value) return
  if (!window.confirm('确认删除当前草稿吗？未提交内容会被清除。')) {
    return
  }

  removeLocalFieldDraft(authStore.user, selectedBatchId.value)
  try {
    await deleteFieldDraftRequest(selectedBatchId.value)
  } catch (error) {
    ElMessage.warning(error?.response?.data?.message || '服务端草稿删除失败，已先清理本机缓存。')
  }
  await refreshDraftList()
  await hydrateEntryForm(selectedBatchId.value, { ignoreDraft: true })
  ElMessage.success('草稿已删除')
}

async function deleteDraftItem(draft) {
  if (!draft || !window.confirm(`确认删除 ${draft.productName} 的草稿吗？`)) {
    return
  }
  removeLocalFieldDraft(authStore.user, draft.batchId)
  try {
    await deleteFieldDraftRequest(draft.batchId)
  } catch (error) {
    ElMessage.warning(error?.response?.data?.message || '服务端草稿删除失败，已先清理本机缓存。')
  }
  await refreshDraftList()
  if (String(selectedBatchId.value) === String(draft.batchId)) {
    draftMeta.value = null
  }
  ElMessage.success('草稿已删除')
}

function continueDraft(draft) {
  if (!draft) return
  listViewMode.value = 'drafts'
  openBatch(draft.batchId)
}

function fileLabel(file) {
  return file.fileName || file.fileUrl || '现场图片'
}

function formatFileSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function imageStatusLabel(status) {
  if (status === 'uploading') return '上传中'
  if (status === 'failed') return '上传失败'
  return '已就绪'
}

async function uploadImageItem(clientId) {
  const item = imageItems.value.find((entry) => entry.clientId === clientId)
  if (!item) return
  if (!item.localFile) {
    item.status = 'failed'
    item.errorMessage = '当前图片文件已失效，请重新选择后再上传。'
    return
  }

  item.status = 'uploading'
  item.progress = 2
  item.errorMessage = ''
  uploading.value = true

  try {
    const response = await uploadBatchFiles(IMAGE_BUSINESS_TYPE, [item.localFile], {
      onUploadProgress: (progressEvent) => {
        if (!progressEvent.total) {
          item.progress = Math.min(item.progress + 12, 92)
          return
        }
        const percent = Math.round((progressEvent.loaded / progressEvent.total) * 100)
        item.progress = Math.min(Math.max(percent, 2), 98)
      }
    })

    const uploadedFile = sanitizeUploadedFiles(response.data ?? [])[0]
    if (!uploadedFile) {
      throw new Error('上传完成但未返回图片结果')
    }

    const previousPreview = item.previewUrl
    const shouldRevokePreview = item.previewIsObjectUrl

    Object.assign(item, {
      ...createImageItemFromUpload(uploadedFile),
      clientId: item.clientId,
      localFile: null
    })

    if (shouldRevokePreview && previousPreview?.startsWith('blob:')) {
      URL.revokeObjectURL(previousPreview)
    }
    syncAttachmentState()
  } catch (error) {
    item.status = 'failed'
    item.progress = 0
    item.errorMessage = error?.response?.data?.message || error?.message || '图片上传失败，请重试。'
    syncAttachmentState()
    ElMessage.error(`${fileLabel(item)} 上传失败，可重试或删除。`)
  } finally {
    uploading.value = imageItems.value.some((entry) => entry.status === 'uploading')
  }
}

async function handleImageChange(event) {
  const files = Array.from(event.target.files || [])
  const remainingSlots = MAX_IMAGE_COUNT - imageItems.value.length

  if (!files.length) return
  if (remainingSlots <= 0) {
    ElMessage.warning(`现场图片最多保留 ${MAX_IMAGE_COUNT} 张，请先删除后再追加。`)
    event.target.value = ''
    return
  }

  const filesToUpload = files.slice(0, remainingSlots)
  if (filesToUpload.length < files.length) {
    ElMessage.warning(`已达到上限，仅保留前 ${remainingSlots} 张。`)
  }

  const pendingItems = filesToUpload.map(createLocalImageItem)
  imageItems.value = [...imageItems.value, ...pendingItems]
  syncAttachmentState()
  event.target.value = ''

  for (const item of pendingItems) {
    await uploadImageItem(item.clientId)
  }

  if (pendingItems.some((item) => item.status === 'uploaded')) {
    ElMessage.success('图片已加入当前作业，可继续排序、补说明或保存草稿。')
  }
}

function retryImageUpload(clientId) {
  const item = imageItems.value.find((entry) => entry.clientId === clientId)
  if (!item) return
  if (!item.localFile) {
    ElMessage.warning('原始图片文件已不可用，请重新选择后再上传。')
    return
  }
  uploadImageItem(clientId)
}

function removeImage(clientId) {
  const index = imageItems.value.findIndex((item) => item.clientId === clientId)
  if (index < 0) return
  const [removed] = imageItems.value.splice(index, 1)
  releaseImageItem(removed)
  syncAttachmentState()
}

function moveImage(clientId, direction) {
  const currentIndex = imageItems.value.findIndex((item) => item.clientId === clientId)
  const targetIndex = currentIndex + direction
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= imageItems.value.length) {
    return
  }
  const nextItems = [...imageItems.value]
  const [moved] = nextItems.splice(currentIndex, 1)
  nextItems.splice(targetIndex, 0, moved)
  imageItems.value = nextItems
  syncAttachmentState()
}

function buildNextEntryForm(sourceForm = traceForm.value) {
  return createEntryForm({
    stage: sourceForm.stage,
    operatorName: sourceForm.operatorName,
    location: sourceForm.location,
    visibleToConsumer: sourceForm.visibleToConsumer
  })
}

function continueNextEntry() {
  lastSuccess.value = null
  traceForm.value = buildNextEntryForm(traceForm.value)
  resetImageItems()
  showAdvancedFields.value = false
  formSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleSubmitSecondaryAction(actionKey) {
  if (actionKey === 'save-draft') {
    saveDraftRecord()
  }
}

async function submitFieldRecord() {
  if (saving.value || draftSaving.value) {
    return
  }
  const error = entryValidationError('submit')
  if (error) {
    ElMessage.warning(error)
    return
  }
  if (!traceForm.value.uploadedFiles?.length) {
    const confirmedWithoutImage = window.confirm('当前未上传现场图片，确认仍要提交记录吗？')
    if (!confirmedWithoutImage) {
      return
    }
  }
  if (!window.confirm('确认提交当前现场记录吗？提交后会同步到批次工作台。')) {
    return
  }

  saving.value = true
  const submittedForm = serializeDraftForm()

  try {
    const response = await createTraceRecord(selectedBatchId.value, {
      stage: traceForm.value.stage,
      title: traceForm.value.title,
      eventTime: traceForm.value.eventTime,
      operatorName: traceForm.value.operatorName,
      location: traceForm.value.location,
      summary: traceForm.value.summary,
      imageUrl: traceForm.value.imageUrl || traceForm.value.uploadedFiles[0]?.fileUrl || '',
      attachmentIds: traceForm.value.attachmentIds,
      visibleToConsumer: traceForm.value.visibleToConsumer
    })

    batchDetail.value = response.data ?? null
    lastSuccess.value = {
      batchId: selectedBatchId.value,
      record: response.data?.trace?.recentRecords?.[0] ?? null,
      imageCount: submittedForm.uploadedFiles.length,
      clearedDraft: Boolean(draftMeta.value)
    }

    removeLocalFieldDraft(authStore.user, selectedBatchId.value)
    draftMeta.value = null
    await refreshDraftList()
    traceForm.value = buildNextEntryForm(submittedForm)
    resetImageItems()

    ElMessage.success(lastSuccess.value.clearedDraft ? '记录已提交，草稿已自动清除。' : '现场记录已提交。')
    await loadBatches({ preserveSelection: true })
  } catch (error) {
    await persistDraft({ silent: true })
    ElMessage.error(`${error?.response?.data?.message || '现场记录提交失败'}，当前内容已保留为草稿。`)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="field-page" data-testid="field-entry-page">
    <template v-if="!selectedBatchId">
      <header class="list-shell" data-testid="field-entry-todo-view">
        <div class="list-shell-topbar">
          <span class="meta-tag subtle-tag">{{ fieldEntryAccountLabel }}</span>
          <button class="ghost-button compact" data-testid="field-entry-logout" @click="handleLogout">退出登录</button>
        </div>
        <div class="shell-copy">
          <p class="eyebrow">操作员手机端</p>
          <h1>现场作业工作台</h1>
        </div>
        <div class="summary-strip">
          <article>
            <small>待处理</small>
            <strong>{{ summaryStats.pendingCount }}</strong>
          </article>
          <article>
            <small>草稿待续</small>
            <strong>{{ summaryStats.draftCount }}</strong>
          </article>
          <article>
            <small>今日已完成</small>
            <strong>{{ summaryStats.doneTodayCount }}</strong>
          </article>
        </div>
        <div class="mode-switch">
          <button
            v-for="item in listModeOptions"
            :key="item.value"
            class="mode-button"
            :class="{ active: listViewMode === item.value }"
            :data-testid="`field-list-mode-${item.value}`"
            @click="listViewMode = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </header>

      <section v-if="loading" class="panel-card loading-card">正在同步我的现场任务...</section>

      <template v-else-if="listViewMode === 'todo'">
        <section class="filter-row">
          <button
            v-for="item in taskFilterOptions"
            :key="item.value"
            class="filter-chip"
            :class="{ active: taskFilter === item.value }"
            :data-testid="`field-task-filter-${item.value.toLowerCase()}`"
            @click="setTaskFilter(item.value)"
          >
            {{ item.label }}
            <span>{{ item.value === 'PENDING' ? summaryStats.pendingCount : item.value === 'DRAFT' ? summaryStats.draftCount : summaryStats.doneTodayCount }}</span>
          </button>
        </section>

        <section v-if="filteredTodoBatches.length" class="task-list">
          <article v-for="item in filteredTodoBatches" :key="item.id" class="task-row" data-testid="field-task-row">
            <div class="task-top">
              <div class="task-title">
                <p class="task-code">{{ item.batchCode }}</p>
                <h2>{{ item.productName }}</h2>
              </div>
              <span class="task-pill" :class="taskToneClass(item)">{{ resolveTaskStatusLabel(item) }}</span>
            </div>
            <div class="task-node">
              <small>当前待处理环节</small>
              <strong>{{ item.currentNode || '待补现场记录' }}</strong>
            </div>
            <div class="task-flags">
              <span class="meta-tag">{{ item.hasDraft ? '有草稿' : '无草稿' }}</span>
              <span class="meta-tag">{{ resolveTodayStatusText(item.todayCompleted) }}</span>
              <span v-if="item.assignedAt" class="meta-tag">分配于 {{ formatDraftTime(item.assignedAt) }}</span>
            </div>
            <dl class="task-meta">
              <div>
                <dt>分配时间</dt>
                <dd>{{ item.assignedAt ? formatDraftTime(item.assignedAt) : '未记录' }}</dd>
              </div>
              <div>
                <dt>最近更新时间</dt>
                <dd>{{ formatDraftTime(item.recentDisplayTime) }}</dd>
              </div>
            </dl>
            <div class="task-actions">
              <button class="primary-button" data-testid="field-todo-open-button" @click="openBatch(item.id)">
                {{ entryButtonText(item) }}
              </button>
              <button class="ghost-button" @click="openBatchWorkbench(item.id)">查看工作台</button>
            </div>
          </article>
        </section>

        <section v-else class="panel-card empty-card">
          <h2>当前看板下还没有待录任务</h2>
          <p>可以切换到草稿待续或今日已完成，继续查看其他批次。</p>
        </section>
      </template>

      <template v-else>
        <section v-if="draftSummaries.length" class="task-list" data-testid="field-draft-list">
          <article v-for="item in draftSummaries" :key="item.id" class="task-row draft-row" data-testid="field-draft-row">
            <div class="task-top">
              <div class="task-title">
                <p class="task-code">{{ item.batchCode }}</p>
                <h2>{{ item.productName }}</h2>
              </div>
              <span class="task-pill draft">草稿待续</span>
            </div>
            <dl class="task-meta">
              <div>
                <dt>批次</dt>
                <dd>{{ item.batchCode }}</dd>
              </div>
              <div>
                <dt>环节</dt>
                <dd>{{ item.stageLabel }}</dd>
              </div>
              <div>
                <dt>上次编辑</dt>
                <dd>{{ formatDraftTime(item.updatedAt) }}</dd>
              </div>
              <div>
                <dt>图片数</dt>
                <dd>{{ item.imageCount }} 张</dd>
              </div>
            </dl>
            <div class="task-node">
              <small>当前待续环节</small>
              <strong>{{ item.currentNode }}</strong>
            </div>
            <div class="task-actions">
              <button class="primary-button" data-testid="field-draft-open" @click="continueDraft(item)">继续编辑</button>
              <button class="ghost-button danger" data-testid="field-draft-delete" @click="deleteDraftItem(item)">删除草稿</button>
            </div>
          </article>
        </section>

        <section v-else class="panel-card empty-card">
          <h2>还没有待续草稿</h2>
          <p>现场记录保存为草稿后，会在这里继续编辑和提交。</p>
        </section>
      </template>
    </template>

    <template v-else>
      <header class="field-header">
        <div class="field-header-top">
          <button class="back-button" @click="closeBatch()">返回{{ listViewMode === 'drafts' ? '我的草稿' : '待办批次' }}</button>
          <button class="ghost-button compact" data-testid="field-entry-logout" @click="handleLogout">退出登录</button>
        </div>
        <div class="field-header-body">
          <div class="field-header-copy">
            <p class="eyebrow">现场作业</p>
            <h1>{{ currentProductName }}</h1>
            <p>{{ currentBatchCode }} / {{ currentCompanyName }}</p>
          </div>
          <div class="field-header-pills">
            <span class="pill">{{ currentStatusLabel }}</span>
            <span class="pill">{{ currentTaskStatusLabel }}</span>
            <span class="pill subtle">{{ currentNode }}</span>
            <span v-if="draftMeta" class="pill draft">草稿续写</span>
          </div>
        </div>
      </header>

      <section v-if="detailLoading && !batchDetail" class="shell-card loading-card">正在加载现场作业批次...</section>

      <template v-else>
        <section class="shell-card batch-card">
          <div class="section-head">
            <div>
              <h2>当前任务</h2>
            </div>
            <button class="ghost-button compact" @click="openBatchWorkbench()">查看批次工作台</button>
          </div>

          <div class="batch-summary">
            <div>
              <small>批次</small>
              <strong>{{ currentBatchCode }}</strong>
            </div>
            <div>
              <small>当前状态</small>
              <strong>{{ currentStatusLabel }}</strong>
            </div>
            <div>
              <small>待处理环节</small>
              <strong>{{ currentNode }}</strong>
            </div>
            <div>
              <small>任务分配</small>
              <strong>{{ currentAssigneeName || '未分配' }}</strong>
            </div>
            <div>
              <small>分配时间</small>
              <strong>{{ currentAssignedAt ? formatDraftTime(currentAssignedAt) : '未记录' }}</strong>
            </div>
            <div>
              <small>最近记录</small>
              <strong>{{ latestRecord?.eventTime || '暂无现场记录' }}</strong>
            </div>
          </div>
        </section>

        <section v-if="draftMeta" class="shell-card draft-banner" data-testid="field-entry-draft-banner">
          <div>
            <strong>正在续写未提交草稿</strong>
            <p>草稿保存于 {{ formatDraftTime(draftMeta.updatedAt) }}，当前文字和已上传图片都会继续保留。</p>
          </div>
          <button class="ghost-button compact danger" @click="discardDraft">删除草稿</button>
        </section>

        <fieldset class="entry-fieldset" :disabled="detailLoading">
          <section class="shell-card">
            <div class="section-head">
              <div>
                <h2>选择环节</h2>
              </div>
              <span class="pill">{{ formatStageLabel(traceForm.stage) }}</span>
            </div>
            <div class="stage-grid">
              <button
                v-for="item in stageOptions"
                :key="item.value"
                class="stage-chip"
                :class="{ active: traceForm.stage === item.value }"
                @click="setStage(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </section>

          <section ref="formSectionRef" class="shell-card form-card">
            <div class="section-head">
              <div>
                <h2>当前要录什么</h2>
              </div>
              <button class="ghost-button compact" type="button" @click="showAdvancedFields = !showAdvancedFields">
                {{ showAdvancedFields ? '收起更多设置' : '更多设置' }}
              </button>
            </div>

            <p v-if="detailLoading" class="field-note">正在载入批次详情，表单就绪后再开始填写。</p>

            <label class="full-width">
              <span>现场说明</span>
              <textarea
                v-model.trim="traceForm.summary"
                rows="4"
                maxlength="180"
                placeholder="例如：已完成分拣与装筐，现场照片已补齐，准备转入下一环节。"
              />
            </label>

            <label class="full-width">
              <span>现场图片</span>
              <div class="upload-box">
                <input type="file" accept="image/*" capture="environment" multiple @change="handleImageChange">
                <small>{{ uploadStatusText }}</small>
              </div>
            </label>

            <div v-if="imageItems.length" class="image-queue full-width" data-testid="field-image-queue">
              <div class="image-list" data-testid="field-entry-image-grid">
                <article v-for="(item, index) in imageItems" :key="item.clientId" class="image-row" :class="`is-${item.status}`">
                  <div class="image-preview">
                    <img v-if="item.previewUrl" :src="item.previewUrl" :alt="fileLabel(item)">
                    <div v-else class="image-fallback">{{ index + 1 }}</div>
                  </div>
                  <div class="image-body">
                    <div class="image-head">
                      <strong>{{ index + 1 }}. {{ fileLabel(item) }}</strong>
                      <span class="image-state" :class="item.status">{{ imageStatusLabel(item.status) }}</span>
                    </div>
                    <div v-if="item.status === 'uploading'" class="progress-track">
                      <span class="progress-value" :style="{ width: `${item.progress}%` }"></span>
                    </div>
                    <p v-if="item.errorMessage" class="image-error">{{ item.errorMessage }}</p>
                    <small>{{ formatFileSize(item.size) }}</small>
                    <div class="image-actions">
                      <button class="ghost-chip" :disabled="index === 0 || item.status === 'uploading'" @click="moveImage(item.clientId, -1)">上移</button>
                      <button class="ghost-chip" :disabled="index === imageItems.length - 1 || item.status === 'uploading'" @click="moveImage(item.clientId, 1)">下移</button>
                      <button v-if="item.status === 'failed'" class="ghost-chip warning" @click="retryImageUpload(item.clientId)">重试</button>
                      <button class="ghost-chip danger" @click="removeImage(item.clientId)">删除</button>
                    </div>
                  </div>
                </article>
              </div>
            </div>

            <section v-if="showAdvancedFields" class="advanced-fields full-width">
              <label>
                <span>记录标题</span>
                <input v-model.trim="traceForm.title" type="text" placeholder="系统已带入默认标题，也可以手动调整">
              </label>

              <label>
                <span>记录时间</span>
                <input v-model="traceForm.eventTime" type="datetime-local">
              </label>

              <label>
                <span>地点</span>
                <input v-model.trim="traceForm.location" type="text" placeholder="填写当前作业地点">
              </label>

              <label>
                <span>操作人</span>
                <input v-model.trim="traceForm.operatorName" type="text" placeholder="填写本次操作人">
              </label>

              <div class="quick-locations full-width">
                <button
                  v-for="location in currentProfile.locations"
                  :key="location"
                  class="ghost-chip"
                  @click="useLocation(location)"
                >
                  {{ location }}
                </button>
              </div>

              <label class="switch-row full-width">
                <input v-model="traceForm.visibleToConsumer" type="checkbox">
                <span>同步展示到消费者追溯页</span>
              </label>
            </section>
          </section>
        </fieldset>

        <section v-if="lastSuccess" class="shell-card success-card" data-testid="field-entry-success">
          <div class="section-head">
            <div>
              <h2>提交成功</h2>
            </div>
            <span class="pill success">已提交</span>
          </div>
          <article class="latest-card">
            <strong>{{ lastSuccess.record?.title || '现场记录已提交' }}</strong>
            <small>{{ lastSuccess.record?.eventTime }} / {{ lastSuccess.record?.operatorName }}</small>
            <p>{{ lastSuccess.record?.summary }}</p>
          </article>
          <div class="success-actions">
            <button class="primary-button" @click="continueNextEntry">继续补下一条</button>
            <button class="ghost-button" @click="closeBatch()">返回待办批次</button>
            <button class="ghost-button" @click="openBatchWorkbench()">查看批次工作台</button>
          </div>
        </section>
      </template>

      <footer class="submit-bar">
        <div>
          <strong>{{ currentBatchCode }}</strong>
          <small>{{ uploading ? uploadStatusText : draftMeta ? `草稿最近编辑于 ${formatDraftTime(draftMeta.updatedAt)}` : hasFailedImages ? uploadStatusText : '提交后会直接同步到批次工作台' }}</small>
        </div>
        <div class="submit-actions">
          <PrimaryActionGroup
            :primary-label="saving ? '正在提交...' : '提交记录'"
            primary-class="primary"
            :primary-disabled="Boolean(submitBlockReason) || saving || draftSaving || detailLoading"
            primary-testid="field-entry-submit"
            :primary-hint="submitPrimaryHint"
            :secondary-actions="submitSecondaryActions"
            @primary-click="submitFieldRecord"
            @secondary-click="handleSubmitSecondaryAction"
          />
        </div>
      </footer>
    </template>
  </div>
</template>

<style scoped>
.field-page {
  min-height: 100vh;
  padding: 18px 14px 120px;
  background:
    radial-gradient(circle at top left, rgba(76, 163, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #eef5ff 0%, #f7fbff 24%, #ffffff 100%);
}

.hero-card,
.list-shell,
.field-header,
.shell-card,
.panel-card,
.submit-bar,
.filter-row,
.task-list {
  width: min(100%, 560px);
  margin: 0 auto;
}

.hero-card,
.list-shell,
.shell-card,
.panel-card,
.submit-bar {
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 42px rgba(19, 75, 135, 0.1);
}

.hero-card,
.list-shell,
.panel-card {
  display: grid;
  gap: 18px;
  padding: 22px 20px;
  margin-bottom: 16px;
}

.hero-copy h1,
.shell-copy h1,
.field-header h1,
.section-head h2 {
  margin: 0;
  color: var(--admin-text);
}

.hero-copy p:last-child,
.shell-copy p:last-child,
.field-header-copy p:last-child,
.section-head p,
.todo-copy,
.task-copy,
.latest-card p {
  margin: 8px 0 0;
  color: var(--admin-text-soft);
  line-height: 1.7;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hero-stats article,
.batch-summary div,
.todo-meta div,
.latest-card {
  padding: 14px;
  border-radius: 18px;
  background: rgba(243, 249, 255, 0.95);
}

.hero-stats small,
.batch-summary small,
.todo-meta small,
.latest-card small,
.image-meta small {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
}

.hero-stats strong,
.batch-summary strong,
.todo-meta strong,
.latest-card strong,
.image-meta strong {
  display: block;
  margin-top: 6px;
  color: var(--admin-text);
}

.eyebrow {
  margin: 0 0 10px;
  color: var(--admin-primary-deep);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.summary-strip,
.task-meta,
.image-queue,
.image-list,
.entry-guide-grid,
.advanced-fields {
  display: grid;
  gap: 12px;
}

.summary-strip {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.summary-strip article,
.task-node,
.task-meta div,
.image-row {
  padding: 14px;
  border-radius: 18px;
  background: rgba(243, 249, 255, 0.95);
}

.summary-strip small,
.task-node small,
.task-meta dt,
.image-body small {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
}

.summary-strip strong,
.task-node strong,
.task-meta dd {
  display: block;
  margin-top: 6px;
  color: var(--admin-text);
}

.mode-switch,
.filter-row,
.task-actions,
.task-flags,
.image-actions,
.header-pills,
.field-header-top,
.list-shell-topbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.list-shell-topbar {
  align-items: center;
  justify-content: space-between;
}

.mode-switch,
.filter-row {
  margin-bottom: 14px;
}

.mode-button,
.filter-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 999px;
  background: #fff;
  color: var(--admin-primary-deep);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.mode-button.active,
.filter-chip.active {
  background: var(--admin-primary);
  color: #fff;
  box-shadow: 0 12px 26px rgba(56, 134, 217, 0.2);
}

.filter-chip span {
  margin-left: 8px;
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(15, 39, 68, 0.08);
  font-size: 12px;
}

.task-list {
  display: grid;
  gap: 14px;
}

.task-row {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 36px rgba(19, 75, 135, 0.08);
}

.task-top,
.image-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.task-title h2,
.task-code,
.task-meta dt,
.task-meta dd {
  margin: 0;
}

.task-code {
  color: var(--admin-primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.task-copy {
  margin: 0;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 39, 68, 0.06);
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.task-meta {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.image-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
}

.image-preview {
  overflow: hidden;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(56, 134, 217, 0.14), rgba(98, 181, 130, 0.14));
}

.image-preview img {
  width: 100%;
  height: 92px;
  display: block;
  object-fit: cover;
}

.image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 92px;
  color: var(--admin-primary-deep);
  font-size: 22px;
  font-weight: 700;
}

.image-body {
  display: grid;
  gap: 8px;
}

.image-state {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 39, 68, 0.06);
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.image-state.failed {
  background: rgba(248, 193, 73, 0.18);
  color: #946200;
}

.image-state.uploaded {
  background: rgba(31, 159, 102, 0.14);
  color: #17784f;
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(56, 134, 217, 0.12);
}

.progress-value {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #3b86f6 0%, #4fbf87 100%);
}

.image-error {
  margin: 0;
  color: #9a5400;
  font-size: 13px;
}

.todo-stack {
  width: min(100%, 560px);
  margin: 0 auto;
  display: grid;
  gap: 14px;
}

.todo-card {
  display: grid;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 36px rgba(19, 75, 135, 0.08);
}

.todo-cover {
  overflow: hidden;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(56, 134, 217, 0.14), rgba(98, 181, 130, 0.14));
}

.todo-cover img,
.image-card img {
  width: 100%;
  display: block;
  object-fit: cover;
}

.todo-cover img {
  height: 150px;
}

.todo-cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 150px;
  color: var(--admin-primary-deep);
  font-size: 30px;
  font-weight: 700;
}

.todo-body {
  display: grid;
  gap: 12px;
}

.todo-topline,
.section-head,
.field-header-body,
.field-header-pills,
.success-actions,
.submit-actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.todo-code {
  margin: 0;
  color: var(--admin-primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.todo-card h2 {
  margin: 0;
  color: var(--admin-text);
  font-size: 22px;
}

.task-pill,
.pill,
.mini-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.task-pill.draft,
.mini-tag.draft,
.pill.draft {
  background: rgba(248, 193, 73, 0.18);
  color: #946200;
}

.task-pill.warning {
  background: rgba(240, 139, 51, 0.16);
  color: #a54d12;
}

.task-pill.pending {
  background: rgba(15, 39, 68, 0.06);
  color: var(--admin-text-soft);
}

.task-pill.risk {
  background: rgba(224, 73, 73, 0.14);
  color: #a33030;
}

.task-pill.done,
.task-pill.ready,
.pill.success {
  background: rgba(31, 159, 102, 0.14);
  color: #17784f;
}

.submit-actions {
  min-width: min(320px, 100%);
}

.submit-actions :deep(.primary-action-group) {
  width: min(320px, 100%);
}

.submit-actions :deep(.action-primary),
.submit-actions :deep(.secondary-menu) {
  width: 100%;
}

.pill {
  background: rgba(56, 134, 217, 0.12);
  color: var(--admin-primary-deep);
}

.pill.subtle {
  background: rgba(15, 39, 68, 0.06);
  color: var(--admin-text-soft);
}

.tag-row,
.todo-actions,
.stage-grid,
.quick-locations,
.success-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.mini-tag {
  background: rgba(15, 39, 68, 0.06);
  color: var(--admin-text-soft);
}

.todo-meta,
.batch-summary,
.form-card {
  display: grid;
  gap: 12px;
}

.entry-fieldset {
  margin: 0;
  padding: 0;
  border: none;
  min-width: 0;
}

.field-header {
  display: grid;
  gap: 14px;
  margin-bottom: 14px;
}

.field-header-top {
  align-items: center;
  justify-content: space-between;
}

.field-header-copy p:last-child {
  margin-top: 6px;
}

.shell-card {
  margin-bottom: 14px;
  padding: 18px;
}

.batch-summary,
.todo-meta {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.pending-list {
  margin: 14px 0 0;
  padding-left: 18px;
  color: #52779a;
  display: grid;
  gap: 8px;
}

.demo-path-card {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 18px;
  background: rgba(243, 249, 255, 0.95);
}

.demo-path-card small,
.advanced-summary small,
.entry-guide-card small {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
}

.demo-path-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.demo-path-flow span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(56, 134, 217, 0.12);
  color: var(--admin-primary-deep);
  font-size: 12px;
  font-weight: 700;
}

.subtle-tag {
  background: rgba(15, 39, 68, 0.06);
  color: var(--admin-text-soft);
}

.entry-guide-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 4px;
}

.entry-guide-card,
.advanced-summary {
  padding: 14px;
  border-radius: 18px;
  background: rgba(243, 249, 255, 0.95);
}

.entry-guide-card strong,
.advanced-summary strong {
  display: block;
  margin-top: 6px;
  color: var(--admin-text);
}

.entry-guide-card p {
  margin: 8px 0 0;
  color: var(--admin-text-soft);
  line-height: 1.6;
}

.advanced-summary {
  border: 1px dashed rgba(56, 134, 217, 0.2);
}

.advanced-summary strong {
  font-size: 13px;
  line-height: 1.7;
}

.advanced-fields {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding-top: 4px;
}

.draft-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-color: rgba(248, 193, 73, 0.28);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 248, 230, 0.96) 100%);
}

label {
  display: grid;
  gap: 8px;
}

label span {
  color: var(--admin-text-soft);
  font-size: 13px;
}

input,
textarea {
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 14px;
  background: #fff;
  color: var(--admin-text);
}

textarea {
  min-height: 112px;
  padding: 14px;
  resize: vertical;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 46px;
  padding: 12px 14px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 14px;
  background: #fff;
}

.switch-row input {
  width: 18px;
  min-height: 18px;
  margin: 0;
}

.back-button,
.ghost-button,
.primary-button,
.ghost-chip,
.stage-chip,
.image-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
}

.back-button,
.ghost-button,
.ghost-chip,
.image-remove {
  background: #fff;
  border-color: rgba(56, 134, 217, 0.18);
  color: var(--admin-primary-deep);
}

.back-button:hover,
.ghost-button:hover,
.primary-button:hover,
.ghost-chip:hover,
.stage-chip:hover,
.image-remove:hover {
  transform: translateY(-1px);
}

.ghost-button.danger,
.ghost-chip.danger {
  border-color: rgba(224, 73, 73, 0.2);
  color: #a33030;
}

.ghost-chip.warning {
  border-color: rgba(248, 193, 73, 0.28);
  color: #946200;
}

.compact {
  min-height: 38px;
  padding: 0 14px;
}

.primary-button {
  background: var(--admin-primary);
  color: #fff;
  box-shadow: 0 12px 26px rgba(56, 134, 217, 0.2);
}

.primary-button:disabled,
.ghost-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.stage-chip {
  background: rgba(243, 249, 255, 0.95);
  color: var(--admin-primary-deep);
}

.stage-chip.active {
  background: var(--admin-primary);
  color: #fff;
  box-shadow: 0 12px 26px rgba(56, 134, 217, 0.2);
}

.upload-box {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px dashed rgba(56, 134, 217, 0.26);
  border-radius: 16px;
  background: rgba(243, 249, 255, 0.95);
}

.upload-box small,
.upload-hint {
  color: var(--admin-text-soft);
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.image-card {
  overflow: hidden;
  border-radius: 18px;
  background: rgba(243, 249, 255, 0.95);
}

.image-card img {
  height: 134px;
}

.image-meta {
  padding: 12px 12px 0;
}

.image-remove {
  width: calc(100% - 24px);
  margin: 12px;
}

.success-card {
  border-color: rgba(31, 159, 102, 0.2);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(241, 251, 246, 0.96) 100%);
}

.submit-bar {
  position: fixed;
  left: 50%;
  bottom: max(12px, env(safe-area-inset-bottom));
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.submit-bar strong {
  display: block;
  color: var(--admin-text);
}

.submit-bar small {
  display: block;
  margin-top: 4px;
  color: var(--admin-text-soft);
  font-size: 12px;
}

.loading-card,
.empty-card {
  text-align: center;
}

.empty-card h2 {
  margin: 0;
  color: var(--admin-text);
}

.empty-card p {
  margin: 10px 0 0;
  color: var(--admin-text-soft);
  line-height: 1.7;
}

.full-width {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .submit-bar {
    width: calc(100% - 24px);
    align-items: stretch;
    flex-direction: column;
  }

  .submit-actions {
    width: 100%;
  }

  .submit-actions :deep(.primary-action-group) {
    width: 100%;
  }
}

@media (max-width: 560px) {
  .field-page {
    padding: 14px 12px 124px;
  }

  .hero-stats,
  .summary-strip,
  .task-meta,
  .batch-summary,
  .todo-meta,
  .image-row,
  .image-grid,
  .entry-guide-grid,
  .advanced-fields {
    grid-template-columns: 1fr;
  }

  .todo-card h2,
  .field-header h1 {
    font-size: 24px;
  }

  .draft-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .field-header-top,
  .list-shell-topbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
