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

const router = useRouter()

const loading = ref(false)
const batches = ref([])
const message = ref('')
const messageType = ref('info')

const filters = ref(createFilterState())
const dialog = ref(createDialogState())
const batchForm = ref(createBatchForm())
const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const statusForm = ref(createStatusForm())

const formCompanyOptions = ref([])
const formProductOptions = ref([])
const traceUploading = ref(false)
const qualityUploading = ref(false)

const statusOptions = [
  { value: '', label: 'All status' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'FROZEN', label: 'Frozen' },
  { value: 'RECALLED', label: 'Recalled' }
]

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

const companyOptions = computed(() => {
  const source = formCompanyOptions.value.length
    ? formCompanyOptions.value.map((item) => item.name)
    : batches.value.map((item) => item.companyName)
  return [...new Set(source.filter(Boolean))]
})

const listStats = computed(() => {
  const total = batches.value.length
  const published = batches.value.filter((item) => item.status === 'PUBLISHED').length
  const draft = batches.value.filter((item) => item.status === 'DRAFT').length
  const risk = batches.value.filter((item) => ['FROZEN', 'RECALLED'].includes(item.status)).length
  return { total, published, draft, risk }
})

const selectedProductOption = computed(() => {
  return formProductOptions.value.find((item) => item.id === batchForm.value.productId) ?? null
})

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'create':
      return 'Create batch'
    case 'edit':
      return `Edit batch: ${dialog.value.batchName}`
    case 'trace':
      return `Add trace record: ${dialog.value.batchName}`
    case 'quality':
      return `Upload quality summary: ${dialog.value.batchName}`
    case 'status':
      return `Change status: ${dialog.value.batchName}`
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
    showMessage(getErrorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

async function loadCompanyOptions(keyword = '') {
  const response = await getCompanyOptions(cleanObject({ keyword }))
  formCompanyOptions.value = response.data ?? []
}

async function loadProductOptions(companyId, keyword = '') {
  if (!companyId) {
    formProductOptions.value = []
    return
  }
  const response = await getProductOptions(cleanObject({ companyId, keyword }))
  formProductOptions.value = response.data ?? []
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

function createStatusForm(targetStatus = 'PUBLISHED') {
  return {
    targetStatus,
    reason: defaultReason(targetStatus),
    operatorName: 'Admin'
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
    showMessage(getErrorMessage(error), 'error')
  }
}

function openTraceDialog(item) {
  traceForm.value = createTraceForm()
  dialog.value = {
    visible: true,
    type: 'trace',
    batchId: item.id,
    batchName: item.batchCode
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

async function submitDialog() {
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
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      router.push(`/batches/${response.data.batch.id}`)
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
      showMessage(response.message, 'success')
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
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      return
    }

    if (dialog.value.type === 'quality') {
      const response = await createQualityReport(dialog.value.batchId, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlights(qualityForm.value.highlightsText),
        attachmentIds: qualityForm.value.attachmentIds
      })
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      return
    }

    if (dialog.value.type === 'status') {
      const response = await changeBatchStatus(dialog.value.batchId, statusForm.value)
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      router.push(`/batches/${response.data.batch.id}`)
    }
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function handleGenerateQr(item) {
  try {
    const response = await generateBatchQr(item.id)
    showMessage(response.message, 'success')
    await fetchBatches()
    router.push(`/batches/${item.id}`)
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

function splitHighlights(text) {
  return text
    .split(/[\n,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function createBatchCode() {
  const now = new Date()
  return `BATCH${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`
}

function todayString() {
  return new Date().toISOString().slice(0, 10)
}

function currentDateTime() {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
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

function actionOf(item, code) {
  return item.actions?.find((action) => action.code === code) ?? {
    code,
    label: code,
    enabled: true,
    hint: '',
    variant: 'neutral'
  }
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[status] ?? 'draft'
}

function qrStatusLabel(status) {
  return status && status !== 'NOT_GENERATED' ? 'Ready' : 'Pending'
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
</script>

<template>
  <div class="page-shell">
    <header class="hero-card">
      <div>
        <p class="eyebrow">Batch center</p>
        <h1>Keep the list as the main entry</h1>
        <p class="lead">
          This page keeps the batch-centered flow simple: choose company and product,
          create a batch, add trace records, upload quality files, generate QR, and publish.
        </p>
      </div>
      <div class="hero-actions">
        <button class="primary" @click="openCreateDialog">Create batch</button>
        <button class="ghost" @click="router.push('/dashboard')">Back to dashboard</button>
      </div>
    </header>

    <section class="stats-grid">
      <article class="stat-card">
        <span>Total</span>
        <strong>{{ listStats.total }}</strong>
      </article>
      <article class="stat-card">
        <span>Published</span>
        <strong>{{ listStats.published }}</strong>
      </article>
      <article class="stat-card">
        <span>Draft</span>
        <strong>{{ listStats.draft }}</strong>
      </article>
      <article class="stat-card warning">
        <span>Risk</span>
        <strong>{{ listStats.risk }}</strong>
      </article>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Filters</p>
          <h2>Find the batch to process</h2>
        </div>
      </div>

      <div class="filter-grid">
        <label>
          <span>Batch code</span>
          <input v-model.trim="filters.batchCode" type="text" placeholder="BATCH2026032401">
        </label>
        <label>
          <span>Product</span>
          <input v-model.trim="filters.productName" type="text" placeholder="navel orange">
        </label>
        <label>
          <span>Status</span>
          <select v-model="filters.status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label>
          <span>Company</span>
          <input
            v-model.trim="filters.companyName"
            type="text"
            list="company-options"
            placeholder="Type or select company"
          >
          <datalist id="company-options">
            <option v-for="item in companyOptions" :key="item" :value="item" />
          </datalist>
        </label>
        <label>
          <span>Date from</span>
          <input v-model="filters.dateFrom" type="date">
        </label>
        <label>
          <span>Date to</span>
          <input v-model="filters.dateTo" type="date">
        </label>
      </div>

      <div class="toolbar">
        <button class="primary" :disabled="loading" @click="fetchBatches">Search</button>
        <button class="ghost" :disabled="loading" @click="resetFilters">Reset</button>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">
      {{ message }}
    </section>

    <section v-if="loading" class="panel empty-state">
      Loading batches...
    </section>

    <section v-else-if="!batches.length" class="panel empty-state">
      No batch matched the filter. Create one to continue the main flow.
    </section>

    <section v-else class="batch-list">
      <article v-for="item in batches" :key="item.id" class="batch-card">
        <div class="batch-main">
          <img class="product-image" :src="item.productImageUrl" :alt="item.productName">
          <div class="batch-copy">
            <div class="title-row">
              <div>
                <p class="batch-code">{{ item.batchCode }}</p>
                <h3>{{ item.productName }}</h3>
              </div>
              <span class="status-badge" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
            </div>

            <div class="meta-grid">
              <div>
                <span>Company</span>
                <strong>{{ item.companyName }}</strong>
              </div>
              <div>
                <span>Origin</span>
                <strong>{{ item.originPlace }}</strong>
              </div>
              <div>
                <span>Current node</span>
                <strong>{{ item.currentNode }}</strong>
              </div>
              <div>
                <span>Production date</span>
                <strong>{{ item.productionDate }}</strong>
              </div>
              <div>
                <span>Market date</span>
                <strong>{{ item.marketDate || 'Not published' }}</strong>
              </div>
              <div>
                <span>QR</span>
                <strong>{{ qrStatusLabel(item.qrStatus) }}</strong>
              </div>
              <div>
                <span>Quality</span>
                <strong>{{ item.qualityStatus }}</strong>
              </div>
            </div>

            <div class="tag-row">
              <span v-for="tag in item.quickTags" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </div>

        <div class="action-row">
          <button class="ghost" @click="router.push(`/batches/${item.id}`)">Workbench</button>
          <button class="ghost" @click="openEditDialog(item)">Edit</button>
          <button class="neutral" @click="openTraceDialog(item)">Trace</button>
          <button class="success" @click="openQualityDialog(item)">Quality</button>
          <button
            class="neutral"
            :title="actionOf(item, 'GENERATE_QR').hint"
            @click="handleGenerateQr(item)"
          >
            QR
          </button>
          <button
            class="primary"
            :disabled="!actionOf(item, 'PUBLISH').enabled"
            :title="actionOf(item, 'PUBLISH').hint"
            @click="openStatusDialog(item, 'PUBLISHED')"
          >
            Publish
          </button>
          <button
            class="success"
            :disabled="!actionOf(item, 'RESUME').enabled"
            :title="actionOf(item, 'RESUME').hint"
            @click="openStatusDialog(item, 'PUBLISHED')"
          >
            Resume
          </button>
          <button
            class="warning"
            :disabled="!actionOf(item, 'FREEZE').enabled"
            :title="actionOf(item, 'FREEZE').hint"
            @click="openStatusDialog(item, 'FROZEN')"
          >
            Freeze
          </button>
          <button
            class="danger"
            :disabled="!actionOf(item, 'RECALL').enabled"
            :title="actionOf(item, 'RECALL').hint"
            @click="openStatusDialog(item, 'RECALLED')"
          >
            Recall
          </button>
        </div>
      </article>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">Quick action</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost icon-button" @click="closeDialog">Close</button>
        </div>

        <div v-if="dialog.type === 'create' || dialog.type === 'edit'" class="form-grid">
          <label v-if="dialog.type === 'create'">
            <span>Batch code</span>
            <input v-model.trim="batchForm.batchCode" type="text">
          </label>
          <label v-else>
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
            <textarea v-model.trim="batchForm.publicRemark" rows="3" placeholder="Shown on public trace page"></textarea>
          </label>
          <label class="full-width">
            <span>Internal remark</span>
            <textarea v-model.trim="batchForm.internalRemark" rows="3" placeholder="Internal note"></textarea>
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
            <input v-model.trim="traceForm.title" type="text" placeholder="Optional title">
          </label>
          <label class="full-width">
            <span>Summary</span>
            <textarea v-model.trim="traceForm.summary" rows="4" placeholder="Short summary of this record"></textarea>
          </label>
          <label class="full-width">
            <span>Trace images</span>
            <div class="upload-box">
              <input type="file" accept="image/*" multiple @change="handleTraceFilesChange">
              <small>Upload real images. URL fallback is still supported.</small>
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
                <small>{{ formatFileSize(item.size) }}</small>
              </div>
              <div class="inline-actions">
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
            <textarea
              v-model.trim="qualityForm.highlightsText"
              rows="4"
              placeholder="Use comma, semicolon or line breaks"
            />
          </label>
          <label class="full-width">
            <span>Quality attachments</span>
            <div class="upload-box">
              <input type="file" multiple @change="handleQualityFilesChange">
              <small>Upload report files or supporting attachments.</small>
            </div>
          </label>
          <div v-if="qualityUploading" class="full-width upload-hint">Uploading quality attachments...</div>
          <div v-if="qualityForm.uploadedFiles.length" class="full-width uploaded-file-list">
            <article v-for="item in qualityForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
              <div>
                <strong>{{ fileLabel(item) }}</strong>
                <small>{{ formatFileSize(item.size) }}</small>
              </div>
              <div class="inline-actions">
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">Open</a>
                <button class="ghost" @click="removeQualityAttachment(item.id)">Remove</button>
              </div>
            </article>
          </div>
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
            <textarea v-model.trim="statusForm.reason" rows="4" placeholder="Write the reason for status change"></textarea>
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
.panel,
.stat-card,
.batch-card,
.dialog-card,
.message-bar {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 42px rgba(31, 55, 47, 0.08);
}

.hero-card {
  display: grid;
  grid-template-columns: 1.5fr auto;
  gap: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(242, 204, 105, 0.26), transparent 28%),
    linear-gradient(140deg, #17362d, #285143 70%, #edf5eb 160%);
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

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.stat-card {
  padding: 22px;
}

.stat-card span {
  display: block;
  color: #60786d;
  font-size: 14px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 32px;
  color: #17362d;
}

.stat-card.warning strong {
  color: #b24b2d;
}

.panel {
  margin-top: 18px;
  padding: 24px;
}

.panel-head {
  margin-bottom: 18px;
}

.panel-head h2 {
  margin-bottom: 0;
}

.filter-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

label {
  display: block;
}

label span {
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

.inline-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.toolbar,
.dialog-actions,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

button:hover,
.preview-link:hover {
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

.message-bar {
  margin-top: 18px;
  padding: 14px 18px;
}

.message-bar.info {
  color: #245846;
}

.message-bar.success {
  color: #245846;
  background: rgba(226, 244, 233, 0.94);
}

.message-bar.error {
  color: #8f2f29;
  background: rgba(253, 236, 235, 0.94);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  color: #546b61;
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
  margin-bottom: 0;
  font-size: 28px;
  color: #17362d;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
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
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.meta-grid div {
  padding: 14px;
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.meta-grid span {
  display: block;
  color: #688176;
  font-size: 12px;
}

.meta-grid strong {
  display: block;
  margin-top: 8px;
  color: #17362d;
  line-height: 1.5;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.tag-row span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
  font-size: 13px;
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
  background: rgba(15, 24, 20, 0.4);
}

.dialog-card {
  width: min(860px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 24px;
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

@media (max-width: 1080px) {
  .stats-grid,
  .meta-grid,
  .filter-grid,
  .form-grid {
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

  .hero-actions {
    justify-content: flex-start;
  }

  .product-image {
    width: 100%;
    height: 220px;
  }

  .title-row {
    flex-direction: column;
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
