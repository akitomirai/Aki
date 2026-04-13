<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { changeBatchStatus, createRiskAction, getBatchDetail, getBatchList } from '../api/batch'
import PrimaryActionGroup from '../components/PrimaryActionGroup.vue'
import StatusTag from '../components/StatusTag.vue'
import { getFriendlyErrorMessage, riskActionOptions } from '../utils/batchExperience'
import { mapBackendRecommendedRiskActionCode } from '../utils/batchStatusFlow'
import { useAuthStore } from '../stores/auth'
import { canManageAdminBatch, isRegulator } from '../utils/access'

const router = useRouter()
const authStore = useAuthStore()
const DEFAULT_PAGE_SIZE = 10

const loading = ref(false)
const rows = ref([])
const allRows = ref([])
const message = ref('')
const messageType = ref('info')
const activeTab = ref('FROZEN')
const filters = ref(createFilterState())
const riskSubmitting = ref(false)
const resumeSubmitting = ref(false)
const riskDialog = ref(createRiskDialogState())
const resumeDialog = ref(createResumeDialogState())
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]
const roleCode = computed(() => authStore.user?.roleCode || '')
const canManageRisk = computed(() => canManageAdminBatch(roleCode.value))
const readOnlyRiskView = computed(() => isRegulator(roleCode.value))
const pageTitle = computed(() => readOnlyRiskView.value ? '风险查看' : '风险处理')
const pageSubtitle = computed(() => {
  if (readOnlyRiskView.value) {
    return '监管账号可统一查看风险状态、最近动作、整改结果和恢复发布条件，不提供补说明、整改或恢复发布入口。'
  }
  return '集中处理已冻结、处理中、已整改和已召回批次，直接在列表里补动作，再回工作台核对状态。'
})
const readOnlyBannerText = computed(() => '当前为监管查看模式，页面保留批次、企业、风险状态、最近动作、整改结果和最近更新时间，便于快速判断风险处置进展。')
const openWorkbenchText = computed(() => readOnlyRiskView.value ? '查看批次详情' : '查看工作台')

const riskTabs = [
  { value: 'FROZEN', label: '已冻结' },
  { value: 'PROCESSING', label: '风险处理中' },
  { value: 'RECTIFIED', label: '已完成整改' },
  { value: 'RECALLED', label: '已召回' }
]

const statusOptions = [
  { value: '', label: '全部批次状态' },
  { value: 'FROZEN', label: '已冻结' },
  { value: 'RECALLED', label: '已召回' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'DRAFT', label: '草稿' }
]

const filteredRows = computed(() => rows.value.filter((item) => matchesKeyword(item) && matchesCompany(item) && matchesTab(item)))
const pageCount = computed(() => Math.max(1, Math.ceil(Number(filteredRows.value.length || 0) / Number(pageSize.value || DEFAULT_PAGE_SIZE))))
const visibleRows = computed(() => {
  const fromIndex = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(fromIndex, fromIndex + pageSize.value)
})

const tabCounts = computed(() => {
  return riskTabs.reduce((acc, item) => {
    acc[item.value] = allRows.value.filter((row) => matchesTab(row, item.value)).length
    return acc
  }, {})
})

const activeTabMeta = computed(() => riskTabs.find((item) => item.value === activeTab.value) ?? riskTabs[0])

const riskOverviewCards = computed(() => [
  {
    key: 'frozen',
    label: '已冻结',
    value: tabCounts.value.FROZEN ?? 0,
    detail: '适合先补处理说明，明确风险范围'
  },
  {
    key: 'processing',
    label: '处理中',
    value: tabCounts.value.PROCESSING ?? 0,
    detail: '重点补整改记录和阶段结论'
  },
  {
    key: 'rectified',
    label: '已整改',
    value: tabCounts.value.RECTIFIED ?? 0,
    detail: '继续核对是否满足恢复发布条件'
  },
  {
    key: 'recalled',
    label: '已召回',
    value: tabCounts.value.RECALLED ?? 0,
    detail: '公开页会持续保留风险提示'
  }
])

const boardLeadText = computed(() => {
  if (readOnlyRiskView.value) {
    return `${activeTabMeta.value.label}看板会优先保留风险状态、最近动作和整改结果，便于直接判断处置进展。`
  }
  return {
    FROZEN: '先补处理说明，让风险判断和当前动作一眼可懂。',
    PROCESSING: '处理中批次优先补整改记录，再决定是否标记已整改。',
    RECTIFIED: '已整改批次适合继续核对恢复发布条件和工作台状态。',
    RECALLED: '召回批次重点讲清风险提示、处理范围和后续安排。'
  }[activeTab.value] ?? '优先处理最接近恢复发布的风险批次。'
})
const listSummary = computed(() => {
  if (!filteredRows.value.length) {
    return '暂无批次。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(filteredRows.value.length, page.value * pageSize.value)
  return `共 ${filteredRows.value.length} 个批次，当前显示 ${from}-${to} 个。`
})

const riskDialogError = computed(() => {
  if (!riskDialog.value.visible) {
    return ''
  }
  if (!riskDialog.value.operatorName?.trim()) {
    return '请填写处理人。'
  }
  if (['PROCESSING', 'RECTIFIED'].includes(riskDialog.value.actionType) && !riskDialog.value.reason?.trim()) {
    return '当前动作必须填写处理说明。'
  }
  if (['COMMENT', 'RECTIFICATION'].includes(riskDialog.value.actionType) && !riskDialog.value.comment?.trim()) {
    return '当前动作必须填写补充记录。'
  }
  return ''
})

const resumeDialogError = computed(() => {
  if (!resumeDialog.value.visible) {
    return ''
  }
  if (!resumeDialog.value.operatorName?.trim()) {
    return '请填写处理人。'
  }
  if (!resumeDialog.value.reason?.trim()) {
    return '请填写恢复说明。'
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

function createRiskDialogState() {
  return {
    visible: false,
    batch: null,
    actionType: 'COMMENT',
    reason: '',
    comment: '',
    operatorName: '平台管理员'
  }
}

function createResumeDialogState() {
  return {
    visible: false,
    batch: null,
    reason: '整改已完成，恢复批次公开流通。',
    operatorName: '平台管理员'
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

function canHandleRisk(item) {
  return ['FROZEN', 'RECALLED'].includes(String(item.status || '').toUpperCase())
}

function canResume(item) {
  return Boolean(item.canResume || actionEnabled(item, 'RESUME'))
}

function resumeText(item) {
  if (String(item.status || '').toUpperCase() === 'RECALLED') {
    return '不适用'
  }
  return canResume(item) ? '可恢复发布' : '暂不可恢复'
}

function latestRiskActionText(item) {
  return item.latestRiskActionLabel || '暂无处理动作'
}

function rectificationText(item) {
  return item.riskResolutionLabel || '无需整改'
}

function latestUpdatedText(item) {
  return item.lastUpdatedAt || '暂无更新'
}

function riskToneClass(item) {
  return {
    PROCESSING: 'processing',
    RECTIFIED: 'rectified',
    RECALLED: 'recalled',
    FROZEN: 'frozen'
  }[String(item.riskStatus || '').toUpperCase()] ?? 'pending'
}

function riskPriorityText(item) {
  const riskCode = String(item.riskStatus || '').toUpperCase()
  const batchStatus = String(item.status || '').toUpperCase()
  if (canResume(item)) {
    return '优先恢复发布'
  }
  if (batchStatus === 'RECALLED') {
    return '保持召回提示并补处理说明'
  }
  if (!item.latestRiskActionLabel) {
    return '先补处理说明'
  }
  if (riskCode === 'PROCESSING') {
    return '继续补整改记录'
  }
  if (riskCode === 'RECTIFIED') {
    return '继续核对恢复条件'
  }
  return '先确认当前风险范围'
}

function riskPriorityHint(item) {
  const riskCode = String(item.riskStatus || '').toUpperCase()
  const batchStatus = String(item.status || '').toUpperCase()
  if (canResume(item)) {
    return '整改链路已基本完成，可直接恢复发布并回工作台核对。'
  }
  if (batchStatus === 'RECALLED') {
    return '召回态会持续展示公开提示，建议补清处理范围和后续安排。'
  }
  if (!item.latestRiskActionLabel) {
    return '先写清当前判断和影响范围，老师更容易顺着处理逻辑理解。'
  }
  if (riskCode === 'PROCESSING') {
    return '整改记录补齐后，再标记已整改会更顺畅。'
  }
  if (riskCode === 'RECTIFIED') {
    return '可以继续查看工作台状态，确认是否满足恢复发布条件。'
  }
  return '先记录当前动作，再逐步补齐整改留痕。'
}

function riskPriorityTone(item) {
  const riskCode = String(item.riskStatus || '').toUpperCase()
  const batchStatus = String(item.status || '').toUpperCase()
  if (canResume(item)) {
    return 'success'
  }
  if (batchStatus === 'RECALLED') {
    return 'danger'
  }
  if (riskCode === 'RECTIFIED') {
    return 'primary'
  }
  return 'warning'
}

function canExecuteRiskAction(item, actionCode) {
  if (actionCode === 'workbench') {
    return true
  }
  if (actionCode === 'resume') {
    return canManageRisk.value && canResume(item)
  }
  if (actionCode === 'comment') {
    return canManageRisk.value && canHandleRisk(item)
  }
  if (actionCode === 'rectification') {
    return canManageRisk.value && canHandleRisk(item)
  }
  if (actionCode === 'processing') {
    return canManageRisk.value && canHandleRisk(item) && String(item.riskStatus || '').toUpperCase() === 'FROZEN'
  }
  if (actionCode === 'rectified') {
    return canManageRisk.value && canHandleRisk(item) && ['FROZEN', 'PROCESSING'].includes(String(item.riskStatus || '').toUpperCase())
  }
  return false
}

function recommendedRiskActionCode(item) {
  if (readOnlyRiskView.value) {
    return 'workbench'
  }
  const backendCode = mapBackendRecommendedRiskActionCode(item?.recommendedActionCode)
  if (backendCode && canExecuteRiskAction(item, backendCode)) {
    return backendCode
  }
  if (canResume(item)) {
    return 'resume'
  }
  if (!item.latestRiskActionLabel) {
    return 'comment'
  }
  if (String(item.riskStatus || '').toUpperCase() === 'PROCESSING') {
    return 'rectification'
  }
  if (String(item.riskStatus || '').toUpperCase() === 'FROZEN') {
    return 'processing'
  }
  return 'workbench'
}

function recommendedRiskActionDisabled(item) {
  return !canExecuteRiskAction(item, recommendedRiskActionCode(item))
}

function recommendedRiskActionLabel(item) {
  return {
    comment: '补处理说明',
    rectification: '补整改记录',
    processing: '标记处理中',
    rectified: '标记已整改',
    resume: '恢复发布',
    workbench: openWorkbenchText.value
  }[recommendedRiskActionCode(item)]
}

function recommendedRiskActionClass(item) {
  return {
    comment: 'primary',
    rectification: 'primary',
    processing: 'warning',
    rectified: 'warning',
    resume: 'success',
    workbench: 'ghost'
  }[recommendedRiskActionCode(item)]
}

function riskSecondaryActions(item) {
  const actions = []
  if (recommendedRiskActionCode(item) !== 'workbench') {
    actions.push({
      key: 'workbench',
      label: openWorkbenchText.value,
      testId: `risk-open-workbench-${item.id}`,
      disabled: false
    })
  }
  if (canManageRisk.value && canHandleRisk(item) && recommendedRiskActionCode(item) !== 'comment') {
    actions.push({
      key: 'comment',
      label: '补处理说明',
      testId: `risk-comment-${item.id}`,
      disabled: false
    })
  }
  if (canManageRisk.value && canHandleRisk(item) && recommendedRiskActionCode(item) !== 'rectification') {
    actions.push({
      key: 'rectification',
      label: '补整改记录',
      testId: `risk-rectification-${item.id}`,
      disabled: false
    })
  }
  if (canManageRisk.value && canHandleRisk(item) && String(item.riskStatus || '').toUpperCase() === 'FROZEN' && recommendedRiskActionCode(item) !== 'processing') {
    actions.push({
      key: 'processing',
      label: '标记处理中',
      testId: `risk-processing-${item.id}`,
      disabled: false
    })
  }
  if (canManageRisk.value && canHandleRisk(item) && ['FROZEN', 'PROCESSING'].includes(String(item.riskStatus || '').toUpperCase()) && recommendedRiskActionCode(item) !== 'rectified') {
    actions.push({
      key: 'rectified',
      label: '标记已整改',
      testId: `risk-rectified-${item.id}`,
      disabled: false
    })
  }
  if (canManageRisk.value && canResume(item) && recommendedRiskActionCode(item) !== 'resume') {
    actions.push({
      key: 'resume',
      label: '恢复发布',
      testId: `risk-resume-${item.id}`,
      disabled: false
    })
  }
  return actions
}

function handleRecommendedRiskAction(item) {
  const code = recommendedRiskActionCode(item)
  if (!canExecuteRiskAction(item, code)) {
    showMessage('当前动作暂不可执行，请先完成前置处理。', 'error')
    return
  }
  if (code === 'resume') {
    openResumeDialog(item)
    return
  }
  if (code === 'comment') {
    openRiskDialog(item, 'COMMENT')
    return
  }
  if (code === 'rectification') {
    openRiskDialog(item, 'RECTIFICATION')
    return
  }
  if (code === 'processing') {
    openRiskDialog(item, 'PROCESSING')
    return
  }
  if (code === 'rectified') {
    openRiskDialog(item, 'RECTIFIED')
    return
  }
  openWorkbenchAfterRefresh(item)
}

function handleSecondaryRiskAction(item, code) {
  if (code === 'workbench') {
    openWorkbenchAfterRefresh(item)
    return
  }
  if (code === 'comment') {
    openRiskDialog(item, 'COMMENT')
    return
  }
  if (code === 'rectification') {
    openRiskDialog(item, 'RECTIFICATION')
    return
  }
  if (code === 'processing') {
    openRiskDialog(item, 'PROCESSING')
    return
  }
  if (code === 'rectified') {
    openRiskDialog(item, 'RECTIFIED')
    return
  }
  if (code === 'resume') {
    openResumeDialog(item)
  }
}

function riskActionLabel(actionType) {
  return riskActionOptions.find((item) => item.value === actionType)?.label || actionType
}

function riskActionHint(actionType) {
  return {
    COMMENT: '补充当前风险判断、范围和处理说明。',
    RECTIFICATION: '记录已完成的整改动作、现场复核和责任落实。',
    PROCESSING: '明确已经进入处理中，方便全局队列快速识别。',
    RECTIFIED: '明确整改完成，便于恢复发布前复核。'
  }[actionType] ?? '记录当前风险处理情况。'
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
  return String(item.riskStatus || '').toUpperCase() === tabValue
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
    showMessage(getFriendlyErrorMessage(error, '风险批次加载失败，请稍后再试。'), 'error')
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
  router.push(`/batches/${item.id}`)
}

function openRiskDialog(item, actionType) {
  riskDialog.value = {
    visible: true,
    batch: item,
    actionType,
    reason: actionType === 'PROCESSING'
      ? '当前批次已进入风险处理中，请继续保持冻结并补充复核。'
      : (actionType === 'RECTIFIED' ? '整改已完成，等待恢复发布复核。' : ''),
    comment: actionType === 'COMMENT'
      ? '已补处理说明，将继续核查现场范围与去向。'
      : (actionType === 'RECTIFICATION' ? '已补整改记录，隔离、复核和责任落实已留痕。' : ''),
    operatorName: '平台管理员'
  }
}

function closeRiskDialog() {
  riskDialog.value = createRiskDialogState()
}

function openResumeDialog(item) {
  resumeDialog.value = {
    visible: true,
    batch: item,
    reason: '整改已完成，恢复批次公开流通。',
    operatorName: '平台管理员'
  }
}

function closeResumeDialog() {
  resumeDialog.value = createResumeDialogState()
}

async function submitRiskAction() {
  if (riskSubmitting.value || readOnlyRiskView.value) {
    return
  }
  if (!riskDialog.value.batch?.id) {
    return
  }
  if (riskDialogError.value) {
    showMessage(riskDialogError.value, 'error')
    return
  }
  const confirmed = window.confirm(`确认提交“${riskActionLabel(riskDialog.value.actionType)}”吗？`)
  if (!confirmed) {
    return
  }
  riskSubmitting.value = true
  try {
    await createRiskAction(riskDialog.value.batch.id, {
      actionType: riskDialog.value.actionType,
      reason: riskDialog.value.reason,
      comment: riskDialog.value.comment,
      operatorName: riskDialog.value.operatorName
    })
    await fetchRows()
    showMessage(`风险动作“${riskActionLabel(riskDialog.value.actionType)}”已记录，列表已刷新。`, 'success')
    closeRiskDialog()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '风险处理保存失败，请稍后再试。'), 'error')
  } finally {
    riskSubmitting.value = false
  }
}

async function submitResume() {
  if (resumeSubmitting.value || readOnlyRiskView.value) {
    return
  }
  if (!resumeDialog.value.batch?.id) {
    return
  }
  if (resumeDialogError.value) {
    showMessage(resumeDialogError.value, 'error')
    return
  }
  const confirmed = window.confirm('确认恢复发布该批次吗？恢复后公开页会重新开放查询。')
  if (!confirmed) {
    return
  }
  resumeSubmitting.value = true
  try {
    await changeBatchStatus(resumeDialog.value.batch.id, {
      targetStatus: 'PUBLISHED',
      reason: resumeDialog.value.reason,
      operatorName: resumeDialog.value.operatorName
    })
    await fetchRows()
    showMessage('批次已恢复发布，风险列表和工作台状态已同步。', 'success')
    closeResumeDialog()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '恢复发布失败，请稍后再试。'), 'error')
  } finally {
    resumeSubmitting.value = false
  }
}

async function openWorkbenchAfterRefresh(item) {
  try {
    await getBatchDetail(item.id)
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '批次工作台加载失败，请稍后再试。'), 'error')
    return
  }
  openWorkbench(item)
}
</script>

<template>
  <div class="page-shell" data-testid="risk-page">
    <div class="manage-page risk-manage">
    <section v-if="readOnlyRiskView" class="panel readonly-banner" data-testid="risk-readonly-banner">
      <strong>监管查看模式</strong>
      <span>{{ readOnlyBannerText }}</span>
    </section>

    <div class="manage-summary-row">
      <div class="manage-summary">
        <button
          v-for="(card, index) in riskOverviewCards"
          :key="riskTabs[index].value"
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': activeTab === riskTabs[index].value }"
          :data-testid="`risk-tab-${riskTabs[index].value}`"
          @click="activeTab = riskTabs[index].value"
        >
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </button>
      </div>

      <div class="manage-summary-actions">
        <button class="ghost" data-testid="risk-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
      </div>
    </div>

    <section class="panel manage-filter-card risk-filter-panel">
      <div class="manage-filter-grid risk-filter-grid">
        <label>
          <span>批次名称 / 编号</span>
          <input v-model.trim="filters.keyword" data-testid="risk-filter-keyword" type="text" placeholder="输入批次编号或产品名称">
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
        <div class="risk-page-size-control">
          <span class="manage-muted">每页显示</span>
          <select v-model="pageSize" data-testid="risk-page-size" class="risk-page-size-select" @change="handlePageSizeChange($event.target.value)">
            <option v-for="item in pageSizeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
        <div class="toolbar-actions risk-filter-actions">
          <button class="primary" data-testid="risk-search-button" :disabled="loading" @click="handleSearch">查询</button>
          <button class="ghost" data-testid="risk-reset-button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>

      <div class="toolbar risk-filter-meta">
        <span class="list-summary">{{ listSummary }}</span>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section v-if="loading" class="panel empty-state">
      <div>
        <h3>正在同步风险看板...</h3>
      </div>
    </section>

    <section v-else-if="!visibleRows.length" class="panel empty-state">
      <div>
        <h3>当前看板下还没有风险任务</h3>
      </div>
    </section>

    <section v-else class="panel ledger-panel risk-ledger-panel">
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">风险处置台账</h2>
        </div>
      </div>

      <div class="table-scroll-shell ledger-table-shell risk-table-shell" style="--table-min-width: 1320px;">
        <div class="ledger-table-head risk-head">
          <span>批次与产品</span>
          <span>企业 / 更新时间</span>
          <span>状态概览</span>
          <span>当前判断</span>
          <span>处置进展</span>
          <span>动作</span>
        </div>

        <div class="ledger-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="ledger-row risk-row"
          :data-testid="`risk-row-${item.id}`"
        >
          <div class="row-main risk-main">
            <strong>{{ item.productName }}</strong>
            <span>{{ item.batchCode }}</span>
          </div>

          <div class="row-meta risk-company">
            <strong>{{ item.companyName }}</strong>
            <small>最近更新：{{ latestUpdatedText(item) }}</small>
          </div>

          <div class="row-status risk-overview">
            <div class="status-chip-row">
              <span class="status-chip" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
              <span class="status-chip" :class="riskToneClass(item)">{{ item.riskStatusLabel }}</span>
            </div>
          </div>

          <div class="row-meta risk-focus">
            <strong>{{ riskPriorityText(item) }}</strong>
          </div>

          <div class="row-status risk-progress">
            <strong>{{ canResume(item) ? '已满足恢复发布条件' : latestRiskActionText(item) }}</strong>
          </div>

          <div class="row-actions risk-actions">
            <PrimaryActionGroup
              :primary-label="recommendedRiskActionLabel(item)"
              :primary-class="recommendedRiskActionClass(item)"
              :primary-disabled="recommendedRiskActionDisabled(item)"
              :primary-testid="recommendedRiskActionCode(item) === 'resume'
                ? `risk-resume-${item.id}`
                : (recommendedRiskActionCode(item) === 'comment'
                  ? `risk-comment-${item.id}`
                  : (recommendedRiskActionCode(item) === 'rectification'
                    ? `risk-rectification-${item.id}`
                    : (recommendedRiskActionCode(item) === 'processing'
                      ? `risk-processing-${item.id}`
                      : (recommendedRiskActionCode(item) === 'rectified'
                        ? `risk-rectified-${item.id}`
                        : `risk-open-workbench-${item.id}`))))"
              primary-hint=""
              :secondary-actions="riskSecondaryActions(item)"
              @primary-click="handleRecommendedRiskAction(item)"
              @secondary-click="(code) => handleSecondaryRiskAction(item, code)"
            />
          </div>
        </article>
        </div>
      </div>

      <div class="toolbar risk-pagination">
        <span class="list-summary">第 {{ page }} / {{ pageCount }} 页</span>
        <div class="toolbar-actions">
          <button class="ghost" data-testid="risk-prev-page" :disabled="loading || page <= 1" @click="goPrevPage">上一页</button>
          <button class="ghost" data-testid="risk-next-page" :disabled="loading || page >= pageCount" @click="goNextPage">下一页</button>
        </div>
      </div>
    </section>

    <div v-if="riskDialog.visible" class="dialog-mask" @click.self="closeRiskDialog">
      <section class="dialog-card" data-testid="risk-action-dialog">
        <div class="dialog-head">
          <div>
            <h3>{{ riskActionLabel(riskDialog.actionType) }}</h3>
            <p>{{ riskDialog.batch?.batchCode }} · {{ riskDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" @click="closeRiskDialog">关闭</button>
        </div>

        <div class="form-grid">
          <label>
            <span>处理动作</span>
            <input :value="riskActionLabel(riskDialog.actionType)" type="text" disabled>
          </label>
          <label>
            <span>处理人</span>
            <input v-model.trim="riskDialog.operatorName" type="text" placeholder="例如 平台管理员">
          </label>
          <label class="full-width">
            <span>动作提示</span>
            <textarea :value="riskActionHint(riskDialog.actionType)" rows="2" disabled></textarea>
          </label>
          <label class="full-width">
            <span>处理说明</span>
            <textarea
              v-model.trim="riskDialog.reason"
              rows="4"
              :placeholder="riskDialog.actionType === 'PROCESSING' || riskDialog.actionType === 'RECTIFIED'
                ? '这里是必填项，请写清当前阶段判断或整改完成结论。'
                : '可选补充当前风险原因、范围和判断。'"
            />
          </label>
          <label class="full-width">
            <span>补充记录</span>
            <textarea
              v-model.trim="riskDialog.comment"
              rows="4"
              :placeholder="riskDialog.actionType === 'COMMENT' || riskDialog.actionType === 'RECTIFICATION'
                ? '这里是必填项，请写清处理说明或整改留痕。'
                : '可选补充当前处理动作、现场情况和下一步安排。'"
            />
          </label>
        </div>

        <p v-if="riskDialogError" class="form-error">{{ riskDialogError }}</p>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="riskSubmitting" @click="closeRiskDialog">取消</button>
          <button
            v-if="!readOnlyRiskView"
            class="warning"
            data-testid="risk-action-submit"
            :disabled="riskSubmitting || Boolean(riskDialogError)"
            @click="submitRiskAction"
          >
            {{ riskSubmitting ? '正在保存...' : '确认保存' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="resumeDialog.visible" class="dialog-mask" @click.self="closeResumeDialog">
      <section class="dialog-card" data-testid="risk-resume-dialog">
        <div class="dialog-head">
          <div>
            <h3>恢复发布</h3>
            <p>{{ resumeDialog.batch?.batchCode }} · {{ resumeDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" @click="closeResumeDialog">关闭</button>
        </div>

        <div class="overview-grid">
          <div>
            <span>当前风险状态</span>
            <strong>{{ resumeDialog.batch?.riskStatusLabel || '待确认' }}</strong>
          </div>
          <div>
            <span>整改结果</span>
            <strong>{{ resumeDialog.batch?.riskResolutionLabel || '待确认' }}</strong>
          </div>
          <div>
            <span>最近风险动作</span>
            <strong>{{ resumeDialog.batch?.latestRiskActionLabel || '暂无处理动作' }}</strong>
          </div>
          <div>
            <span>恢复条件</span>
            <strong>{{ canResume(resumeDialog.batch || {}) ? '已满足' : '未满足' }}</strong>
          </div>
        </div>

        <div class="form-grid" style="margin-top: 16px;">
          <label>
            <span>处理人</span>
            <input v-model.trim="resumeDialog.operatorName" type="text" placeholder="例如 平台管理员">
          </label>
          <label class="full-width">
            <span>恢复说明</span>
            <textarea
              v-model.trim="resumeDialog.reason"
              rows="4"
              placeholder="写清为什么可以恢复发布，以及已完成哪些整改与复核。"
            />
          </label>
        </div>

        <p v-if="resumeDialogError" class="form-error">{{ resumeDialogError }}</p>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="resumeSubmitting" @click="closeResumeDialog">取消</button>
          <button
            v-if="!readOnlyRiskView"
            class="success"
            data-testid="risk-resume-submit"
            :disabled="resumeSubmitting || Boolean(resumeDialogError)"
            @click="submitResume"
          >
            {{ resumeSubmitting ? '正在恢复...' : '确认恢复发布' }}
          </button>
        </div>
        </section>
      </div>
    </div>
    </div>
</template>

<style src="../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.risk-head,
.risk-row {
  grid-template-columns:
    minmax(0, 1.05fr)
    minmax(0, 0.95fr)
    minmax(0, 0.9fr)
    minmax(0, 0.95fr)
    minmax(0, 0.95fr)
    minmax(0, 1.2fr);
}

.risk-filter-grid {
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr) minmax(180px, 0.9fr) minmax(188px, max-content) auto;
  align-items: end;
}

.risk-filter-grid label {
  display: grid;
  gap: 8px;
}

.risk-filter-grid label > span,
.risk-page-size-control .manage-muted {
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
}

.risk-filter-grid input,
.risk-filter-grid select,
.risk-page-size-select {
  width: 100%;
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid var(--admin-border);
  border-radius: 10px;
  background: #fff;
  color: var(--admin-text);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.risk-filter-grid input:focus,
.risk-filter-grid select:focus,
.risk-page-size-select:focus {
  border-color: rgba(48, 149, 246, 0.26);
  box-shadow: 0 0 0 3px rgba(48, 149, 246, 0.08);
  outline: none;
}

.risk-page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 188px;
}

.risk-page-size-select {
  width: 128px;
}

.risk-filter-actions {
  justify-content: flex-end;
  flex-wrap: nowrap;
  min-width: 170px;
}

.risk-filter-meta,
.risk-pagination {
  margin-top: 18px;
}

.risk-pagination {
  padding: 0 28px;
}

.risk-table-shell .ledger-table-head,
.risk-table-shell .ledger-row-list {
  width: 100%;
  min-width: 0;
}

.risk-company,
.risk-focus,
.risk-progress,
.risk-overview,
.risk-actions {
  min-width: 0;
}

.risk-overview,
.risk-progress {
  display: grid;
  gap: 8px;
}

.risk-overview .status-chip {
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

.risk-actions :deep(.primary-action-group) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.risk-actions :deep(.action-hint) {
  display: none;
}

.risk-actions :deep(.action-primary) {
  min-height: 34px;
  padding: 0 12px;
  font-size: 13px;
  box-shadow: none;
}

.risk-actions :deep(.secondary-menu) {
  border: none;
  background: transparent;
}

.risk-actions :deep(.secondary-menu > summary) {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(56, 134, 217, 0.16);
  border-radius: 999px;
  background: #fff;
}

.risk-actions :deep(.secondary-menu[open] > summary) {
  border-bottom: 1px solid rgba(56, 134, 217, 0.16);
}

.risk-actions :deep(.secondary-list) {
  position: absolute;
  z-index: 5;
  min-width: 160px;
  padding: 10px;
  border: 1px solid rgba(194, 212, 230, 0.72);
  border-radius: 14px;
  background: #fff;
  box-shadow: var(--admin-shadow);
}

.risk-actions :deep(.secondary-button) {
  min-height: 34px;
  font-size: 12px;
}

.risk-filter-grid {
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr) minmax(180px, 0.9fr) minmax(188px, max-content) auto;
  align-items: end;
}

.risk-filter-grid label {
  display: grid;
  gap: 8px;
}

.risk-page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 188px;
}

.risk-page-size-select {
  width: 128px;
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

.risk-filter-actions {
  justify-content: flex-end;
  flex-wrap: nowrap;
  min-width: 170px;
}

.risk-filter-meta,
.risk-pagination {
  margin-top: 18px;
}

.risk-pagination {
  padding: 0 28px;
}

.risk-actions {
  align-content: start;
  min-width: 0;
}

.risk-company,
.risk-focus,
.risk-progress,
.risk-overview {
  min-width: 0;
}

.risk-overview,
.risk-progress {
  display: grid;
  gap: 8px;
}

.processing {
  background: rgba(242, 139, 34, 0.14);
  color: #b96b16;
}

.rectified {
  background: rgba(46, 166, 106, 0.12);
  color: #1e7d50;
}

.recalled {
  background: rgba(221, 74, 74, 0.12);
  color: #b63f3f;
}

.frozen,
.pending {
  background: rgba(129, 154, 184, 0.14);
  color: #57718e;
}

.risk-table-shell .ledger-table-head,
.risk-table-shell .ledger-row-list {
  width: 100%;
  min-width: 0;
}

.risk-actions :deep(.primary-action-group) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.risk-actions :deep(.action-hint) {
  display: none;
}

.risk-actions :deep(.action-primary) {
  min-height: 34px;
  padding: 0 12px;
  font-size: 13px;
  box-shadow: none;
}

.risk-actions :deep(.secondary-menu) {
  border: none;
  background: transparent;
}

.risk-actions :deep(.secondary-menu > summary) {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(56, 134, 217, 0.16);
  border-radius: 999px;
  background: #fff;
}

.risk-actions :deep(.secondary-menu[open] > summary) {
  border-bottom: 1px solid rgba(56, 134, 217, 0.16);
}

.risk-actions :deep(.secondary-list) {
  position: absolute;
  z-index: 5;
  min-width: 160px;
  padding: 10px;
  border: 1px solid rgba(194, 212, 230, 0.72);
  border-radius: 14px;
  background: #fff;
  box-shadow: var(--admin-shadow);
}

.risk-actions :deep(.secondary-button) {
  min-height: 34px;
  font-size: 12px;
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
  .risk-filter-grid {
    grid-template-columns: 1fr;
  }

  .risk-row {
    grid-template-columns: 1fr;
  }
}
</style>
