<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { changeBatchStatus, generateBatchQr, getBatchDetail, getBatchList } from '../api/batch'
import { useAuthStore } from '../stores/auth'
import { getFriendlyErrorMessage } from '../utils/batchExperience'
import { resolvePublishBlockState } from '../utils/batchStatusFlow'
import { openPrintPreviewWindow, renderQrPrintPreview } from '../utils/exportTools'
import { resolveQrStatusText } from '../utils/statusPresentation'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const rows = ref([])
const allRows = ref([])
const message = ref('')
const messageType = ref('info')
const activeTab = ref('NEED_QR')
const filters = ref(createFilterState())
const qrSubmittingId = ref(null)
const downloadSubmittingId = ref(null)
const bulkPrintSubmitting = ref(false)
const publishSubmitting = ref(false)
const previewDialog = ref(createPreviewDialogState())
const publishDialog = ref(createPublishDialogState())
const selectedIds = ref([])
const DEFAULT_PAGE_SIZE = 10
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]

const tabs = [
  { value: 'NEED_QR', label: '待生成二维码' },
  { value: 'READY', label: '已生成待发布' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'BLOCKED', label: '不可发布' }
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
const selectableVisibleRows = computed(() => visibleRows.value.filter((item) => hasQr(item)))
const selectedVisibleRows = computed(() => visibleRows.value.filter((item) => selectedIds.value.includes(item.id)))
const selectedPrintableRows = computed(() => selectedVisibleRows.value.filter((item) => hasQr(item)))
const allSelectableVisibleChecked = computed(() => {
  return Boolean(selectableVisibleRows.value.length) && selectableVisibleRows.value.every((item) => selectedIds.value.includes(item.id))
})

const tabCounts = computed(() => {
  return tabs.reduce((acc, tab) => {
    acc[tab.value] = allRows.value.filter((item) => matchesTab(item, tab.value)).length
    return acc
  }, {})
})

const qrOverviewCards = computed(() => ([
  {
    value: 'NEED_QR',
    label: '待生成二维码',
    count: tabCounts.value.NEED_QR ?? 0,
    detail: '还需要补齐二维码入口的批次'
  },
  {
    value: 'READY',
    label: '已生成待发布',
    count: tabCounts.value.READY ?? 0,
    detail: '二维码就绪，可继续检查发布条件'
  },
  {
    value: 'PUBLISHED',
    label: '已发布',
    count: tabCounts.value.PUBLISHED ?? 0,
    detail: '可回查公开入口与扫码展示'
  },
  {
    value: 'BLOCKED',
    label: '不可发布',
    count: tabCounts.value.BLOCKED ?? 0,
    detail: '仍有前置条件没有满足'
  }
]))

const publishDialogError = computed(() => {
  if (!publishDialog.value.visible) {
    return ''
  }
  const batch = publishDialog.value.batch
  if (!batch?.id) {
    return '未找到要发布的批次。'
  }
  const gate = publishGate(batch)
  if (!gate.allowed) {
    return gate.reason
  }
  if (!canPublish(batch)) {
    return gate.reason || publishHint(batch)
  }
  if (!String(publishDialog.value.reason || '').trim()) {
    return '请填写处理说明。'
  }
  return ''
})
const listSummary = computed(() => {
  if (!filteredRows.value.length) {
    return '当前没有可展示的批次。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(filteredRows.value.length, page.value * pageSize.value)
  return `共 ${filteredRows.value.length} 个批次，当前显示 ${from}-${to} 个。`
})

onMounted(async () => {
  await fetchRows()
})

watch([activeTab, filters], () => {
  page.value = 1
  const visibleIds = new Set(visibleRows.value.map((item) => item.id))
  selectedIds.value = selectedIds.value.filter((id) => visibleIds.has(id))
}, { deep: true })

function createFilterState() {
  return {
    keyword: '',
    companyName: '',
    status: ''
  }
}

function createPreviewDialogState() {
  return {
    visible: false,
    loading: false,
    batch: null,
    qr: null
  }
}

function createPublishDialogState() {
  return {
    visible: false,
    batch: null,
    reason: ''
  }
}

function cleanObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, item]) => item !== '' && item !== null && item !== undefined)
  )
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function actionOf(item, code) {
  return item.actions?.find((action) => action.code === code) ?? {
    code,
    label: code,
    enabled: false,
    hint: ''
  }
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[String(status || 'DRAFT').toUpperCase()] ?? 'draft'
}

function qualityToneClass(item) {
  return {
    PASS: 'success',
    FAIL: 'danger',
    PENDING: 'pending'
  }[String(item.qualityStatusCode || 'PENDING').toUpperCase()] ?? 'pending'
}

function publishToneClass(item) {
  if (String(item.status || '').toUpperCase() === 'PUBLISHED') {
    return 'success'
  }
  return canPublish(item) ? 'success' : 'danger'
}

function batchStatusTone(item) {
  const status = String(item.status || '').toUpperCase()
  if (status === 'PUBLISHED') return 'success'
  if (status === 'RECALLED') return 'danger'
  if (status === 'FROZEN') return 'warning'
  return 'pending'
}

function hasQr(item) {
  return String(item.qrStatus || 'NOT_GENERATED').toUpperCase() !== 'NOT_GENERATED'
}

function qualityUploaded(item) {
  return String(item.qualityStatusCode || 'PENDING').toUpperCase() !== 'PENDING'
}

function qualityPassed(item) {
  return String(item.qualityStatusCode || 'PENDING').toUpperCase() === 'PASS'
}

function riskAllowsPublish(item) {
  const batchStatus = String(item.status || '').toUpperCase()
  const riskStatus = String(item.riskStatus || 'NORMAL').toUpperCase()
  if (batchStatus === 'RECALLED') {
    return false
  }
  if (batchStatus === 'FROZEN') {
    return Boolean(item.canResume)
  }
  return !['FROZEN', 'PROCESSING', 'RISK_PENDING', 'RECALLED'].includes(riskStatus)
}

function publishGate(item) {
  return resolvePublishBlockState(item)
}

function canPublish(item) {
  const gate = publishGate(item)
  return gate.allowed && Boolean(actionOf(item, 'PUBLISH').enabled || actionOf(item, 'RESUME').enabled || item.publishReady)
}

function publishActionLabel(item) {
  return actionOf(item, 'RESUME').enabled ? '恢复发布' : '发布批次'
}

function publishSummary(item) {
  if (String(item.status || '').toUpperCase() === 'PUBLISHED') {
    return '当前批次已发布'
  }
  return canPublish(item) ? '允许发布' : '暂不可发布'
}

function publishChecks(item) {
  return [
    {
      key: 'quality-uploaded',
      label: '已上传质检',
      done: qualityUploaded(item),
      detail: qualityUploaded(item) ? `当前状态：${item.qualityStatus || '已上传'}` : '还没有质检摘要'
    },
    {
      key: 'quality-passed',
      label: '质检允许发布',
      done: qualityPassed(item),
      detail: qualityUploaded(item)
        ? (qualityPassed(item) ? '当前结论为合格' : '当前结论为不合格')
        : '先上传质检摘要'
    },
    {
      key: 'qr-generated',
      label: '已生成二维码',
      done: hasQr(item),
      detail: hasQr(item) ? `公开标识：${item.qrToken || '已生成'}` : '还没有二维码'
    },
    {
      key: 'risk-clear',
      label: '风险状态允许发布',
      done: riskAllowsPublish(item),
      detail: riskAllowsPublish(item)
        ? (String(item.status || '').toUpperCase() === 'FROZEN' ? '整改已完成，可恢复发布' : '当前没有风险阻塞')
        : `${item.riskStatusLabel || '当前风险状态'}仍在阻塞`
    }
  ]
}

function publishBlockers(item) {
  if (String(item.status || '').toUpperCase() === 'PUBLISHED') {
    return []
  }
  return publishChecks(item).filter((entry) => !entry.done)
}

function publishHint(item) {
  const batchStatus = String(item.status || '').toUpperCase()
  if (batchStatus === 'PUBLISHED') {
    return '二维码和公开页已经生效，可以直接核对扫码入口和详情状态。'
  }
  const gate = publishGate(item)
  if (!gate.allowed) {
    return gate.reason
  }
  if (canPublish(item)) {
    return actionOf(item, 'RESUME').enabled
      ? '整改和复核条件都已满足，可以直接恢复发布。'
      : '质检、二维码和风险条件都已就绪，可以直接发布。'
  }
  const blockers = publishBlockers(item)
  if (blockers.length) {
    return `当前阻塞：${blockers.map((entry) => entry.label).join('、')}`
  }
  return actionOf(item, 'PUBLISH').hint || actionOf(item, 'RESUME').hint || '当前还不满足发布条件。'
}

function currentStateChips(item) {
  return [
    {
      key: 'batch',
      label: item.statusLabel || '状态待确认',
      tone: batchStatusTone(item)
    },
    {
      key: 'quality',
      label: item.qualityStatus || '待上传质检',
      tone: qualityToneClass(item)
    }
  ]
}

function compactPublishChecks(item) {
  if (!riskAllowsPublish(item)) {
    return [{
      key: 'risk',
      label: String(item.status || '').toUpperCase() === 'RECALLED' ? '已召回' : '风险未解除',
      tone: String(item.status || '').toUpperCase() === 'RECALLED' ? 'danger' : 'warning'
    }]
  }
  if (!qualityPassed(item)) {
    return [{
      key: 'quality',
      label: qualityUploaded(item) ? '质检不通过' : '缺质检',
      tone: qualityUploaded(item) ? 'danger' : 'pending'
    }]
  }
  if (!hasQr(item)) {
    return [{
      key: 'qr',
      label: '缺二维码',
      tone: 'pending'
    }]
  }
  return [{
    key: 'ready',
    label: '已生成二维码',
    tone: 'info'
  }]
}

function latestUpdatedText(item) {
  return item.lastUpdatedAt || item.latestTraceTime || '暂无更新'
}

function printTimestamp() {
  return new Date().toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function primaryQrActionCode(item) {
  const published = String(item.status || '').toUpperCase() === 'PUBLISHED'
  if (!hasQr(item)) {
    return 'generate'
  }
  if (!published && canPublish(item)) {
    return 'publish'
  }
  if (published) {
    return 'public'
  }
  return 'preview'
}

function primaryQrActionLabel(item) {
  return {
    generate: '生成二维码',
    publish: publishActionLabel(item),
    public: '公开页',
    preview: '预览'
  }[primaryQrActionCode(item)]
}

function primaryQrActionClass(item) {
  return {
    generate: 'primary',
    publish: 'success',
    public: 'ghost',
    preview: 'ghost'
  }[primaryQrActionCode(item)]
}

function primaryQrActionDisabled(item) {
  const action = primaryQrActionCode(item)
  if (action === 'generate') {
    return !actionOf(item, 'GENERATE_QR').enabled || qrSubmittingId.value === item.id
  }
  if (action === 'publish') {
    return !canPublish(item)
  }
  return !hasQr(item)
}

function runPrimaryQrAction(item) {
  const action = primaryQrActionCode(item)
  if (action === 'generate') {
    handleGenerateQr(item)
    return
  }
  if (action === 'publish') {
    openPublishDialog(item)
    return
  }
  if (action === 'public') {
    openPublicPage(item)
    return
  }
  openPreviewDialog(item)
}

function handleQrRowCommand(item, command) {
  if (command === 'preview') {
    openPreviewDialog(item)
    return
  }
  if (command === 'download') {
    downloadQr(item)
    return
  }
  if (command === 'public') {
    openPublicPage(item)
    return
  }
  if (command === 'publish') {
    openPublishDialog(item)
    return
  }
  if (command === 'generate') {
    handleGenerateQr(item)
  }
}

function toggleRowSelection(item, checked) {
  if (!hasQr(item)) {
    return
  }
  if (checked) {
    selectedIds.value = [...new Set([...selectedIds.value, item.id])]
    return
  }
  selectedIds.value = selectedIds.value.filter((id) => id !== item.id)
}

function toggleSelectAllPrintable(checked) {
  if (!checked) {
    selectedIds.value = selectedIds.value.filter((id) => !selectableVisibleRows.value.some((item) => item.id === id))
    return
  }
  selectedIds.value = [...new Set([...selectedIds.value, ...selectableVisibleRows.value.map((item) => item.id)])]
}

function clearSelection() {
  selectedIds.value = []
}

function matchesKeyword(item) {
  const keyword = filters.value.keyword.trim().toLowerCase()
  if (!keyword) {
    return true
  }
  return [item.batchCode, item.productName]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword))
}

function matchesCompany(item) {
  const companyKeyword = filters.value.companyName.trim().toLowerCase()
  if (!companyKeyword) {
    return true
  }
  return String(item.companyName || '').toLowerCase().includes(companyKeyword)
}

function matchesTab(item, tabValue = activeTab.value) {
  const published = String(item.status || '').toUpperCase() === 'PUBLISHED'
  if (tabValue === 'NEED_QR') {
    return !hasQr(item) && !published
  }
  if (tabValue === 'READY') {
    return !published && hasQr(item) && canPublish(item)
  }
  if (tabValue === 'PUBLISHED') {
    return published
  }
  return !published && !canPublish(item)
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
    const rowIds = new Set(rows.value.map((item) => item.id))
    selectedIds.value = selectedIds.value.filter((id) => rowIds.has(id))
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码与发布列表加载失败，请稍后再试。'), 'error')
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
  if (nextPageSize === pageSize.value) {
    return
  }
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
  router.push(`/batches/${item.id}`)
}

async function loadBatchDetail(item) {
  const response = await getBatchDetail(item.id)
  return response.data ?? null
}

async function resolveQrPreview(item) {
  const detail = await loadBatchDetail(item)
  if (!detail?.qr?.generated) {
    throw new Error('当前批次还没有生成二维码。')
  }
  return {
    token: detail.qr.token,
    publicUrl: detail.qr.publicUrl,
    imageUrl: detail.qr.imageUrl,
    generatedAt: detail.qr.generatedAt
  }
}

async function handleGenerateQr(item) {
  qrSubmittingId.value = item.id
  try {
    await generateBatchQr(item.id)
    await fetchRows()
    showMessage(`批次 ${item.batchCode} 的二维码已生成。`, 'success')
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码生成失败，请稍后再试。'), 'error')
  } finally {
    qrSubmittingId.value = null
  }
}

async function openPreviewDialog(item) {
  previewDialog.value = {
    visible: true,
    loading: true,
    batch: item,
    qr: null
  }
  try {
    previewDialog.value.qr = await resolveQrPreview(item)
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码预览加载失败，请稍后再试。'), 'error')
    previewDialog.value.visible = false
  } finally {
    previewDialog.value.loading = false
  }
}

function closePreviewDialog() {
  previewDialog.value = createPreviewDialogState()
}

async function openBulkPrintPreview() {
  if (!selectedPrintableRows.value.length) {
    showMessage('请先勾选至少 1 个已生成二维码的批次。', 'info')
    return
  }

  let popup
  try {
    popup = openPrintPreviewWindow('二维码批量打印预览')
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '打印预览窗口打开失败，请稍后重试。'), 'error')
    return
  }

  bulkPrintSubmitting.value = true
  try {
    const items = await Promise.all(
      selectedPrintableRows.value.map(async (item) => {
        const qr = await resolveQrPreview(item)
        return {
          batchCode: item.batchCode,
          productName: item.productName || item.batchCode,
          companyName: item.companyName || '未标注企业',
          qrToken: qr.token,
          publicUrl: qr.publicUrl,
          imageUrl: qr.imageUrl
        }
      })
    )
    renderQrPrintPreview(popup, {
      title: '二维码批量打印预览',
      subtitle: `共 ${items.length} 个批次，已按当前列表选择生成打印内容。`,
      printedAt: printTimestamp(),
      items
    })
    showMessage(`已生成 ${items.length} 个批次的打印预览。`, 'success')
  } catch (error) {
    popup.close()
    showMessage(getFriendlyErrorMessage(error, '批量打印预览生成失败，请稍后重试。'), 'error')
  } finally {
    bulkPrintSubmitting.value = false
  }
}

async function downloadQr(item) {
  downloadSubmittingId.value = item.id
  try {
    const qr = await resolveQrPreview(item)
    const response = await fetch(qr.imageUrl)
    if (!response.ok) {
      throw new Error(`二维码下载失败：${response.status}`)
    }
    const blob = await response.blob()
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `${item.batchCode}-qr.png`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
    showMessage(`二维码已下载：${item.batchCode}-qr.png`, 'success')
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码下载失败，请稍后再试。'), 'error')
  } finally {
    downloadSubmittingId.value = null
  }
}

async function openPublicPage(item) {
  try {
    const qr = await resolveQrPreview(item)
    window.open(qr.publicUrl, '_blank', 'noopener')
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '公开页入口加载失败，请稍后再试。'), 'error')
  }
}

function defaultPublishReason(item) {
  return String(item.status || '').toUpperCase() === 'FROZEN'
    ? '整改已完成，恢复批次公开流通。'
    : '质检与二维码已齐，准备对外发布。'
}

function openPublishDialog(item) {
  publishDialog.value = {
    visible: true,
    batch: item,
    reason: defaultPublishReason(item)
  }
}

function closePublishDialog() {
  publishDialog.value = createPublishDialogState()
}

async function submitPublish() {
  if (publishSubmitting.value) {
    return
  }
  if (!publishDialog.value.batch?.id) {
    return
  }
  if (publishDialogError.value) {
    showMessage(publishDialogError.value, 'error')
    return
  }
  const actionLabel = publishActionLabel(publishDialog.value.batch || {})
  const confirmed = window.confirm(`确认${actionLabel}批次 ${publishDialog.value.batch.batchCode} 吗？`)
  if (!confirmed) {
    return
  }
  publishSubmitting.value = true
  try {
    await changeBatchStatus(publishDialog.value.batch.id, {
      targetStatus: 'PUBLISHED',
      reason: publishDialog.value.reason.trim(),
      operatorName: authStore.user?.realName || authStore.user?.username || '平台管理员'
    })
    await fetchRows()
    showMessage(`${publishActionLabel(publishDialog.value.batch)}已完成，列表状态已刷新。`, 'success')
    closePublishDialog()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '发布失败，请稍后再试。'), 'error')
  } finally {
    publishSubmitting.value = false
  }
}
</script>

<template>
  <div class="page-shell" data-testid="qr-publish-page">
    <div class="manage-page qr-manage">
    <div class="manage-summary-row">
      <div class="manage-summary">
        <button
          v-for="card in qrOverviewCards"
          :key="card.value"
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': activeTab === card.value }"
          :data-testid="`qr-tab-${card.value}`"
          @click="activeTab = card.value"
        >
          <span>{{ card.label }}</span>
          <strong>{{ card.count }}</strong>
        </button>
      </div>

      <div class="manage-summary-actions">
        <button class="ghost" data-testid="qr-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
      </div>
    </div>

    <section class="panel manage-filter-card qr-filter-panel">
      <div class="manage-filter-grid qr-filter-grid">
        <label>
          <span>批次名称 / 编号</span>
          <input v-model.trim="filters.keyword" data-testid="qr-filter-keyword" type="text" placeholder="输入批次编号或产品名称">
        </label>
        <label>
          <span>企业</span>
          <input v-model.trim="filters.companyName" type="text" placeholder="输入企业名称">
        </label>
        <label>
          <span>批次状态</span>
          <select v-model="filters.status">
            <option v-for="option in statusOptions" :key="option.value || 'all'" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <div class="qr-page-size-control">
          <span class="manage-muted">每页显示</span>
          <select v-model="pageSize" data-testid="qr-page-size" class="qr-page-size-select" @change="handlePageSizeChange($event.target.value)">
            <option v-for="item in pageSizeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
        <div class="toolbar-actions qr-filter-actions">
          <button class="primary" data-testid="qr-search-button" :disabled="loading" @click="handleSearch">查询</button>
          <button class="ghost" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>

      <div class="toolbar qr-filter-meta">
        <div class="list-summary">{{ listSummary }}</div>
      </div>

      <div class="toolbar qr-bulk-toolbar">
        <div class="list-summary">已勾选 {{ selectedPrintableRows.length }} 个可打印批次</div>
        <div class="toolbar-actions">
          <button
            class="ghost"
            data-testid="qr-bulk-select-all"
            :disabled="!selectableVisibleRows.length"
            @click="toggleSelectAllPrintable(!allSelectableVisibleChecked)"
          >
            {{ allSelectableVisibleChecked ? '取消本页全选' : '全选本页已有码批次' }}
          </button>
          <button
            class="ghost"
            data-testid="qr-bulk-clear"
            :disabled="!selectedIds.length"
            @click="clearSelection"
          >
            清空选择
          </button>
          <button
            class="primary"
            data-testid="qr-bulk-print"
            :disabled="bulkPrintSubmitting || !selectedPrintableRows.length"
            @click="openBulkPrintPreview"
          >
            {{ bulkPrintSubmitting ? '正在生成打印页...' : `批量打印预览（${selectedPrintableRows.length}）` }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section class="panel ledger-panel qr-ledger-panel">
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">二维码与发布台账</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div>
          <h3>正在加载二维码与发布列表...</h3>
        </div>
      </div>

      <div v-else-if="!visibleRows.length" class="empty-state">
        <div>
          <h3>当前筛选下没有批次</h3>
        </div>
      </div>

      <div v-else class="table-scroll-shell ledger-table-shell qr-table-shell">
        <div class="ledger-table-head qr-head">
          <span>选择</span>
          <span>批次</span>
          <span>企业 / 更新时间</span>
          <span>当前状态</span>
          <span>发布前检查</span>
          <span>二维码 / 公开入口</span>
          <span>操作</span>
        </div>

        <div class="ledger-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="ledger-row qr-row"
          :data-testid="`qr-row-${item.id}`"
        >
          <div class="row-select">
            <label class="selection-check">
              <input
                :checked="selectedIds.includes(item.id)"
                :disabled="!hasQr(item)"
                :data-testid="`qr-select-row-${item.id}`"
                type="checkbox"
                @change="toggleRowSelection(item, $event.target.checked)"
              >
            </label>
          </div>

          <div class="row-main">
            <strong>{{ item.productName }}</strong>
            <small>{{ item.batchCode }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ item.companyName }}</strong>
            <small>{{ latestUpdatedText(item) }}</small>
          </div>

          <div class="row-status qr-status-overview">
            <div class="status-chip-row qr-status-chip-row">
              <span
                v-for="chip in currentStateChips(item)"
                :key="chip.key"
                class="status-chip"
                :class="chip.tone"
              >
                {{ chip.label }}
              </span>
            </div>
          </div>

          <div class="row-status qr-publish-overview">
            <strong class="publish-summary" :class="publishToneClass(item)">{{ publishSummary(item) }}</strong>
            <div class="status-chip-row qr-check-chip-row">
              <span
                v-for="entry in compactPublishChecks(item)"
                :key="entry.key"
                class="qr-check-chip"
                :class="entry.tone"
              >
                {{ entry.label }}
              </span>
            </div>
          </div>

          <div class="row-meta qr-entry-panel">
            <strong>{{ item.qrToken || '暂无公开标识' }}</strong>
            <small v-if="item.marketDate">公开时间：{{ item.marketDate }}</small>
          </div>

          <div class="row-actions">
            <div class="row-actions-scroll qr-actions-row">
              <button
                :class="primaryQrActionClass(item)"
                class="action-primary-button"
                :disabled="primaryQrActionDisabled(item)"
                :data-testid="`qr-primary-${item.id}`"
                @click="runPrimaryQrAction(item)"
              >
                {{ qrSubmittingId === item.id && primaryQrActionCode(item) === 'generate' ? '正在生成...' : primaryQrActionLabel(item) }}
              </button>
              <button
                class="text-button primary-text"
                :data-testid="`qr-workbench-${item.id}`"
                @click="openWorkbench(item)"
              >
                  详情
              </button>
              <el-dropdown @command="(command) => handleQrRowCommand(item, command)">
                <button type="button" class="text-button">更多</button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-if="hasQr(item) && primaryQrActionCode(item) !== 'public'"
                      command="public"
                    >
                      公开页
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="hasQr(item) && primaryQrActionCode(item) !== 'preview'"
                      command="preview"
                    >
                      预览
                    </el-dropdown-item>
                    <el-dropdown-item v-if="hasQr(item)" command="download">
                      下载二维码
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="!hasQr(item) && primaryQrActionCode(item) !== 'generate'"
                      command="generate"
                    >
                      生成二维码
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="canPublish(item) && primaryQrActionCode(item) !== 'publish'"
                      command="publish"
                    >
                      {{ publishActionLabel(item) }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </article>
        </div>
      </div>

      <div class="toolbar qr-pagination">
        <span class="list-summary">第 {{ page }} / {{ pageCount }} 页</span>
        <div class="toolbar-actions">
          <button class="ghost" data-testid="qr-prev-page" :disabled="loading || page <= 1" @click="goPrevPage">上一页</button>
          <button class="ghost" data-testid="qr-next-page" :disabled="loading || page >= pageCount" @click="goNextPage">下一页</button>
        </div>
      </div>
    </section>

    <div v-if="previewDialog.visible" class="dialog-mask">
      <section class="dialog-card qr-preview-dialog" data-testid="qr-preview-dialog">
        <div class="dialog-head">
          <div>
            <h3>二维码预览</h3>
            <p>{{ previewDialog.batch?.batchCode }} · {{ previewDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" data-testid="qr-preview-close" @click="closePreviewDialog">关闭</button>
        </div>

        <div v-if="previewDialog.loading" class="empty-state">
          <div>
            <h3>二维码加载中...</h3>
            <p class="empty-copy">正在读取当前批次的公开入口。</p>
          </div>
        </div>

        <template v-else>
          <div class="qr-preview-body">
            <article class="qr-preview-frame">
              <img class="qr-preview-image" :src="previewDialog.qr?.imageUrl" alt="追溯二维码" data-testid="qr-preview-image">
            </article>
            <article class="overview-grid">
              <div>
                <span>公开标识</span>
                <strong data-testid="qr-preview-token">{{ previewDialog.qr?.token || '-' }}</strong>
              </div>
              <div>
                <span>生成时间</span>
                <strong>{{ previewDialog.qr?.generatedAt || '暂无时间' }}</strong>
              </div>
            </article>
          </div>

          <div class="inline-link-group">
            <a class="preview-link" :href="previewDialog.qr?.imageUrl" target="_blank" rel="noreferrer">查看原图</a>
            <a class="preview-link" :href="previewDialog.qr?.publicUrl" target="_blank" rel="noreferrer">查看公开页</a>
          </div>

          <div class="dialog-actions" style="margin-top: 18px;">
            <button class="ghost" @click="downloadQr(previewDialog.batch)">下载二维码</button>
          <button class="primary" @click="openWorkbench(previewDialog.batch)">查看详情</button>
          </div>
        </template>
      </section>
    </div>

    <div v-if="publishDialog.visible" class="dialog-mask">
      <section class="dialog-card" data-testid="qr-publish-dialog">
        <div class="dialog-head">
          <div>
            <h3>{{ publishActionLabel(publishDialog.batch) }}</h3>
            <p>{{ publishDialog.batch?.batchCode }} · {{ publishDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" :disabled="publishSubmitting" @click="closePublishDialog">关闭</button>
        </div>

        <div class="publish-checklist publish-dialog-checks">
          <article
            v-for="entry in publishChecks(publishDialog.batch || {})"
            :key="entry.key"
            class="publish-check"
            :class="{ done: entry.done, blocked: !entry.done }"
          >
            <strong>{{ entry.label }}</strong>
            <small>{{ entry.detail }}</small>
          </article>
        </div>

        <label class="full-width" style="display: grid; gap: 8px; margin-top: 18px;">
          <span>处理说明</span>
          <textarea
            v-model.trim="publishDialog.reason"
            data-testid="qr-publish-reason"
            placeholder="补充本次发布或恢复发布的说明"
          />
        </label>

        <p v-if="publishDialogError" class="form-error">{{ publishDialogError }}</p>

          <p class="hint-note" style="margin-top: 14px;">不可发布时，先回列表查看阻塞项或回详情补齐质检、二维码和风险处理。</p>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="publishSubmitting" @click="closePublishDialog">取消</button>
          <button
            class="success"
            data-testid="qr-publish-submit"
            :disabled="publishSubmitting || !canPublish(publishDialog.batch || {}) || Boolean(publishDialogError)"
            @click="submitPublish"
          >
            {{ publishSubmitting ? '正在提交...' : publishActionLabel(publishDialog.batch || {}) }}
          </button>
        </div>
        </section>
      </div>
    </div>
    </div>
</template>

<style src="../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.qr-manage {
  gap: 14px;
}

.qr-filter-grid {
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr) minmax(180px, 0.9fr) minmax(188px, max-content) auto;
  align-items: end;
}

.qr-filter-grid label {
  display: grid;
  gap: 8px;
}

.qr-filter-grid label > span,
.qr-page-size-control .manage-muted {
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
}

.qr-filter-grid input,
.qr-filter-grid select,
.qr-page-size-select {
  width: 100%;
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid var(--admin-border);
  border-radius: 10px;
  background: #fff;
  color: var(--admin-text);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.qr-filter-grid input:focus,
.qr-filter-grid select:focus,
.qr-page-size-select:focus {
  border-color: rgba(48, 149, 246, 0.26);
  box-shadow: 0 0 0 3px rgba(48, 149, 246, 0.08);
  outline: none;
}

.qr-page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 188px;
}

.qr-page-size-select {
  width: 128px;
}

.qr-filter-actions {
  justify-content: flex-end;
  flex-wrap: nowrap;
  min-width: 170px;
}

.qr-filter-meta {
  margin-top: 12px;
}

.qr-bulk-toolbar {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(56, 134, 217, 0.1);
}

.qr-head,
.qr-row {
  grid-template-columns:
    minmax(34px, 0.18fr)
    minmax(0, 1.16fr)
    minmax(0, 0.9fr)
    minmax(0, 0.7fr)
    minmax(0, 0.82fr)
    minmax(0, 0.72fr)
    minmax(0, 1.6fr);
}

.row-select {
  display: flex;
  align-items: center;
  justify-content: center;
}

.selection-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.selection-check input {
  width: 18px;
  height: 18px;
  accent-color: var(--admin-primary);
}

.qr-table-shell .ledger-table-head,
.qr-table-shell .ledger-row-list {
  width: 100%;
  min-width: 0;
}

.qr-publish-overview {
  min-width: 0;
}

.qr-check-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.qr-check-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.qr-status-chip-row .status-chip {
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

.qr-status-chip-row .status-chip.success,
.qr-check-chip.success {
  border-color: rgba(46, 166, 106, 0.22);
  background: rgba(46, 166, 106, 0.1);
  color: #1e7d50;
}

.qr-status-chip-row .status-chip.info,
.qr-check-chip.info {
  border-color: rgba(48, 149, 246, 0.18);
  background: rgba(48, 149, 246, 0.12);
  color: var(--admin-primary-deep);
}

.qr-status-chip-row .status-chip.warning,
.qr-check-chip.warning {
  border-color: rgba(242, 154, 52, 0.22);
  background: rgba(242, 154, 52, 0.12);
  color: #b96b16;
}

.qr-status-chip-row .status-chip.danger,
.qr-check-chip.danger {
  border-color: rgba(221, 74, 74, 0.22);
  background: rgba(221, 74, 74, 0.1);
  color: #b63f3f;
}

.qr-status-chip-row .status-chip.pending,
.qr-check-chip.pending {
  border-color: rgba(120, 146, 173, 0.18);
  background: rgba(120, 146, 173, 0.12);
  color: #5f7b98;
}

.qr-row .row-actions {
  align-items: flex-start;
  min-width: 0;
}

.qr-actions-row {
  flex-wrap: nowrap;
  overflow: visible;
  width: 100%;
  justify-content: flex-start;
}

.qr-actions-row .action-primary-button {
  min-width: auto;
  white-space: nowrap;
  min-height: 34px;
  padding: 0 12px;
  font-size: 13px;
}

.qr-actions-row .text-button {
  min-width: auto;
  white-space: nowrap;
  min-height: 34px;
  padding: 0 10px;
  font-size: 12px;
}

.qr-row .row-main,
.qr-row .row-meta,
.qr-row .qr-publish-overview,
.qr-row .row-select {
  min-width: 0;
}

.qr-status-overview,
.qr-publish-overview {
  gap: 6px;
}

.qr-status-chip-row,
.qr-check-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.publish-summary {
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
  width: fit-content;
}

.qr-pagination {
  margin-top: 18px;
  padding: 0 28px;
}

.qr-pagination .list-summary {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
}

.success {
  background: rgba(46, 166, 106, 0.12);
  color: #1e7d50;
}

.danger {
  background: rgba(221, 74, 74, 0.12);
  color: #b63f3f;
}

.pending {
  background: rgba(129, 154, 184, 0.14);
  color: #57718e;
}

.form-error {
  margin: 12px 0 0;
  color: #b63f3f;
  line-height: 1.6;
}

.empty-state {
  min-height: 150px;
}

@media (max-width: 1180px) {
  .qr-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .qr-page-size-control {
    min-width: 0;
  }

  .qr-filter-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .qr-filter-grid {
    grid-template-columns: 1fr;
  }

  .qr-row {
    grid-template-columns: 1fr;
  }

  .qr-page-size-control {
    justify-content: space-between;
  }

  .qr-page-size-select {
    width: 100%;
  }

  .row-select {
    margin-bottom: 4px;
  }
}
</style>
