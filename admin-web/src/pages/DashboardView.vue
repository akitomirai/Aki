<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Teleport } from 'vue'
import { ElMessage } from 'element-plus'
import { createDashboardBackup, getDashboardStatistics } from '../api/dashboard'
import { formatDateTime, normalizeDisplayNumber, normalizeDisplayText } from '../utils/display'
import { downloadJsonFile } from '../utils/exportTools'

const loading = ref(true)
const loadError = ref('')
const backupLoading = ref(false)
const importLoading = ref(false)
const backupImportInputRef = ref(null)
const importPreviewVisible = ref(false)
const importedBackup = ref(null)
const statistics = ref(createEmptyStatistics())

const analysis = computed(() => statistics.value.analysis || createEmptyAnalysis())
const qrScanAnalysis = computed(() => statistics.value.qrScanAnalysis || createEmptyQrScanAnalysis())
const qrScanTrend = computed(() => qrScanAnalysis.value.sevenDayTrend || [])
const hotTraceCodes = computed(() => qrScanAnalysis.value.hotTraceCodes || [])
const statusQueryCounts = computed(() => qrScanAnalysis.value.statusQueryCounts || [])
const publishRecords = computed(() => statistics.value.publishRecords || [])
const publishedTraceRecords = computed(() => publishRecords.value.filter(isPublishedTraceRecord))
const productTraceAnalysis = computed(() => statistics.value.productTraceAnalysis || [])
const qualityRiskAnalysis = computed(() => statistics.value.qualityRiskAnalysis || createEmptyQualityRiskAnalysis())
const headerActionsReady = ref(false)
const maxTrendCount = computed(() => Math.max(1, ...qrScanTrend.value.map((item) => Number(item.count || 0))))

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

const productSummaryGroups = computed(() => {
  const summary = statistics.value.publishStatusSummary || {}
  return [
    {
      key: 'scale',
      title: '追溯规模',
      main: countText(statistics.value.productTotal),
      unit: '个产品',
      items: [
        { label: '批次', value: countText(statistics.value.batchTotal) },
        { label: '已发布', value: countText(summary.published) }
      ]
    },
    {
      key: 'publish',
      title: '发布进度',
      main: percentText(analysis.value.publishRate),
      unit: '',
      items: [
        { label: '未发布', value: countText(summary.unpublished) },
        { label: '待质检', value: countText(summary.pendingQuality) }
      ]
    },
    {
      key: 'risk',
      title: '待办风险',
      main: countText(analysis.value.pendingActionTotal),
      unit: '项',
      tone: 'danger',
      items: [
        { label: '风险处理中', value: countText(summary.riskProcessing) },
        { label: '召回/冻结', value: countText(summary.recalledOrFrozen) }
      ]
    }
  ]
})

const qrScanCards = computed(() => [
  {
    key: 'total',
    title: '扫码总次数',
    value: countText(qrScanAnalysis.value.totalScanCount),
    tone: 'total',
    subCounts: statusQueryCounts.value.map((item) => ({
      key: item.key,
      label: displayText(item.label),
      value: countText(item.count)
    }))
  },
  { key: 'today', title: '今日扫码次数', value: countText(qrScanAnalysis.value.todayScanCount) },
  { key: 'records', title: '公开查询记录', value: countText(statistics.value.queryTotal) },
  { key: 'hot', title: '热门追溯码', value: qrScanAnalysis.value.hotTraceCodes.length ? displayText(qrScanAnalysis.value.hotTraceCodes[0].traceToken) : '-' }
])

const qualityRiskCards = computed(() => [
  { key: 'pass', label: '质检合格', value: qualityRiskAnalysis.value.qualityPassedCount, tone: 'success' },
  { key: 'fail', label: '质检不合格', value: qualityRiskAnalysis.value.qualityFailedCount, tone: 'danger' },
  { key: 'pending', label: '待质检', value: qualityRiskAnalysis.value.pendingQualityCount, tone: 'info' },
  { key: 'risk', label: '风险批次', value: qualityRiskAnalysis.value.riskBatchCount, tone: 'danger' },
  { key: 'action', label: '冻结/召回', value: qualityRiskAnalysis.value.frozenOrRecalledCount, tone: 'warning' }
])

const productHotRanking = computed(() => {
  const maxQuery = Math.max(1, ...productTraceAnalysis.value.map((item) => Number(item.queryCount || 0)))
  return productTraceAnalysis.value
    .slice()
    .sort((left, right) => Number(right.queryCount || 0) - Number(left.queryCount || 0))
    .map((item) => ({
      ...item,
      heatRate: safeRate((Number(item.queryCount || 0) / maxQuery) * 100)
    }))
})

const importedBackupStats = computed(() => {
  const snapshot = importedBackup.value || {}
  return [
    { label: '企业', value: countText(snapshot.companyTotal) },
    { label: '产品', value: countText(snapshot.productTotal) },
    { label: '批次', value: countText(snapshot.batchTotal ?? snapshot.batches?.length) },
    { label: '质检', value: countText(snapshot.qualityReportTotal) },
    { label: '二维码', value: countText(snapshot.qrCodeTotal) },
    { label: '查询', value: countText(snapshot.qrQueryTotal) }
  ]
})

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

function createEmptyQrScanAnalysis() {
  return {
    totalScanCount: 0,
    todayScanCount: 0,
    sevenDayTrend: [],
    hotTraceCodes: [],
    statusQueryCounts: [
      { key: 'normal', label: '正常码', count: 0 },
      { key: 'risk', label: '风险码', count: 0 },
      { key: 'invalid', label: '无效码', count: 0 }
    ]
  }
}

function createEmptyQualityRiskAnalysis() {
  return {
    qualityPassedCount: 0,
    qualityFailedCount: 0,
    pendingQualityCount: 0,
    riskBatchCount: 0,
    frozenOrRecalledCount: 0,
    qualityPassRate: 0,
    riskRate: 0
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
    qrScanAnalysis: createEmptyQrScanAnalysis(),
    analysis: createEmptyAnalysis(),
    productTraceAnalysis: [],
    qualityRiskAnalysis: createEmptyQualityRiskAnalysis()
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
  const sourceQrScanAnalysis = data.qrScanAnalysis || {}
  const sourceQualityRisk = data.qualityRiskAnalysis || {}
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
    qrScanAnalysis: {
      ...createEmptyQrScanAnalysis(),
      ...sourceQrScanAnalysis,
      totalScanCount: Number(sourceQrScanAnalysis.totalScanCount ?? data.queryTotal ?? 0),
      todayScanCount: Number(sourceQrScanAnalysis.todayScanCount ?? 0),
      sevenDayTrend: Array.isArray(sourceQrScanAnalysis.sevenDayTrend)
        ? sourceQrScanAnalysis.sevenDayTrend.map(normalizeTrendPoint)
        : [],
      hotTraceCodes: Array.isArray(sourceQrScanAnalysis.hotTraceCodes)
        ? sourceQrScanAnalysis.hotTraceCodes.map(normalizeHotTraceCode)
        : [],
      statusQueryCounts: Array.isArray(sourceQrScanAnalysis.statusQueryCounts)
        ? sourceQrScanAnalysis.statusQueryCounts.map(normalizeStatusQueryCount)
        : createEmptyQrScanAnalysis().statusQueryCounts
    },
    analysis: {
      ...createEmptyAnalysis(),
      ...sourceAnalysis,
      publishRate: safeRate(sourceAnalysis.publishRate),
      qualityPassRate: safeRate(sourceAnalysis.qualityPassRate),
      riskRate: safeRate(sourceAnalysis.riskRate),
      pendingActionTotal: Number(sourceAnalysis.pendingActionTotal ?? 0),
      analysisText: normalizeDisplayText(sourceAnalysis.analysisText, '-'),
      managementTips: Array.isArray(sourceAnalysis.managementTips) ? sourceAnalysis.managementTips : []
    },
    productTraceAnalysis: Array.isArray(data.productTraceAnalysis)
      ? data.productTraceAnalysis.map(normalizeProductTraceAnalysis)
      : [],
    qualityRiskAnalysis: {
      ...createEmptyQualityRiskAnalysis(),
      ...sourceQualityRisk,
      qualityPassedCount: Number(sourceQualityRisk.qualityPassedCount ?? sourceQualityRisk.passCount ?? 0),
      qualityFailedCount: Number(sourceQualityRisk.qualityFailedCount ?? sourceQualityRisk.failCount ?? 0),
      pendingQualityCount: Number(sourceQualityRisk.pendingQualityCount ?? sourceQualityRisk.pendingCount ?? 0),
      riskBatchCount: Number(sourceQualityRisk.riskBatchCount ?? 0),
      frozenOrRecalledCount: Number(sourceQualityRisk.frozenOrRecalledCount ?? sourceQualityRisk.pendingRiskActionCount ?? 0),
      qualityPassRate: safeRate(sourceQualityRisk.qualityPassRate ?? sourceQualityRisk.passRate),
      riskRate: safeRate(sourceQualityRisk.riskRate)
    }
  }
}

function normalizeTrendPoint(item = {}) {
  return {
    date: normalizeDisplayText(item.date || item.day, '-'),
    count: Number(item.count ?? item.pv ?? 0)
  }
}

function normalizeHotTraceCode(item = {}) {
  return {
    traceToken: normalizeDisplayText(item.traceToken, '-'),
    batchNo: normalizeDisplayText(item.batchNo, '-'),
    productName: normalizeDisplayText(item.productName, '-'),
    queryCount: Number(item.queryCount ?? 0)
  }
}

function normalizeStatusQueryCount(item = {}) {
  return {
    key: normalizeDisplayText(item.key, 'normal'),
    label: normalizeDisplayText(item.label, '-'),
    count: Number(item.count ?? 0)
  }
}

function normalizeProductTraceAnalysis(item = {}) {
  return {
    productName: normalizeDisplayText(item.productName, '-'),
    category: normalizeDisplayText(item.category, '-'),
    batchCount: Number(item.batchCount ?? 0),
    publishedCount: Number(item.publishedCount ?? 0),
    qualityPassedCount: Number(item.qualityPassedCount ?? 0),
    riskCount: Number(item.riskCount ?? 0),
    queryCount: Number(item.queryCount ?? 0)
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

function trendBarStyle(count) {
  const ratio = Math.max(0.08, Number(count || 0) / maxTrendCount.value)
  return { height: `${Math.round(ratio * 100)}%` }
}

function heatBarStyle(rate) {
  return { width: `${Math.max(6, safeRate(rate))}%` }
}

function productPublishRate(item) {
  return item.batchCount > 0 ? safeRate((item.publishedCount / item.batchCount) * 100) : 0
}

function productRiskRate(item) {
  return item.batchCount > 0 ? safeRate((item.riskCount / item.batchCount) * 100) : 0
}

function shortDate(value) {
  const text = displayText(value)
  if (text === '-') return text
  return text.slice(5).replace('-', '/')
}

function timestampForFilename() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

function downloadFolderHint() {
  return '浏览器默认下载目录，Windows 通常为 C:\\Users\\Lenovo\\Downloads'
}

async function handleBackupDownload() {
  backupLoading.value = true
  try {
    const response = await createDashboardBackup()
    if (!isSuccessResponse(response)) {
      throw new Error(response?.message || '备份文件生成失败')
    }
    const snapshot = response.data || {}
    const filename = `${snapshot.backupNo || `trace-backup-${timestampForFilename()}`}.json`
    downloadJsonFile({ filename, data: snapshot })
    ElMessage.success(`已生成 ${filename}，文件会保存到${downloadFolderHint()}。`)
  } catch (error) {
    ElMessage.error(error?.message || '备份文件生成失败')
  } finally {
    backupLoading.value = false
  }
}

function handleReportImport() {
  backupImportInputRef.value?.click()
}

function handleBackupFileSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  importLoading.value = true

  const reader = new FileReader()
  reader.onload = () => {
    try {
      const parsed = JSON.parse(String(reader.result || '{}'))
      importedBackup.value = normalizeImportedBackup(parsed, file.name)
      importPreviewVisible.value = true
      ElMessage.success(`已导入 ${file.name}，可查看备份内容。`)
    } catch (error) {
      ElMessage.error(error?.message || '备份文件格式不正确，请选择“备份”生成的 JSON 文件。')
    } finally {
      importLoading.value = false
    }
  }
  reader.onerror = () => {
    importLoading.value = false
    ElMessage.error('备份文件读取失败，请重新选择文件。')
  }
  reader.readAsText(file, 'utf-8')
}

function normalizeImportedBackup(data, filename) {
  const snapshot = data?.backupNo || data?.batches || data?.generatedAt ? data : data?.data
  if (!snapshot || typeof snapshot !== 'object') {
    throw new Error('备份文件内容为空或格式不正确。')
  }
  if (!Array.isArray(snapshot.batches)) {
    throw new Error('未识别到批次备份数据，请选择系统生成的备份 JSON。')
  }
  return {
    ...snapshot,
    filename,
    batches: snapshot.batches
  }
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
  headerActionsReady.value = Boolean(document.getElementById('admin-header-actions'))
  loadDashboard()
})

onUnmounted(() => {
  headerActionsReady.value = false
})
</script>

<template>
  <div class="dashboard-page" data-testid="dashboard-page">
    <Teleport v-if="headerActionsReady" to="#admin-header-actions">
      <div class="dashboard-header-actions">
        <el-button :loading="backupLoading" type="primary" @click="handleBackupDownload">备份</el-button>
        <input
          ref="backupImportInputRef"
          class="backup-import-input"
          type="file"
          accept=".json,application/json"
          @change="handleBackupFileSelected"
        >
        <el-button :loading="importLoading" @click="handleReportImport">导入</el-button>
        <el-button :loading="loading" @click="loadDashboard">刷新</el-button>
      </div>
    </Teleport>

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
      <section class="section-card product-analysis-section" data-testid="product-analysis-section">
        <div class="section-head section-head--stacked">
          <div>
            <h2>产品追溯表现分析</h2>
          </div>
        </div>
        <div class="product-summary-strip">
          <article
            v-for="group in productSummaryGroups"
            :key="group.key"
            class="product-summary-card"
            :class="group.tone"
          >
            <div>
              <span>{{ group.title }}</span>
              <strong>{{ group.main }}<small v-if="group.unit">{{ group.unit }}</small></strong>
            </div>
            <div class="product-summary-items">
              <span v-for="item in group.items" :key="item.label">
                {{ item.label }} <b>{{ item.value }}</b>
              </span>
            </div>
          </article>
        </div>
        <div v-if="!productTraceAnalysis.length" class="plain-empty">
          暂无产品维度统计数据
        </div>
        <div v-else class="product-analysis-list">
          <article
            v-for="item in productTraceAnalysis"
            :key="item.productName"
            class="product-analysis-row"
            :class="{ 'is-risk': item.riskCount > 0 }"
          >
            <div class="product-main">
              <strong :title="item.productName">{{ item.productName }}</strong>
              <span>{{ item.category }}</span>
            </div>
            <div class="product-metrics">
              <span>批次 <b>{{ countText(item.batchCount) }}</b></span>
              <span>已发布 <b>{{ countText(item.publishedCount) }}</b></span>
              <span>合格 <b>{{ countText(item.qualityPassedCount) }}</b></span>
              <span class="danger">风险 <b>{{ countText(item.riskCount) }}</b></span>
              <span>查询 <b>{{ countText(item.queryCount) }}</b></span>
            </div>
            <div class="product-progress-group">
              <div class="mini-progress">
                <span>发布率</span>
                <el-progress :percentage="productPublishRate(item)" :stroke-width="8" :show-text="false" />
                <b>{{ percentText(productPublishRate(item)) }}</b>
              </div>
              <div class="mini-progress risk">
                <span>风险占比</span>
                <el-progress
                  :percentage="productRiskRate(item)"
                  :status="rateStatus(productRiskRate(item), 'risk')"
                  :stroke-width="8"
                  :show-text="false"
                />
                <b>{{ percentText(productRiskRate(item)) }}</b>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="analysis-two-column">
        <section class="section-card product-hot-section" data-testid="product-hot-section">
          <div class="section-head">
            <div>
              <h2>产品扫码热度排行</h2>
            </div>
          </div>
          <div v-if="!productHotRanking.length" class="plain-empty">
            暂无产品扫码数据
          </div>
          <div v-else class="product-hot-list">
            <article v-for="item in productHotRanking" :key="item.productName" class="product-hot-item">
              <div class="product-hot-top">
                <strong :title="item.productName">{{ item.productName }}</strong>
                <span>{{ countText(item.queryCount) }} 次</span>
              </div>
              <div class="product-hot-sub">
                {{ item.category }} / {{ countText(item.batchCount) }} 个批次 / 已发布 {{ countText(item.publishedCount) }}
              </div>
              <div class="heat-bar-shell">
                <span class="heat-bar" :style="heatBarStyle(item.heatRate)"></span>
              </div>
            </article>
          </div>
        </section>

        <section class="section-card quality-risk-section" data-testid="quality-risk-section">
          <div class="section-head">
            <div>
              <h2>质量风险统计</h2>
            </div>
          </div>
          <div class="quality-risk-card-grid">
            <article
              v-for="item in qualityRiskCards"
              :key="item.key"
              class="quality-risk-card"
              :class="item.tone"
            >
              <span>{{ item.label }}</span>
              <strong>{{ countText(item.value) }}</strong>
            </article>
          </div>
          <div class="quality-rate-list">
            <div class="quality-rate-row">
              <span>质检通过率</span>
              <el-progress :percentage="qualityRiskAnalysis.qualityPassRate" :stroke-width="9" />
            </div>
            <div class="quality-rate-row">
              <span>风险占比</span>
              <el-progress
                :percentage="qualityRiskAnalysis.riskRate"
                :status="rateStatus(qualityRiskAnalysis.riskRate, 'risk')"
                :stroke-width="9"
              />
            </div>
          </div>
        </section>
      </section>

      <section class="section-card qr-scan-section" data-testid="qr-scan-analysis-section">
        <div class="section-head table-head">
          <div>
            <h2>二维码扫码</h2>
          </div>
          <el-tag type="primary" effect="plain">
            近 7 日趋势
          </el-tag>
        </div>

        <div class="qr-scan-card-grid">
          <article v-for="card in qrScanCards" :key="card.key" class="qr-scan-card" :class="[card.key, card.tone]">
            <span>{{ card.title }}</span>
            <div class="qr-card-value-row">
              <strong :title="card.value">{{ card.value }}</strong>
              <div v-if="card.subCounts?.length" class="qr-card-subcounts">
                <span v-for="sub in card.subCounts" :key="sub.key">
                  {{ sub.label }} <b>{{ sub.value }}</b>
                </span>
              </div>
            </div>
          </article>
        </div>

        <div class="qr-scan-content-grid">
          <div class="qr-trend-panel">
            <div class="qr-panel-head">
              <h3>近 7 日扫码趋势</h3>
            </div>
            <div class="qr-trend-chart">
              <div v-for="item in qrScanTrend" :key="item.date" class="qr-trend-item">
                <div class="qr-trend-bar-shell">
                  <span class="qr-trend-bar" :style="trendBarStyle(item.count)"></span>
                </div>
                <strong>{{ countText(item.count) }}</strong>
                <span>{{ shortDate(item.date) }}</span>
              </div>
            </div>
          </div>

          <div class="qr-hot-panel">
            <div class="qr-panel-head">
              <h3>热门追溯码或热门批次</h3>
            </div>
            <div v-if="!hotTraceCodes.length" class="qr-empty">
              暂无公开查询记录
            </div>
            <div v-else class="qr-hot-list">
              <article v-for="item in hotTraceCodes" :key="item.traceToken" class="qr-hot-item">
                <div>
                  <strong :title="displayText(item.traceToken)">{{ displayText(item.traceToken) }}</strong>
                  <span>{{ displayText(item.productName) }} / {{ displayText(item.batchNo) }}</span>
                </div>
                <b>{{ countText(item.queryCount) }} 次</b>
              </article>
            </div>
          </div>
        </div>

      </section>

      <section class="section-card publish-table-card" data-testid="publish-records-section">
        <div class="section-head table-head">
          <div>
            <h2>公开溯源码状态</h2>
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
            暂无公开溯源信息
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

    <el-dialog v-model="importPreviewVisible" title="备份文件导入结果" width="560px">
      <div v-if="importedBackup" class="backup-import-preview">
        <div class="backup-import-meta">
          <strong>{{ displayText(importedBackup.backupNo || importedBackup.filename) }}</strong>
          <span>生成时间：{{ displayDateTime(importedBackup.generatedAt) }}</span>
          <span>生成账号：{{ displayText(importedBackup.generatedBy) }}</span>
          <span>文件名称：{{ displayText(importedBackup.filename) }}</span>
        </div>
        <div class="backup-import-grid">
          <article v-for="item in importedBackupStats" :key="item.label" class="backup-import-stat">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <p class="backup-import-note">
          当前导入用于校验和查看备份内容，不会覆盖现有业务数据。
        </p>
      </div>
      <template #footer>
        <el-button type="primary" @click="importPreviewVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 20px 24px 24px;
}

.dashboard-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.backup-import-input {
  display: none;
}

.backup-import-preview {
  display: grid;
  gap: 14px;
}

.backup-import-meta {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  background: var(--admin-surface-soft);
}

.backup-import-meta strong {
  color: var(--admin-text);
  font-size: 16px;
}

.backup-import-meta span {
  color: var(--admin-text-soft);
  font-size: 13px;
}

.backup-import-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.backup-import-stat {
  padding: 10px 12px;
  border: 1px solid var(--admin-border);
  border-radius: 10px;
  background: #ffffff;
}

.backup-import-stat span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.backup-import-stat strong {
  display: block;
  margin-top: 4px;
  color: var(--admin-primary-deep);
  font-size: 22px;
  line-height: 1;
}

.backup-import-note {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(48, 149, 246, 0.08);
  color: var(--admin-text-soft);
  font-size: 13px;
}

.section-card {
  border: 1px solid var(--admin-border);
  border-radius: 14px;
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

.section-card {
  padding: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.section-head h2 {
  margin: 0;
  color: var(--admin-text);
  font-size: 16px;
}

.section-head p {
  margin: 6px 0 0;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.section-head--stacked {
  align-items: flex-start;
}

.product-summary-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 4px 0 12px;
}

.product-summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 12px;
  background: #ffffff;
}

.product-summary-card > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.product-summary-card span {
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.product-summary-card strong {
  color: var(--admin-text);
  font-size: 22px;
  font-weight: 850;
  line-height: 1;
}

.product-summary-card small {
  margin-left: 4px;
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.product-summary-card.danger strong {
  color: var(--admin-danger-text);
}

.product-summary-items {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  min-width: 0;
}

.product-summary-items span {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: var(--admin-surface-soft);
  white-space: nowrap;
}

.product-summary-items b {
  margin-left: 4px;
  color: var(--admin-text);
}

.plain-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  border-radius: 14px;
  background: var(--admin-surface-soft);
  color: var(--admin-text-soft);
  font-size: 14px;
}

.product-analysis-list {
  display: grid;
  gap: 8px;
}

.product-analysis-row {
  display: grid;
  grid-template-columns: minmax(150px, 0.9fr) minmax(360px, 1.4fr) minmax(260px, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 66px;
  padding: 10px 12px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 12px;
  background: #ffffff;
}

.product-analysis-row:hover {
  background: rgba(48, 149, 246, 0.03);
}

.product-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.product-main strong {
  overflow: hidden;
  color: var(--admin-text);
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-main span,
.product-hot-sub {
  color: var(--admin-text-soft);
  font-size: 13px;
}

.product-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.product-metrics span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--admin-surface-soft);
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.product-metrics b {
  color: var(--admin-text);
}

.product-metrics .danger b {
  color: var(--admin-danger-text);
}

.product-progress-group {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.mini-progress {
  display: grid;
  grid-template-columns: 54px minmax(80px, 1fr) 48px;
  align-items: center;
  gap: 8px;
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.mini-progress b {
  color: var(--admin-text);
  text-align: right;
}

.mini-progress.risk b {
  color: var(--admin-danger-text);
}

.analysis-two-column {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.product-hot-list {
  display: grid;
  gap: 8px;
}

.product-hot-item {
  display: grid;
  gap: 6px;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 12px;
  background: #ffffff;
}

.product-hot-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.product-hot-top strong {
  overflow: hidden;
  color: var(--admin-text);
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-hot-top span {
  flex: 0 0 auto;
  color: var(--admin-primary-deep);
  font-size: 13px;
  font-weight: 800;
}

.heat-bar-shell {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef4fb;
}

.heat-bar {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(180deg, #5aa0f2 0%, #2f7fd3 100%);
}

.quality-risk-card-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.quality-risk-card {
  min-height: 58px;
  padding: 10px;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 12px;
  background: #ffffff;
}

.quality-risk-card span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.quality-risk-card strong {
  display: block;
  margin-top: 6px;
  color: var(--admin-text);
  font-size: 22px;
  font-weight: 850;
  line-height: 1;
}

.quality-risk-card.success strong {
  color: #1f8a55;
}

.quality-risk-card.danger strong {
  color: var(--admin-danger-text);
}

.quality-risk-card.warning strong {
  color: #9a6512;
}

.quality-risk-card.info strong {
  color: var(--admin-primary-deep);
}

.quality-rate-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.quality-rate-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: var(--admin-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.table-head {
  align-items: center;
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

.batch-link {
  color: var(--admin-primary-deep);
  font-size: 15px;
  font-weight: 800;
  text-decoration: none;
}

.qr-scan-section {
  display: grid;
  gap: 12px;
}

.qr-scan-card-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.qr-scan-card,
.qr-trend-panel,
.qr-hot-panel {
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 12px;
  background: #ffffff;
}

.qr-scan-card {
  min-height: 62px;
  padding: 10px 12px;
}

.qr-scan-card span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.qr-scan-card strong {
  min-width: 0;
  overflow: hidden;
  color: var(--admin-text);
  font-size: 20px;
  font-weight: 800;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-scan-card.total {
  grid-column: span 2;
}

.qr-scan-card.hot {
  grid-column: span 2;
}

.qr-card-value-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  margin-top: 5px;
}

.qr-scan-card:not(.total) .qr-card-value-row {
  display: block;
}

.qr-card-subcounts {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: nowrap;
  gap: 8px;
  min-width: 0;
  margin-top: 0;
}

.qr-card-subcounts span {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: var(--admin-surface-soft);
  color: var(--admin-text-soft);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.qr-card-subcounts b {
  margin-left: 4px;
  color: var(--admin-text);
}

.qr-scan-card.normal strong {
  color: #1f8a55;
}

.qr-scan-card.risk strong {
  color: var(--admin-danger-text);
}

.qr-scan-card.invalid strong {
  color: #6f86a4;
}

.qr-scan-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 12px;
}

.qr-trend-panel,
.qr-hot-panel {
  min-height: 204px;
  padding: 12px;
}

.qr-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.qr-panel-head h3 {
  margin: 0;
  color: var(--admin-text);
  font-size: 15px;
}

.qr-trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  align-items: end;
  gap: 8px;
  min-height: 146px;
  padding-top: 6px;
}

.qr-trend-item {
  display: grid;
  grid-template-rows: 104px auto auto;
  justify-items: center;
  gap: 5px;
  min-width: 0;
}

.qr-trend-bar-shell {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  width: 100%;
  height: 104px;
  border-radius: 10px;
  background: rgba(56, 134, 217, 0.08);
}

.qr-trend-bar {
  display: block;
  width: min(34px, 58%);
  min-height: 8px;
  border-radius: 10px 10px 4px 4px;
  background: linear-gradient(180deg, #5aa0f2 0%, #2f7fd3 100%);
}

.qr-trend-item strong {
  color: var(--admin-text);
  font-size: 13px;
  line-height: 1;
}

.qr-trend-item span {
  color: var(--admin-text-soft);
  font-size: 12px;
  line-height: 1;
}

.qr-hot-list {
  display: grid;
  gap: 6px;
}

.qr-hot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 0;
  background: #ffffff;
}

.qr-hot-item div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.qr-hot-item strong,
.qr-hot-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-hot-item strong {
  color: var(--admin-text);
  font-size: 13px;
  font-weight: 800;
}

.qr-hot-item span {
  color: var(--admin-text-soft);
  font-size: 12px;
}

.qr-hot-item b {
  flex: 0 0 auto;
  color: var(--admin-primary-deep);
  font-size: 13px;
}

.qr-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 150px;
  color: var(--admin-text-soft);
  font-size: 13px;
}

@media (max-width: 1180px) {
  .product-analysis-row,
  .analysis-two-column {
    grid-template-columns: 1fr;
  }

  .quality-risk-card-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .qr-scan-card-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .qr-scan-content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .qr-scan-card.total {
    grid-column: span 1;
  }

  .qr-scan-card.hot {
    grid-column: span 1;
  }
}

@media (max-width: 820px) {
  .dashboard-page {
    padding: var(--admin-page-shell-padding-mobile);
  }

  .table-head,
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard-header-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .qr-scan-card-grid,
  .qr-scan-content-grid,
  .quality-risk-card-grid {
    grid-template-columns: 1fr;
  }

  .publish-ledger-shell {
    overflow-x: auto;
  }

  .publish-table-head,
  .publish-row {
    min-width: 920px;
  }

  .qr-trend-chart {
    overflow-x: auto;
  }

  .qr-trend-item {
    min-width: 56px;
  }
}
</style>
