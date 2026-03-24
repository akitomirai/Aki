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
const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const riskForm = ref(createRiskForm())
const statusForm = ref(createStatusForm())

const formCompanyOptions = ref([])
const formProductOptions = ref([])
const traceUploading = ref(false)
const qualityUploading = ref(false)

const stageOptions = [
  { value: 'ARCHIVE', label: 'Archive' },
  { value: 'PRODUCE', label: 'Produce' },
  { value: 'QUALITY', label: 'Quality' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'WAREHOUSE', label: 'Warehouse' },
  { value: 'DELIVERY', label: 'Delivery' },
  { value: 'MARKET', label: 'Market' },
  { value: 'REGULATION', label: 'Regulation' }
]

const qualityOptions = [
  { value: 'PASS', label: 'Pass' },
  { value: 'FAIL', label: 'Fail' },
  { value: 'REVIEW', label: 'Review' }
]

const riskActionOptions = [
  { value: 'COMMENT', label: 'Handling comment' },
  { value: 'RECTIFICATION', label: 'Rectification record' },
  { value: 'PROCESSING', label: 'Mark processing' },
  { value: 'RECTIFIED', label: 'Mark rectified' }
]

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'edit':
      return 'Edit batch'
    case 'trace':
      return 'Add trace record'
    case 'quality':
      return 'Upload quality'
    case 'status':
      return 'Change status'
    case 'risk':
      return 'Update risk handling'
    default:
      return ''
  }
})

const canPreviewPublic = computed(() => detail.value?.qr?.generated && detail.value?.qr?.publicUrl)

const selectedProductOption = computed(() => {
  return formProductOptions.value.find((item) => item.id === batchForm.value.productId) ?? null
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
    showMessage(getErrorMessage(error), 'error')
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
  return {
    stage: 'PRODUCE',
    title: '',
    eventTime: currentDateTime(),
    operatorName: 'Entry Operator',
    location: '',
    summary: '',
    imageUrl: '',
    attachmentIds: [],
    uploadedFiles: [],
    visibleToConsumer: true
  }
}

function createQualityForm() {
  return {
    reportNo: '',
    agency: '',
    result: 'PASS',
    reportTime: currentDateTime(),
    highlightsText: 'passed',
    attachmentIds: [],
    uploadedFiles: []
  }
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

function currentDateTime() {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
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
  traceForm.value = createTraceForm()
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

async function submitDialog() {
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
    showMessage(response.message, 'success')
    closeDialog()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function handleQrAction() {
  try {
    const response = await generateBatchQr(route.params.id)
    detail.value = response.data
    showMessage(response.message, 'success')
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
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
    showMessage(getErrorMessage(error), 'error')
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
    showMessage('Trace image uploaded', 'success')
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
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
    showMessage('Quality attachment uploaded', 'success')
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
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

function getErrorMessage(error) {
  return error?.response?.data?.message || error?.message || 'Operation failed. Please try again.'
}

function defaultReason(targetStatus) {
  return {
    PUBLISHED: 'Quality and QR are ready for publish.',
    FROZEN: 'An issue was found and the batch is frozen for review.',
    RECALLED: 'A risk was confirmed and recall is required.'
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
  return file?.businessId ? 'Bound' : 'Pending bind'
}

function uploadStateClass(file) {
  return file?.businessId ? 'bound' : 'pending'
}

function canHandleRisk() {
  return ['FROZEN', 'RECALLED'].includes(detail.value?.status?.code)
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
            <p class="eyebrow">Batch workbench</p>
            <h1>{{ detail.product.name }}</h1>
            <p class="batch-code">{{ detail.batch.batchCode }}</p>
            <p class="hero-intro">{{ detail.batch.publicRemark || 'This page keeps the batch flow focused on trace, quality, QR and scan stats.' }}</p>

            <div class="hero-grid">
              <div>
                <span>Company</span>
                <strong>{{ detail.company.name }}</strong>
              </div>
              <div>
                <span>Origin</span>
                <strong>{{ detail.batch.originPlace }}</strong>
              </div>
              <div>
                <span>Production date</span>
                <strong>{{ detail.batch.productionDate }}</strong>
              </div>
              <div>
                <span>Market date</span>
                <strong>{{ detail.batch.marketDate || 'Not published' }}</strong>
              </div>
              <div>
                <span>Product spec</span>
                <strong>{{ detail.product.specification || 'N/A' }}</strong>
              </div>
              <div>
                <span>Quality</span>
                <strong>{{ detail.quality.label }}</strong>
              </div>
            </div>
          </div>
        </div>

        <aside class="hero-side">
          <span class="status-badge" :class="statusClass(detail.status.code)">{{ detail.status.label }}</span>
          <p><strong>Current node:</strong> {{ detail.status.currentNode }}</p>
          <p><strong>Status reason:</strong> {{ detail.status.reason || 'No extra risk note.' }}</p>
          <p><strong>Last operator:</strong> {{ detail.status.operatorName || 'N/A' }}</p>
          <p><strong>Changed at:</strong> {{ detail.status.changedAt || 'N/A' }}</p>
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
          <p class="eyebrow">Risk status</p>
          <h2>{{ detail.risk.title }}</h2>
          <p class="risk-copy">{{ detail.risk.reason }}</p>
        </div>
        <div class="risk-meta">
          <p><strong>Status:</strong> {{ detail.risk.status }}</p>
          <p><strong>Updated:</strong> {{ detail.risk.updatedAt || detail.status.changedAt || 'N/A' }}</p>
          <p><strong>Hint:</strong> {{ detail.risk.tip }}</p>
        </div>
      </section>

      <section class="quick-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Quick actions</p>
            <h2>Keep high-frequency actions visible</h2>
          </div>
          <button class="ghost" @click="router.push('/batches')">Back to list</button>
        </div>

        <div class="quick-actions">
          <button class="ghost" @click="openEditDialog">Edit batch</button>
          <button class="primary" @click="openTraceDialog">Add trace</button>
          <button class="success" @click="openQualityDialog">Upload quality</button>
          <button
            :class="actionClass('GENERATE_QR')"
            :disabled="!actionOf('GENERATE_QR').enabled"
            :title="actionOf('GENERATE_QR').hint"
            @click="handleQrAction"
          >
            Generate QR
          </button>
          <button
            :class="actionClass('PUBLISH')"
            :disabled="!actionOf('PUBLISH').enabled"
            :title="actionOf('PUBLISH').hint"
            @click="openStatusDialog('PUBLISHED')"
          >
            Publish
          </button>
          <button
            :class="actionClass('RESUME')"
            :disabled="!actionOf('RESUME').enabled"
            :title="actionOf('RESUME').hint"
            @click="openStatusDialog('PUBLISHED')"
          >
            Resume
          </button>
          <button
            :class="actionClass('FREEZE')"
            :disabled="!actionOf('FREEZE').enabled"
            :title="actionOf('FREEZE').hint"
            @click="openStatusDialog('FROZEN')"
          >
            Freeze
          </button>
          <button
            :class="actionClass('RECALL')"
            :disabled="!actionOf('RECALL').enabled"
            :title="actionOf('RECALL').hint"
            @click="openStatusDialog('RECALLED')"
          >
            Recall
          </button>
          <a v-if="canPreviewPublic" class="preview-link" :href="detail.qr.publicUrl" target="_blank" rel="noreferrer">
            Public preview
          </a>
          <button class="ghost" @click="handleAttachmentCleanup">Run file cleanup</button>
        </div>
      </section>

      <section class="grid">
        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Trace records</p>
              <h2>Recent records and fast entry</h2>
            </div>
            <button class="ghost" @click="openTraceDialog">Add more</button>
          </div>

          <div class="trace-summary">
            <div>
              <span>Total records</span>
              <strong>{{ detail.trace.totalCount }}</strong>
            </div>
            <div>
              <span>Latest update</span>
              <strong>{{ detail.trace.latestRecordedAt || 'No record yet' }}</strong>
            </div>
            <div>
              <span>Hint</span>
              <strong>{{ detail.trace.quickEntryHint }}</strong>
            </div>
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
              <small>{{ item.visibleToConsumer ? 'Public page visible' : 'Internal only' }}</small>
            </article>
          </div>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Quality</p>
              <h2>High-value quality summary</h2>
            </div>
            <button class="ghost" @click="openQualityDialog">Upload new report</button>
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
              <p class="eyebrow">QR</p>
              <h2>Stable QR for workbench and public page</h2>
            </div>
            <button
              class="ghost"
              :disabled="!actionOf('GENERATE_QR').enabled"
              @click="handleQrAction"
            >
              Generate QR
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
            Open the public page and verify the first screen is readable within 3 seconds.
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
              <p class="eyebrow">Risk handling</p>
              <h2>Close the loop before resume</h2>
            </div>
          </div>

          <div class="scan-grid">
            <div>
              <span>Current stage</span>
              <strong>{{ detail.riskHandling.currentStageLabel }}</strong>
            </div>
            <div>
              <span>Can resume</span>
              <strong>{{ detail.riskHandling.canResume ? 'Yes' : 'Not yet' }}</strong>
            </div>
            <div>
              <span>Current risk</span>
              <strong>{{ detail.risk.title }}</strong>
            </div>
          </div>

          <div class="quick-actions risk-actions">
            <button class="ghost" :disabled="!canHandleRisk()" @click="openRiskDialog('COMMENT')">Add handling comment</button>
            <button class="ghost" :disabled="!canHandleRisk()" @click="openRiskDialog('RECTIFICATION')">Add rectification</button>
            <button class="warning" :disabled="!canHandleRisk()" @click="openRiskDialog('PROCESSING')">Mark processing</button>
            <button class="success" :disabled="!canHandleRisk()" @click="openRiskDialog('RECTIFIED')">Mark rectified</button>
          </div>

          <ul class="risk-history">
            <li v-if="!detail.riskHandling.history.length" class="risk-history-empty">
              No handling record yet. Add a comment and rectification step before trying to resume.
            </li>
            <li v-for="item in detail.riskHandling.history" :key="item.id">
              <div>
                <strong>{{ item.actionLabel }}</strong>
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
              <p class="eyebrow">Status log</p>
              <h2>Reason, operator and timestamp</h2>
            </div>
          </div>

          <ol class="status-timeline">
            <li v-for="item in detail.statusHistory" :key="`${item.status}-${item.operatedAt}`">
              <span class="status-dot" :class="statusClass(item.status)" />
              <div>
                <h3>{{ item.status }}</h3>
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
          <label>
            <span>Stage</span>
            <select v-model="traceForm.stage">
              <option v-for="item in stageOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>Event time</span>
            <input v-model="traceForm.eventTime" type="datetime-local">
          </label>
          <label>
            <span>Operator</span>
            <input v-model.trim="traceForm.operatorName" type="text">
          </label>
          <label>
            <span>Location</span>
            <input v-model.trim="traceForm.location" type="text">
          </label>
          <label class="full-width">
            <span>Title</span>
            <input v-model.trim="traceForm.title" type="text">
          </label>
          <label class="full-width">
            <span>Summary</span>
            <textarea v-model.trim="traceForm.summary" rows="4"></textarea>
          </label>
          <label class="full-width">
            <span>Trace images</span>
            <div class="upload-box">
              <input type="file" accept="image/*" multiple @change="handleTraceFilesChange">
              <small>Only PNG, JPG, JPEG, WEBP or GIF are allowed for trace images.</small>
            </div>
          </label>
          <label class="full-width">
            <span>Image URL fallback</span>
            <input v-model.trim="traceForm.imageUrl" type="url" placeholder="Optional external image URL">
          </label>
          <div v-if="traceUploading" class="full-width upload-hint">Uploading trace images...</div>
          <div v-if="traceForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in traceForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }} · {{ uploadStateLabel(item) }}</small>
              </div>
              <div class="inline-actions">
                <span class="upload-state" :class="uploadStateClass(item)">{{ uploadStateLabel(item) }}</span>
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">Open</a>
                <button class="ghost" @click="removeTraceAttachment(item.id)">Remove</button>
              </div>
            </article>
          </div>
          <label class="checkbox-field full-width">
            <input v-model="traceForm.visibleToConsumer" type="checkbox">
            <span>Show this record on the public page</span>
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
          <button class="ghost" @click="closeDialog">Cancel</button>
          <button class="primary" @click="submitDialog">Submit</button>
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

  .risk-history li {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 30px;
  }

  .hero-grid,
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
