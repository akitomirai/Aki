<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboardStatistics } from '../api/dashboard'
import { formatDateTime, normalizeDisplayNumber, normalizeDisplayText } from '../utils/display'

const loading = ref(true)
const loadError = ref('')
const statistics = ref(createEmptyStatistics())

const statCards = computed(() => [
  { key: 'productTotal', title: '产品总数', value: countText(statistics.value.productTotal) },
  { key: 'batchTotal', title: '批次总数', value: countText(statistics.value.batchTotal) },
  { key: 'publishedBatchTotal', title: '已发布', value: countText(statistics.value.publishedBatchTotal) },
  { key: 'pendingQualityTotal', title: '待质检', value: countText(statistics.value.pendingQualityTotal) },
  { key: 'riskBatchTotal', title: '风险批次', value: countText(statistics.value.riskBatchTotal), risk: true },
  { key: 'queryTotal', title: '公开查询', value: countText(statistics.value.queryTotal) }
])

const statusRows = computed(() => {
  const summary = statistics.value.publishStatusSummary || {}
  return [
    { key: 'published', label: '已发布', value: summary.published, tone: 'success' },
    { key: 'unpublished', label: '未发布', value: summary.unpublished, tone: 'warning' },
    { key: 'pendingQuality', label: '待质检', value: summary.pendingQuality, tone: 'info' },
    { key: 'riskProcessing', label: '风险处理中', value: summary.riskProcessing, tone: 'danger' },
    { key: 'recalledOrFrozen', label: '已召回/冻结', value: summary.recalledOrFrozen, tone: 'danger' }
  ]
})

const publishRecords = computed(() => statistics.value.publishRecords || [])

const analysis = computed(() => statistics.value.analysis || createEmptyAnalysis())

const pendingActionItems = computed(() => [
  {
    key: 'risk',
    label: '风险',
    value: countText(statistics.value.riskBatchTotal)
  },
  {
    key: 'quality',
    label: '待质检',
    value: countText(statistics.value.pendingQualityTotal)
  },
  {
    key: 'publish',
    label: '未发布',
    value: countText(statistics.value.unpublishedBatchTotal)
  }
])

const publishedTraceRecords = computed(() => (
  publishRecords.value.filter((record) => isPublishedTraceRecord(record))
))

function createEmptyAnalysis() {
  return {
    publishRate: 0,
    qualityPassRate: 0,
    riskRate: 0,
    pendingActionTotal: 0,
    analysisText: '-',
    managementTips: []
  }
}

function createEmptyStatistics() {
  return {
    productTotal: 0,
    batchTotal: 0,
    publishedBatchTotal: 0,
    unpublishedBatchTotal: 0,
    pendingQualityTotal: 0,
    qualityPassedTotal: 0,
    riskBatchTotal: 0,
    queryTotal: 0,
    publishStatusSummary: {
      published: 0,
      unpublished: 0,
      pendingQuality: 0,
      riskProcessing: 0,
      recalledOrFrozen: 0
    },
    publishRecords: [],
    analysis: createEmptyAnalysis()
  }
}

function isSuccessResponse(res) {
  return res?.success === true || Number(res?.code) === 0 || String(res?.code) === '0'
}

async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getDashboardStatistics()
    if (!isSuccessResponse(response)) {
      throw new Error(response?.message || '统计数据加载失败')
    }
    statistics.value = normalizeStatistics(response?.data)
  } catch (error) {
    loadError.value = error?.message || '统计数据加载失败，请稍后重试。'
    statistics.value = createEmptyStatistics()
  } finally {
    loading.value = false
  }
}

function normalizeStatistics(data = {}) {
  const empty = createEmptyStatistics()
  const summary = data.publishStatusSummary || {}
  const sourceAnalysis = data.analysis || {}
  return {
    ...empty,
    ...data,
    productTotal: Number(data.productTotal ?? 0),
    batchTotal: Number(data.batchTotal ?? data.totalBatches ?? 0),
    publishedBatchTotal: Number(data.publishedBatchTotal ?? data.publishedBatches ?? 0),
    unpublishedBatchTotal: Number(data.unpublishedBatchTotal ?? data.draftBatches ?? 0),
    pendingQualityTotal: Number(data.pendingQualityTotal ?? 0),
    qualityPassedTotal: Number(data.qualityPassedTotal ?? 0),
    riskBatchTotal: Number(data.riskBatchTotal ?? data.riskBatches ?? 0),
    queryTotal: Number(data.queryTotal ?? 0),
    publishStatusSummary: {
      published: Number(summary.published ?? data.publishedBatchTotal ?? data.publishedBatches ?? 0),
      unpublished: Number(summary.unpublished ?? data.unpublishedBatchTotal ?? data.draftBatches ?? 0),
      pendingQuality: Number(summary.pendingQuality ?? data.pendingQualityTotal ?? 0),
      riskProcessing: Number(summary.riskProcessing ?? 0),
      recalledOrFrozen: Number(summary.recalledOrFrozen ?? data.riskBatchTotal ?? data.riskBatches ?? 0)
    },
    publishRecords: Array.isArray(data.publishRecords) ? data.publishRecords : [],
    analysis: {
      ...createEmptyAnalysis(),
      ...sourceAnalysis,
      publishRate: safeRate(sourceAnalysis.publishRate),
      qualityPassRate: safeRate(sourceAnalysis.qualityPassRate),
      riskRate: safeRate(sourceAnalysis.riskRate),
      pendingActionTotal: Number(sourceAnalysis.pendingActionTotal ?? 0),
      analysisText: normalizeDisplayText(sourceAnalysis.analysisText, '-'),
      managementTips: Array.isArray(sourceAnalysis.managementTips) ? sourceAnalysis.managementTips : []
    }
  }
}

function countText(value) {
  return normalizeDisplayNumber(value, '0')
}

function displayText(value) {
  return normalizeDisplayText(value, '-')
}

function displayDateTime(value) {
  return formatDateTime(value) || '-'
}

function safeRate(value) {
  const num = Number(value)
  if (!Number.isFinite(num) || num < 0) return 0
  if (num > 100) return 100
  return Number(num.toFixed(1))
}

function percentText(value) {
  return `${safeRate(value).toFixed(1)}%`
}

function rateStatus(value, type) {
  const rate = safeRate(value)
  if (type === 'risk') {
    if (rate > 0) return 'exception'
    return 'success'
  }
  if (rate >= 80) return 'success'
  if (rate > 0) return 'warning'
  return ''
}

function qualityTagType(record) {
  const result = String(record?.qualityResult || record?.qualityStatus || '').toUpperCase()
  if (['PASS', '合格'].includes(result)) return 'success'
  if (['FAIL', '不合格'].includes(result)) return 'danger'
  return 'info'
}

function publishTagType(record) {
  const status = String(record?.publishStatus || '').toUpperCase()
  if (status === 'PUBLISHED') return 'success'
  if (['FROZEN', 'RECALLED'].includes(status)) return 'danger'
  return 'warning'
}

function isPublishedTraceRecord(record) {
  const publishStatus = String(record?.publishStatus || '').toUpperCase()
  return ['PUBLISHED', 'FROZEN', 'RECALLED'].includes(publishStatus) || Boolean(record?.publishTime)
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="dashboard-page" data-testid="dashboard-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">数据统计分析管理</h1>
      </div>
      <div class="manage-page-actions">
        <el-button :loading="loading" @click="loadDashboard">刷新</el-button>
      </div>
    </section>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="warning"
      show-icon
      :closable="false"
    />

    <section v-if="loading" class="section-card loading-card">
      正在加载统计分析数据...
    </section>

    <template v-else>
      <section class="core-metrics-grid" data-testid="analysis-section">
        <article class="core-metric-card publish">
          <div class="core-metric-head">
            <span>溯源发布率</span>
          </div>
          <strong>{{ percentText(analysis.publishRate) }}</strong>
          <div class="core-metric-sub">
            {{ countText(statistics.publishedBatchTotal) }} / {{ countText(statistics.batchTotal) }}
          </div>
          <el-progress
            :percentage="analysis.publishRate"
            :status="rateStatus(analysis.publishRate, 'publish')"
            :stroke-width="10"
            :show-text="false"
          />
        </article>

        <article class="core-metric-card pending">
          <div class="core-metric-head">
            <span>待处理事项</span>
          </div>
          <strong>{{ countText(analysis.pendingActionTotal) }} 项</strong>
          <div class="pending-action-split">
            <span v-for="item in pendingActionItems" :key="item.key">
              {{ item.label }} {{ item.value }}
            </span>
          </div>
        </article>

        <article class="core-metric-card risk">
          <div class="core-metric-head">
            <span>风险批次占比</span>
          </div>
          <strong>{{ percentText(analysis.riskRate) }}</strong>
          <div class="core-metric-sub">
            {{ countText(statistics.riskBatchTotal) }} / {{ countText(statistics.batchTotal) }}
          </div>
          <el-progress
            :percentage="analysis.riskRate"
            :status="rateStatus(analysis.riskRate, 'risk')"
            :stroke-width="10"
            :show-text="false"
          />
        </article>
      </section>

      <section class="stats-grid" data-testid="statistics-cards">
        <article
          v-for="card in statCards"
          :key="card.key"
          class="stat-card"
          :class="{ risk: card.risk }"
        >
          <span>{{ card.title }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </section>

      <section class="section-card status-section">
        <div class="section-head">
          <h2>溯源发布状态统计</h2>
        </div>
        <div class="status-summary-grid">
          <div v-for="item in statusRows" :key="item.key" class="status-summary-item">
            <span>{{ item.label }}</span>
            <strong :class="item.tone">{{ countText(item.value) }}</strong>
          </div>
        </div>
      </section>

      <section class="section-card publish-table-card" data-testid="publish-records-section">
        <div class="section-head table-head">
          <div>
            <h2>已发布溯源情况</h2>
          </div>
          <el-tag type="primary" effect="plain">
            共 {{ publishedTraceRecords.length }} 条
          </el-tag>
        </div>

        <div class="publish-ledger-shell">
          <div class="publish-table-head ledger-table-head">
            <span>产品名称</span>
            <span>批次编号</span>
            <span>追溯码</span>
            <span>发布时间</span>
            <span>质检结论</span>
            <span>公开状态</span>
            <span class="table-head-cell--center">查询次数</span>
          </div>

          <div v-if="!publishedTraceRecords.length" class="publish-empty">
            暂无已发布溯源信息
          </div>

          <div v-else class="publish-row-list ledger-row-list">
            <article
              v-for="row in publishedTraceRecords"
              :key="row.batchId || row.batchNo || row.traceToken"
              class="publish-row ledger-row"
            >
              <div class="row-product">
                <strong :title="displayText(row.productName)">{{ displayText(row.productName) }}</strong>
              </div>
              <div class="row-batch">
                <RouterLink v-if="row.batchId" :to="`/batches/${row.batchId}`" class="batch-link">
                  {{ displayText(row.batchNo) }}
                </RouterLink>
                <strong v-else>{{ displayText(row.batchNo) }}</strong>
              </div>
              <div class="trace-token-cell">
                {{ displayText(row.traceToken) }}
              </div>
              <div class="publish-time-cell">
                {{ displayDateTime(row.publishTime) }}
              </div>
              <div class="status-chip-stack">
                <span class="status-chip" :class="qualityTagType(row)">
                  {{ displayText(row.qualityStatus) }}
                </span>
              </div>
              <div class="status-chip-stack">
                <span class="status-chip" :class="publishTagType(row)">
                  {{ displayText(row.publishStatusLabel || row.publishStatus) }}
                </span>
              </div>
              <div class="publish-query-cell">
                {{ countText(row.queryCount) }}
              </div>
            </article>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: var(--admin-page-section-gap);
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--admin-page-shell-padding);
}

.section-card,
.stat-card,
.core-metric-card {
  border: 1px solid var(--admin-border);
  border-radius: 18px;
  background: var(--admin-surface);
  box-shadow: var(--admin-shadow);
}

.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  margin-top: 18px;
  color: var(--admin-text-soft);
}

.core-metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.core-metric-card {
  display: grid;
  align-content: space-between;
  min-height: 178px;
  padding: 22px;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, rgba(242, 248, 255, 0.86) 100%);
}

.core-metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.core-metric-head span {
  color: var(--admin-text);
  font-size: 15px;
  font-weight: 800;
}

.core-metric-card strong {
  display: block;
  margin-top: 16px;
  color: var(--admin-text);
  font-size: 46px;
  font-weight: 850;
  line-height: 1;
}

.core-metric-sub {
  margin: 10px 0 18px;
  color: var(--admin-text-soft);
  font-size: 15px;
  font-weight: 700;
}

.core-metric-card.publish {
  border-color: rgba(56, 134, 217, 0.22);
}

.core-metric-card.pending {
  border-color: rgba(242, 139, 34, 0.24);
  background: linear-gradient(180deg, #ffffff 0%, rgba(255, 248, 238, 0.9) 100%);
}

.core-metric-card.pending strong {
  color: #9a6512;
}

.core-metric-card.risk {
  border-color: rgba(190, 70, 58, 0.24);
  background: linear-gradient(180deg, #ffffff 0%, rgba(255, 243, 241, 0.9) 100%);
}

.core-metric-card.risk strong {
  color: var(--admin-danger-text);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  min-height: 76px;
  padding: 14px 16px;
  border-radius: 14px;
  box-shadow: none;
}

.stat-card span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: var(--admin-text);
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

.stat-card.risk strong {
  color: var(--admin-danger-text);
}

.section-card {
  padding: 20px;
}

.section-head {
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 0;
  color: var(--admin-text);
  font-size: 18px;
}

.status-summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.status-summary-item {
  min-height: 72px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--admin-surface-soft);
}

.status-summary-item span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.status-summary-item strong {
  display: block;
  margin-top: 8px;
  color: var(--admin-text);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.status-summary-item strong.success {
  color: #1f8a55;
}

.status-summary-item strong.warning {
  color: #9a6512;
}

.status-summary-item strong.info {
  color: var(--admin-primary-deep);
}

.status-summary-item strong.danger {
  color: var(--admin-danger-text);
}

.pending-action-split {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.pending-action-split span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 11px;
  border: 1px solid var(--admin-border);
  border-radius: 999px;
  background: var(--admin-surface);
  color: var(--admin-text-soft);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}


.publish-ledger-shell {
  overflow-x: visible;
  padding-bottom: 2px;
}

.publish-table-head,
.publish-row {
  display: grid;
  grid-template-columns:
    minmax(0, 1.32fr)
    minmax(0, 1.08fr)
    minmax(0, 0.96fr)
    minmax(0, 0.96fr)
    minmax(0, 0.72fr)
    minmax(66px, 0.42fr)
    minmax(76px, 0.44fr);
  width: 100%;
  column-gap: 10px;
}

.publish-table-head {
  align-items: center;
  padding: 0 14px 10px;
  color: #6f86a4;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.publish-row-list {
  display: grid;
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 24px;
  background: #ffffff;
}

.publish-row {
  align-items: center;
  min-height: 74px;
  padding: 14px;
  border-top: 1px solid rgba(56, 134, 217, 0.1);
  background: #ffffff;
  transition: background-color 0.18s ease;
}

.publish-row:first-child {
  border-top: 0;
}

.publish-row:hover {
  background: rgba(48, 149, 246, 0.03);
}

.publish-row > * {
  min-width: 0;
}

.row-product,
.row-batch {
  display: grid;
  gap: 5px;
}

.row-product strong,
.row-batch strong {
  overflow: hidden;
  color: var(--admin-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-product strong,
.row-batch strong {
  font-size: 15px;
  font-weight: 800;
}

.trace-token-cell {
  overflow: hidden;
  color: var(--admin-text-soft);
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}


.status-chip-stack {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.publish-time-cell,
.publish-query-cell {
  color: var(--admin-text);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  white-space: normal;
  word-break: keep-all;
}

.publish-query-cell {
  text-align: center;
}


.publish-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 100%;
  min-height: 160px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 24px;
  background: #ffffff;
  color: var(--admin-text-soft);
  font-size: 14px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  max-width: 100%;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(120, 147, 180, 0.14);
  color: #5f7b9e;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-chip.success {
  background: rgba(46, 166, 106, 0.12);
  color: #1e7d50;
}

.status-chip.warning {
  background: rgba(242, 139, 34, 0.14);
  color: #9a6512;
}

.status-chip.danger {
  background: rgba(190, 70, 58, 0.14);
  color: #8f2f29;
}

.status-chip.info {
  background: rgba(120, 147, 180, 0.14);
  color: #5f7b9e;
}


.table-head-cell--center {
  display: flex;
  justify-content: center;
  text-align: center;
}

.table-head-cell--end {
  display: flex;
  justify-content: flex-end;
  text-align: right;
}

.batch-link {
  color: var(--admin-primary-deep);
  font-size: 15px;
  font-weight: 800;
  text-decoration: none;
}

@media (max-width: 1080px) {
  .core-metrics-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .status-summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .dashboard-page {
    padding: var(--admin-page-shell-padding-mobile);
  }

  .stats-grid,
  .status-summary-grid {
    grid-template-columns: 1fr;
  }

  .core-metric-card {
    min-height: 156px;
  }

  .core-metric-card strong {
    font-size: 38px;
  }

  .table-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .publish-ledger-shell {
    overflow-x: auto;
  }

  .publish-table-head,
  .publish-row {
    min-width: 920px;
  }
}
</style>
