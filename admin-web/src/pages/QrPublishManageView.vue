<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changeBatchStatus, generateBatchQr, getBatchDetail, getBatchList } from '../api/batch'
import { useAuthStore } from '../stores/auth'
import { getFriendlyErrorMessage } from '../utils/batchExperience'
import { resolveQrStatusText } from '../utils/statusPresentation'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const rows = ref([])
const message = ref('')
const messageType = ref('info')
const activeTab = ref('NEED_QR')
const filters = ref(createFilterState())
const qrSubmittingId = ref(null)
const downloadSubmittingId = ref(null)
const publishSubmitting = ref(false)
const previewDialog = ref(createPreviewDialogState())
const publishDialog = ref(createPublishDialogState())

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

const visibleRows = computed(() => rows.value.filter((item) => matchesKeyword(item) && matchesCompany(item) && matchesTab(item)))

const tabCounts = computed(() => {
  return tabs.reduce((acc, tab) => {
    acc[tab.value] = rows.value.filter((item) => matchesTab(item, tab.value)).length
    return acc
  }, {})
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

function canPublish(item) {
  return Boolean(actionOf(item, 'PUBLISH').enabled || actionOf(item, 'RESUME').enabled || item.publishReady)
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
    return '二维码和公开页已经生效，可以直接核对扫码入口和工作台状态。'
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

function latestUpdatedText(item) {
  return item.lastUpdatedAt || item.latestTraceTime || '暂无更新'
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
    const response = await getBatchList(cleanObject({
      status: filters.value.status || undefined
    }))
    rows.value = response.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '二维码与发布列表加载失败，请稍后再试。'), 'error')
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
  if (!publishDialog.value.batch?.id) {
    return
  }
  publishSubmitting.value = true
  try {
    await changeBatchStatus(publishDialog.value.batch.id, {
      targetStatus: 'PUBLISHED',
      reason: publishDialog.value.reason,
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
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">二维码与发布管理</h1>
        <p class="manage-page-subtitle">集中处理二维码生成、公开入口预览、发布前检查和批次发布，不必逐个进入工作台再核对。</p>
      </div>
      <div class="manage-page-actions">
        <button class="ghost" data-testid="qr-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
      </div>
    </section>

    <section class="panel todo-tabs-panel">
      <div class="todo-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          class="todo-tab"
          :class="{ active: activeTab === tab.value }"
          :data-testid="`qr-tab-${tab.value}`"
          @click="activeTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <strong>{{ tabCounts[tab.value] ?? 0 }}</strong>
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="filter-grid">
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
        <div class="toolbar-actions" style="align-items: end;">
          <button class="ghost" @click="resetFilters">重置筛选</button>
        </div>
      </div>

      <div class="toolbar">
        <div class="list-summary">
          当前显示 {{ visibleRows.length }} 个批次，直接看二维码是否就绪、为什么能发，以及下一步应该做什么。
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section class="panel">
      <div class="todo-table-head qr-head">
        <span>批次</span>
        <span>企业 / 更新时间</span>
        <span>当前状态</span>
        <span>发布前检查</span>
        <span>二维码 / 公开入口</span>
        <span>操作</span>
      </div>

      <div v-if="loading" class="empty-state">
        <div>
          <h3>正在加载二维码与发布列表...</h3>
          <p class="empty-copy">稍等一下，正在汇总批次状态和公开入口。</p>
        </div>
      </div>

      <div v-else-if="!visibleRows.length" class="empty-state">
        <div>
          <h3>当前筛选下没有批次</h3>
          <p class="empty-copy">可以切换 tabs 或调整企业、状态筛选后再看。</p>
        </div>
      </div>

      <div v-else class="todo-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="todo-row qr-row"
          :data-testid="`qr-row-${item.id}`"
        >
          <div class="row-main">
            <strong>{{ item.productName }}</strong>
            <small>{{ item.batchCode }}</small>
            <small>{{ item.originPlace || '暂无产地' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ item.companyName }}</strong>
            <small>最近更新时间</small>
            <small>{{ latestUpdatedText(item) }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
            <span class="info-pill" :class="qualityToneClass(item)">{{ item.qualityStatus || '待上传' }}</span>
            <span class="info-pill">{{ resolveQrStatusText({ status: item.qrStatus, statusLabel: item.qrStatusLabel }) }}</span>
            <small v-if="item.qrToken" class="qr-token-line">公开标识：{{ item.qrToken }}</small>
          </div>

          <div class="publish-check-panel">
            <strong class="publish-summary" :class="publishToneClass(item)">{{ publishSummary(item) }}</strong>
            <div class="publish-checklist">
              <article
                v-for="entry in publishChecks(item)"
                :key="entry.key"
                class="publish-check"
                :class="{ done: entry.done, blocked: !entry.done }"
              >
                <strong>{{ entry.label }}</strong>
                <small>{{ entry.detail }}</small>
              </article>
            </div>
            <p class="hint-note">{{ publishHint(item) }}</p>
          </div>

          <div class="row-meta qr-entry-panel">
            <strong>{{ item.qrToken || '暂无公开标识' }}</strong>
            <small>{{ hasQr(item) ? '二维码入口已准备' : '先生成二维码，再预览或打开公开页' }}</small>
            <small v-if="item.marketDate">公开时间：{{ item.marketDate }}</small>
          </div>

          <div class="action-cluster">
            <button
              class="primary"
              :disabled="!actionOf(item, 'GENERATE_QR').enabled || qrSubmittingId === item.id"
              :data-testid="`qr-generate-${item.id}`"
              @click="handleGenerateQr(item)"
            >
              {{ qrSubmittingId === item.id ? '正在生成...' : (hasQr(item) ? '二维码已生成' : '生成二维码') }}
            </button>
            <button
              class="success"
              :disabled="!canPublish(item)"
              :data-testid="`qr-publish-${item.id}`"
              @click="openPublishDialog(item)"
            >
              {{ publishActionLabel(item) }}
            </button>
            <button
              class="ghost"
              :data-testid="`qr-workbench-${item.id}`"
              @click="openWorkbench(item)"
            >
              进入工作台
            </button>
            <div class="inline-actions">
              <button
                class="text-button"
                :disabled="!hasQr(item)"
                :data-testid="`qr-preview-${item.id}`"
                @click="openPreviewDialog(item)"
              >
                预览二维码
              </button>
              <button
                class="text-button"
                :disabled="!hasQr(item) || downloadSubmittingId === item.id"
                :data-testid="`qr-download-${item.id}`"
                @click="downloadQr(item)"
              >
                {{ downloadSubmittingId === item.id ? '正在下载...' : '下载二维码' }}
              </button>
              <button
                class="text-button"
                :disabled="!hasQr(item)"
                :data-testid="`qr-public-${item.id}`"
                @click="openPublicPage(item)"
              >
                打开公开页
              </button>
            </div>
          </div>
        </article>
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
            <a class="preview-link" :href="previewDialog.qr?.publicUrl" target="_blank" rel="noreferrer">打开公开页</a>
          </div>

          <div class="dialog-actions" style="margin-top: 18px;">
            <button class="ghost" @click="downloadQr(previewDialog.batch)">下载二维码</button>
            <button class="primary" @click="openWorkbench(previewDialog.batch)">进入工作台</button>
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

        <p class="hint-note" style="margin-top: 14px;">不可发布时，先回列表查看阻塞项或进入工作台补齐质检、二维码和风险处理。</p>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="publishSubmitting" @click="closePublishDialog">取消</button>
          <button
            class="success"
            data-testid="qr-publish-submit"
            :disabled="publishSubmitting || !canPublish(publishDialog.batch || {})"
            @click="submitPublish"
          >
            {{ publishSubmitting ? '正在提交...' : publishActionLabel(publishDialog.batch || {}) }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style src="../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.qr-head,
.qr-row {
  grid-template-columns:
    minmax(0, 1.25fr)
    minmax(0, 1fr)
    minmax(0, 1fr)
    minmax(0, 1.5fr)
    minmax(0, 0.95fr)
    minmax(0, 1.35fr);
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

@media (max-width: 760px) {
  .qr-row {
    grid-template-columns: 1fr;
  }
}
</style>
