<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { createQualityReport, getBatchDetail, getBatchList, uploadBatchFiles } from '../api/batch'
import AdminListPagination from '../components/AdminListPagination.vue'
import AdminListTemplate from '../components/AdminListTemplate.vue'
import AdminOverviewCards from '../components/AdminOverviewCards.vue'
import BatchWorkbenchDrawerPanel from '../components/BatchWorkbenchDrawerPanel.vue'
import StatusTag from '../components/StatusTag.vue'
import { useAuthStore } from '../stores/auth'
import { createQualityForm, getFriendlyErrorMessage, getFriendlyUploadError, qualityOptions, splitHighlights } from '../utils/batchExperience'
import { mapBackendRecommendedQualityActionCode } from '../utils/batchStatusFlow'
import { isRegulator } from '../utils/access'

const router = useRouter()
const authStore = useAuthStore()
const DEFAULT_PAGE_SIZE = 10

const loading = ref(false)
const rows = ref([])
const allRows = ref([])
const message = ref('')
const messageType = ref('info')
const activeTab = ref('PENDING')
const filters = ref(createFilterState())
const qualityUploading = ref(false)
const qualitySubmitting = ref(false)
const qualityForm = ref(createQualityForm())
const resultDialog = ref(createResultDialogState())
const uploadDialog = ref(createUploadDialogState())
const detailDrawer = ref(createWorkbenchDrawerState())
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const batchSideDrawerSize = 'min(620px, 96vw)'
const pageTitle = computed(() => readOnlyQualityView.value ? '质检查看' : '质检待办')
const pageSubtitle = computed(() => {
  if (readOnlyQualityView.value) {
    return '统一查看批次质检结论、最近更新与发布准备度，便于答辩时从监管视角快速讲解。'
  }
  return '围绕上传质检、查看结果和继续发布准备三类高频动作整理成统一台账。'
})
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]
const cleanPageSubtitle = ''
const roleCode = computed(() => authStore.user?.roleCode || '')
const readOnlyQualityView = computed(() => isRegulator(roleCode.value))
const readOnlyBannerText = computed(() => '当前为监管查看模式，页面保留批次、企业、质检结论、二维码状态和最近更新时间，便于直接核对质检链路。')
const openWorkbenchText = computed(() => readOnlyQualityView.value ? '批次详情' : '工作台')
const actionWorkbenchText = computed(() => readOnlyQualityView.value ? '详情' : '工作台')

const qualityTabs = [
  { value: 'PENDING', label: '待上传' },
  { value: 'PASS', label: '已合格' },
  { value: 'FAIL', label: '不合格' },
  { value: 'READY', label: '已上传待发布' }
]

const statusOptions = [
  { value: '', label: '全部批次状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'FROZEN', label: '已冻结' },
  { value: 'RECALLED', label: '已召回' }
]

const filteredRows = computed(() => rows.value.filter((item) => matchesKeyword(item) && matchesCompany(item) && matchesTab(item)))
const pageCount = computed(() => Math.max(1, Math.ceil(Number(filteredRows.value.length || 0) / Number(pageSize.value || DEFAULT_PAGE_SIZE))))
const visibleRows = computed(() => {
  const fromIndex = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(fromIndex, fromIndex + pageSize.value)
})

const tabCounts = computed(() => {
  return qualityTabs.reduce((acc, item) => {
    acc[item.value] = allRows.value.filter((row) => matchesTab(row, item.value)).length
    return acc
  }, {})
})

const activeTabMeta = computed(() => qualityTabs.find((item) => item.value === activeTab.value) ?? qualityTabs[0])

const qualityOverviewCards = computed(() => [
  {
    key: 'pending',
    label: '待上传',
    value: tabCounts.value.PENDING ?? 0,
    detail: readOnlyQualityView.value ? '还没有质检结果的批次' : '优先补质检摘要后再继续发布'
  },
  {
    key: 'pass',
    label: '已合格',
    value: tabCounts.value.PASS ?? 0,
    detail: '可继续核对二维码和公开页表现'
  },
  {
    key: 'fail',
    label: '不合格',
    value: tabCounts.value.FAIL ?? 0,
    detail: '建议先回工作台确认风险与整改'
  },
  {
    key: 'ready',
    label: '待发布',
    value: tabCounts.value.READY ?? 0,
    detail: '质检条件已齐，可继续发布'
  }
])
const qualityBoardCards = computed(() => {
  return qualityOverviewCards.value.map((item, index) => ({
    key: qualityTabs[index].value,
    label: item.label,
    value: item.value,
    detail: item.detail
  }))
})

const listSummary = computed(() => {
  if (!filteredRows.value.length) {
    return `当前看板为“${activeTabMeta.value.label}”，暂无批次。`
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(filteredRows.value.length, page.value * pageSize.value)
  return `当前看板为“${activeTabMeta.value.label}”，共 ${filteredRows.value.length} 个批次，当前显示 ${from}-${to} 个。`
})

const paginationSummary = computed(() => `第 ${page.value} / ${pageCount.value} 页`)

const cleanListSummary = computed(() => {
  if (!filteredRows.value.length) {
    return '暂无批次数据。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(filteredRows.value.length, page.value * pageSize.value)
  return `共 ${filteredRows.value.length} 个批次，当前显示 ${from}-${to} 个。`
})
const cleanPaginationSummary = computed(() => `第 ${page.value} / ${pageCount.value} 页`)

const uploadDialogError = computed(() => {
  if (!uploadDialog.value.visible) {
    return ''
  }
  if (!qualityForm.value.reportNo?.trim()) {
    return '请填写报告编号。'
  }
  if (!qualityForm.value.agency?.trim()) {
    return '请填写检测机构。'
  }
  if (!qualityForm.value.reportTime) {
    return '请填写检测时间。'
  }
  if (!splitHighlights(qualityForm.value.highlightsText).length) {
    return '请至少填写一条质检摘要。'
  }
  return ''
})

onMounted(async () => {
  await fetchRows()
})

watch([activeTab, filters], () => {
  page.value = 1
}, { deep: true })

function createFilterState() {
  return {
    keyword: '',
    companyName: '',
    status: ''
  }
}

function switchActiveTab(tab) {
  activeTab.value = tab
  filters.value.status = ''
}

function createResultDialogState() {
  return {
    visible: false,
    loading: false,
    batch: null,
    detail: null,
    report: null
  }
}

function createUploadDialogState() {
  return {
    visible: false,
    batch: null
  }
}

function createWorkbenchDrawerState() {
  return {
    visible: false,
    batchId: null,
    batchCode: ''
  }
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function cleanObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, item]) => item !== '' && item !== null && item !== undefined)
  )
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[String(status || 'DRAFT').toUpperCase()] ?? 'draft'
}

function actionEnabled(item, code) {
  return Boolean((item.actions || []).find((action) => action.code === code)?.enabled)
}

function resolvePublishReady(item) {
  return Boolean(item.publishReady || actionEnabled(item, 'PUBLISH') || actionEnabled(item, 'RESUME'))
}

function qualityResultText(item) {
  return item.qualityStatus || '待上传质检'
}

function qualityPriorityText(item) {
  const qualityCode = String(item.qualityStatusCode || 'PENDING').toUpperCase()
  if (qualityCode === 'PENDING') {
    return '优先补质检摘要'
  }
  if (qualityCode === 'FAIL') {
    return '先核对不合格结论'
  }
  if (item.status === 'PUBLISHED') {
    return '已发布，继续核对公开页'
  }
  if (resolvePublishReady(item)) {
    return '可回工作台继续发布'
  }
  return '继续核对二维码准备情况'
}

function qualityPriorityTone(item) {
  const qualityCode = String(item.qualityStatusCode || 'PENDING').toUpperCase()
  if (qualityCode === 'FAIL') {
    return 'danger'
  }
  if (qualityCode === 'PASS' && resolvePublishReady(item)) {
    return 'success'
  }
  if (qualityCode === 'PASS') {
    return 'primary'
  }
  return 'warning'
}

function qualityReportAvailable(item) {
  return String(item.qualityStatusCode || 'PENDING').toUpperCase() !== 'PENDING'
}

function canExecuteQualityAction(item, actionCode) {
  if (actionCode === 'workbench') {
    return true
  }
  if (actionCode === 'upload') {
    return !readOnlyQualityView.value && actionEnabled(item, 'UPLOAD_QUALITY')
  }
  if (actionCode === 'report') {
    return qualityReportAvailable(item)
  }
  return false
}

function recommendedQualityActionCode(item) {
  if (readOnlyQualityView.value) {
    return qualityReportAvailable(item) ? 'report' : 'workbench'
  }
  const backendCode = mapBackendRecommendedQualityActionCode(item?.recommendedActionCode)
  if (backendCode && canExecuteQualityAction(item, backendCode)) {
    return backendCode
  }
  if (!qualityReportAvailable(item) && actionEnabled(item, 'UPLOAD_QUALITY')) {
    return 'upload'
  }
  if (qualityReportAvailable(item) && String(item.qualityStatusCode || '').toUpperCase() === 'FAIL') {
    return 'report'
  }
  if (resolvePublishReady(item)) {
    return 'workbench'
  }
  return qualityReportAvailable(item) ? 'report' : 'workbench'
}

function recommendedQualityActionDisabled(item) {
  return !canExecuteQualityAction(item, recommendedQualityActionCode(item))
}

function recommendedQualityActionLabel(item) {
  return {
    upload: '上传质检',
    report: '质检结果',
    workbench: openWorkbenchText.value
  }[recommendedQualityActionCode(item)]
}

function primaryQualityActionLabel(item) {
  const actionCode = recommendedQualityActionCode(item)
  if (actionCode !== 'workbench') {
    return recommendedQualityActionLabel(item)
  }
  if (readOnlyQualityView.value) {
    return '批次详情'
  }
  if (item.status === 'PUBLISHED') {
    return '公开状态'
  }
  if (resolvePublishReady(item)) {
    return '继续发布准备'
  }
  return '继续核对详情'
}

function recommendedQualityActionClass(item) {
  return {
    upload: 'primary',
    report: String(item.qualityStatusCode || '').toUpperCase() === 'FAIL' ? 'warning' : 'ghost',
    workbench: resolvePublishReady(item) ? 'success' : 'ghost'
  }[recommendedQualityActionCode(item)]
}

function primaryQualityActionTestId(item) {
  return {
    report: `quality-open-report-${item.id}`,
    upload: `quality-upload-${item.id}`,
    workbench: `quality-open-workbench-${item.id}`
  }[recommendedQualityActionCode(item)]
}

function qualityMoreActions(item) {
  const actions = []
  if (qualityReportAvailable(item) && recommendedQualityActionCode(item) !== 'report') {
    actions.push({
      key: 'report',
      label: '质检结果',
      testId: `quality-open-report-${item.id}`,
      disabled: false
    })
  }
  if (!readOnlyQualityView.value && actionEnabled(item, 'UPLOAD_QUALITY') && recommendedQualityActionCode(item) !== 'upload') {
    actions.push({
      key: 'upload',
      label: '上传质检',
      testId: `quality-upload-${item.id}`,
      disabled: false
    })
  }
  return actions
}

function showSecondaryWorkbenchAction(item) {
  return recommendedQualityActionCode(item) !== 'workbench'
}

function handleRecommendedQualityAction(item) {
  const code = recommendedQualityActionCode(item)
  if (!canExecuteQualityAction(item, code)) {
    showMessage('当前动作暂不可执行，请先补齐前置条件。', 'error')
    return
  }
  if (code === 'upload') {
    openUploadDialog(item)
    return
  }
  if (code === 'report') {
    openResultDialog(item)
    return
  }
  openWorkbench(item)
}

function handleQualityRowCommand(item, code) {
  if (code === 'report') {
    openResultDialog(item)
    return
  }
  if (code === 'upload') {
    openUploadDialog(item)
  }
}

function latestUpdatedText(item) {
  return item.lastUpdatedAt || item.latestTraceTime || '暂无更新'
}

function resultStatusTone(item) {
  return {
    PASS: 'success',
    FAIL: 'danger',
    PENDING: 'pending'
  }[String(item.qualityStatusCode || 'PENDING').toUpperCase()] ?? 'pending'
}

function qualityStateChips(item) {
  return [
    {
      key: 'batch',
      label: item.statusLabel || '状态待确认',
      tone: statusClass(item.status)
    },
    {
      key: 'quality',
      label: qualityResultText(item),
      tone: resultStatusTone(item)
    }
  ]
}

function qualityOverviewChips(item) {
  const chips = [...qualityStateChips(item)]
  if (item.status === 'PUBLISHED') {
    chips.push({
      key: 'publish',
      label: '已发布',
      tone: 'success'
    })
  } else if (String(item.qrStatus || '').toUpperCase() === 'NOT_GENERATED') {
    chips.push({
      key: 'qr',
      label: '缺二维码',
      tone: 'pending'
    })
  } else if (resolvePublishReady(item)) {
    chips.push({
      key: 'publish-ready',
      label: '可继续发布',
      tone: 'info'
    })
  } else {
    chips.push({
      key: 'judge',
      label: '已形成判断',
      tone: 'pending'
    })
  }
  return chips.slice(0, 3)
}

function qualityPriorityBadgeText(item) {
  const qualityCode = String(item.qualityStatusCode || 'PENDING').toUpperCase()
  if (qualityCode === 'PENDING') {
    return '优先处理'
  }
  if (qualityCode === 'FAIL') {
    return '结果需复核'
  }
  if (item.status === 'PUBLISHED') {
    return '已发布'
  }
  if (resolvePublishReady(item)) {
    return '继续发布'
  }
  return '继续核对'
}

function qualityReadinessTags(item) {
  const tags = []
  if (String(item.status || '').toUpperCase() === 'PUBLISHED') {
    tags.push({ key: 'publish', label: '已发布', tone: 'success' })
  } else if (String(item.qualityStatusCode || 'PENDING').toUpperCase() === 'FAIL') {
    tags.push({ key: 'blocked', label: '暂不可发布', tone: 'danger' })
  } else if (resolvePublishReady(item)) {
    tags.push({ key: 'ready', label: '可继续发布', tone: 'success' })
  } else {
    tags.push({ key: 'pending', label: '已形成判断', tone: 'pending' })
  }
  if (String(item.qrStatus || '').toUpperCase() === 'NOT_GENERATED') {
    tags.push({ key: 'qr', label: '缺二维码', tone: 'pending' })
  } else {
    tags.push({ key: 'qr', label: '二维码已生成', tone: 'info' })
  }
  return tags.slice(0, 2)
}

function qualityPreparationTitle(item) {
  const qualityCode = String(item.qualityStatusCode || 'PENDING').toUpperCase()
  if (item.status === 'PUBLISHED') {
    return '已发布，可继续回查公开页'
  }
  if (qualityCode === 'FAIL') {
    return '暂不可发布，需先处理质检异常'
  }
  if (resolvePublishReady(item)) {
    return '发布准备已就绪，可继续推进'
  }
  if (String(item.qrStatus || '').toUpperCase() === 'NOT_GENERATED') {
    return '缺少二维码，发布准备未齐'
  }
  return '已形成判断，继续补齐发布条件'
}

function qualityBatchStatusTags(item) {
  const tags = [
    {
      key: 'batch',
      text: item.statusLabel || '状态待确认',
      tone: statusClass(item.status)
    }
  ]

  if (String(item.status || '').toUpperCase() !== 'PUBLISHED' && resolvePublishReady(item)) {
    tags.push({
      key: 'ready',
      text: '可发布',
      tone: 'success'
    })
  }

  return tags
}

function defaultReportNo(item) {
  const stamp = new Date().toISOString().replace(/[^\d]/g, '').slice(0, 12)
  return `QA-${item.batchCode}-${stamp}`
}

function matchesKeyword(item) {
  const keyword = filters.value.keyword.trim().toLowerCase()
  if (!keyword) {
    return true
  }
  return [item.batchCode, item.productName, item.productCode]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword))
}

function matchesCompany(item) {
  const keyword = filters.value.companyName.trim().toLowerCase()
  if (!keyword) {
    return true
  }
  return String(item.companyName || '').toLowerCase().includes(keyword)
}

function matchesTab(item, tabValue = activeTab.value) {
  const qualityCode = String(item.qualityStatusCode || 'PENDING').toUpperCase()
  if (tabValue === 'READY') {
    return qualityCode !== 'PENDING' && item.status !== 'PUBLISHED' && resolvePublishReady(item)
  }
  return qualityCode === tabValue
}

async function fetchRows() {
  loading.value = true
  try {
    const [response, summaryResponse] = await Promise.all([
      getBatchList(cleanObject({
        status: filters.value.status || undefined
      })),
      getBatchList()
    ])
    rows.value = response.data ?? []
    allRows.value = summaryResponse.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '质检待办加载失败，请稍后再试。'), 'error')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = createFilterState()
  page.value = 1
  pageSize.value = DEFAULT_PAGE_SIZE
  fetchRows()
}

function handleSearch() {
  page.value = 1
  fetchRows()
}

function handlePageSizeChange(value) {
  const nextPageSize = Number(value || DEFAULT_PAGE_SIZE)
  if (nextPageSize === pageSize.value) return
  pageSize.value = nextPageSize
  page.value = 1
}

function goPrevPage() {
  if (loading.value || page.value <= 1) return
  page.value -= 1
}

function goNextPage() {
  if (loading.value || page.value >= pageCount.value) return
  page.value += 1
}

function openWorkbench(item) {
  detailDrawer.value = {
    visible: true,
    batchId: item?.id ?? null,
    batchCode: item?.batchCode ?? ''
  }
}

function closeWorkbenchDrawer() {
  detailDrawer.value = createWorkbenchDrawerState()
}

async function openResultDialog(item) {
  resultDialog.value = {
    visible: true,
    loading: true,
    batch: item,
    detail: null,
    report: null
  }
  try {
    const response = await getBatchDetail(item.id)
    const detail = response.data ?? null
    resultDialog.value.detail = detail
    resultDialog.value.report = detail?.quality?.latestReport ?? null
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '质检结果加载失败，请稍后再试。'), 'error')
  } finally {
    resultDialog.value.loading = false
  }
}

function closeResultDialog() {
  resultDialog.value = createResultDialogState()
}

function openUploadDialog(item) {
  if (readOnlyQualityView.value) {
    return
  }
  if (!actionEnabled(item, 'UPLOAD_QUALITY')) {
    showMessage('当前批次暂不允许上传质检，请先检查状态条件。', 'error')
    return
  }
  uploadDialog.value = {
    visible: true,
    batch: item
  }
  qualityForm.value = createQualityForm({
    reportNo: defaultReportNo(item),
    agency: `${item.companyName || '企业'}质检中心`
  })
}

function closeUploadDialog() {
  uploadDialog.value = createUploadDialogState()
  qualityForm.value = createQualityForm()
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

function removeQualityAttachment(fileId) {
  qualityForm.value.uploadedFiles = qualityForm.value.uploadedFiles.filter((item) => item.id !== fileId)
  qualityForm.value.attachmentIds = qualityForm.value.uploadedFiles.map((item) => item.id)
}

async function submitQualityUpload() {
  if (qualitySubmitting.value || readOnlyQualityView.value) {
    return
  }
  if (!uploadDialog.value.batch?.id) {
    return
  }
  if (uploadDialogError.value) {
    showMessage(uploadDialogError.value, 'error')
    return
  }
  if (String(qualityForm.value.result || '').toUpperCase() === 'FAIL') {
    const confirmed = window.confirm('当前质检结论为“不合格”，提交后会影响发布流程，确认继续吗？')
    if (!confirmed) {
      return
    }
  }
  qualitySubmitting.value = true
  try {
    await createQualityReport(uploadDialog.value.batch.id, {
      reportNo: qualityForm.value.reportNo,
      agency: qualityForm.value.agency,
      result: qualityForm.value.result,
      reportTime: qualityForm.value.reportTime,
      highlights: splitHighlights(qualityForm.value.highlightsText),
      attachmentIds: qualityForm.value.attachmentIds
    })
    await fetchRows()
    showMessage('质检摘要已上传，列表状态已刷新。', 'success')
    closeUploadDialog()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '质检上传失败，请稍后再试。'), 'error')
  } finally {
    qualitySubmitting.value = false
  }
}

function fileLabel(file) {
  return file.fileName || file.fileUrl || '已上传附件'
}

function formatFileSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <div class="page-shell" data-testid="quality-page">
    <div class="manage-page quality-manage">
    <AdminListTemplate
      template-class="quality-card-stack"
      filter-card-class="quality-filter-card"
      ledger-card-class="quality-ledger-panel quality-ledger-card"
    >
      <template #summary>
        <AdminOverviewCards
          :items="qualityBoardCards"
          :active-key="activeTab"
          test-id-prefix="quality-tab"
          @select="switchActiveTab($event)"
        />
      </template>

      <template #actions>
        <button class="ghost" data-testid="quality-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
      </template>

      <template #filterPrimary>
      <div class="quality-filter-layout">
        <div class="manage-filter-grid quality-filter-grid">
          <label class="manage-filter-field quality-filter-field quality-filter-field--keyword">
            <span class="manage-filter-field__label">批次名称 / 编号</span>
            <input v-model.trim="filters.keyword" data-testid="quality-filter-keyword" type="text" placeholder="输入批次编号或产品名称">
          </label>
          <label class="manage-filter-field quality-filter-field quality-filter-field--company">
            <span class="manage-filter-field__label">企业</span>
            <input v-model.trim="filters.companyName" type="text" placeholder="输入企业名称">
          </label>
          <label class="manage-filter-field quality-filter-field quality-filter-field--status">
            <span class="manage-filter-field__label">批次状态</span>
            <el-select
              v-model="filters.status"
              class="manage-filter-item"
              placeholder="全部批次状态"
            >
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="manage-filter-field quality-filter-field quality-filter-field--page-size">
            <span class="manage-filter-field__label">每页显示</span>
            <el-select
              :model-value="pageSize"
              data-testid="quality-page-size"
              class="manage-filter-item quality-page-size-select"
              @change="handlePageSizeChange"
            >
              <el-option v-for="item in pageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
        </div>
      </div>
      </template>

      <template #filterSecondary>
        <div class="quality-filter-toolbar">
          <div class="quality-filter-actions">
          <button class="primary" data-testid="quality-search-button" :disabled="loading" @click="handleSearch">查询</button>
          <button class="ghost" data-testid="quality-reset-button" :disabled="loading" @click="resetFilters">重置</button>
          </div>
          <div class="quality-list-summary">
            <span class="manage-muted">{{ cleanListSummary }}</span>
          </div>
        </div>
      </template>

      <template #message>
        <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>
      </template>

      <template #ledger>
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">质检任务台账</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div>
          <h3>正在同步质检看板...</h3>
        </div>
      </div>

      <div v-else-if="!visibleRows.length" class="empty-state">
        <div>
          <h3>当前看板下还没有质检任务</h3>
        </div>
      </div>

      <div v-else class="table-scroll-shell ledger-table-shell quality-table-shell">
        <div class="ledger-table-head quality-head">
          <span>批次与产品</span>
          <span>企业 / 更新时间</span>
          <span class="table-head-cell--center">质检结果</span>
          <span>发布校验状态</span>
          <span>动作</span>
        </div>

        <div class="ledger-row-list quality-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="ledger-row quality-row"
          :data-testid="`quality-row-${item.id}`"
        >
          <div class="row-main quality-main">
            <strong>{{ item.productName || item.batchCode }}</strong>
            <span class="ledger-code">{{ item.batchCode }}</span>
          </div>

          <div class="row-meta quality-company">
            <strong>{{ item.companyName }}</strong>
            <small>最近更新：{{ latestUpdatedText(item) }}</small>
          </div>

          <div class="row-status quality-overview table-cell--center">
            <StatusTag :text="qualityResultText(item)" :tone="resultStatusTone(item)" />
          </div>

          <div class="row-status row-next quality-next">
            <div class="quality-preparation-tags">
              <StatusTag
                v-for="tag in qualityReadinessTags(item)"
                :key="tag.key"
                :text="tag.label || tag.text"
                :tone="tag.tone"
              />
            </div>
          </div>

          <div class="row-actions quality-actions table-cell--actions">
            <div class="row-actions-scroll ledger-actions-scroll quality-actions-row">
              <button
                :class="recommendedQualityActionClass(item)"
                class="action-primary-button"
                :disabled="recommendedQualityActionDisabled(item)"
                :data-testid="primaryQualityActionTestId(item)"
                @click="handleRecommendedQualityAction(item)"
              >
                {{ recommendedQualityActionLabel(item) }}
              </button>
              <button
                v-if="showSecondaryWorkbenchAction(item)"
                class="text-button primary-text"
                :data-testid="`quality-workbench-link-${item.id}`"
                @click="openWorkbench(item)"
              >
                {{ actionWorkbenchText }}
              </button>
              <button
                v-for="action in qualityMoreActions(item)"
                :key="action.key"
                type="button"
                class="text-button"
                :disabled="action.disabled"
                :data-testid="action.testId"
                @click="handleQualityRowCommand(item, action.key)"
              >
                {{ action.label }}
              </button>
            </div>
          </div>
        </article>
        </div>
      </div>

      <AdminListPagination
        :summary="cleanPaginationSummary"
        :prev-disabled="loading || page <= 1"
        :next-disabled="loading || page >= pageCount"
        @prev="goPrevPage"
        @next="goNextPage"
      />

      </template>
    </AdminListTemplate>

    <div v-if="resultDialog.visible" class="dialog-mask" @click.self="closeResultDialog">
      <section class="dialog-card" data-testid="quality-result-dialog">
        <div class="dialog-head">
          <div>
            <h3>质检结果</h3>
            <p>{{ resultDialog.batch?.batchCode }} · {{ resultDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" @click="closeResultDialog">关闭</button>
        </div>

        <div v-if="resultDialog.loading" class="upload-hint">正在加载最新质检结果...</div>

        <template v-else-if="resultDialog.report">
          <div class="report-grid">
            <article>
              <h4>报告摘要</h4>
              <div class="report-copy">
                <span>报告编号</span>
                <strong data-testid="quality-report-no">{{ resultDialog.report.reportNo }}</strong>
                <span>检测机构</span>
                <strong>{{ resultDialog.report.agency }}</strong>
                <span>检测结论</span>
                <strong>{{ resultDialog.report.resultLabel || resultDialog.batch?.qualityStatus }}</strong>
                <span>检测时间</span>
                <strong>{{ resultDialog.report.reportTime || '暂无时间' }}</strong>
              </div>
            </article>

            <article>
              <h4>质检重点</h4>
              <ul v-if="resultDialog.report.highlights?.length" class="text-list">
                <li v-for="item in resultDialog.report.highlights" :key="item">{{ item }}</li>
              </ul>
              <p v-else class="empty-copy">当前没有额外质检亮点。</p>
            </article>
          </div>

          <div class="panel" style="margin-top: 16px; padding: 16px;">
            <p class="panel-label">附件</p>
            <div v-if="resultDialog.report.attachments?.length" class="report-file-list">
              <article v-for="item in resultDialog.report.attachments" :key="item.id" class="report-file-item">
                <div>
                  <strong>{{ fileLabel(item) }}</strong>
                  <small>{{ formatFileSize(item.size) }}</small>
                </div>
                <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看附件</a>
              </article>
            </div>
            <p v-else class="empty-copy">当前没有上传质检附件。</p>
          </div>
        </template>

        <p v-else class="empty-copy">当前批次还没有质检结果。</p>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" @click="closeResultDialog">关闭</button>
          <button
            v-if="resultDialog.batch"
            class="primary"
            data-testid="quality-result-open-workbench"
            @click="openWorkbench(resultDialog.batch); closeResultDialog()"
          >
            {{ openWorkbenchText }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="uploadDialog.visible" class="dialog-mask" @click.self="closeUploadDialog">
      <section class="dialog-card" data-testid="quality-upload-dialog">
        <div class="dialog-head">
          <div>
            <h3>上传质检</h3>
            <p>{{ uploadDialog.batch?.batchCode }} · {{ uploadDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" @click="closeUploadDialog">关闭</button>
        </div>

        <div class="form-grid">
          <label>
            <span>报告编号</span>
            <input v-model.trim="qualityForm.reportNo" type="text" placeholder="例如 QA-ORANGE-202603-D1-01">
          </label>
          <label>
            <span>检测机构</span>
            <input v-model.trim="qualityForm.agency" type="text" placeholder="例如 江西省农产品质检中心">
          </label>
          <label>
            <span>检测结论</span>
            <select v-model="qualityForm.result">
              <option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>检测时间</span>
            <input v-model="qualityForm.reportTime" type="datetime-local">
          </label>
          <label class="full-width">
            <span>关键指标摘要</span>
            <textarea
              v-model.trim="qualityForm.highlightsText"
              rows="4"
              placeholder="每行一条摘要"
            />
          </label>
          <label class="full-width">
            <span>附件</span>
            <div class="upload-box">
              <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
              <small class="empty-copy">支持 PDF 或图片附件。</small>
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

        <p v-if="uploadDialogError" class="form-error">{{ uploadDialogError }}</p>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="qualitySubmitting" @click="closeUploadDialog">取消</button>
          <button class="primary" data-testid="quality-upload-submit" :disabled="qualitySubmitting || qualityUploading || Boolean(uploadDialogError)" @click="submitQualityUpload">
            {{ qualitySubmitting ? '正在保存...' : '确认上传' }}
          </button>
        </div>
        </section>
      </div>
    </div>

    <el-drawer
      v-model="detailDrawer.visible"
      direction="rtl"
      :size="batchSideDrawerSize"
      append-to-body
      destroy-on-close
      :with-header="false"
      data-testid="quality-workbench-drawer"
    >
      <div>
        <div class="dialog-head">
          <div>
            <h3>批次工作台</h3>
            <p>批次 {{ detailDrawer.batchCode || '详情查看' }}</p>
          </div>
          <button class="ghost" @click="closeWorkbenchDrawer">关闭</button>
        </div>
        <BatchWorkbenchDrawerPanel
          v-if="detailDrawer.visible && detailDrawer.batchId"
          :batch-id="detailDrawer.batchId"
        />
      </div>
    </el-drawer>
    </div>
</template>

<style src="../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.quality-manage {
  gap: 14px;
  --quality-filter-card-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 251, 255, 0.98) 100%);
  --quality-filter-card-border: rgba(56, 134, 217, 0.14);
  --quality-filter-card-shadow: 0 16px 38px rgba(45, 113, 194, 0.08);
  --quality-grid-inline-padding: 12px;
  --quality-filter-group-left-shift: 14px;
  --quality-filter-text-inset: 12px;
  --quality-filter-control-height: 42px;
  --quality-filter-control-radius: 12px;
  --quality-keyword-width: 264px;
  --quality-company-width: 220px;
  --quality-status-width: 150px;
  --quality-page-size-width: 146px;
}

.quality-card-stack {
  display: grid;
  gap: 16px;
}

:deep(.quality-filter-card),
:deep(.quality-ledger-card) {
  position: relative;
  overflow: hidden;
}

:deep(.quality-filter-card) {
  padding: 18px 22px 0;
  border-color: var(--quality-filter-card-border) !important;
  background: var(--quality-filter-card-bg) !important;
  box-shadow: var(--quality-filter-card-shadow) !important;
}

:deep(.quality-ledger-card) {
  padding: 20px 22px 18px;
  border: 1px solid rgba(56, 134, 217, 0.14) !important;
  border-radius: 24px !important;
  background: #fff !important;
  box-shadow: 0 18px 42px rgba(45, 113, 194, 0.08) !important;
}

.quality-table-shell {
  --ledger-grid-columns:
    minmax(208px, 1.08fr)
    minmax(200px, 1fr)
    minmax(148px, 0.78fr)
    minmax(168px, 0.86fr)
    minmax(256px, 1.08fr);
  --ledger-min-width: 1320px;
}

.quality-mode-summary {
  margin-top: 0;
}

.quality-filter-layout {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 18px;
  margin-left: calc(-1 * var(--quality-filter-group-left-shift));
  padding: 0 12px 0 calc(var(--quality-grid-inline-padding) - var(--quality-filter-text-inset));
  flex-wrap: wrap;
}

.quality-filter-grid {
  grid-template-columns:
    minmax(0, var(--quality-keyword-width))
    minmax(0, var(--quality-company-width))
    minmax(0, var(--quality-status-width))
    minmax(0, var(--quality-page-size-width));
  gap: 16px 14px;
  align-items: end;
}

.quality-filter-field {
  display: grid;
  gap: 8px;
  min-width: 0;
  width: 100%;
}

.quality-filter-field--keyword {
  max-width: var(--quality-keyword-width);
}

.quality-filter-field--company {
  max-width: var(--quality-company-width);
}

.quality-filter-field--status {
  max-width: var(--quality-status-width);
}

.quality-filter-field--page-size {
  max-width: var(--quality-page-size-width);
}

.quality-filter-grid .manage-filter-field__label {
  padding-inline-start: var(--quality-filter-text-inset);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.quality-filter-grid .quality-filter-field > input {
  width: 100%;
  min-height: var(--quality-filter-control-height);
  padding: 0 14px;
  border: 1px solid var(--admin-border);
  border-radius: var(--quality-filter-control-radius);
  background: #fff;
  color: var(--admin-text);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.quality-filter-grid .quality-filter-field > input:focus {
  border-color: rgba(48, 149, 246, 0.26);
  box-shadow: 0 0 0 3px rgba(48, 149, 246, 0.08);
  outline: none;
}

.quality-filter-grid :deep(.manage-filter-item .el-select__wrapper) {
  min-height: var(--quality-filter-control-height);
  padding: 0 14px;
  border-radius: var(--quality-filter-control-radius);
  background: #fff;
  box-shadow: 0 0 0 1px var(--admin-border) inset !important;
}

.quality-filter-grid :deep(.manage-filter-item .el-select__selected-item),
.quality-filter-grid :deep(.manage-filter-item .el-select__placeholder) {
  text-align: left;
}

.quality-page-size-select {
  width: 100%;
}

.quality-page-size-select :deep(.el-select__wrapper) {
  min-height: var(--quality-filter-control-height);
}

.quality-filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  margin-left: calc(-1 * var(--quality-filter-group-left-shift));
  padding: 0 12px 18px var(--quality-grid-inline-padding);
  min-width: 0;
}

.quality-filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-start;
  flex-wrap: nowrap;
}

.quality-filter-actions button {
  min-height: 38px;
  padding-inline: 16px;
  border-radius: 12px;
}

.quality-list-summary .manage-muted {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  color: var(--admin-text-soft);
}

:deep(.quality-ledger-card .table-scroll-shell),
:deep(.quality-ledger-card .admin-list-pagination),
:deep(.quality-ledger-card .table-scroll-shell .quality-head),
:deep(.quality-ledger-card .panel-heading),
:deep(.quality-ledger-card .panel-heading > div) {
  background: #fff !important;
}

.quality-row-list,
.quality-row-list.ledger-row-list {
  display: grid;
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 26px;
  background: #fff;
}

.quality-row,
.quality-row.ledger-row {
  background: #fff;
  border-top: 1px solid rgba(56, 134, 217, 0.1);
}

.quality-company,
.quality-next,
.quality-task,
.quality-overview,
.quality-actions {
  min-width: 0;
}

.quality-overview,
.quality-next,
.quality-task {
  display: grid;
  gap: 8px;
}

.quality-status-chip-row,
.quality-preparation-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quality-status-chip-row .status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.quality-status-chip-row .status-chip.success,
.quality-status-chip-row .status-chip.published {
  border-color: rgba(46, 166, 106, 0.22);
  background: rgba(46, 166, 106, 0.1);
  color: #1e7d50;
}

.quality-status-chip-row .status-chip.info,
.quality-status-chip-row .status-chip.primary {
  border-color: rgba(48, 149, 246, 0.18);
  background: rgba(48, 149, 246, 0.12);
  color: var(--admin-primary-deep);
}

.quality-status-chip-row .status-chip.warning,
.quality-status-chip-row .status-chip.frozen {
  border-color: rgba(242, 154, 52, 0.22);
  background: rgba(242, 154, 52, 0.12);
  color: #b96b16;
}

.quality-status-chip-row .status-chip.danger,
.quality-status-chip-row .status-chip.recalled {
  border-color: rgba(221, 74, 74, 0.22);
  background: rgba(221, 74, 74, 0.1);
  color: #b63f3f;
}

.quality-status-chip-row .status-chip.pending,
.quality-status-chip-row .status-chip.normal,
.quality-status-chip-row .status-chip.draft {
  border-color: rgba(120, 146, 173, 0.18);
  background: rgba(120, 146, 173, 0.12);
  color: #5f7b98;
}

.quality-next :deep(.status-tag) {
  width: fit-content;
}

.quality-task strong {
  color: var(--admin-text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.45;
}

.quality-task :deep(.status-tag) {
  width: fit-content;
}

.quality-actions {
  align-items: flex-start;
}

.quality-actions-row {
  overflow: visible;
}

.quality-actions-row .action-primary-button,
.quality-actions-row .text-button {
  min-height: 34px;
  padding: 0 12px;
  font-size: 13px;
  white-space: nowrap;
}

.quality-actions-row .action-primary-button {
  box-shadow: none;
}

.readonly-banner {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.readonly-banner strong {
  color: var(--admin-text);
}

.readonly-banner span {
  color: var(--admin-text-soft);
  line-height: 1.7;
}

.form-error {
  margin: 14px 0 0;
  color: #b63f3f;
  font-size: 13px;
  line-height: 1.6;
}

.empty-state {
  min-height: 160px;
}

@media (max-width: 1180px) {
  .quality-filter-layout,
  .quality-filter-toolbar {
    margin-left: 0;
    padding-left: 0;
    padding-right: 0;
  }

  .quality-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quality-filter-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 760px) {
  .quality-filter-grid {
    grid-template-columns: 1fr;
  }

  .quality-head {
    display: none;
  }

  .quality-row {
    grid-template-columns: 1fr;
  }

  .quality-page-size-select {
    width: 100%;
  }

  .quality-actions-row {
    flex-wrap: wrap;
  }
}
</style>

<style src="../assets/styles/admin-ledger-unified.css" scoped></style>
