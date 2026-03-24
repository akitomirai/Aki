<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  changeBatchStatus,
  createBatch,
  createQualityReport,
  createTraceRecord,
  generateBatchQr,
  getBatchDetail,
  getBatchList,
  updateBatch
} from '../api/batch'

const router = useRouter()

const loading = ref(false)
const batches = ref([])
const message = ref('')
const messageType = ref('info')

const filters = ref(createFilterState())
const dialog = ref(createDialogState())
const batchForm = ref(createBatchForm())
const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const statusForm = ref(createStatusForm())

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'FROZEN', label: '已冻结' },
  { value: 'RECALLED', label: '已召回' }
]

const stageOptions = [
  { value: 'ARCHIVE', label: '建档' },
  { value: 'PRODUCE', label: '生产' },
  { value: 'QUALITY', label: '质检' },
  { value: 'TRANSPORT', label: '运输' },
  { value: 'WAREHOUSE', label: '仓储' },
  { value: 'DELIVERY', label: '出库' },
  { value: 'MARKET', label: '上市' },
  { value: 'REGULATION', label: '监管' }
]

const qualityOptions = [
  { value: 'PASS', label: '合格' },
  { value: 'FAIL', label: '不合格' },
  { value: 'REVIEW', label: '待确认' }
]

const companyOptions = computed(() => {
  return [...new Set(batches.value.map((item) => item.companyName).filter(Boolean))]
})

const listStats = computed(() => {
  const total = batches.value.length
  const published = batches.value.filter((item) => item.status === 'PUBLISHED').length
  const draft = batches.value.filter((item) => item.status === 'DRAFT').length
  const risk = batches.value.filter((item) => ['FROZEN', 'RECALLED'].includes(item.status)).length
  return { total, published, draft, risk }
})

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'create':
      return '新建批次'
    case 'edit':
      return `编辑批次：${dialog.value.batchName}`
    case 'trace':
      return `新增追溯记录：${dialog.value.batchName}`
    case 'quality':
      return `上传质检摘要：${dialog.value.batchName}`
    case 'status':
      return `变更批次状态：${dialog.value.batchName}`
    default:
      return ''
  }
})

onMounted(() => {
  fetchBatches()
})

async function fetchBatches() {
  loading.value = true
  try {
    const response = await getBatchList(cleanObject(filters.value))
    batches.value = response.data ?? []
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

function createFilterState() {
  return {
    batchCode: '',
    productName: '',
    status: '',
    companyName: '',
    dateFrom: '',
    dateTo: ''
  }
}

function createDialogState() {
  return {
    visible: false,
    type: '',
    batchId: null,
    batchName: ''
  }
}

function createBatchForm() {
  return {
    batchCode: createBatchCode(),
    productName: '',
    category: '水果',
    companyName: '赣南脐橙示范基地',
    originPlace: '',
    productionDate: todayString(),
    publicRemark: '',
    internalRemark: ''
  }
}

function createTraceForm() {
  return {
    stage: 'PRODUCE',
    title: '',
    eventTime: currentDateTime(),
    operatorName: '企业录入员',
    location: '',
    summary: '',
    imageUrl: '',
    visibleToConsumer: true
  }
}

function createQualityForm() {
  return {
    reportNo: '',
    agency: '',
    result: 'PASS',
    reportTime: currentDateTime(),
    highlightsText: '检测通过'
  }
}

function createStatusForm(targetStatus = 'PUBLISHED') {
  return {
    targetStatus,
    reason: defaultReason(targetStatus),
    operatorName: '企业管理员'
  }
}

function cleanObject(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== null && value !== undefined && value !== '')
  )
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function resetFilters() {
  filters.value = createFilterState()
  fetchBatches()
}

function openCreateDialog() {
  dialog.value = {
    visible: true,
    type: 'create',
    batchId: null,
    batchName: ''
  }
  batchForm.value = createBatchForm()
}

async function openEditDialog(item) {
  try {
    const response = await getBatchDetail(item.id)
    const detail = response.data
    batchForm.value = {
      batchCode: detail.batch.batchCode,
      productName: detail.product.name,
      category: detail.product.category,
      companyName: detail.company.name,
      originPlace: detail.batch.originPlace,
      productionDate: detail.batch.productionDate,
      publicRemark: detail.batch.publicRemark ?? '',
      internalRemark: detail.batch.internalRemark ?? ''
    }
    dialog.value = {
      visible: true,
      type: 'edit',
      batchId: item.id,
      batchName: item.batchCode
    }
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

function openTraceDialog(item) {
  traceForm.value = createTraceForm()
  dialog.value = {
    visible: true,
    type: 'trace',
    batchId: item.id,
    batchName: item.batchCode
  }
}

function openQualityDialog(item) {
  qualityForm.value = createQualityForm()
  dialog.value = {
    visible: true,
    type: 'quality',
    batchId: item.id,
    batchName: item.batchCode
  }
}

function openStatusDialog(item, targetStatus) {
  statusForm.value = createStatusForm(targetStatus)
  dialog.value = {
    visible: true,
    type: 'status',
    batchId: item.id,
    batchName: item.batchCode
  }
}

function closeDialog() {
  dialog.value = createDialogState()
}

async function submitDialog() {
  try {
    if (dialog.value.type === 'create') {
      const response = await createBatch(batchForm.value)
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      router.push(`/batches/${response.data.batch.id}`)
      return
    }

    if (dialog.value.type === 'edit') {
      const response = await updateBatch(dialog.value.batchId, {
        productName: batchForm.value.productName,
        category: batchForm.value.category,
        companyName: batchForm.value.companyName,
        originPlace: batchForm.value.originPlace,
        productionDate: batchForm.value.productionDate,
        publicRemark: batchForm.value.publicRemark,
        internalRemark: batchForm.value.internalRemark
      })
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      router.push(`/batches/${response.data.batch.id}`)
      return
    }

    if (dialog.value.type === 'trace') {
      const response = await createTraceRecord(dialog.value.batchId, traceForm.value)
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      return
    }

    if (dialog.value.type === 'quality') {
      const response = await createQualityReport(dialog.value.batchId, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlights(qualityForm.value.highlightsText)
      })
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      return
    }

    if (dialog.value.type === 'status') {
      const response = await changeBatchStatus(dialog.value.batchId, statusForm.value)
      showMessage(response.message, 'success')
      closeDialog()
      await fetchBatches()
      router.push(`/batches/${response.data.batch.id}`)
    }
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function handleGenerateQr(item) {
  try {
    const response = await generateBatchQr(item.id)
    showMessage(response.message, 'success')
    await fetchBatches()
    router.push(`/batches/${item.id}`)
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

function splitHighlights(text) {
  return text
    .split(/[\n,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function createBatchCode() {
  const now = new Date()
  return `BATCH${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`
}

function todayString() {
  return new Date().toISOString().slice(0, 10)
}

function currentDateTime() {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
}

function getErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '操作失败，请稍后重试。'
}

function defaultReason(targetStatus) {
  return {
    PUBLISHED: '资料与二维码已准备完成，可以对外发布。',
    FROZEN: '发现异常，先冻结公开展示并排查原因。',
    RECALLED: '检测或监管提示存在风险，立即召回处理。'
  }[targetStatus] ?? ''
}

function actionOf(item, code) {
  return item.actions?.find((action) => action.code === code) ?? {
    code,
    label: code,
    enabled: true,
    hint: '',
    variant: 'neutral'
  }
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[status] ?? 'draft'
}

function qrStatusLabel(status) {
  return status && status !== 'NOT_GENERATED' ? '已生成' : '待生成'
}
</script>

<template>
  <div class="page-shell">
    <header class="hero-card">
      <div>
        <p class="eyebrow">批次中心后台</p>
        <h1>把批次列表做成主入口</h1>
        <p class="lead">
          这里优先服务企业建档、批次创建、过程补录、质检上传、二维码生成和发布操作，减少跳转，保证主流程顺手。
        </p>
      </div>
      <div class="hero-actions">
        <button class="primary" @click="openCreateDialog">新建批次</button>
        <button class="ghost" @click="router.push('/dashboard')">返回概览</button>
      </div>
    </header>

    <section class="stats-grid">
      <article class="stat-card">
        <span>当前列表批次数</span>
        <strong>{{ listStats.total }}</strong>
      </article>
      <article class="stat-card">
        <span>已发布</span>
        <strong>{{ listStats.published }}</strong>
      </article>
      <article class="stat-card">
        <span>草稿</span>
        <strong>{{ listStats.draft }}</strong>
      </article>
      <article class="stat-card warning">
        <span>风险批次</span>
        <strong>{{ listStats.risk }}</strong>
      </article>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">筛选条件</p>
          <h2>围绕批次快速找到要处理的记录</h2>
        </div>
      </div>

      <div class="filter-grid">
        <label>
          <span>批次号</span>
          <input v-model.trim="filters.batchCode" type="text" placeholder="例如 BATCH2026032401">
        </label>
        <label>
          <span>产品名称</span>
          <input v-model.trim="filters.productName" type="text" placeholder="例如 赣南脐橙">
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label>
          <span>企业</span>
          <input
            v-model.trim="filters.companyName"
            type="text"
            list="company-options"
            placeholder="输入或选择企业名"
          >
          <datalist id="company-options">
            <option v-for="item in companyOptions" :key="item" :value="item" />
          </datalist>
        </label>
        <label>
          <span>生产日期起</span>
          <input v-model="filters.dateFrom" type="date">
        </label>
        <label>
          <span>生产日期止</span>
          <input v-model="filters.dateTo" type="date">
        </label>
      </div>

      <div class="toolbar">
        <button class="primary" :disabled="loading" @click="fetchBatches">查询批次</button>
        <button class="ghost" :disabled="loading" @click="resetFilters">重置筛选</button>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">
      {{ message }}
    </section>

    <section v-if="loading" class="panel empty-state">
      正在加载批次列表...
    </section>

    <section v-else-if="!batches.length" class="panel empty-state">
      当前没有匹配的批次，可以直接新建一个批次进入主流程。
    </section>

    <section v-else class="batch-list">
      <article v-for="item in batches" :key="item.id" class="batch-card">
        <div class="batch-main">
          <img class="product-image" :src="item.productImageUrl" :alt="item.productName">
          <div class="batch-copy">
            <div class="title-row">
              <div>
                <p class="batch-code">{{ item.batchCode }}</p>
                <h3>{{ item.productName }}</h3>
              </div>
              <span class="status-badge" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
            </div>

            <div class="meta-grid">
              <div>
                <span>企业</span>
                <strong>{{ item.companyName }}</strong>
              </div>
              <div>
                <span>产地</span>
                <strong>{{ item.originPlace }}</strong>
              </div>
              <div>
                <span>当前节点</span>
                <strong>{{ item.currentNode }}</strong>
              </div>
              <div>
                <span>生产日期</span>
                <strong>{{ item.productionDate }}</strong>
              </div>
              <div>
                <span>上市日期</span>
                <strong>{{ item.marketDate || '未发布' }}</strong>
              </div>
              <div>
                <span>二维码</span>
                <strong>{{ qrStatusLabel(item.qrStatus) }}</strong>
              </div>
              <div>
                <span>质检</span>
                <strong>{{ item.qualityStatus }}</strong>
              </div>
            </div>

            <div class="tag-row">
              <span v-for="tag in item.quickTags" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </div>

        <div class="action-row">
          <button class="ghost" @click="router.push(`/batches/${item.id}`)">查看详情</button>
          <button class="ghost" @click="openEditDialog(item)">编辑</button>
          <button class="neutral" @click="openTraceDialog(item)">追溯记录</button>
          <button class="success" @click="openQualityDialog(item)">质检</button>
          <button
            class="neutral"
            :title="actionOf(item, 'GENERATE_QR').hint"
            @click="handleGenerateQr(item)"
          >
            二维码
          </button>
          <button
            class="primary"
            :disabled="!actionOf(item, 'PUBLISH').enabled"
            :title="actionOf(item, 'PUBLISH').hint"
            @click="openStatusDialog(item, 'PUBLISHED')"
          >
            发布
          </button>
          <button
            class="success"
            :disabled="!actionOf(item, 'RESUME').enabled"
            :title="actionOf(item, 'RESUME').hint"
            @click="openStatusDialog(item, 'PUBLISHED')"
          >
            恢复
          </button>
          <button
            class="warning"
            :disabled="!actionOf(item, 'FREEZE').enabled"
            :title="actionOf(item, 'FREEZE').hint"
            @click="openStatusDialog(item, 'FROZEN')"
          >
            冻结
          </button>
          <button
            class="danger"
            :disabled="!actionOf(item, 'RECALL').enabled"
            :title="actionOf(item, 'RECALL').hint"
            @click="openStatusDialog(item, 'RECALLED')"
          >
            召回
          </button>
        </div>
      </article>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">快速处理</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost icon-button" @click="closeDialog">关闭</button>
        </div>

        <div v-if="dialog.type === 'create' || dialog.type === 'edit'" class="form-grid">
          <label v-if="dialog.type === 'create'">
            <span>批次号</span>
            <input v-model.trim="batchForm.batchCode" type="text">
          </label>
          <label>
            <span>产品名称</span>
            <input v-model.trim="batchForm.productName" type="text">
          </label>
          <label>
            <span>品类</span>
            <input v-model.trim="batchForm.category" type="text">
          </label>
          <label>
            <span>企业名称</span>
            <input v-model.trim="batchForm.companyName" type="text">
          </label>
          <label>
            <span>产地</span>
            <input v-model.trim="batchForm.originPlace" type="text">
          </label>
          <label>
            <span>生产日期</span>
            <input v-model="batchForm.productionDate" type="date">
          </label>
          <label class="full-width">
            <span>公开说明</span>
            <textarea v-model.trim="batchForm.publicRemark" rows="3" placeholder="面向消费者展示的简短说明" />
          </label>
          <label class="full-width">
            <span>内部备注</span>
            <textarea v-model.trim="batchForm.internalRemark" rows="3" placeholder="内部工作备注" />
          </label>
        </div>

        <div v-else-if="dialog.type === 'trace'" class="form-grid">
          <label>
            <span>阶段</span>
            <select v-model="traceForm.stage">
              <option v-for="item in stageOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>发生时间</span>
            <input v-model="traceForm.eventTime" type="datetime-local">
          </label>
          <label>
            <span>操作人</span>
            <input v-model.trim="traceForm.operatorName" type="text">
          </label>
          <label>
            <span>地点</span>
            <input v-model.trim="traceForm.location" type="text">
          </label>
          <label class="full-width">
            <span>记录标题</span>
            <input v-model.trim="traceForm.title" type="text" placeholder="可选，默认会按阶段自动命名">
          </label>
          <label class="full-width">
            <span>摘要说明</span>
            <textarea v-model.trim="traceForm.summary" rows="4" placeholder="尽量一句话说明本次处理结果" />
          </label>
          <label class="full-width">
            <span>现场图片链接</span>
            <input v-model.trim="traceForm.imageUrl" type="url" placeholder="可选，用于工作台与公开页展示">
          </label>
          <label class="checkbox-field full-width">
            <input v-model="traceForm.visibleToConsumer" type="checkbox">
            <span>允许在公开追溯页展示这条记录</span>
          </label>
        </div>

        <div v-else-if="dialog.type === 'quality'" class="form-grid">
          <label>
            <span>报告编号</span>
            <input v-model.trim="qualityForm.reportNo" type="text">
          </label>
          <label>
            <span>检测机构</span>
            <input v-model.trim="qualityForm.agency" type="text">
          </label>
          <label>
            <span>检测结果</span>
            <select v-model="qualityForm.result">
              <option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>
            <span>检测时间</span>
            <input v-model="qualityForm.reportTime" type="datetime-local">
          </label>
          <label class="full-width">
            <span>检测亮点</span>
            <textarea
              v-model.trim="qualityForm.highlightsText"
              rows="4"
              placeholder="多个要点可用逗号、分号或换行分隔"
            />
          </label>
        </div>

        <div v-else-if="dialog.type === 'status'" class="form-grid">
          <label>
            <span>目标状态</span>
            <select v-model="statusForm.targetStatus">
              <option value="PUBLISHED">发布</option>
              <option value="FROZEN">冻结</option>
              <option value="RECALLED">召回</option>
            </select>
          </label>
          <label>
            <span>操作人</span>
            <input v-model.trim="statusForm.operatorName" type="text">
          </label>
          <label class="full-width">
            <span>变更原因</span>
            <textarea v-model.trim="statusForm.reason" rows="4" placeholder="请写明状态变更原因" />
          </label>
        </div>

        <div class="dialog-actions">
          <button class="ghost" @click="closeDialog">取消</button>
          <button class="primary" @click="submitDialog">提交</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page-shell {
  max-width: 1240px;
  margin: 0 auto;
  padding: 28px 20px 44px;
}

.hero-card,
.panel,
.stat-card,
.batch-card,
.dialog-card,
.message-bar {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 42px rgba(31, 55, 47, 0.08);
}

.hero-card {
  display: grid;
  grid-template-columns: 1.5fr auto;
  gap: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(242, 204, 105, 0.26), transparent 28%),
    linear-gradient(140deg, #17362d, #285143 70%, #edf5eb 160%);
  color: #f7f2e7;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f2cc69;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 12px;
  font-size: 38px;
}

.lead {
  margin-bottom: 0;
  max-width: 760px;
  line-height: 1.8;
  color: rgba(247, 242, 231, 0.92);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.stat-card {
  padding: 22px;
}

.stat-card span {
  display: block;
  color: #60786d;
  font-size: 14px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 32px;
  color: #17362d;
}

.stat-card.warning strong {
  color: #b24b2d;
}

.panel {
  margin-top: 18px;
  padding: 24px;
}

.panel-head {
  margin-bottom: 18px;
}

.panel-head h2 {
  margin-bottom: 0;
}

.filter-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

label {
  display: block;
}

label span {
  display: block;
  margin-bottom: 8px;
  color: #4b6358;
  font-size: 14px;
}

input,
select,
textarea,
button {
  font: inherit;
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid rgba(45, 85, 71, 0.16);
  border-radius: 16px;
  background: rgba(247, 249, 246, 0.95);
  color: #17362d;
}

textarea {
  resize: vertical;
}

.full-width {
  grid-column: 1 / -1;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.checkbox-field input {
  width: 18px;
  height: 18px;
}

.checkbox-field span {
  margin: 0;
}

.toolbar,
.dialog-actions,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar {
  margin-top: 18px;
}

button,
.preview-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

button:hover,
.preview-link:hover {
  transform: translateY(-1px);
}

button:disabled {
  opacity: 0.46;
  cursor: not-allowed;
  transform: none;
}

.primary {
  background: #245846;
  color: #fff7ea;
}

.ghost {
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

.neutral {
  background: rgba(242, 204, 105, 0.18);
  color: #7b5d13;
}

.success {
  background: rgba(40, 110, 74, 0.12);
  color: #245846;
}

.warning {
  background: rgba(214, 137, 58, 0.16);
  color: #8b4c13;
}

.danger {
  background: rgba(190, 70, 58, 0.14);
  color: #8f2f29;
}

.message-bar {
  margin-top: 18px;
  padding: 14px 18px;
}

.message-bar.info {
  color: #245846;
}

.message-bar.success {
  color: #245846;
  background: rgba(226, 244, 233, 0.94);
}

.message-bar.error {
  color: #8f2f29;
  background: rgba(253, 236, 235, 0.94);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  color: #546b61;
}

.batch-list {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.batch-card {
  padding: 22px;
}

.batch-main {
  display: grid;
  grid-template-columns: 132px 1fr;
  gap: 18px;
}

.product-image {
  width: 132px;
  height: 132px;
  object-fit: cover;
  border-radius: 22px;
  background: linear-gradient(160deg, #eef5ed, #dce9df);
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.batch-code {
  margin-bottom: 6px;
  color: #60786d;
  font-size: 14px;
  letter-spacing: 0.04em;
}

.batch-copy h3 {
  margin-bottom: 0;
  font-size: 28px;
  color: #17362d;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
}

.status-badge.draft {
  background: rgba(93, 120, 110, 0.16);
  color: #556b63;
}

.status-badge.published {
  background: rgba(40, 110, 74, 0.14);
  color: #245846;
}

.status-badge.frozen {
  background: rgba(214, 137, 58, 0.18);
  color: #8b4c13;
}

.status-badge.recalled {
  background: rgba(190, 70, 58, 0.16);
  color: #8f2f29;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.meta-grid div {
  padding: 14px;
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.meta-grid span {
  display: block;
  color: #688176;
  font-size: 12px;
}

.meta-grid strong {
  display: block;
  margin-top: 8px;
  color: #17362d;
  line-height: 1.5;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.tag-row span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
  font-size: 13px;
}

.action-row {
  margin-top: 18px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 24, 20, 0.4);
}

.dialog-card {
  width: min(820px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 24px;
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.icon-button {
  min-width: 72px;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 1080px) {
  .stats-grid,
  .meta-grid,
  .filter-grid,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .page-shell {
    padding-inline: 14px;
  }

  .hero-card,
  .batch-main {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .product-image {
    width: 100%;
    height: 220px;
  }

  .title-row {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 30px;
  }

  .stats-grid,
  .meta-grid,
  .filter-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .batch-copy h3 {
    font-size: 24px;
  }
}
</style>
