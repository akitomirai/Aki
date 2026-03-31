<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createQualityReport, getBatchDetail, getBatchList, uploadBatchFiles } from '../api/batch'
import { createQualityForm, getFriendlyErrorMessage, getFriendlyUploadError, qualityOptions, splitHighlights } from '../utils/batchExperience'
import { resolveQrStatusText } from '../utils/statusPresentation'

const router = useRouter()

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
  if (!uploadDialog.value.batch?.id) {
    return
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
        <h1 class="manage-page-title">质检待办</h1>
        <p class="manage-page-subtitle">从全局视角处理待上传、已合格、不合格和已上传待发布的批次，不必逐个进工作台找。</p>
      </div>
      <div class="manage-page-actions">
        <button class="ghost" data-testid="quality-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
      </div>
    </section>

    <section class="panel todo-tabs-panel">
      <div class="todo-tabs">
        <button
          v-for="tab in qualityTabs"
          :key="tab.value"
          type="button"
          class="todo-tab"
          :class="{ active: activeTab === tab.value }"
          :data-testid="`quality-tab-${tab.value}`"
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
        <label>
          <span>当前筛选</span>
          <input :value="qualityTabs.find((item) => item.value === activeTab)?.label || '待上传'" type="text" disabled>
        </label>
      </div>

      <div class="toolbar">
        <span class="list-summary">共 {{ rows.length }} 个批次，当前显示 {{ visibleRows.length }} 个。</span>
        <div class="toolbar-actions">
          <button class="primary" data-testid="quality-search-button" :disabled="loading" @click="fetchRows">查询</button>
          <button class="ghost" data-testid="quality-reset-button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section v-if="loading" class="panel empty-state">
      <div>
        <h3>正在加载质检待办...</h3>
        <p class="empty-copy">请稍等，正在汇总批次与质检状态。</p>
      </div>
    </section>

    <section v-else-if="!visibleRows.length" class="panel empty-state">
      <div>
        <h3>当前筛选下没有质检待办</h3>
        <p class="empty-copy">可以切换 tabs 或调整检索条件后再看。</p>
      </div>
    </section>

    <section v-else class="panel">
      <div class="todo-table-head quality-head">
        <span>批次</span>
        <span>企业</span>
        <span>批次状态</span>
        <span>质检状态</span>
        <span>二维码</span>
        <span>允许发布</span>
        <span>最近更新</span>
        <span>操作</span>
      </div>

      <div class="todo-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="todo-row quality-row"
          :data-testid="`quality-row-${item.id}`"
        >
          <div class="row-main">
            <strong>{{ item.productName }}</strong>
            <span>{{ item.batchCode }}</span>
            <small>{{ item.currentNode || '待确认环节' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ item.companyName }}</strong>
            <small>{{ item.originPlace || '暂无产地' }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
            <small>{{ item.marketDate || '未发布' }}</small>
          </div>

          <div class="status-stack">
            <span class="info-pill" :class="resultStatusTone(item)">{{ item.qualityStatus }}</span>
            <small>{{ item.qualityStatusCode === 'PENDING' ? '当前还没有质检摘要' : '已有最新质检结论' }}</small>
          </div>

          <div class="status-stack">
            <strong>{{ resolveQrStatusText({ status: item.qrStatus, statusLabel: item.qrStatusLabel }) }}</strong>
            <small>{{ item.qrStatus === 'NOT_GENERATED' ? '发布前需要先生成二维码' : '公开页入口已就绪' }}</small>
          </div>

          <div class="status-stack">
            <strong>{{ publishReadyText(item) }}</strong>
            <small>{{ resolvePublishReady(item) ? '当前条件允许发布或恢复发布' : '当前还不满足发布条件' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ latestUpdatedText(item) }}</strong>
            <small>最近更新时间</small>
          </div>

          <div class="row-actions">
            <button class="text-button primary-text" :data-testid="`quality-open-workbench-${item.id}`" @click="openWorkbench(item)">进入工作台</button>
            <button class="text-button" :data-testid="`quality-open-report-${item.id}`" :disabled="item.qualityStatusCode === 'PENDING'" @click="openResultDialog(item)">查看质检结果</button>
            <button class="text-button" :data-testid="`quality-upload-${item.id}`" :disabled="!actionEnabled(item, 'UPLOAD_QUALITY')" @click="openUploadDialog(item)">上传质检</button>
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
            去工作台核对
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

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="qualitySubmitting" @click="closeUploadDialog">取消</button>
          <button class="primary" data-testid="quality-upload-submit" :disabled="qualitySubmitting || qualityUploading" @click="submitQualityUpload">
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
    minmax(0, 1.6fr)
    minmax(0, 1.3fr)
    minmax(0, 0.9fr)
    minmax(0, 1fr)
    minmax(0, 0.8fr)
    minmax(0, 1fr)
    minmax(0, 0.9fr)
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

.primary-text {
  font-weight: 700;
}

@media (max-width: 760px) {
  .quality-row {
    grid-template-columns: 1fr;
  }
}
</style>
