<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  changeBatchStatus,
  createQualityReport,
  createTraceRecord,
  generateBatchQr,
  getBatchDetail,
  updateBatch
} from '../api/batch'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref(null)
const message = ref('')
const messageType = ref('info')

const dialog = ref({
  visible: false,
  type: ''
})

const batchForm = ref(createBatchForm())
const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const statusForm = ref(createStatusForm())

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

const dialogTitle = computed(() => {
  switch (dialog.value.type) {
    case 'edit':
      return '编辑批次信息'
    case 'trace':
      return '新增追溯记录'
    case 'quality':
      return '上传质检摘要'
    case 'status':
      return '批次状态操作'
    default:
      return ''
  }
})

const canPreviewPublic = computed(() => detail.value?.qr?.generated && detail.value?.qr?.publicUrl)

onMounted(() => {
  loadDetail(route.params.id)
})

watch(
  () => route.params.id,
  (id) => {
    loadDetail(id)
  }
)

async function loadDetail(id) {
  if (!id) {
    return
  }
  loading.value = true
  try {
    const response = await getBatchDetail(id)
    detail.value = response.data
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

function createBatchForm() {
  return {
    productName: '',
    category: '',
    companyName: '',
    originPlace: '',
    productionDate: '',
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

function currentDateTime() {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function openEditDialog() {
  batchForm.value = {
    productName: detail.value.product.name,
    category: detail.value.product.category,
    companyName: detail.value.company.name,
    originPlace: detail.value.batch.originPlace,
    productionDate: detail.value.batch.productionDate,
    publicRemark: detail.value.batch.publicRemark ?? '',
    internalRemark: detail.value.batch.internalRemark ?? ''
  }
  dialog.value = {
    visible: true,
    type: 'edit'
  }
}

function openTraceDialog() {
  traceForm.value = createTraceForm()
  dialog.value = {
    visible: true,
    type: 'trace'
  }
}

function openQualityDialog() {
  qualityForm.value = createQualityForm()
  dialog.value = {
    visible: true,
    type: 'quality'
  }
}

function openStatusDialog(targetStatus) {
  statusForm.value = createStatusForm(targetStatus)
  dialog.value = {
    visible: true,
    type: 'status'
  }
}

function closeDialog() {
  dialog.value = {
    visible: false,
    type: ''
  }
}

async function submitDialog() {
  try {
    let response

    if (dialog.value.type === 'edit') {
      response = await updateBatch(route.params.id, batchForm.value)
    } else if (dialog.value.type === 'trace') {
      response = await createTraceRecord(route.params.id, traceForm.value)
    } else if (dialog.value.type === 'quality') {
      response = await createQualityReport(route.params.id, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlights(qualityForm.value.highlightsText)
      })
    } else if (dialog.value.type === 'status') {
      response = await changeBatchStatus(route.params.id, statusForm.value)
    } else {
      return
    }

    detail.value = response.data
    showMessage(response.message, 'success')
    closeDialog()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function handleQrAction() {
  try {
    const response = await generateBatchQr(route.params.id)
    detail.value = response.data
    showMessage(response.message, 'success')
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

function actionOf(code) {
  return detail.value?.actions?.find((action) => action.code === code) ?? {
    code,
    label: code,
    enabled: true,
    hint: '',
    variant: 'neutral'
  }
}

function actionClass(code) {
  return actionOf(code).variant || 'neutral'
}

function splitHighlights(text) {
  return text
    .split(/[\n,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function getErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '操作失败，请稍后重试。'
}

function defaultReason(targetStatus) {
  return {
    PUBLISHED: '资料和二维码已准备完成，允许正式发布。',
    FROZEN: '发现异常，先冻结公开展示并继续排查。',
    RECALLED: '确认存在风险，需要立即召回。'
  }[targetStatus] ?? ''
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[status] ?? 'draft'
}
</script>

<template>
  <div class="page-shell">
    <div v-if="loading" class="loading-card">正在加载批次工作台...</div>

    <template v-else-if="detail">
      <header class="hero-card">
        <div class="hero-main">
          <img class="hero-image" :src="detail.product.imageUrl || detail.batch.coverImageUrl" :alt="detail.product.name">
          <div class="hero-copy">
            <p class="eyebrow">批次工作台</p>
            <h1>{{ detail.product.name }}</h1>
            <p class="batch-code">{{ detail.batch.batchCode }}</p>
            <p class="hero-intro">{{ detail.batch.publicRemark || '当前批次已收敛为围绕扫码追溯的工作台视图。' }}</p>

            <div class="hero-grid">
              <div>
                <span>企业</span>
                <strong>{{ detail.company.name }}</strong>
              </div>
              <div>
                <span>产地</span>
                <strong>{{ detail.batch.originPlace }}</strong>
              </div>
              <div>
                <span>生产日期</span>
                <strong>{{ detail.batch.productionDate }}</strong>
              </div>
              <div>
                <span>上市日期</span>
                <strong>{{ detail.batch.marketDate || '未发布' }}</strong>
              </div>
              <div>
                <span>最近记录</span>
                <strong>{{ detail.trace.latestRecordedAt || '暂未补录' }}</strong>
              </div>
              <div>
                <span>质检摘要</span>
                <strong>{{ detail.quality.label }}</strong>
              </div>
            </div>
          </div>
        </div>

        <aside class="hero-side">
          <span class="status-badge" :class="statusClass(detail.status.code)">{{ detail.status.label }}</span>
          <p><strong>当前节点：</strong>{{ detail.status.currentNode }}</p>
          <p><strong>状态原因：</strong>{{ detail.status.reason || '当前没有额外风险说明。' }}</p>
          <p><strong>最近操作：</strong>{{ detail.status.operatorName || '暂无' }}</p>
          <p><strong>操作时间：</strong>{{ detail.status.changedAt || '暂无' }}</p>
        </aside>
      </header>

      <section v-if="message" class="message-bar" :class="messageType">
        {{ message }}
      </section>

      <section class="quick-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">快捷操作区</p>
            <h2>让高频动作直接可见</h2>
          </div>
          <button class="ghost" @click="router.push('/batches')">返回批次列表</button>
        </div>

        <div class="quick-actions">
          <button class="ghost" @click="openEditDialog">编辑批次</button>
          <button class="primary" @click="openTraceDialog">新增追溯记录</button>
          <button class="success" @click="openQualityDialog">上传质检</button>
          <button
            :class="actionClass('GENERATE_QR')"
            :disabled="!actionOf('GENERATE_QR').enabled"
            :title="actionOf('GENERATE_QR').hint"
            @click="handleQrAction"
          >
            生成二维码
          </button>
          <button
            :class="actionClass('PUBLISH')"
            :disabled="!actionOf('PUBLISH').enabled"
            :title="actionOf('PUBLISH').hint"
            @click="openStatusDialog('PUBLISHED')"
          >
            发布
          </button>
          <button
            :class="actionClass('FREEZE')"
            :disabled="!actionOf('FREEZE').enabled"
            :title="actionOf('FREEZE').hint"
            @click="openStatusDialog('FROZEN')"
          >
            冻结
          </button>
          <button
            :class="actionClass('RECALL')"
            :disabled="!actionOf('RECALL').enabled"
            :title="actionOf('RECALL').hint"
            @click="openStatusDialog('RECALLED')"
          >
            召回
          </button>
          <a v-if="canPreviewPublic" class="preview-link" :href="detail.qr.publicUrl" target="_blank" rel="noreferrer">
            公开页预览
          </a>
        </div>
      </section>

      <section class="grid">
        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">追溯记录区</p>
              <h2>最近记录与快速补录</h2>
            </div>
            <button class="ghost" @click="openTraceDialog">继续补录</button>
          </div>

          <div class="trace-summary">
            <div>
              <span>记录总数</span>
              <strong>{{ detail.trace.totalCount }}</strong>
            </div>
            <div>
              <span>最近更新时间</span>
              <strong>{{ detail.trace.latestRecordedAt || '暂未补录' }}</strong>
            </div>
            <div>
              <span>录入提示</span>
              <strong>{{ detail.trace.quickEntryHint }}</strong>
            </div>
          </div>

          <div class="trace-list">
            <article v-for="item in detail.trace.recentRecords" :key="item.id" class="trace-card">
              <div class="trace-head">
                <div>
                  <p>{{ item.stageName }}</p>
                  <h3>{{ item.title }}</h3>
                </div>
                <span>{{ item.eventTime }}</span>
              </div>
              <p class="trace-meta">{{ item.location }} · {{ item.operatorName }}</p>
              <p class="trace-summary-text">{{ item.summary }}</p>
              <img v-if="item.imageUrl" class="trace-image" :src="item.imageUrl" :alt="item.title">
              <small>{{ item.visibleToConsumer ? '公开页可见' : '仅后台可见' }}</small>
            </article>
          </div>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">质检摘要区</p>
              <h2>保留高价值检测结果</h2>
            </div>
            <button class="ghost" @click="openQualityDialog">上传新质检</button>
          </div>

          <div class="quality-hero">
            <span class="quality-chip">{{ detail.quality.label }}</span>
            <p>{{ detail.quality.summary }}</p>
          </div>

          <article v-if="detail.quality.latestReport" class="quality-card">
            <div class="quality-top">
              <div>
                <p>{{ detail.quality.latestReport.resultLabel }}</p>
                <h3>{{ detail.quality.latestReport.reportNo }}</h3>
              </div>
              <span>{{ detail.quality.latestReport.reportTime }}</span>
            </div>
            <p>{{ detail.quality.latestReport.agency }}</p>
            <div class="pill-row">
              <span v-for="item in detail.quality.latestReport.highlights" :key="item">{{ item }}</span>
            </div>
          </article>

          <ul class="quality-history">
            <li v-for="item in detail.quality.reports" :key="item.id">
              <strong>{{ item.reportNo }}</strong>
              <span>{{ item.resultLabel }}</span>
              <small>{{ item.reportTime }}</small>
            </li>
          </ul>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">二维码区</p>
              <h2>给工作台和公开页稳定使用</h2>
            </div>
            <button
              class="ghost"
              :disabled="!actionOf('GENERATE_QR').enabled"
              @click="handleQrAction"
            >
              生成二维码
            </button>
          </div>

          <div class="qr-grid">
            <div>
              <span>二维码状态</span>
              <strong>{{ detail.qr.generated ? '已生成' : '待生成' }}</strong>
            </div>
            <div>
              <span>公开令牌</span>
              <strong>{{ detail.qr.token || '暂未生成' }}</strong>
            </div>
            <div>
              <span>生成时间</span>
              <strong>{{ detail.qr.generatedAt || '暂未生成' }}</strong>
            </div>
            <div>
              <span>最近扫码</span>
              <strong>{{ detail.qr.lastScanAt || '暂无' }}</strong>
            </div>
          </div>

          <a v-if="canPreviewPublic" class="preview-block" :href="detail.qr.publicUrl" target="_blank" rel="noreferrer">
            打开公开追溯页，检查扫码首屏是否 3 秒可读
          </a>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">状态留痕</p>
              <h2>记录原因、操作人和时间</h2>
            </div>
          </div>

          <ol class="status-timeline">
            <li v-for="item in detail.statusHistory" :key="`${item.status}-${item.operatedAt}`">
              <span class="status-dot" :class="statusClass(item.status)" />
              <div>
                <h3>{{ item.status }}</h3>
                <p>{{ item.reason }}</p>
                <small>{{ item.operatorName }} · {{ item.operatedAt }}</small>
              </div>
            </li>
          </ol>
        </article>
      </section>
    </template>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">批次工作台</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost icon-button" @click="closeDialog">关闭</button>
        </div>

        <div v-if="dialog.type === 'edit'" class="form-grid">
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
            <textarea v-model.trim="batchForm.publicRemark" rows="3" />
          </label>
          <label class="full-width">
            <span>内部备注</span>
            <textarea v-model.trim="batchForm.internalRemark" rows="3" />
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
            <input v-model.trim="traceForm.title" type="text">
          </label>
          <label class="full-width">
            <span>摘要说明</span>
            <textarea v-model.trim="traceForm.summary" rows="4" />
          </label>
          <label class="full-width">
            <span>现场图片链接</span>
            <input v-model.trim="traceForm.imageUrl" type="url">
          </label>
          <label class="checkbox-field full-width">
            <input v-model="traceForm.visibleToConsumer" type="checkbox">
            <span>允许公开页展示本条记录</span>
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
            <textarea v-model.trim="qualityForm.highlightsText" rows="4" />
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
            <textarea v-model.trim="statusForm.reason" rows="4" />
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
.quick-panel,
.panel,
.dialog-card,
.message-bar,
.loading-card {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(31, 55, 47, 0.08);
}

.hero-card {
  display: grid;
  grid-template-columns: 1.6fr 0.7fr;
  gap: 18px;
  padding: 24px;
}

.hero-main {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 18px;
}

.hero-image {
  width: 240px;
  height: 240px;
  object-fit: cover;
  border-radius: 28px;
  background: linear-gradient(160deg, #eef5ed, #dce9df);
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;
  color: #a67d2a;
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
  margin-bottom: 10px;
  font-size: 36px;
  color: #17362d;
}

.batch-code {
  margin-bottom: 10px;
  color: #60786d;
  font-size: 15px;
  letter-spacing: 0.04em;
}

.hero-intro {
  margin-bottom: 18px;
  color: #42594f;
  line-height: 1.8;
}

.hero-grid,
.trace-summary,
.qr-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-grid div,
.trace-summary div,
.qr-grid div {
  padding: 14px;
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.hero-grid span,
.trace-summary span,
.qr-grid span,
label span {
  display: block;
  color: #688176;
  font-size: 12px;
}

.hero-grid strong,
.trace-summary strong,
.qr-grid strong {
  display: block;
  margin-top: 8px;
  color: #17362d;
  line-height: 1.5;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(242, 204, 105, 0.2), transparent 30%),
    linear-gradient(160deg, #17362d, #24483c 78%, #edf5eb 160%);
  color: #f7f2e7;
}

.hero-side p {
  margin-bottom: 0;
  line-height: 1.7;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  font-weight: 700;
}

.status-badge.draft,
.status-dot.draft {
  background: rgba(141, 162, 152, 0.16);
  color: #5b6f67;
}

.status-badge.published,
.status-dot.published {
  background: rgba(64, 167, 117, 0.18);
  color: #e3f6ea;
}

.status-badge.frozen,
.status-dot.frozen {
  background: rgba(242, 198, 119, 0.22);
  color: #7b5308;
}

.status-badge.recalled,
.status-dot.recalled {
  background: rgba(213, 111, 100, 0.22);
  color: #8f2f29;
}

.quick-panel,
.panel,
.message-bar,
.loading-card {
  margin-top: 18px;
  padding: 24px;
}

.message-bar.info {
  color: #245846;
}

.message-bar.success {
  background: rgba(226, 244, 233, 0.94);
  color: #245846;
}

.message-bar.error {
  background: rgba(253, 236, 235, 0.94);
  color: #8f2f29;
}

.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #546b61;
}

.section-head,
.dialog-head,
.quality-top,
.trace-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.quick-actions,
.dialog-actions,
.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.trace-list,
.quality-history {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.trace-card,
.quality-card,
.preview-block {
  border-radius: 22px;
  background: rgba(245, 248, 244, 0.96);
}

.trace-card,
.quality-card {
  padding: 18px;
}

.trace-head p,
.quality-top p {
  margin-bottom: 6px;
  color: #688176;
}

.trace-head h3,
.quality-top h3 {
  margin-bottom: 0;
  color: #17362d;
}

.trace-head span,
.quality-top span,
.quality-history small {
  color: #688176;
  font-size: 13px;
}

.trace-meta,
.trace-summary-text,
.quality-card p,
.preview-block {
  color: #42594f;
  line-height: 1.7;
}

.trace-image {
  width: 100%;
  margin-top: 12px;
  border-radius: 18px;
  object-fit: cover;
  max-height: 220px;
}

.quality-hero {
  margin-top: 18px;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(160deg, rgba(36, 88, 70, 0.08), rgba(242, 204, 105, 0.12));
}

.quality-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.14);
  color: #245846;
  font-weight: 700;
}

.pill-row span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
  font-size: 13px;
}

.quality-history {
  list-style: none;
  padding: 0;
  margin-bottom: 0;
}

.quality-history li {
  display: grid;
  grid-template-columns: 1.4fr auto auto;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(36, 88, 70, 0.08);
}

.preview-block {
  display: block;
  margin-top: 16px;
  padding: 18px;
  text-decoration: none;
}

.status-timeline {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
}

.status-timeline li {
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 12px;
  padding-bottom: 16px;
}

.status-dot {
  width: 14px;
  height: 14px;
  margin-top: 4px;
  border-radius: 999px;
}

.status-timeline h3 {
  margin-bottom: 6px;
  color: #17362d;
}

.status-timeline p,
.status-timeline small {
  color: #42594f;
  line-height: 1.7;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 24, 20, 0.42);
}

.dialog-card {
  width: min(820px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 24px;
}

.icon-button {
  min-width: 72px;
}

label {
  display: block;
}

input,
select,
textarea,
button,
.preview-link {
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
.preview-link:hover,
.preview-block:hover {
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

.preview-link {
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 1080px) {
  .hero-card,
  .grid {
    grid-template-columns: 1fr;
  }

  .hero-grid,
  .trace-summary,
  .qr-grid,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .page-shell {
    padding-inline: 14px;
  }

  .hero-main {
    grid-template-columns: 1fr;
  }

  .hero-image {
    width: 100%;
    height: 240px;
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 30px;
  }

  .hero-grid,
  .trace-summary,
  .qr-grid,
  .form-grid,
  .quality-history li {
    grid-template-columns: 1fr;
  }
}
</style>
