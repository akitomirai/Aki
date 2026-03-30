<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  changeBatchStatus,
  createQualityReport,
  createRiskAction,
  createTraceRecord,
  generateBatchQr,
  getBatchDetail,
  uploadBatchFiles
} from '../api/batch'
import {
  createQualityForm,
  createTraceForm,
  formatStageLabel,
  qualityOptions,
  riskActionOptions,
  splitHighlightsInput,
  stageOptions
} from '../utils/traceWorkflow'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref(null)
const message = ref('')
const messageType = ref('info')
const dialog = ref({ visible: false, type: '' })

const traceForm = ref(createTraceForm())
const qualityForm = ref(createQualityForm())
const riskForm = ref(createRiskForm())
const statusForm = ref(createStatusForm())

const traceUploading = ref(false)
const qualityUploading = ref(false)

const recentRecords = computed(() => detail.value?.trace?.recentRecords ?? [])
const latestRecord = computed(() => recentRecords.value[0] ?? null)
const canHandleRisk = computed(() => ['FROZEN', 'RECALLED'].includes(detail.value?.status?.code))
const canPreviewPublic = computed(() => Boolean(detail.value?.qr?.publicUrl))
const latestRiskAction = computed(() => detail.value?.riskHandling?.history?.[0] ?? null)
const riskChecklist = computed(() => {
  if (!canHandleRisk.value) {
    return []
  }
  const history = detail.value?.riskHandling?.history ?? []
  return [
    {
      label: '处理说明',
      done: history.some((item) => String(item.actionType).toUpperCase() === 'COMMENT'),
      hint: '先说明风险原因和当前判断。'
    },
    {
      label: '整改记录',
      done: history.some((item) => String(item.actionType).toUpperCase() === 'RECTIFICATION'),
      hint: '补齐整改动作、责任人和现场情况。'
    },
    {
      label: '整改完成',
      done: history.some((item) => String(item.actionType).toUpperCase() === 'RECTIFIED'),
      hint: '需要标记已整改，恢复发布才会开放。'
    }
  ]
})

const pendingItems = computed(() => {
  if (!detail.value) return []
  const items = []
  if (!recentRecords.value.length) items.push('补录追溯')
  if (detail.value.quality?.status === 'PENDING') items.push('上传质检')
  if (!detail.value.qr?.generated) items.push('生成二维码')
  if (detail.value.status?.code !== 'PUBLISHED') items.push('发布批次')
  return items
})

const quickActions = computed(() => {
  const publishEnabled = actionOf('PUBLISH').enabled || actionOf('RESUME').enabled
  return [
    {
      key: 'trace',
      title: '补录追溯',
      desc: recentRecords.value.length ? '继续补录最新环节，现场记录会直接进入时间线。' : '先补一条关键追溯记录，让批次具备基本可追溯信息。',
      primaryText: '打开补录窗口',
      primaryClass: 'primary',
      primaryAction: openTraceDialog,
      secondaryText: '现场作业页',
      secondaryAction: openFieldEntry
    },
    {
      key: 'quality',
      title: '上传质检',
      desc: detail.value?.quality?.status === 'PENDING' ? '发布前先补质检摘要，公开页首屏会直接展示结论。' : `当前结论：${detail.value?.quality?.label || '待补充'}`,
      primaryText: '上传质检',
      primaryClass: 'primary',
      primaryAction: openQualityDialog
    },
    {
      key: 'qr',
      title: '生成二维码',
      desc: detail.value?.qr?.generated ? '二维码已可用，可直接打开公开页核对结果。' : '二维码生成后，扫码查询才能连到公开页。',
      primaryText: detail.value?.qr?.generated ? '刷新二维码' : '生成二维码',
      primaryClass: detail.value?.qr?.generated ? 'ghost' : 'primary',
      primaryAction: handleGenerateQr,
      secondaryText: canPreviewPublic.value ? '查看公开页' : '',
      secondaryAction: openPublicPreview
    },
    {
      key: 'publish',
      title: '发布',
      desc: publishEnabled ? '关键资料已满足要求，可以直接推进状态。' : (actionOf('PUBLISH').hint || actionOf('RESUME').hint || '暂不满足发布条件'),
      primaryText: actionOf('RESUME').enabled ? '恢复发布' : '发布批次',
      primaryClass: publishEnabled ? 'success' : 'ghost',
      primaryAction: () => openStatusDialog('PUBLISHED'),
      disabled: !publishEnabled
    },
    {
      key: 'risk',
      title: '风险处理',
      desc: canHandleRisk.value ? '当前批次处于风险状态，可继续补处理说明和整改记录。' : '当前无风险状态，如发现异常可在此冻结或召回。',
      primaryText: canHandleRisk.value ? '补风险处理' : '状态处理',
      primaryClass: canHandleRisk.value ? 'warning' : 'ghost',
      primaryAction: canHandleRisk.value ? () => openRiskDialog('COMMENT') : () => openStatusDialog('FROZEN')
    }
  ]
})

function createRiskForm(actionType = 'COMMENT') {
  return { actionType, reason: '', comment: '', operatorName: '监管人员' }
}

function createStatusForm(targetStatus = 'PUBLISHED') {
  return {
    targetStatus,
    reason: {
      PUBLISHED: '关键资料已齐，准备对外发布。',
      FROZEN: '发现异常，先冻结批次并进入处理。',
      RECALLED: '风险已确认，立即召回并保留公开提醒。'
    }[targetStatus] ?? '',
    operatorName: '企业管理员'
  }
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function actionOf(code) {
  return detail.value?.actions?.find((item) => item.code === code) ?? { enabled: false, hint: '' }
}

function statusClass(status) {
  return { DRAFT: 'draft', PUBLISHED: 'published', FROZEN: 'frozen', RECALLED: 'recalled' }[status] ?? 'draft'
}

function fileLabel(file) {
  return file.fileName || file.fileUrl || '已上传文件'
}

function formatFileSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function recordPreviewImages(record) {
  const attachments = Array.isArray(record?.attachments) ? record.attachments : []
  if (attachments.length) {
    return attachments.map((item, index) => ({
      id: item.id ?? `${record?.id || 'record'}-${index}`,
      fileUrl: item.fileUrl,
      fileName: item.fileName || record?.title || '现场图片'
    }))
  }
  if (record?.imageUrl) {
    return [{
      id: `cover-${record.id || 'record'}`,
      fileUrl: record.imageUrl,
      fileName: record.title || '现场图片'
    }]
  }
  return []
}

function openFieldEntry() {
  router.push({ path: '/field-entry', query: { batchId: detail.value?.batch?.id } })
}

function openPublicPreview() {
  if (!detail.value?.qr?.publicUrl) return
  window.open(detail.value.qr.publicUrl, '_blank', 'noopener')
}

function openTraceDialog() {
  traceForm.value = latestRecord.value
    ? createTraceForm({ stage: latestRecord.value.stageCode, operatorName: latestRecord.value.operatorName, location: latestRecord.value.location })
    : createTraceForm()
  dialog.value = { visible: true, type: 'trace' }
}

function openQualityDialog() {
  qualityForm.value = createQualityForm()
  dialog.value = { visible: true, type: 'quality' }
}
function openRiskDialog(actionType = 'COMMENT') {
  riskForm.value = createRiskForm(actionType)
  dialog.value = { visible: true, type: 'risk' }
}

function openStatusDialog(targetStatus = 'PUBLISHED') {
  statusForm.value = createStatusForm(targetStatus)
  dialog.value = { visible: true, type: 'status' }
}

function closeDialog() {
  dialog.value = { visible: false, type: '' }
}

async function loadDetail(id) {
  if (!id) return
  loading.value = true
  try {
    const response = await getBatchDetail(id)
    detail.value = response.data
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '批次工作台加载失败，请稍后再试。', 'error')
  } finally {
    loading.value = false
  }
}

async function submitDialog(options = {}) {
  const { keepOpen = false } = options
  try {
    let response
    if (dialog.value.type === 'trace') {
      response = await createTraceRecord(route.params.id, {
        stage: traceForm.value.stage,
        title: traceForm.value.title,
        eventTime: traceForm.value.eventTime,
        operatorName: traceForm.value.operatorName,
        location: traceForm.value.location,
        summary: traceForm.value.summary,
        imageUrl: traceForm.value.imageUrl,
        attachmentIds: traceForm.value.attachmentIds,
        visibleToConsumer: traceForm.value.visibleToConsumer
      })
      detail.value = response.data
      if (keepOpen) {
        traceForm.value = createTraceForm({ stage: traceForm.value.stage, operatorName: traceForm.value.operatorName, location: traceForm.value.location })
        showMessage('这条记录已保存，可以继续补下一条。', 'success')
        return
      }
      showMessage('追溯记录已保存。', 'success')
    } else if (dialog.value.type === 'quality') {
      response = await createQualityReport(route.params.id, {
        reportNo: qualityForm.value.reportNo,
        agency: qualityForm.value.agency,
        result: qualityForm.value.result,
        reportTime: qualityForm.value.reportTime,
        highlights: splitHighlightsInput(qualityForm.value.highlightsText),
        attachmentIds: qualityForm.value.attachmentIds
      })
      detail.value = response.data
      showMessage('质检摘要已上传。', 'success')
    } else if (dialog.value.type === 'risk') {
      response = await createRiskAction(route.params.id, riskForm.value)
      detail.value = response.data
      showMessage('风险处理已记录。', 'success')
    } else if (dialog.value.type === 'status') {
      response = await changeBatchStatus(route.params.id, statusForm.value)
      detail.value = response.data
      showMessage('批次状态已更新。', 'success')
    }
    closeDialog()
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '操作未完成，请稍后再试。', 'error')
  }
}

async function handleGenerateQr() {
  try {
    const response = await generateBatchQr(route.params.id)
    detail.value = response.data
    showMessage('二维码已生成。', 'success')
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '二维码生成失败，请稍后再试。', 'error')
  }
}

async function handleTraceFilesChange(event) {
  const files = [...(event.target.files ?? [])]
  if (!files.length) return
  traceUploading.value = true
  try {
    const response = await uploadBatchFiles('trace-image', files)
    const uploadedFiles = response.data ?? []
    traceForm.value.uploadedFiles = [...traceForm.value.uploadedFiles, ...uploadedFiles]
    traceForm.value.attachmentIds = traceForm.value.uploadedFiles.map((item) => item.id)
    if (!traceForm.value.imageUrl && uploadedFiles[0]?.fileUrl) {
      traceForm.value.imageUrl = uploadedFiles[0].fileUrl
    }
    showMessage(`现场图片已上传 ${uploadedFiles.length} 个。`, 'success')
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '现场图片上传失败。', 'error')
  } finally {
    traceUploading.value = false
    event.target.value = ''
  }
}

async function handleQualityFilesChange(event) {
  const files = [...(event.target.files ?? [])]
  if (!files.length) return
  qualityUploading.value = true
  try {
    const response = await uploadBatchFiles('quality-attachment', files)
    const uploadedFiles = response.data ?? []
    qualityForm.value.uploadedFiles = [...qualityForm.value.uploadedFiles, ...uploadedFiles]
    qualityForm.value.attachmentIds = qualityForm.value.uploadedFiles.map((item) => item.id)
    showMessage(`质检附件已上传 ${uploadedFiles.length} 个。`, 'success')
  } catch (error) {
    showMessage(error?.response?.data?.message || error?.message || '质检附件上传失败。', 'error')
  } finally {
    qualityUploading.value = false
    event.target.value = ''
  }
}

function removeTraceAttachment(fileId) {
  traceForm.value.uploadedFiles = traceForm.value.uploadedFiles.filter((item) => item.id !== fileId)
  traceForm.value.attachmentIds = traceForm.value.uploadedFiles.map((item) => item.id)
  if (traceForm.value.imageUrl && !traceForm.value.uploadedFiles.some((item) => item.fileUrl === traceForm.value.imageUrl)) {
    traceForm.value.imageUrl = traceForm.value.uploadedFiles[0]?.fileUrl ?? ''
  }
}

function removeQualityAttachment(fileId) {
  qualityForm.value.uploadedFiles = qualityForm.value.uploadedFiles.filter((item) => item.id !== fileId)
  qualityForm.value.attachmentIds = qualityForm.value.uploadedFiles.map((item) => item.id)
}

function riskActionLabel(actionType = '') {
  return { COMMENT: '处理说明', RECTIFICATION: '整改记录', PROCESSING: '处理中', RECTIFIED: '已整改' }[String(actionType).toUpperCase()] ?? actionType
}

watch(() => route.params.id, async (id) => { await loadDetail(id) })
onMounted(async () => { await loadDetail(route.params.id) })
</script>

<template>
  <div class="page-shell" data-testid="batch-workbench-page">
    <div v-if="loading" class="loading-card">正在加载批次工作台...</div>

    <template v-else-if="detail">
      <section class="manage-page-header">
        <div>
          <h1 class="manage-page-title">{{ detail.product.name }}</h1>
          <p class="workbench-meta">
            {{ detail.batch.batchCode }} · {{ detail.company.name }}
            <template v-if="detail.task?.assigneeName"> · 已分配给 {{ detail.task.assigneeName }}</template>
          </p>
        </div>
        <div class="manage-page-actions">
          <button class="ghost" @click="router.push('/batches')">返回批次列表</button>
          <button class="ghost" data-testid="workbench-field-entry-button" @click="openFieldEntry">现场作业页</button>
          <button class="ghost" :disabled="!canPreviewPublic" @click="openPublicPreview">公开页预览</button>
        </div>
      </section>

      <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

      <section class="top-grid" data-testid="workbench-top-grid">
        <article class="summary-card" data-testid="workbench-next-step-card">
          <span class="card-label">批次当前状态</span>
          <div class="card-head">
            <strong class="status-badge" :class="statusClass(detail.status.code)">{{ detail.status.label }}</strong>
            <small>{{ detail.status.changedAt }}</small>
          </div>
          <p class="card-title">{{ detail.status.currentNode }}</p>
          <p class="card-copy">{{ detail.status.reason }}</p>
        </article>

        <article class="summary-card">
          <span class="card-label">待完成事项</span>
          <ul v-if="pendingItems.length" class="mini-list">
            <li v-for="item in pendingItems" :key="item"><strong>{{ item }}</strong></li>
          </ul>
          <p v-else class="empty-copy">当前关键事项已完成，可以继续核对公开页或后续流转。</p>
        </article>

        <article class="summary-card" :class="`risk-${detail.status.code === 'RECALLED' ? 'danger' : detail.status.code === 'FROZEN' ? 'warning' : 'normal'}`">
          <span class="card-label">风险事项</span>
          <p class="card-title">{{ detail.risk.title }}</p>
          <p class="card-copy">{{ detail.risk.reason }}</p>
          <small v-if="latestRiskAction">{{ riskActionLabel(latestRiskAction.actionType) }} · {{ latestRiskAction.operatorName }}</small>
          <small v-else>{{ canHandleRisk ? '当前需要继续补齐处理记录。' : '当前没有风险处理动作。' }}</small>
        </article>

        <article class="summary-card">
          <span class="card-label">任务分配</span>
          <p class="card-title">{{ detail.task?.assigneeName || '未分配操作员' }}</p>
          <p class="card-copy">任务状态：{{ detail.task?.taskStatusLabel || '待处理' }}</p>
          <small>
            <template v-if="detail.task?.assignedAt">分配时间 {{ detail.task.assignedAt }}</template>
            <template v-else>暂未记录分配时间</template>
            <template v-if="detail.task?.todayCompleted"> · 今日已完成</template>
          </small>
        </article>

        <article class="summary-card">
          <span class="card-label">最近记录</span>
          <template v-if="latestRecord">
            <p class="card-title">{{ latestRecord.title }}</p>
            <p class="card-copy">{{ latestRecord.summary }}</p>
            <small>{{ latestRecord.eventTime }} · {{ latestRecord.operatorName }}</small>
          </template>
          <p v-else class="empty-copy">当前还没有追溯记录，建议先补第一条现场节点。</p>
        </article>
      </section>

      <section class="action-panel" data-testid="workbench-action-groups">
        <div class="section-head"><div><h2>业务操作</h2><p>把补录、质检、二维码、发布和风险处理集中到同一处。</p></div></div>
        <div class="action-grid">
          <article v-for="item in quickActions" :key="item.key" class="action-card">
            <span class="card-label">{{ item.title }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
            <div class="action-buttons">
              <button :class="item.primaryClass" :disabled="item.disabled" @click="item.primaryAction">{{ item.primaryText }}</button>
              <button v-if="item.secondaryText" class="ghost" @click="item.secondaryAction">{{ item.secondaryText }}</button>
            </div>
          </article>
        </div>
      </section>
      <section class="content-grid">
        <article class="panel" data-testid="workbench-recent-records">
          <div class="section-head">
            <div>
              <h2>最近追溯记录</h2>
              <p>优先查看最近几条现场记录，确认时间、地点、图片和说明是否完整。</p>
            </div>
            <div class="inline-actions">
              <button class="primary" @click="openTraceDialog">补录追溯</button>
            </div>
          </div>
          <div v-if="recentRecords.length" class="record-list">
            <article v-for="item in recentRecords" :key="item.id" class="record-card">
              <div class="record-head">
                <div>
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.eventTime }} · {{ formatStageLabel(item.stageCode) }}</span>
                </div>
                <span class="record-tag">{{ item.operatorName }}</span>
              </div>
              <p>{{ item.summary }}</p>
              <small>{{ item.location }}</small>
              <div v-if="recordPreviewImages(item).length" class="record-image-grid">
                <article v-for="asset in recordPreviewImages(item)" :key="asset.id" class="record-image-frame">
                  <img class="record-image" :src="asset.fileUrl" :alt="asset.fileName">
                </article>
              </div>
            </article>
          </div>
          <p v-else class="empty-copy">当前还没有追溯记录。</p>
        </article>

        <div class="side-stack">
          <article class="panel" data-testid="workbench-quality-panel">
            <div class="section-head">
              <div>
                <h2>质检</h2>
                <p>保留最关键的质检结论、机构和摘要。</p>
              </div>
              <button class="ghost" @click="openQualityDialog">上传质检</button>
            </div>
            <div class="info-grid">
              <div><span>当前状态</span><strong>{{ detail.quality.label }}</strong></div>
              <div><span>报告数量</span><strong>{{ detail.quality.reportCount }}</strong></div>
            </div>
            <p class="panel-copy">{{ detail.quality.summary }}</p>
            <div v-if="detail.quality.latestReport" class="report-card">
              <strong>{{ detail.quality.latestReport.reportNo }}</strong>
              <span>{{ detail.quality.latestReport.agency }}</span>
              <small>{{ detail.quality.latestReport.reportTime }}</small>
            </div>
          </article>

          <article class="panel" data-testid="workbench-qr-panel">
            <div class="section-head">
              <div>
                <h2>二维码与发布</h2>
                <p>二维码生成后可直接核对公开页，发布入口也在这里。</p>
              </div>
              <div class="inline-actions">
                <button class="ghost" data-testid="workbench-qr-action-0" @click="handleGenerateQr">{{ detail.qr.generated ? '刷新二维码' : '生成二维码' }}</button>
                <button class="success" :disabled="!(actionOf('PUBLISH').enabled || actionOf('RESUME').enabled)" @click="openStatusDialog('PUBLISHED')">{{ actionOf('RESUME').enabled ? '恢复发布' : '发布批次' }}</button>
              </div>
            </div>
            <div class="info-grid">
              <div><span>二维码状态</span><strong data-testid="workbench-qr-status">{{ detail.qr.generated ? '已生成' : '待生成' }}</strong></div>
              <div><span>公开访问标识</span><strong>{{ detail.qr.token || '暂无' }}</strong></div>
            </div>
            <a v-if="detail.qr.publicUrl" class="public-link" data-testid="workbench-public-preview" :href="detail.qr.publicUrl" target="_blank" rel="noreferrer">打开公开页</a>
          </article>
        </div>
      </section>

      <section class="risk-panel panel" data-testid="workbench-risk-panel">
        <div class="section-head">
          <div>
            <h2>风险处理</h2>
            <p>冻结、召回、整改和恢复发布都从这里处理。</p>
          </div>
          <div class="inline-actions" data-testid="workbench-group-status">
            <button class="ghost" :disabled="!canHandleRisk" @click="openRiskDialog('COMMENT')">补处理说明</button>
            <button class="ghost" :disabled="!canHandleRisk" @click="openRiskDialog('RECTIFICATION')">补整改记录</button>
            <button class="warning" :disabled="!canHandleRisk" @click="openRiskDialog('PROCESSING')">标记处理中</button>
            <button class="success" :disabled="!canHandleRisk" @click="openRiskDialog('RECTIFIED')">标记已整改</button>
          </div>
        </div>
        <div class="risk-grid">
          <div>
            <div class="info-grid">
              <div><span>当前风险</span><strong>{{ detail.risk.title }}</strong></div>
              <div><span>当前阶段</span><strong>{{ detail.riskHandling.currentStageLabel }}</strong></div>
              <div><span>可恢复发布</span><strong>{{ detail.riskHandling.canResume ? '可以' : '还不行' }}</strong></div>
            </div>
            <p class="panel-copy">{{ detail.risk.tip }}</p>
            <div v-if="riskChecklist.length" class="check-grid" data-testid="workbench-risk-checklist">
              <article v-for="item in riskChecklist" :key="item.label" class="check-card">
                <strong>{{ item.label }}</strong>
                <span :class="{ done: item.done }">{{ item.done ? '已完成' : '待补齐' }}</span>
                <small>{{ item.hint }}</small>
              </article>
            </div>
          </div>
          <div>
            <div v-if="detail.riskHandling.history.length" class="history-list">
              <article v-for="item in detail.riskHandling.history" :key="item.id" class="history-card">
                <strong>{{ riskActionLabel(item.actionType) }}</strong>
                <p v-if="item.reason">{{ item.reason }}</p>
                <p v-if="item.comment">{{ item.comment }}</p>
                <small>{{ item.operatorName }} · {{ item.createdAt || item.operatedAt }}</small>
              </article>
            </div>
            <p v-else class="empty-copy">当前还没有风险处理记录。</p>
          </div>
        </div>
      </section>

      <section class="panel" data-testid="workbench-status-history">
        <div class="section-head"><div><h2>状态流转</h2><p>查看批次从草稿到发布或风险处置的变化记录。</p></div></div>
        <ul class="timeline-list">
          <li v-for="item in detail.statusHistory" :key="`${item.status}-${item.operatedAt}`">
            <span class="timeline-dot" :class="statusClass(item.status)" />
            <div>
              <strong>{{ item.status }}</strong>
              <p>{{ item.reason }}</p>
              <small>{{ item.operatorName }} · {{ item.operatedAt }}</small>
            </div>
          </li>
        </ul>
      </section>

      <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
        <section class="dialog-card">
          <div class="section-head dialog-head">
            <div>
              <h2 v-if="dialog.type === 'trace'">补录追溯</h2>
              <h2 v-else-if="dialog.type === 'quality'">上传质检</h2>
              <h2 v-else-if="dialog.type === 'risk'">风险处理</h2>
              <h2 v-else-if="dialog.type === 'status'">状态处理</h2>
              <p>{{ detail.batch.batchCode }} · {{ detail.product.name }}</p>
            </div>
            <button class="ghost" @click="closeDialog">关闭</button>
          </div>
          <div v-if="dialog.type === 'trace'" class="form-grid" data-testid="workbench-trace-dialog">
            <label>
              <span>环节</span>
              <select v-model="traceForm.stage">
                <option v-for="item in stageOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label><span>时间</span><input v-model="traceForm.eventTime" type="datetime-local"></label>
            <label><span>操作人</span><input v-model.trim="traceForm.operatorName" type="text"></label>
            <label class="full-width"><span>标题</span><input v-model.trim="traceForm.title" type="text"></label>
            <label><span>地点</span><input v-model.trim="traceForm.location" type="text"></label>
            <label class="full-width"><span>说明</span><textarea v-model.trim="traceForm.summary" rows="4"></textarea></label>
            <label class="full-width">
              <span>现场图片</span>
              <div class="upload-box">
                <input type="file" accept="image/*" multiple @change="handleTraceFilesChange">
                <small>支持上传多张图片，保存记录后会自动绑定到本条追溯记录。</small>
              </div>
            </label>
            <div v-if="traceUploading" class="full-width upload-hint">正在上传现场图片...</div>
            <div v-if="traceForm.uploadedFiles.length" class="full-width uploaded-file-list">
              <article v-for="item in traceForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
                <div><strong>{{ fileLabel(item) }}</strong><small>{{ formatFileSize(item.size) }}</small></div>
                <div class="inline-actions">
                  <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看</a>
                  <button class="ghost" @click="removeTraceAttachment(item.id)">移除</button>
                </div>
              </article>
            </div>
            <label class="checkbox-field full-width"><input v-model="traceForm.visibleToConsumer" type="checkbox"><span>同步展示给消费者</span></label>
          </div>

          <div v-else-if="dialog.type === 'quality'" class="form-grid" data-testid="workbench-quality-dialog">
            <label><span>报告编号</span><input v-model.trim="qualityForm.reportNo" type="text"></label>
            <label><span>检测机构</span><input v-model.trim="qualityForm.agency" type="text"></label>
            <label>
              <span>检测结果</span>
              <select v-model="qualityForm.result">
                <option v-for="item in qualityOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label><span>检测时间</span><input v-model="qualityForm.reportTime" type="datetime-local"></label>
            <label class="full-width"><span>质检摘要</span><textarea v-model.trim="qualityForm.highlightsText" rows="4"></textarea></label>
            <label class="full-width">
              <span>质检附件</span>
              <div class="upload-box">
                <input type="file" accept=".pdf,image/png,image/jpeg,image/webp" multiple @change="handleQualityFilesChange">
                <small>支持上传 PDF 或图片，后台会保留附件备查。</small>
              </div>
            </label>
            <div v-if="qualityUploading" class="full-width upload-hint">正在上传质检附件...</div>
            <div v-if="qualityForm.uploadedFiles.length" class="full-width uploaded-file-list">
              <article v-for="item in qualityForm.uploadedFiles" :key="item.id" class="uploaded-file-item">
                <div><strong>{{ fileLabel(item) }}</strong><small>{{ formatFileSize(item.size) }}</small></div>
                <div class="inline-actions">
                  <a class="preview-link" :href="item.fileUrl" target="_blank" rel="noreferrer">查看</a>
                  <button class="ghost" @click="removeQualityAttachment(item.id)">移除</button>
                </div>
              </article>
            </div>
          </div>

          <div v-else-if="dialog.type === 'risk'" class="form-grid" data-testid="workbench-risk-dialog">
            <label>
              <span>处理类型</span>
              <select v-model="riskForm.actionType">
                <option v-for="item in riskActionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </label>
            <label><span>处理人</span><input v-model.trim="riskForm.operatorName" type="text"></label>
            <label class="full-width"><span>处理原因</span><textarea v-model.trim="riskForm.reason" rows="3"></textarea></label>
            <label class="full-width"><span>处理说明</span><textarea v-model.trim="riskForm.comment" rows="4"></textarea></label>
          </div>

          <div v-else-if="dialog.type === 'status'" class="form-grid" data-testid="workbench-status-dialog">
            <label>
              <span>目标状态</span>
              <select v-model="statusForm.targetStatus">
                <option value="PUBLISHED">发布</option>
                <option value="FROZEN">冻结</option>
                <option value="RECALLED">召回</option>
              </select>
            </label>
            <label><span>处理人</span><input v-model.trim="statusForm.operatorName" type="text"></label>
            <label class="full-width"><span>处理原因</span><textarea v-model.trim="statusForm.reason" rows="4"></textarea></label>
          </div>

          <div class="dialog-actions">
            <button class="ghost" @click="closeDialog">取消</button>
            <button v-if="dialog.type === 'trace'" class="ghost" @click="submitDialog({ keepOpen: true })">保存并继续</button>
            <button class="primary" @click="submitDialog()">确认保存</button>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-shell { max-width: 1240px; margin: 0 auto; padding: 24px 20px 44px; }
.workbench-meta { margin: 8px 0 0; color: var(--admin-text-soft); font-size: 14px; }
.message-bar,.summary-card,.action-panel,.panel,.dialog-card,.loading-card { border: 1px solid var(--admin-border); border-radius: 20px; background: var(--admin-surface); box-shadow: var(--admin-shadow); }
.message-bar,.action-panel,.panel,.loading-card { margin-top: 18px; padding: 22px; }
.message-bar.success { background: var(--admin-success-bg); color: var(--admin-success-text); }
.message-bar.error { background: rgba(253,236,235,.94); color: #8f2f29; }
.loading-card { display: flex; align-items: center; justify-content: center; min-height: 220px; color: var(--admin-text-soft); }
.top-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-top: 18px; }
.summary-card { padding: 20px; }
.card-label { display: block; color: var(--admin-text-soft); font-size: 12px; letter-spacing: .08em; text-transform: uppercase; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; }
.card-head small,.summary-card small { color: var(--admin-text-soft); font-size: 12px; }
.card-title { margin: 14px 0 8px; color: var(--admin-text); font-size: 18px; font-weight: 700; }
.card-copy,.panel-copy,.action-card p,.history-card p { margin: 0; color: #4a6b90; line-height: 1.7; }
.empty-copy { margin: 14px 0 0; color: var(--admin-text-soft); line-height: 1.7; }
.mini-list { margin: 12px 0 0; padding: 0; list-style: none; display: grid; gap: 10px; }
.mini-list li { padding: 12px 14px; border-radius: 14px; background: var(--admin-surface-soft); }
.mini-list strong { display: block; color: var(--admin-text); }
.action-panel .section-head,.panel .section-head,.dialog-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.section-head h2 { margin: 0; color: var(--admin-text); font-size: 18px; }
.section-head p { margin: 6px 0 0; color: var(--admin-text-soft); font-size: 13px; line-height: 1.7; }
.action-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-top: 16px; }
.action-card,.record-card,.report-card,.history-card,.check-card,.uploaded-file-item { padding: 16px; border: 1px solid rgba(56,134,217,.1); border-radius: 16px; background: var(--admin-surface-soft); }
.action-card h3 { margin: 10px 0 8px; color: var(--admin-text); font-size: 18px; }
.action-buttons,.inline-actions,.dialog-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.action-buttons { margin-top: 16px; }
.content-grid,.risk-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; margin-top: 18px; }
.side-stack,.record-list,.history-list,.timeline-list,.uploaded-file-list { display: grid; gap: 12px; }
.record-head { display: flex; justify-content: space-between; gap: 12px; }
.record-head strong,.report-card strong,.history-card strong,.check-card strong,.timeline-list strong { display: block; color: var(--admin-text); }
.record-head span,.report-card span,.check-card small,.history-card small,.timeline-list small,.record-card small { color: var(--admin-text-soft); font-size: 13px; }
.record-tag { display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px; border-radius: 999px; background: rgba(48,149,246,.12); color: var(--admin-primary-deep) !important; }
.record-card p { margin: 10px 0 8px; color: #4a6b90; }
.record-image-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 10px; margin-top: 12px; }
.record-image-frame { overflow: hidden; border-radius: 14px; background: rgba(255,255,255,.88); }
.record-image { width: 100%; height: 148px; display: block; object-fit: cover; }
.info-grid,.form-grid,.check-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.info-grid { margin-top: 14px; }
.info-grid div { padding: 14px; border-radius: 14px; background: var(--admin-surface-soft); }
.info-grid span,label span { display: block; margin-bottom: 8px; color: var(--admin-text-soft); font-size: 13px; }
.info-grid strong { display: block; color: var(--admin-text); }
.public-link { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; margin-top: 14px; padding: 0 16px; border-radius: 999px; background: var(--admin-primary-soft); color: var(--admin-primary-deep); }
.check-grid { margin-top: 16px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.check-card span { display: block; margin-top: 8px; color: #9a6512; font-size: 12px; font-weight: 700; }
.check-card span.done { color: var(--admin-success-text); }
.timeline-list { list-style: none; padding: 0; margin: 18px 0 0; }
.timeline-list li { display: grid; grid-template-columns: 18px 1fr; gap: 12px; }
.timeline-dot { width: 14px; height: 14px; margin-top: 6px; border-radius: 999px; }
.status-badge { display: inline-flex; align-items: center; justify-content: center; min-height: 34px; padding: 0 14px; border-radius: 999px; font-weight: 700; }
.status-badge.draft { background: rgba(248, 193, 73, 0.16); color: #946200; }
.status-badge.published { background: rgba(33, 170, 110, 0.14); color: #17784f; }
.status-badge.frozen { background: rgba(255, 132, 59, 0.16); color: #a54d12; }
.status-badge.recalled { background: rgba(224, 73, 73, 0.14); color: #a33030; }
.timeline-dot.draft { background: #f1c85a; }
.timeline-dot.published { background: #21aa6e; }
.timeline-dot.frozen { background: #ff843b; }
.timeline-dot.recalled { background: #e04949; }
.dialog-mask { position: fixed; inset: 0; z-index: 60; display: flex; align-items: center; justify-content: center; padding: 28px 18px; background: rgba(14, 28, 52, 0.38); }
.dialog-card { width: min(760px, 100%); padding: 24px; }
.dialog-card h2 { margin: 0; color: var(--admin-text); font-size: 20px; }
.dialog-card p { margin: 8px 0 0; color: var(--admin-text-soft); line-height: 1.7; }
.dialog-actions { justify-content: flex-end; margin-top: 18px; }
label { display: block; }
input,
select,
textarea {
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 14px;
  background: #fff;
  color: var(--admin-text);
}
textarea { min-height: 120px; padding: 14px; resize: vertical; }
button,
.public-link,
.preview-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease, color 0.16s ease;
}
button:hover,
.public-link:hover,
.preview-link:hover { transform: translateY(-1px); }
button:disabled { opacity: 0.55; cursor: not-allowed; transform: none; }
button.primary { background: var(--admin-primary); color: #fff; box-shadow: 0 10px 24px rgba(56, 134, 217, 0.18); }
button.success { background: #1f9f66; color: #fff; box-shadow: 0 10px 24px rgba(31, 159, 102, 0.18); }
button.warning { background: #f08b33; color: #fff; box-shadow: 0 10px 24px rgba(240, 139, 51, 0.2); }
button.ghost,
.preview-link { border-color: rgba(56, 134, 217, 0.18); background: #fff; color: var(--admin-primary-deep); }
.upload-box {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px dashed rgba(56, 134, 217, 0.24);
  border-radius: 14px;
  background: rgba(245, 250, 255, 0.9);
}
.upload-box small,
.upload-hint { color: var(--admin-text-soft); }
.uploaded-file-item { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.checkbox-field {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 12px 14px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 14px;
  background: #fff;
}
.checkbox-field input { width: 18px; min-height: 18px; margin: 0; }
.full-width { grid-column: 1 / -1; }
@media (max-width: 1100px) {
  .top-grid,
  .action-grid,
  .info-grid,
  .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .content-grid,
  .risk-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .page-shell { padding: 18px 14px 36px; }
  .top-grid,
  .action-grid,
  .info-grid,
  .form-grid,
  .check-grid { grid-template-columns: 1fr; }
  .action-panel .section-head,
  .panel .section-head,
  .record-head,
  .card-head,
  .dialog-head,
  .uploaded-file-item { flex-direction: column; align-items: flex-start; }
  .dialog-mask { padding: 16px; }
  .dialog-card { padding: 18px; }
  .dialog-actions { width: 100%; }
  .dialog-actions button { flex: 1; }
}
</style>
