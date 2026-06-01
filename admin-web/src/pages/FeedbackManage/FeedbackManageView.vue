<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { handleFeedbackApi, listFeedbackApi } from '../../api/feedback'
import AdminListPagination from '../../components/AdminListPagination.vue'
import AdminListTemplate from '../../components/AdminListTemplate.vue'
import AdminOverviewCards from '../../components/AdminOverviewCards.vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const DEFAULT_PAGE_SIZE = 10

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const activeTab = ref('ALL')
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const filters = ref({
  keyword: '',
  feedbackType: '',
  status: ''
})
const detailDialog = ref({
  visible: false,
  item: null
})
const handleDialog = ref({
  visible: false,
  item: null,
  status: 'PROCESSING',
  handleResult: ''
})

const roleCode = computed(() => authStore.user?.roleCode || '')
const canHandle = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(roleCode.value))
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]
const feedbackTypes = [
  { value: '', label: '全部反馈类型' },
  { value: '信息不一致', label: '信息不一致' },
  { value: '质量疑问', label: '质量疑问' },
  { value: '二维码无法识别', label: '二维码无法识别' },
  { value: '页面显示异常', label: '页面显示异常' },
  { value: '其他', label: '其他' }
]
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'CLOSED', label: '已处理' }
]
const feedbackTabs = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'CLOSED', label: '已处理' }
]
const feedbackBoardCards = computed(() => feedbackTabs.map((item) => ({
  key: item.value,
  label: item.label,
  value: tabCounts.value[item.value] ?? 0
})))

const tabCounts = computed(() => {
  return feedbackTabs.reduce((acc, item) => {
    if (item.value === 'ALL') {
      acc[item.value] = rows.value.length
    } else {
      acc[item.value] = rows.value.filter((row) => normalizeStatus(row.status) === item.value).length
    }
    return acc
  }, {})
})

const filteredRows = computed(() => {
  return rows.value.filter((item) => {
    const keyword = filters.value.keyword.trim().toLowerCase()
    const keywordMatched = !keyword || [
      item.productName,
      item.batchNo,
      item.traceCode,
      item.companyName,
      item.content,
      item.contact
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
    const typeMatched = !filters.value.feedbackType || item.feedbackType === filters.value.feedbackType
    const statusMatched = !filters.value.status || normalizeStatus(item.status) === filters.value.status
    const tabMatched = activeTab.value === 'ALL' || normalizeStatus(item.status) === activeTab.value
    return keywordMatched && typeMatched && statusMatched && tabMatched
  })
})

const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / Number(pageSize.value || DEFAULT_PAGE_SIZE))))
const visibleRows = computed(() => {
  const from = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(from, from + pageSize.value)
})
const listSummary = computed(() => {
  if (!filteredRows.value.length) {
    return '暂无反馈数据。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(filteredRows.value.length, page.value * pageSize.value)
  return `共 ${filteredRows.value.length} 条反馈，当前显示 ${from}-${to} 条。`
})
const paginationSummary = computed(() => `第 ${page.value} / ${pageCount.value} 页`)

onMounted(async () => {
  await fetchRows()
})

watch([activeTab, filters, pageSize], () => {
  page.value = 1
}, { deep: true })

async function fetchRows() {
  loading.value = true
  try {
    const response = await listFeedbackApi()
    rows.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    rows.value = []
    ElMessage.error('反馈列表暂时无法加载')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = {
    keyword: '',
    feedbackType: '',
    status: ''
  }
  page.value = 1
}

function setActiveTab(tab) {
  activeTab.value = tab
  filters.value.status = ''
}

function openDetail(row) {
  detailDialog.value = {
    visible: true,
    item: row
  }
}

function openHandle(row) {
  handleDialog.value = {
    visible: true,
    item: row,
    status: normalizeStatus(row.status) === 'CLOSED' ? 'CLOSED' : 'PROCESSING',
    handleResult: row.handleResult || ''
  }
}

async function submitHandle() {
  const item = handleDialog.value.item
  if (!item?.id) {
    return
  }
  if (!handleDialog.value.status) {
    ElMessage.warning('请选择处理状态')
    return
  }
  saving.value = true
  try {
    await handleFeedbackApi(item.id, {
      status: handleDialog.value.status,
      handleResult: handleDialog.value.handleResult
    })
    ElMessage.success('反馈处理已保存')
    handleDialog.value.visible = false
    await fetchRows()
  } catch (error) {
    ElMessage.error('反馈处理暂时无法保存')
  } finally {
    saving.value = false
  }
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1
  }
}

function nextPage() {
  if (page.value < pageCount.value) {
    page.value += 1
  }
}

function normalizeStatus(status) {
  return String(status || 'PENDING').toUpperCase()
}

function statusText(status) {
  return {
    PENDING: '待处理',
    PROCESSING: '处理中',
    CLOSED: '已处理'
  }[normalizeStatus(status)] || '待处理'
}

function statusClass(status) {
  return {
    PENDING: 'pending',
    PROCESSING: 'processing',
    CLOSED: 'closed'
  }[normalizeStatus(status)] || 'pending'
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <div class="page-shell feedback-page">
    <AdminListTemplate
      template-class="feedback-card-stack"
      toolbar-class="feedback-mode-summary"
      filter-card-class="feedback-filter-panel"
      ledger-card-class="feedback-ledger-panel feedback-ledger-card"
    >
      <template #summary>
        <AdminOverviewCards
          :items="feedbackBoardCards"
          :active-key="activeTab"
          test-id-prefix="feedback-tab"
          @select="setActiveTab($event)"
        />
      </template>

      <template #actions>
        <button class="ghost" :disabled="loading" @click="fetchRows">刷新</button>
      </template>

      <template #filterPrimary>
        <div class="feedback-filter-layout">
          <div class="filter-grid feedback-filter-grid">
            <label class="manage-filter-field feedback-filter-field feedback-filter-field--keyword">
              <span class="manage-filter-field__label">关键词</span>
              <input v-model.trim="filters.keyword" type="text" placeholder="输入产品、批次或追溯码">
            </label>

            <label class="manage-filter-field feedback-filter-field feedback-filter-field--type">
              <span class="manage-filter-field__label">反馈类型</span>
              <select v-model="filters.feedbackType">
                <option v-for="item in feedbackTypes" :key="item.value || 'all'" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>

            <label class="manage-filter-field feedback-filter-field feedback-filter-field--status">
              <span class="manage-filter-field__label">处理状态</span>
              <select v-model="filters.status">
                <option v-for="item in statusOptions" :key="item.value || 'all'" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>

            <label class="manage-filter-field feedback-filter-field feedback-filter-field--page-size">
              <span class="manage-filter-field__label">每页显示</span>
              <select v-model="pageSize" class="page-size-select">
                <option v-for="item in pageSizeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
          </div>
        </div>
      </template>

      <template #filterSecondary>
        <div class="feedback-filter-toolbar">
          <div class="feedback-filter-actions">
            <button class="primary" :disabled="loading" @click="fetchRows">查询</button>
            <button class="ghost" :disabled="loading" @click="resetFilters">重置</button>
          </div>
          <div class="feedback-list-summary">
            <span class="manage-muted">{{ listSummary }}</span>
          </div>
        </div>
      </template>

      <template #ledger>
        <div class="panel-heading">
          <div>
            <h2 class="panel-heading__title">公开反馈台账</h2>
          </div>
        </div>

        <div v-if="!loading && !visibleRows.length" class="empty-state feedback-ledger-empty">
          <h3>当前条件下没有反馈数据</h3>
        </div>

        <div v-else v-loading="loading" class="table-scroll-shell ledger-table-shell feedback-table-shell">
          <div class="feedback-table-head ledger-table-head">
            <span>产品 / 批次</span>
            <span>反馈类型</span>
            <span>反馈内容</span>
            <span>状态</span>
            <span>提交时间</span>
            <span>操作</span>
          </div>

          <div class="feedback-row-list ledger-row-list">
            <article
              v-for="item in visibleRows"
              :key="item.id"
              class="feedback-row ledger-row"
            >
              <div class="row-main feedback-product-cell">
                <strong>{{ item.productName || '未识别' }}</strong>
                <span>{{ item.batchNo || '未识别' }}</span>
                <small>{{ item.traceCode || '未识别' }}</small>
              </div>
              <div>
                <span class="feedback-type">{{ item.feedbackType || '其他' }}</span>
              </div>
              <div class="row-meta feedback-content-cell">
                <strong>{{ item.content || '-' }}</strong>
                <span>{{ item.companyName || '未关联企业' }}</span>
              </div>
              <div>
                <span class="feedback-status" :class="statusClass(item.status)">
                  {{ statusText(item.status) }}
                </span>
              </div>
              <div class="row-meta feedback-time-cell">
                <strong>{{ formatDateTime(item.createdAt) }}</strong>
                <span>{{ item.contact || '未留联系方式' }}</span>
              </div>
              <div class="feedback-actions table-cell--actions">
                <div class="ledger-actions-scroll">
                  <button type="button" class="text-button" @click="openDetail(item)">查看</button>
                  <button v-if="canHandle" type="button" class="text-button primary-text" @click="openHandle(item)">处理</button>
                </div>
              </div>
            </article>
          </div>
        </div>

        <AdminListPagination
          :summary="paginationSummary"
          :prev-disabled="page <= 1"
          :next-disabled="page >= pageCount"
          @prev="prevPage"
          @next="nextPage"
        />
      </template>
    </AdminListTemplate>

    <el-dialog
      v-model="detailDialog.visible"
      title="反馈详情"
      width="640px"
      append-to-body
    >
      <div v-if="detailDialog.item" class="feedback-detail-grid">
        <div>
          <span>当前产品</span>
          <strong>{{ detailDialog.item.productName || '未识别' }}</strong>
        </div>
        <div>
          <span>批次编号</span>
          <strong>{{ detailDialog.item.batchNo || '未识别' }}</strong>
        </div>
        <div>
          <span>追溯码</span>
          <strong>{{ detailDialog.item.traceCode || '未识别' }}</strong>
        </div>
        <div>
          <span>反馈类型</span>
          <strong>{{ detailDialog.item.feedbackType || '其他' }}</strong>
        </div>
        <div>
          <span>联系方式</span>
          <strong>{{ detailDialog.item.contact || '-' }}</strong>
        </div>
        <div>
          <span>处理状态</span>
          <strong>{{ statusText(detailDialog.item.status) }}</strong>
        </div>
        <div class="full">
          <span>反馈内容</span>
          <strong>{{ detailDialog.item.content || '-' }}</strong>
        </div>
        <div class="full">
          <span>处理结果</span>
          <strong>{{ detailDialog.item.handleResult || '-' }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="handleDialog.visible"
      title="反馈处理"
      width="520px"
      append-to-body
    >
      <el-form label-width="88px">
        <el-form-item label="处理状态">
          <el-select v-model="handleDialog.status" class="dialog-select">
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已处理" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input
            v-model="handleDialog.handleResult"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="填写处理结果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialog.visible = false">关闭</el-button>
        <el-button type="primary" :loading="saving" @click="submitHandle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style src="../../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.feedback-page {
  --feedback-grid: minmax(150px, 0.95fr) minmax(100px, 0.5fr) minmax(220px, 1.2fr) minmax(82px, 0.42fr) minmax(135px, 0.62fr) minmax(112px, 0.5fr);
  --feedback-code-filter-width: 260px;
  --feedback-type-filter-width: 178px;
  --feedback-status-filter-width: 160px;
  --feedback-page-size-width: 142px;
  --feedback-filter-control-height: 40px;
  --feedback-filter-control-radius: 12px;
  --feedback-filter-text-inset: 14px;
  --feedback-grid-inline-padding: 18px;
  --feedback-grid-column-gap: 14px;
  --feedback-filter-group-left-shift: 8px;
}

:deep(.feedback-card-stack) {
  gap: 14px;
}

:deep(.feedback-filter-panel),
:deep(.feedback-ledger-panel) {
  border-radius: 24px !important;
  background: #fff !important;
  box-shadow: 0 18px 42px rgba(45, 113, 194, 0.08) !important;
}

:deep(.feedback-filter-panel) {
  padding: 18px 22px 0 !important;
}

:deep(.feedback-mode-summary .manage-summary-chip) {
  min-height: 38px;
  padding: 8px 12px;
  border: 1px solid var(--admin-border);
  border-radius: 999px;
  background: var(--admin-surface-soft);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  box-shadow: none;
}

:deep(.feedback-mode-summary .manage-summary-chip strong) {
  color: var(--admin-text-strong);
  font-weight: 700;
}

:deep(.feedback-mode-summary .manage-summary-chip--interactive:hover) {
  border-color: rgba(48, 149, 246, 0.28);
  background: rgba(48, 149, 246, 0.08);
}

:deep(.feedback-mode-summary .manage-summary-chip.is-active) {
  border-color: rgba(48, 149, 246, 0.3);
  background: rgba(48, 149, 246, 0.14);
  box-shadow: 0 10px 20px rgba(48, 149, 246, 0.1);
}

:deep(.feedback-mode-summary .manage-summary-chip.is-active),
:deep(.feedback-mode-summary .manage-summary-chip.is-active strong) {
  color: var(--admin-primary-deep);
}

.feedback-filter-layout {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 18px;
  margin-left: calc(-1 * var(--feedback-filter-group-left-shift));
  padding: 0 12px 0 calc(var(--feedback-grid-inline-padding) - var(--feedback-filter-text-inset));
  flex-wrap: wrap;
}

.feedback-filter-grid {
  display: grid;
  grid-template-columns:
    minmax(var(--feedback-code-filter-width), max-content)
    minmax(var(--feedback-type-filter-width), max-content)
    minmax(var(--feedback-status-filter-width), max-content)
    minmax(var(--feedback-page-size-width), max-content);
  align-items: end;
  column-gap: 12px;
  row-gap: 12px;
  padding: 0;
  flex: 0 1 auto;
  min-width: 0;
  width: max-content;
  max-width: 100%;
}

.feedback-filter-field {
  gap: 8px;
  width: 100%;
  justify-self: start;
}

.feedback-filter-field--keyword {
  max-width: var(--feedback-code-filter-width);
}

.feedback-filter-field--type {
  max-width: var(--feedback-type-filter-width);
}

.feedback-filter-field--status {
  max-width: var(--feedback-status-filter-width);
}

.feedback-filter-field--page-size {
  max-width: var(--feedback-page-size-width);
}

.feedback-filter-grid .manage-filter-field__label {
  padding-inline-start: var(--feedback-filter-text-inset);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.feedback-filter-grid input,
.feedback-filter-grid select {
  width: 100%;
  min-height: var(--feedback-filter-control-height);
  box-sizing: border-box;
  padding: 0 14px 0 var(--feedback-filter-text-inset);
  border: 0;
  border-radius: var(--feedback-filter-control-radius);
  background: #fff;
  color: var(--admin-text);
  box-shadow: 0 0 0 1px rgba(56, 134, 217, 0.14) inset;
}

.feedback-filter-grid input::placeholder {
  color: var(--admin-text-faint);
}

.feedback-filter-grid select {
  appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #9bb0c8 50%),
    linear-gradient(135deg, #9bb0c8 50%, transparent 50%);
  background-position:
    calc(100% - 18px) calc(50% - 2px),
    calc(100% - 12px) calc(50% - 2px);
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
}

.feedback-filter-toolbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
  margin-left: calc(-1 * var(--feedback-filter-group-left-shift));
  padding: 0 12px 18px var(--feedback-grid-inline-padding);
  min-width: 0;
  flex-wrap: wrap;
}

.feedback-filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  min-width: 0;
}

.feedback-filter-actions button {
  min-height: 38px;
  padding-inline: 16px;
  border-radius: 12px;
}

.feedback-list-summary .manage-muted {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.feedback-table-shell {
  --ledger-grid-columns: var(--feedback-grid);
  --ledger-column-gap: var(--feedback-grid-column-gap);
  --ledger-inline-padding: var(--feedback-grid-inline-padding);
  --ledger-row-padding-block: 18px;
  --ledger-min-width: 0px;
}

.feedback-table-head {
  color: #6f86a4;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  background: #fff;
}

.feedback-row-list.ledger-row-list {
  background: #fff;
}

.feedback-row.ledger-row {
  min-height: 88px;
  background: #fff;
}

.feedback-row:hover {
  background: rgba(48, 149, 246, 0.03);
}

.feedback-product-cell strong,
.feedback-content-cell strong,
.feedback-time-cell strong {
  color: var(--admin-text-strong);
  font-size: 15px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.feedback-product-cell span,
.feedback-content-cell span,
.feedback-time-cell span,
.feedback-product-cell small {
  color: var(--admin-text-mid);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.feedback-product-cell strong,
.feedback-product-cell span,
.feedback-product-cell small,
.feedback-content-cell strong,
.feedback-content-cell span,
.feedback-time-cell strong,
.feedback-time-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feedback-type,
.feedback-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.feedback-type {
  background: #eef6ff;
  color: #0d68c5;
}

.feedback-status.pending {
  background: #fff3dd;
  color: #b25b00;
}

.feedback-status.processing {
  background: #e8f3ff;
  color: #0d68c5;
}

.feedback-status.closed {
  background: #e8f7ef;
  color: #078149;
}

.feedback-actions {
  min-width: 0;
}

.feedback-actions .text-button {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: #fff;
  color: #2f5f95;
  border: 1px solid var(--admin-border);
  font-size: 13px;
  font-weight: 600;
}

.feedback-actions .primary-text {
  border-color: transparent;
  background: var(--admin-primary);
  color: #fff;
}

:deep(.feedback-ledger-panel .panel-heading) {
  padding-left: 28px;
}

:deep(.feedback-ledger-card .table-scroll-shell) {
  background: #fff !important;
}

:deep(.feedback-ledger-card .table-scroll-shell .feedback-table-head) {
  background: #fff !important;
}

:deep(.feedback-ledger-card .admin-list-pagination) {
  background: #fff !important;
}

:deep(.feedback-ledger-card .panel-heading),
:deep(.feedback-ledger-card .panel-heading > div) {
  background: #fff;
}

.feedback-ledger-empty {
  min-height: 220px;
}

.feedback-ledger-empty h3 {
  margin: 0;
  color: var(--admin-text);
  font-size: 22px;
  font-weight: 800;
}

.feedback-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.feedback-detail-grid > div {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid #d7e8f8;
  border-radius: 12px;
  background: #f8fbff;
}

.feedback-detail-grid .full {
  grid-column: 1 / -1;
}

.feedback-detail-grid span {
  color: var(--admin-text-mid);
  font-size: 13px;
}

.feedback-detail-grid strong {
  color: var(--admin-text-strong);
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.dialog-select {
  width: 100%;
}

@media (max-width: 1180px) {
  .feedback-page {
    --feedback-grid: minmax(140px, 0.9fr) minmax(96px, 0.52fr) minmax(190px, 1.08fr) minmax(76px, 0.42fr) minmax(128px, 0.62fr) minmax(106px, 0.54fr);
  }
}

@media (max-width: 760px) {
  .feedback-filter-grid {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .feedback-filter-layout,
  .feedback-filter-toolbar {
    padding-left: 0;
    padding-right: 0;
  }

  .feedback-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
