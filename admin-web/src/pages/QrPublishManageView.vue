<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { changeBatchStatus, generateBatchQr, getBatchDetail, getBatchList } from '../api/batch'
import AdminListPagination from '../components/AdminListPagination.vue'
import AdminListTemplate from '../components/AdminListTemplate.vue'
import BatchWorkbenchDrawerPanel from '../components/BatchWorkbenchDrawerPanel.vue'
import { useAuthStore } from '../stores/auth'
import { getFriendlyErrorMessage } from '../utils/batchExperience'
import { resolvePublishBlockState } from '../utils/batchStatusFlow'
import { openPrintPreviewWindow, renderQrPrintPreview } from '../utils/exportTools'
import { resolveQrStatusText } from '../utils/statusPresentation'
import { generateBrandedQrDataUrl } from '../utils/brandedQr'

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
const detailDrawer = ref(createWorkbenchDrawerState())
const selectedIds = ref([])
const DEFAULT_PAGE_SIZE = 10
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const batchSideDrawerSize = 'min(620px, 96vw)'
const pageTitle = '二维码发布'
const pageSubtitle = '统一查看二维码生成、发布校验、公开入口与批量打印状态，适合做答辩演示台账。'
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]

const cleanPageTitle = '二维码与发布'
const cleanPageSubtitle = ''

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
const qrBoardCards = computed(() => {
  return qrOverviewCards.value.map((item) => ({
    key: item.value,
    label: item.label,
    value: item.count,
    detail: item.detail
  }))
})
const qrSummaryTabs = computed(() => ([
  { value: 'NEED_QR', label: '待生成', count: tabCounts.value.NEED_QR ?? 0 },
  { value: 'READY', label: '待发布', count: tabCounts.value.READY ?? 0 },
  { value: 'PUBLISHED', label: '已发布', count: tabCounts.value.PUBLISHED ?? 0 },
  { value: 'BLOCKED', label: '不可发布', count: tabCounts.value.BLOCKED ?? 0 }
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

function switchActiveTab(tab) {
  activeTab.value = tab
  filters.value.status = ''
  selectedIds.value = []
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

function createWorkbenchDrawerState() {
  return {
    visible: false,
    batchId: null,
    batchCode: ''
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

function qrStageText(item) {
  const published = String(item.status || '').toUpperCase() === 'PUBLISHED'
  if (published) {
    return '已发布'
  }
  if (!hasQr(item)) {
    return '待生成'
  }
  if (canPublish(item)) {
    return '待发布'
  }
  return '不可发布'
}

function qrStageTone(item) {
  const stage = qrStageText(item)
  if (stage === '已发布') return 'success'
  if (stage === '待发布') return 'info'
  if (stage === '待生成') return 'pending'
  return 'warning'
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

function qrStatusChips(item) {
  const chips = [
    {
      key: 'batch',
      label: item.statusLabel || '状态待确认',
      tone: statusClass(item.status)
    },
    {
      key: 'qr',
      label: hasQr(item) ? '二维码已生成' : '待生成二维码',
      tone: hasQr(item) ? 'info' : 'pending'
    }
  ]

  if (String(item.status || '').toUpperCase() === 'PUBLISHED') {
    chips.push({
      key: 'publish',
      label: '已发布',
      tone: 'success'
    })
  } else if (canPublish(item)) {
    chips.push({
      key: 'publish',
      label: '可发布',
      tone: 'success'
    })
  }

  return chips
}

function latestUpdatedText(item) {
  return item.lastUpdatedAt || item.latestTraceTime || '暂无更新'
}

function compactQrStatusChips(item) {
  return [
    {
      key: 'batch',
      label: item.statusLabel || '状态待确认',
      tone: statusClass(item.status)
    },
    {
      key: 'stage',
      label: qrStageText(item),
      tone: qrStageTone(item)
    }
  ]
}

function shortQrToken(item) {
  const token = String(item.qrToken || '').trim()
  if (!token) {
    return '暂无公开标识'
  }
  if (token.length <= 18) {
    return token
  }
  return `${token.slice(0, 10)}...${token.slice(-4)}`
}

function displayPublishTime(item) {
  return item.marketDate || latestUpdatedText(item)
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
  if (!hasQr(item)) {
    return 'generate'
  }
  if (canPublish(item) && String(item.status || '').toUpperCase() !== 'PUBLISHED') {
    return 'publish'
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

function showPublicAction(item) {
  return hasQr(item)
}

function showPreviewAction(item) {
  return hasQr(item)
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
  return [item.batchCode, item.productName, item.productCode]
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
  if (!item?.id) return
  detailDrawer.value = {
    visible: true,
    batchId: item.id,
    batchCode: item.batchCode ?? ''
  }
}

function closeWorkbenchDrawer() {
  detailDrawer.value = createWorkbenchDrawerState()
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
  const token = detail.qr.token
  const publicUrl = resolvePublicTraceUrl(token, detail.qr.publicUrl)
  return {
    token,
    publicUrl,
    imageUrl: await generateBrandedQrDataUrl(publicUrl, { width: 320 }),
    originalImageUrl: detail.qr.imageUrl,
    generatedAt: detail.qr.generatedAt
  }
}

function resolvePublicTraceUrl(token, fallbackUrl = '') {
  const traceToken = String(token || '').trim()
  if (!traceToken) return fallbackUrl || ''
  const tracePath = `/t/${encodeURIComponent(traceToken)}`

  const envOrigin = String(import.meta.env.VITE_TRACE_WEB_ORIGIN || '').trim().replace(/\/$/, '')
  if (envOrigin) {
    return `${envOrigin}${tracePath}`
  }

  if (import.meta.env.DEV) {
    return `http://127.0.0.1:5173${tracePath}`
  }

  if (typeof window !== 'undefined' && window.location?.hostname) {
    const { protocol, hostname } = window.location
    return `${protocol}//${hostname}:5173${tracePath}`
  }

  return fallbackUrl || `http://127.0.0.1:5173${tracePath}`
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
    <AdminListTemplate
      template-class="qr-card-stack"
      filter-card-class="qr-filter-panel"
      ledger-card-class="qr-ledger-panel qr-ledger-card"
    >
      <template #summary>
      <div class="manage-summary">
        <button
          v-for="item in qrSummaryTabs"
          :key="item.value"
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': activeTab === item.value }"
          :data-testid="`qr-tab-${item.value}`"
          @click="switchActiveTab(item.value)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.count }}</strong>
        </button>
      </div>
      </template>

      <template #actions>
        <el-button data-testid="qr-refresh-button" :loading="loading" @click="fetchRows">刷新</el-button>
      </template>

    <template #filterPrimary>
      <div class="qr-filter-layout">
        <div class="manage-filter-grid qr-filter-grid">
          <label class="manage-filter-field qr-filter-field qr-filter-field--keyword">
            <span class="manage-filter-field__label">批次名称 / 编号</span>
            <el-input
              v-model.trim="filters.keyword"
              clearable
              class="manage-filter-item"
              data-testid="qr-filter-keyword"
              placeholder="输入批次编号或产品名称"
              @keyup.enter="handleSearch"
            />
          </label>
          <label class="manage-filter-field qr-filter-field qr-filter-field--company">
            <span class="manage-filter-field__label">企业</span>
            <el-input
              v-model.trim="filters.companyName"
              clearable
              class="manage-filter-item"
              placeholder="输入企业名称"
              @keyup.enter="handleSearch"
            />
          </label>
          <label class="manage-filter-field qr-filter-field qr-filter-field--status">
            <span class="manage-filter-field__label">批次状态</span>
            <el-select
              v-model="filters.status"
              class="manage-filter-item"
              placeholder="全部批次状态"
            >
              <el-option
                v-for="option in statusOptions"
                :key="option.value || 'all'"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>
          <label class="manage-filter-field qr-filter-field qr-filter-field--page-size">
            <span class="manage-filter-field__label">每页显示</span>
            <el-select
              :model-value="pageSize"
              class="manage-filter-item qr-page-size-select"
              data-testid="qr-page-size"
              @change="handlePageSizeChange"
            >
              <el-option v-for="item in pageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
        </div>
      </div>
    </template>

    <template #filterSecondary>
      <div class="qr-filter-toolbar">
        <div class="qr-filter-actions">
          <el-button type="primary" data-testid="qr-search-button" :disabled="loading" @click="handleSearch">查询</el-button>
          <el-button :disabled="loading" @click="resetFilters">重置</el-button>
        </div>
        <div class="qr-list-summary">
          <span class="manage-muted">{{ cleanListSummary }}</span>
        </div>
      </div>
    </template>

      <template #message>
        <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>
      </template>

    <template #ledger>
        <div class="panel-heading">
          <div class="panel-heading__copy">
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
          <span>批次</span>
          <span>企业 / 更新时间</span>
          <span>状态</span>
          <span>公开标识</span>
          <span>操作</span>
        </div>

        <div class="ledger-row-list qr-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="ledger-row qr-row"
          :data-testid="`qr-row-${item.id}`"
        >
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
                v-for="chip in compactQrStatusChips(item)"
                :key="chip.key"
                class="status-chip"
                :class="chip.tone"
              >
                {{ chip.label }}
              </span>
            </div>
          </div>

          <div class="row-meta qr-entry-panel">
            <strong :title="item.qrToken || '暂无公开标识'">{{ shortQrToken(item) }}</strong>
            <small>{{ displayPublishTime(item) }}</small>
          </div>

          <div class="row-actions table-cell--actions">
            <div class="row-actions-scroll ledger-actions-scroll qr-actions-row">
              <button
                v-if="!hasQr(item) || primaryQrActionCode(item) === 'publish'"
                class="text-button primary-text"
                :disabled="primaryQrActionDisabled(item)"
                :data-testid="`qr-primary-${item.id}`"
                @click="runPrimaryQrAction(item)"
              >
                {{ qrSubmittingId === item.id && primaryQrActionCode(item) === 'generate' ? '正在生成...' : primaryQrActionLabel(item) }}
              </button>
              <button
                v-if="showPreviewAction(item)"
                type="button"
                class="text-button primary-text"
                @click="openPreviewDialog(item)"
              >
                预览
              </button>
              <button
                class="text-button primary-text"
                :data-testid="`qr-workbench-${item.id}`"
                @click="openWorkbench(item)"
              >
                详情
              </button>
              <button
                v-if="showPublicAction(item)"
                type="button"
                class="text-button"
                @click="openPublicPage(item)"
              >
                公开页
              </button>
              <button
                v-if="hasQr(item)"
                type="button"
                class="text-button"
                :disabled="downloadSubmittingId === item.id"
                @click="downloadQr(item)"
              >
                {{ downloadSubmittingId === item.id ? '下载中...' : '下载二维码' }}
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
        prev-testid="qr-prev-page"
        next-testid="qr-next-page"
        @prev="goPrevPage"
        @next="goNextPage"
      />
    </template>
    </AdminListTemplate>

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
            <article class="overview-grid qr-preview-meta">
              <div>
                <span>公开标识</span>
                <strong data-testid="qr-preview-token">{{ previewDialog.qr?.token || '-' }}</strong>
              </div>
              <div>
                <span>生成时间</span>
                <strong>{{ previewDialog.qr?.generatedAt || '暂无时间' }}</strong>
              </div>
            </article>
            <article class="qr-preview-frame">
              <img
                class="qr-preview-image"
                :src="previewDialog.qr?.imageUrl"
                alt="追溯二维码"
                data-testid="qr-preview-image"
              >
            </article>
          </div>

          <div class="dialog-actions" style="margin-top: 18px;">
            <button class="ghost" @click="downloadQr(previewDialog.batch)">下载二维码</button>
            <button class="primary" @click="openWorkbench(previewDialog.batch); closePreviewDialog()">查看详情</button>
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

    <el-drawer
      v-model="detailDrawer.visible"
      direction="rtl"
      :size="batchSideDrawerSize"
      append-to-body
      destroy-on-close
      :with-header="false"
      class="batch-side-drawer qr-workbench-side-drawer"
      data-testid="qr-workbench-drawer"
    >
      <div class="drawer-shell batch-workbench-drawer-shell" data-testid="qr-workbench-drawer-shell">
        <div class="dialog-head">
          <div>
            <h3>批次工作台</h3>
            <p>批次 {{ detailDrawer.batchCode || '详情查看' }}</p>
          </div>
          <button class="ghost icon-button" @click="closeWorkbenchDrawer">关闭</button>
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
.qr-manage {
  gap: 14px;
  font-family: "PingFang SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif;
  --qr-filter-text-inset: 14px;
  --qr-grid-inline-padding: 18px;
  --qr-grid-column-gap: 12px;
  --qr-filter-group-left-shift: 8px;
  --qr-keyword-filter-width: 248px;
  --qr-company-filter-width: 188px;
  --qr-status-filter-width: 148px;
  --qr-page-size-width: 148px;
  --qr-filter-card-border: rgba(56, 134, 217, 0.14);
  --qr-filter-card-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 251, 255, 0.94) 100%);
  --qr-filter-card-shadow: 0 16px 34px rgba(45, 113, 194, 0.1);
  --qr-filter-control-height: 40px;
  --qr-filter-control-radius: 12px;
}

.qr-card-stack {
  display: grid;
  gap: 16px;
}

:deep(.qr-filter-panel),
:deep(.qr-ledger-card) {
  position: relative;
  overflow: hidden;
}

:deep(.qr-filter-panel) {
  padding: 18px 22px 0;
  border-color: var(--qr-filter-card-border) !important;
  background: var(--qr-filter-card-bg) !important;
  box-shadow: var(--qr-filter-card-shadow) !important;
}

:deep(.qr-ledger-card) {
  padding: 20px 22px 18px;
  border: 1px solid rgba(56, 134, 217, 0.14) !important;
  border-radius: 24px !important;
  background: #fff !important;
  box-shadow: 0 18px 42px rgba(45, 113, 194, 0.08) !important;
}

.qr-filter-layout {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 18px;
  margin-left: calc(-1 * var(--qr-filter-group-left-shift));
  padding: 0 12px 0 calc(var(--qr-grid-inline-padding) - var(--qr-filter-text-inset));
  flex-wrap: wrap;
}

.qr-filter-grid {
  grid-template-columns:
    minmax(var(--qr-keyword-filter-width), max-content)
    minmax(var(--qr-company-filter-width), max-content)
    minmax(var(--qr-status-filter-width), max-content)
    minmax(var(--qr-page-size-width), max-content);
  align-items: end;
  column-gap: 12px;
  row-gap: 12px;
  padding: 0;
  flex: 0 1 auto;
  min-width: 0;
}

.qr-filter-field {
  gap: 8px;
  width: 100%;
  justify-self: start;
}

.qr-filter-field--keyword {
  max-width: var(--qr-keyword-filter-width);
}

.qr-filter-field--company {
  max-width: var(--qr-company-filter-width);
}

.qr-filter-field--status {
  max-width: var(--qr-status-filter-width);
}

.qr-filter-field--page-size {
  max-width: var(--qr-page-size-width);
}

.qr-filter-grid .manage-filter-field__label {
  padding-inline-start: var(--qr-filter-text-inset);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.qr-filter-grid :deep(.manage-filter-item .el-input__wrapper),
.qr-filter-grid :deep(.manage-filter-item .el-select__wrapper) {
  min-height: var(--qr-filter-control-height);
  padding-inline-start: var(--qr-filter-text-inset);
  padding-inline-end: 14px;
  border-radius: var(--qr-filter-control-radius);
  background: #fff;
  box-shadow: 0 0 0 1px rgba(56, 134, 217, 0.14) inset !important;
}

.qr-filter-grid :deep(.manage-filter-item .el-input__inner),
.qr-filter-grid :deep(.manage-filter-item .el-select__selected-item),
.qr-filter-grid :deep(.manage-filter-item .el-select__placeholder) {
  text-align: left;
}

.qr-filter-grid :deep(.manage-filter-item .el-select__placeholder),
.qr-filter-grid :deep(.manage-filter-item .el-input__inner::placeholder) {
  color: var(--admin-text-faint);
}

.qr-page-size-select {
  width: 100%;
}

.qr-page-size-select :deep(.el-select__wrapper) {
  min-height: var(--qr-filter-control-height);
}

.qr-filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  min-width: 0;
}

.qr-filter-actions :deep(.el-button) {
  min-height: 38px;
  padding-inline: 16px;
  border-radius: 12px;
}

.qr-filter-toolbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  margin-left: calc(-1 * var(--qr-filter-group-left-shift));
  padding: 0 12px 18px var(--qr-grid-inline-padding);
  min-width: 0;
  flex-wrap: wrap;
}

.qr-list-summary .manage-muted {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.qr-table-shell {
  --ledger-grid-columns:
    minmax(190px, 1.08fr)
    minmax(176px, 0.95fr)
    minmax(140px, 0.72fr)
    minmax(156px, 0.8fr)
    minmax(210px, 1.08fr);
  --ledger-column-gap: var(--qr-grid-column-gap);
  --ledger-inline-padding: var(--qr-grid-inline-padding);
  --ledger-min-width: 920px;
}

.qr-head {
  background: #fff;
}

:deep(.qr-ledger-panel .panel-heading) {
  padding-left: 28px;
}

:deep(.qr-ledger-card .table-scroll-shell) {
  background: #fff !important;
}

:deep(.qr-ledger-card .table-scroll-shell .qr-head) {
  background: #fff !important;
}

:deep(.qr-ledger-card .admin-list-pagination) {
  background: #fff !important;
}

:deep(.qr-ledger-card .panel-heading),
:deep(.qr-ledger-card .panel-heading > div) {
  background: #fff;
}

.qr-row-list,
.qr-row-list.ledger-row-list {
  display: grid;
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 26px;
  background: #fff;
}

.qr-row,
.qr-row.ledger-row {
  background: #fff;
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
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  overflow: visible;
  width: 100%;
}

.qr-actions-row .text-button {
  min-width: auto;
  white-space: nowrap;
  min-height: 34px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
  color: #2f5f95;
}

.qr-actions-row .primary-text {
  font-weight: 600;
}

.qr-row .row-main,
.qr-row .row-meta,
.qr-row .qr-publish-overview,
.qr-row .row-select {
  min-width: 0;
}

.qr-entry-panel strong {
  display: block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-status-overview {
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

.qr-preview-dialog {
  width: min(560px, calc(100vw - 48px));
  padding: 18px 20px 20px;
}

.qr-preview-dialog .dialog-head {
  margin-bottom: 14px;
}

.qr-preview-dialog .qr-preview-body {
  grid-template-columns: minmax(0, 1fr);
  justify-items: center;
  gap: 14px;
}

.qr-preview-dialog .qr-preview-meta {
  width: min(100%, 360px);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.qr-preview-dialog .qr-preview-meta strong {
  word-break: keep-all;
  overflow-wrap: break-word;
}

.qr-preview-dialog .qr-preview-frame {
  width: min(100%, 300px);
}

.qr-preview-dialog .dialog-actions {
  justify-content: center;
  margin-top: 14px !important;
}

.batch-side-drawer :deep(.el-drawer__body) {
  padding: 18px 18px 22px;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.batch-side-drawer .el-drawer__body) {
  overflow-y: auto;
  overflow-x: hidden;
}

.batch-workbench-drawer-shell {
  align-content: start;
}

@media (max-width: 1180px) {
  .qr-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .qr-filter-layout,
  .qr-filter-toolbar {
    margin-left: 0;
    padding-left: 0;
    padding-right: 0;
  }

  .qr-filter-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 760px) {
  .qr-filter-grid {
    grid-template-columns: 1fr;
  }

  .qr-filter-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .qr-row {
    grid-template-columns: 1fr;
  }

  .qr-filter-actions {
    width: 100%;
  }

  .qr-filter-actions :deep(.el-button) {
    flex: 1 1 auto;
  }

  .row-select {
    margin-bottom: 4px;
  }

  .qr-preview-dialog {
    width: min(100%, calc(100vw - 24px));
    padding: 16px;
  }

}
</style>

<style src="../assets/styles/admin-ledger-unified.css" scoped></style>
