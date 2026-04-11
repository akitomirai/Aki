<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createQualityReport, getBatchDetail, getBatchList, uploadBatchFiles } from '../api/batch'
import PrimaryActionGroup from '../components/PrimaryActionGroup.vue'
import StatusTag from '../components/StatusTag.vue'
import { useAuthStore } from '../stores/auth'
import { createQualityForm, getFriendlyErrorMessage, getFriendlyUploadError, qualityOptions, splitHighlights } from '../utils/batchExperience'
import { mapBackendRecommendedQualityActionCode } from '../utils/batchStatusFlow'
import { isRegulator } from '../utils/access'
import { resolveQrStatusText } from '../utils/statusPresentation'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const rows = ref([])
const message = ref('')
const messageType = ref('info')
const activeTab = ref('PENDING')
const filters = ref(createFilterState())
const qualityUploading = ref(false)
const qualitySubmitting = ref(false)
const qualityForm = ref(createQualityForm())
const resultDialog = ref(createResultDialogState())
const uploadDialog = ref(createUploadDialogState())
const roleCode = computed(() => authStore.user?.roleCode || '')
const readOnlyQualityView = computed(() => isRegulator(roleCode.value))
const pageTitle = computed(() => readOnlyQualityView.value ? '质检查看' : '质检待办')
const pageSubtitle = computed(() => {
  if (readOnlyQualityView.value) {
    return '监管账号可统一查看批次质检状态、报告结果、附件和发布准备情况，不提供上传或修改入口。'
  }
  return '从全局视角处理待上传、已合格、不合格和已上传待发布的批次，不必逐个进工作台找。'
})
const readOnlyBannerText = computed(() => '当前为监管查看模式，页面保留批次、企业、质检结论、二维码状态和最近更新时间，便于直接核对质检链路。')
const openWorkbenchText = computed(() => readOnlyQualityView.value ? '查看批次详情' : '查看工作台')

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

const visibleRows = computed(() => {
  return rows.value.filter((item) => matchesKeyword(item) && matchesCompany(item) && matchesTab(item))
})

const tabCounts = computed(() => {
  return qualityTabs.reduce((acc, item) => {
    acc[item.value] = rows.value.filter((row) => matchesTab(row, item.value)).length
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

const boardLeadText = computed(() => {
  if (readOnlyQualityView.value) {
    return `${activeTabMeta.value.label}看板会优先保留最新质检结论、二维码状态和发布时间线索，便于直接核对资料完整度。`
  }
  return {
    PENDING: '先补质检摘要，再回工作台继续生成二维码或发布。',
    PASS: '已合格批次重点核对二维码、公开页和发布动作。',
    FAIL: '不合格批次先看结果，再决定是否进入风险处理。',
    READY: '这一组批次已接近发布完成，适合答辩时直接讲主链路闭环。'
  }[activeTab.value] ?? '优先处理当前看板里最靠近发布的批次。'
})

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

function createFilterState() {
  return {
    keyword: '',
    companyName: '',
    status: ''
  }
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

function publishReadyText(item) {
  if (item.status === 'PUBLISHED') {
    return '已发布'
  }
  return resolvePublishReady(item) ? '允许发布' : '暂不可发布'
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

function qualityPriorityHint(item) {
  const qualityCode = String(item.qualityStatusCode || 'PENDING').toUpperCase()
  if (qualityCode === 'PENDING') {
    return '补完摘要后，工作台会同步更新发布准备度。'
  }
  if (qualityCode === 'FAIL') {
    return '建议先查看报告，再判断是否需要进入风险处理。'
  }
  if (resolvePublishReady(item)) {
    return '当前资料已接近收口，适合继续讲解发布和公开页。'
  }
  return item.qrStatus === 'NOT_GENERATED'
    ? '二维码还没准备好，先回工作台完成最后一步。'
    : '可以继续核对最近更新和公开页入口。'
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
    report: '查看质检结果',
    workbench: openWorkbenchText.value
  }[recommendedQualityActionCode(item)]
}

function recommendedQualityActionClass(item) {
  return {
    upload: 'primary',
    report: String(item.qualityStatusCode || '').toUpperCase() === 'FAIL' ? 'warning' : 'ghost',
    workbench: resolvePublishReady(item) ? 'success' : 'ghost'
  }[recommendedQualityActionCode(item)]
}

function qualitySecondaryActions(item) {
  const actions = []
  if (recommendedQualityActionCode(item) !== 'workbench') {
    actions.push({
      key: 'workbench',
      label: openWorkbenchText.value,
      testId: `quality-open-workbench-${item.id}`,
      disabled: false
    })
  }
  if (qualityReportAvailable(item) && recommendedQualityActionCode(item) !== 'report') {
    actions.push({
      key: 'report',
      label: '查看质检结果',
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

function handleSecondaryQualityAction(item, code) {
  if (code === 'workbench') {
    openWorkbench(item)
    return
  }
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

function defaultReportNo(item) {
  const stamp = new Date().toISOString().replace(/[^\d]/g, '').slice(0, 12)
  return `QA-${item.batchCode}-${stamp}`
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
    const response = await getBatchList(cleanObject({
      status: filters.value.status || undefined
    }))
    rows.value = response.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '质检待办加载失败，请稍后再试。'), 'error')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = createFilterState()
  fetchRows()
}

function openWorkbench(item) {
  router.push(`/batches/${item.id}`)
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
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">{{ pageTitle }}</h1>
        <p class="manage-page-subtitle">{{ pageSubtitle }}</p>
      </div>
      <div class="manage-page-actions">
        <button class="ghost" data-testid="quality-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
      </div>
    </section>

    <section v-if="readOnlyQualityView" class="panel readonly-banner" data-testid="quality-readonly-banner">
      <strong>监管查看模式</strong>
      <span>{{ readOnlyBannerText }}</span>
    </section>

    <section class="overview-cards">
      <article
        v-for="(card, index) in qualityOverviewCards"
        :key="qualityTabs[index].value"
        class="overview-card"
        :class="{ 'is-active': activeTab === qualityTabs[index].value }"
        :data-testid="`quality-tab-${qualityTabs[index].value}`"
        @click="activeTab = qualityTabs[index].value"
      >
        <span class="overview-card__icon" />
        <div class="overview-card__body">
          <span class="overview-card__label">{{ card.label }}</span>
          <strong class="overview-card__value">{{ card.value }}</strong>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">筛选条件</h2>
        </div>
      </div>

      <div class="filter-grid">
        <label>
          <span>批次名称 / 编号</span>
          <input v-model.trim="filters.keyword" data-testid="quality-filter-keyword" type="text" placeholder="输入批次编号或产品名称">
        </label>
        <label>
          <span>企业</span>
          <input v-model.trim="filters.companyName" type="text" placeholder="输入企业名称">
        </label>
        <label>
          <span>批次状态</span>
          <select v-model="filters.status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
      </div>

      <div class="toolbar">
        <span class="list-summary">当前看板为“{{ activeTabMeta.label }}”，共 {{ rows.length }} 个批次，当前显示 {{ visibleRows.length }} 个。</span>
        <div class="toolbar-actions">
          <button class="primary" data-testid="quality-search-button" :disabled="loading" @click="fetchRows">查询</button>
          <button class="ghost" data-testid="quality-reset-button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section v-if="loading" class="panel empty-state">
      <div>
        <h3>正在同步质检看板...</h3>
        <p class="empty-copy">请稍等，正在汇总批次、质检结论和发布准备度。</p>
      </div>
    </section>

    <section v-else-if="!visibleRows.length" class="panel empty-state">
      <div>
        <h3>当前看板下还没有质检任务</h3>
        <p class="empty-copy">可以切换看板或调整筛选条件后再看。</p>
      </div>
    </section>

    <section v-else class="panel">
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">质检任务台账</h2>
        </div>
      </div>

      <div class="todo-table-head quality-head">
        <span>批次概况</span>
        <span>当前判断</span>
        <span>发布准备</span>
        <span>推荐动作</span>
      </div>

      <div class="todo-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="todo-row quality-row"
          :data-testid="`quality-row-${item.id}`"
        >
          <div class="row-main quality-main">
            <div class="row-title-line">
              <strong>{{ item.productName }}</strong>
              <span>{{ item.batchCode }}</span>
            </div>
            <div class="row-chip-group">
              <StatusTag :text="item.statusLabel" :tone="statusClass(item.status)" />
              <StatusTag :text="qualityResultText(item)" :tone="resultStatusTone(item)" />
              <StatusTag :text="resolveQrStatusText({ status: item.qrStatus, statusLabel: item.qrStatusLabel })" tone="neutral" />
            </div>
            <small>{{ item.companyName }} · {{ item.currentNode || '待确认环节' }} · {{ item.originPlace || '产地待补充' }}</small>
          </div>

          <div class="row-meta quality-focus">
            <span class="row-label">当前判断</span>
            <strong>{{ qualityPriorityText(item) }}</strong>
            <small>{{ qualityPriorityHint(item) }}</small>
            <span class="priority-chip" :class="qualityPriorityTone(item)">{{ item.qualityStatusCode === 'PENDING' ? '优先处理' : '已形成判断' }}</span>
          </div>

          <div class="status-stack quality-ready">
            <span class="row-label">发布准备</span>
            <strong>{{ publishReadyText(item) }}</strong>
            <small>{{ resolvePublishReady(item) ? '当前条件允许继续发布或恢复发布。' : '仍需补齐质检、二维码或状态条件。' }}</small>
            <div class="meta-inline">
              <span>最近更新：{{ latestUpdatedText(item) }}</span>
              <span>{{ item.marketDate ? `公开时间：${item.marketDate}` : '还没有公开时间' }}</span>
            </div>
          </div>

          <div class="row-actions quality-actions">
            <PrimaryActionGroup
              :primary-label="recommendedQualityActionLabel(item)"
              :primary-class="recommendedQualityActionClass(item)"
              :primary-disabled="recommendedQualityActionDisabled(item)"
              :primary-testid="recommendedQualityActionCode(item) === 'report'
                ? `quality-open-report-${item.id}`
                : (recommendedQualityActionCode(item) === 'upload'
                  ? `quality-upload-${item.id}`
                  : `quality-open-workbench-${item.id}`)"
              :primary-hint="qualityPriorityHint(item)"
              :secondary-actions="qualitySecondaryActions(item)"
              @primary-click="handleRecommendedQualityAction(item)"
              @secondary-click="(code) => handleSecondaryQualityAction(item, code)"
            />
          </div>
        </article>
      </div>
    </section>

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
            <span>质检摘要</span>
            <textarea
              v-model.trim="qualityForm.highlightsText"
              rows="4"
              placeholder="每行一个重点，例如：关键指标合格 / 样品抽检正常 / 允许进入发布流程"
            />
          </label>
          <label class="full-width">
            <span>附件</span>
            <div class="upload-box">
              <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
              <small class="empty-copy">支持上传 PDF 或图片，上传成功后会随本次质检一起绑定。</small>
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
</template>

<style src="../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.quality-head,
.quality-row {
  grid-template-columns:
    minmax(0, 1fr)
    minmax(0, 0.8fr)
    minmax(0, 0.95fr)
    minmax(0, 1.15fr);
}

.board-panel,
.board-head,
.board-grid,
.row-chip-group,
.secondary-actions,
.meta-inline {
  display: grid;
  gap: 12px;
}

.board-head {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
}

.board-head h2 {
  margin: 6px 0 0;
  color: var(--admin-text);
  font-size: 22px;
}

.board-copy {
  margin: 10px 0 0;
  color: var(--admin-text-soft);
  line-height: 1.7;
}

.board-badge,
.priority-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  width: fit-content;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.board-badge {
  background: rgba(48, 149, 246, 0.12);
  color: var(--admin-primary-deep);
}

.board-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 16px;
}

.board-card {
  padding: 16px;
  border: 1px solid rgba(194, 212, 230, 0.72);
  border-radius: 20px;
  background: var(--admin-surface-soft);
}

.board-card small,
.board-card p {
  color: var(--admin-text-soft);
}

.board-card strong {
  display: block;
  margin-top: 8px;
  color: var(--admin-text);
  font-size: 24px;
}

.board-card p {
  margin: 10px 0 0;
  line-height: 1.6;
}

.quality-main,
.quality-focus,
.quality-ready,
.quality-actions {
  align-self: stretch;
}

.row-title-line {
  display: grid;
  gap: 4px;
}

.row-chip-group {
  margin-top: 4px;
}

.row-label {
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.priority-chip.primary {
  background: rgba(48, 149, 246, 0.12);
  color: var(--admin-primary-deep);
}

.priority-chip.success {
  background: rgba(46, 166, 106, 0.12);
  color: #1e7d50;
}

.priority-chip.warning {
  background: rgba(242, 139, 34, 0.14);
  color: #b96b16;
}

.priority-chip.danger {
  background: rgba(221, 74, 74, 0.12);
  color: #b63f3f;
}

.quality-ready {
  gap: 10px;
}

.meta-inline {
  margin-top: 2px;
}

.meta-inline span {
  color: var(--admin-text-soft);
  font-size: 12px;
}

.quality-actions {
  align-content: start;
}

.action-copy {
  margin: 0;
  color: var(--admin-text-soft);
  line-height: 1.7;
}

.secondary-actions {
  grid-template-columns: repeat(auto-fit, minmax(120px, max-content));
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

.neutral {
  background: rgba(129, 154, 184, 0.14);
  color: #57718e;
}

.primary-text {
  font-weight: 700;
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

@media (max-width: 760px) {
  .quality-row {
    grid-template-columns: 1fr;
  }
}
</style>
