<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceDetail, submitTraceFeedback } from '../api/trace'

const route = useRoute()
const feedbackTypeOptions = ['信息不一致', '质量疑问', '二维码无法识别', '页面显示异常', '其他']

const detail = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const showFullTimeline = ref(false)
const reportDialogVisible = ref(false)
const feedbackDialogVisible = ref(false)
const feedbackSubmitting = ref(false)
const feedbackForm = ref(createFeedbackForm())
const feedbackFormError = ref('')
const feedbackNotice = ref('')
const feedbackNoticeType = ref('success')
let feedbackNoticeTimer = null

const summary = computed(() => detail.value?.summary ?? {})
const company = computed(() => detail.value?.company ?? {})
const quality = computed(() => detail.value?.quality ?? {})
const risk = computed(() => detail.value?.risk ?? {})
const verification = computed(() => detail.value?.verification ?? {})
const timelineItems = computed(() => detail.value?.timeline ?? [])
const latestTimelineItem = computed(() => {
  const timeline = timelineItems.value
  return timeline.length ? timeline[timeline.length - 1] : null
})

const visibleTimeline = computed(() => {
  return showFullTimeline.value ? timelineItems.value : timelineItems.value.slice(0, 4)
})

const qualityHighlights = computed(() => quality.value.highlights ?? [])
const verificationStatusText = computed(() => {
  if (verification.value.statusLabel) {
    return verification.value.statusLabel
  }
  return verification.value.passed ? '校验通过' : '校验失败'
})

const verificationHashText = computed(() => {
  const latestHash = String(verification.value.latestHash || '').trim()
  if (!latestHash) {
    return '待生成'
  }
  return `${latestHash.slice(0, 16)}...${latestHash.slice(-12)}`
})

const verificationFacts = computed(() => [
  {
    label: '链上记录数',
    value: `${Number(verification.value.totalRecords || 0)} 条`
  },
  {
    label: '最近校验',
    value: verification.value.checkedAt || '刚刚完成'
  },
  {
    label: '链摘要',
    value: verificationHashText.value,
    fullValue: verification.value.latestHash || ''
  }
])

const publicStatusText = computed(() => summary.value.statusLabel || '状态待确认')
const publicQualityText = computed(() => quality.value.resultLabel || summary.value.qualityResult || '待补质检')
const publicPublishedAtText = computed(() => {
  const publishedAt = summary.value.publishedAt
  if (publishedAt) {
    return publishedAt
  }
  if (publicStatusText.value === '草稿') {
    return '-'
  }
  return '-'
})

const traceCodeText = computed(() => {
  return detail.value?.qrToken || String(route.params.token || '').trim() || '-'
})

const feedbackProductName = computed(() => feedbackDisplayValue(summary.value.productName))
const feedbackBatchNo = computed(() => feedbackDisplayValue(summary.value.batchCode))
const feedbackTraceCode = computed(() => feedbackDisplayValue(traceCodeText.value))

const feedbackReadonlyItems = computed(() => [
  {
    label: '当前产品',
    value: feedbackProductName.value
  },
  {
    label: '批次编号',
    value: feedbackBatchNo.value
  },
  {
    label: '追溯码',
    value: feedbackTraceCode.value
  }
])

const heroFacts = computed(() => compactFacts([
  {
    label: '产品名称',
    value: summary.value.productName
  },
  {
    label: '批次编号',
    value: summary.value.batchCode
  },
  {
    label: '主体企业',
    value: summary.value.companyName || company.value.name
  },
  {
    label: '产地',
    value: localizeVisibleText(summary.value.originPlace)
  },
  {
    label: '生产日期',
    value: summary.value.productionDate
  },
  {
    label: '发布时间',
    value: publicPublishedAtText.value
  },
  {
    label: '质检结论',
    value: publicQualityText.value
  },
  {
    label: '批次状态',
    value: publicStatusText.value
  },
  {
    label: '追溯码',
    value: traceCodeText.value
  },
  {
    label: '最近质检时间',
    value: quality.value.reportTime
  }
]))

const qualitySummaryText = computed(() => {
  return displayValue(localizeVisibleText(quality.value.summary))
})

const qualityProjectItems = computed(() => {
  const rawItems = qualityHighlights.value
  const items = Array.isArray(rawItems)
    ? rawItems
    : String(rawItems || '').split(/[，,、]/)
  return items.map((item) => localizeVisibleText(item)).filter(Boolean)
})

const qualityReportFacts = computed(() => [
  {
    label: '检测结论',
    value: displayValue(publicQualityText.value)
  },
  {
    label: '检测机构',
    value: displayValue(quality.value.agency)
  },
  {
    label: '报告编号',
    value: displayValue(quality.value.reportNo)
  },
  {
    label: '检测时间',
    value: displayValue(quality.value.reportTime)
  }
])

const qualityReportDetailFacts = computed(() => [
  {
    label: '报告编号',
    value: displayValue(quality.value.reportNo)
  },
  {
    label: '检测机构',
    value: displayValue(quality.value.agency)
  },
  {
    label: '检测结论',
    value: displayValue(publicQualityText.value)
  },
  {
    label: '检测时间',
    value: displayValue(quality.value.reportTime)
  },
  {
    label: '关联批次编号',
    value: displayValue(summary.value.batchCode)
  },
  {
    label: '产品名称',
    value: displayValue(summary.value.productName)
  }
])

const qualityReportFileUrl = computed(() => {
  return String(
    quality.value.reportUrl || quality.value.attachmentUrl || quality.value.fileUrl || ''
  ).trim()
})

const errorState = computed(() => {
  const message = String(errorMessage.value || '')
  if (/不存在|未找到|无效|失效|not found|invalid/i.test(message)) {
    return {
      title: '这个追溯码暂时无法查询',
      copy: '可能是追溯码输入有误、二维码已失效，或该批次当前没有开放公开查询。',
      tips: [
        '请核对二维码是否完整，或重新扫码一次。',
        '如果页面来自旧截图或旧海报，请以最新二维码为准。',
        '若仍无法查询，可联系销售方或企业客服核实。'
      ]
    }
  }

  return {
    title: '追溯信息加载失败',
    copy: '当前网络或服务状态异常，系统暂时没能返回这批产品的公开信息。',
    tips: [
      '请稍后刷新后再次尝试。',
      '如页面持续异常，建议更换网络环境后重试。',
      '若这是答辩演示环境，请回到后台确认服务是否正常运行。'
    ]
  }
})

onMounted(() => {
  loadDetail(route.params.token)
})

watch(
  () => route.params.token,
  (token) => {
    showFullTimeline.value = false
    reportDialogVisible.value = false
    feedbackDialogVisible.value = false
    feedbackFormError.value = ''
    loadDetail(token)
  }
)

async function loadDetail(token) {
  if (!token) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getTraceDetail(token)
    detail.value = response.data
  } catch (error) {
    detail.value = null
    errorMessage.value = error?.response?.data?.message || error?.message || '追溯页加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function riskClass(riskInfo) {
  return {
    pending: 'pending',
    warning: 'warning',
    danger: 'danger'
  }[riskInfo?.riskLevel] ?? 'warning'
}

function verificationClass(info) {
  if (!Number(info?.totalRecords || 0)) {
    return 'pending'
  }
  return info?.passed ? 'pass' : 'fail'
}

function openQualityReport() {
  reportDialogVisible.value = true
}

function closeQualityReport() {
  reportDialogVisible.value = false
}

function createFeedbackForm(defaultType = '信息不一致') {
  return {
    feedbackType: defaultType,
    contact: '',
    content: ''
  }
}

function openFeedbackDialog() {
  feedbackForm.value = createFeedbackForm(errorMessage.value ? '二维码无法识别' : '信息不一致')
  feedbackFormError.value = ''
  feedbackDialogVisible.value = true
}

function closeFeedbackDialog() {
  feedbackDialogVisible.value = false
  feedbackSubmitting.value = false
  feedbackFormError.value = ''
  feedbackForm.value = createFeedbackForm()
}

async function submitFeedback() {
  const content = String(feedbackForm.value.content || '').trim()
  if (!content) {
    feedbackFormError.value = '请输入反馈内容'
    return
  }
  if (content.length < 10) {
    feedbackFormError.value = '反馈内容不能少于 10 个字'
    return
  }
  if (content.length > 300) {
    feedbackFormError.value = '反馈内容不能超过 300 个字'
    return
  }

  feedbackSubmitting.value = true
  feedbackFormError.value = ''
  try {
    const response = await submitTraceFeedback({
      productName: feedbackProductName.value,
      batchNo: feedbackBatchNo.value,
      traceCode: feedbackTraceCode.value,
      feedbackType: feedbackForm.value.feedbackType,
      contact: String(feedbackForm.value.contact || '').trim(),
      content,
      createdAt: new Date().toISOString()
    })
    if (response?.success === false) {
      throw new Error(response.message || 'feedback rejected')
    }
    closeFeedbackDialog()
    showFeedbackNotice('反馈已提交，感谢您的反馈。', 'success')
  } catch (error) {
    showFeedbackNotice('反馈暂时无法提交，请稍后再试。', 'error')
  } finally {
    feedbackSubmitting.value = false
  }
}

function showFeedbackNotice(message, type = 'success') {
  if (feedbackNoticeTimer) {
    clearTimeout(feedbackNoticeTimer)
  }
  feedbackNotice.value = message
  feedbackNoticeType.value = type
  feedbackNoticeTimer = window.setTimeout(() => {
    feedbackNotice.value = ''
    feedbackNoticeTimer = null
  }, 8000)
}

function openReportFile() {
  const url = qualityReportFileUrl.value
  if (!url) {
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

function localizeVisibleText(text) {
  const value = String(text || '').trim()
  if (!value) {
    return ''
  }
  return {
    'Xinfeng Orchard Base': '江西省赣州市信丰果园基地',
    'Wuyuan Tea Base': '江西省上饶市婺源县茶园基地',
    'Public trace page is available for this batch.': '当前批次已开放公开查询。',
    'Used to verify released-batch linkage with the workbench.': '用于核对已发布批次与工作台、公开页的联动状态。',
    'The batch has been created and still needs field records, QA and QR data.': '当前批次已建档，仍需补录现场记录、质检和二维码。',
    'Used for continuous field-entry verification before publish.': '用于发布前连续补录现场作业与工作台联动验证。',
    'The batch is paused and waiting for follow-up handling.': '当前批次已暂停流通，等待后续风险处理。',
    'Used to review frozen-batch rectification flow.': '用于核对冻结批次的整改处理流程。',
    'Latest QA failed and the batch is waiting for recheck.': '最近一次质检未通过，当前批次等待复检。'
  }[value] ?? value
}

function shortSummary(text, length = 72) {
  const value = String(text || '').trim()
  if (!value) {
    return '该节点已留痕。'
  }
  return value.length > length ? `${value.slice(0, length)}...` : value
}

function displayValue(value) {
  const text = String(value ?? '').trim()
  return text && !['null', 'undefined'].includes(text.toLowerCase()) ? text : '-'
}

function feedbackDisplayValue(value) {
  const text = displayValue(value)
  return text === '-' ? '未识别' : text
}

function compactFacts(items) {
  return items
    .map((item) => ({
      ...item,
      value: displayValue(item.value)
    }))
    .filter((item) => item.value !== '-')
}

function heroFactTestId(label) {
  return {
    批次编号: 'public-batch-code',
    主体企业: 'public-company',
    产地: 'public-origin',
    发布时间: 'public-published-at',
    质检结论: 'public-quality',
    批次状态: 'public-status'
  }[label]
}
</script>

<template>
  <div class="trace-page" data-testid="public-trace-page">
    <section v-if="loading" class="state-card loading-card">
      <p class="state-eyebrow">正在查询</p>
      <h1>正在读取追溯信息</h1>
      <p>系统正在核对当前批次的状态、质检结论和关键节点，请稍候。</p>
    </section>

    <section v-else-if="errorMessage" class="state-card error-card" data-testid="public-error-state">
      <p class="state-eyebrow">查询异常</p>
      <h1 data-testid="public-error-title">{{ errorState.title }}</h1>
      <p data-testid="public-error-copy">{{ errorState.copy }}</p>
      <ul class="error-list" data-testid="public-error-tips">
        <li v-for="tip in errorState.tips" :key="tip">{{ tip }}</li>
      </ul>
      <button
        class="feedback-entry-button feedback-entry-button--state"
        type="button"
        data-testid="public-feedback-entry"
        @click="openFeedbackDialog"
      >
        信息反馈
      </button>
    </section>

    <template v-else-if="detail">
      <section
        v-if="risk.hasRisk"
        class="risk-banner"
        data-testid="public-risk-banner"
        :class="riskClass(risk)"
      >
        <div>
          <p class="state-eyebrow">风险提示</p>
          <h2>{{ risk.statusLabel }}</h2>
          <p>{{ localizeVisibleText(risk.reason) }}</p>
        </div>
        <div class="risk-meta">
          <span>当前阶段：{{ risk.statusLabel }}</span>
          <span>最近更新：{{ risk.updatedAt || '暂无记录' }}</span>
        </div>
        <small>{{ localizeVisibleText(risk.tip) || '请优先关注企业说明和风险处置结果。' }}</small>
      </section>

      <section class="hero-card" data-testid="public-summary">
        <div class="hero-image-panel">
          <img
            class="product-image"
            :src="summary.productImageUrl"
            :alt="summary.productName || '产品图片'"
          >
        </div>

        <div class="hero-info-panel">
          <div class="hero-title-row">
            <div class="hero-title-block">
              <p class="eyebrow">公开追溯信息</p>
              <h1 data-testid="public-product-name">{{ displayValue(summary.productName) }}</h1>
            </div>
            <button
              class="feedback-entry-button"
              type="button"
              data-testid="public-feedback-entry"
              @click="openFeedbackDialog"
            >
              信息反馈
            </button>
          </div>

          <div class="hero-info-grid">
            <div v-for="item in heroFacts" :key="item.label" class="info-item">
              <span>{{ item.label }}</span>
              <strong
                :data-testid="heroFactTestId(item.label)"
              >
                {{ item.value }}
              </strong>
            </div>
          </div>
        </div>
      </section>

      <section class="card timeline-card" data-testid="public-timeline">
        <div class="section-head">
          <div>
            <h2>关键追溯节点时间线</h2>
          </div>
          <span>{{ timelineItems.length }} 个节点</span>
        </div>

        <article class="latest-event-card recent-card">
          <span>最近动态</span>
          <strong data-testid="public-latest-record">{{ latestTimelineItem?.title || '暂无追溯记录' }}</strong>
          <p>{{ latestTimelineItem?.time || '暂无时间' }} · {{ localizeVisibleText(latestTimelineItem?.location) || '地点待补充' }}</p>
          <small>{{ shortSummary(latestTimelineItem?.summary) }}</small>
        </article>

        <ol class="timeline">
          <li
            v-for="(item, index) in visibleTimeline"
            :key="`${item.stageCode}-${item.time}`"
            class="timeline-item"
            :data-testid="`public-timeline-item-${index}`"
          >
            <div class="timeline-marker" />
            <div class="timeline-body">
              <div class="timeline-top">
                <div>
                  <p class="timeline-stage">{{ item.stageName }}</p>
                  <h3>{{ item.title }}</h3>
                </div>
                <span>{{ item.time }}</span>
              </div>
              <p class="timeline-meta">{{ localizeVisibleText(item.location) || '地点已留痕' }}</p>
              <p class="timeline-summary">{{ shortSummary(item.summary) }}</p>
              <img v-if="item.imageUrl" class="timeline-image" :src="item.imageUrl" :alt="item.title">
            </div>
          </li>
        </ol>

        <button
          v-if="timelineItems.length > 4"
          class="toggle-button"
          @click="showFullTimeline = !showFullTimeline"
        >
          {{ showFullTimeline ? '收起完整过程' : '展开更多追溯节点' }}
        </button>
      </section>

      <section class="quality-report-section">
        <article class="card quality-report-card" data-testid="public-quality-summary">
          <div class="quality-report-layout">
            <div class="quality-report-info">
              <div class="section-head">
                <div>
                  <h2>质检摘要</h2>
                </div>
              </div>

              <div class="quality-fact-grid">
                <div v-for="item in qualityReportFacts" :key="item.label" class="fact-card">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>

              <div v-if="qualityProjectItems.length" class="quality-project-block">
                <span>检测项目</span>
                <div class="pill-row quality-pill-row">
                  <span v-for="item in qualityProjectItems" :key="item">{{ item }}</span>
                </div>
              </div>

              <div class="quality-note-block">
                <span>重点信息 / 备注</span>
                <p class="section-copy">{{ qualitySummaryText }}</p>
              </div>
            </div>

            <aside class="quality-report-action">
              <span>质检报告</span>
              <strong>{{ displayValue(quality.reportNo) }}</strong>
              <small>检测时间：{{ displayValue(quality.reportTime) }}</small>
              <button class="report-button" type="button" @click="openQualityReport">
                查看质检报告
              </button>
            </aside>
          </div>
        </article>

        <article v-if="risk.hasRisk" class="card risk-detail-card">
          <div class="section-head">
            <div>
              <h2>风险提示</h2>
            </div>
          </div>

          <div class="fact-grid">
            <div class="fact-card">
              <span>当前状态</span>
              <strong>{{ risk.statusLabel }}</strong>
            </div>
            <div class="fact-card">
              <span>更新时间</span>
              <strong>{{ risk.updatedAt || '未记录' }}</strong>
            </div>
          </div>

          <p class="section-copy">{{ localizeVisibleText(risk.reason) }}</p>
          <p v-if="risk.tip" class="address-copy">{{ localizeVisibleText(risk.tip) }}</p>
        </article>
      </section>

      <div
        v-if="reportDialogVisible"
        class="report-dialog-mask"
        data-testid="quality-report-dialog"
        @click.self="closeQualityReport"
      >
        <section
          class="report-dialog-card"
          role="dialog"
          aria-modal="true"
          aria-labelledby="quality-report-title"
        >
          <div class="report-dialog-head">
            <h2 id="quality-report-title">质检报告详情</h2>
            <button class="dialog-close-button" type="button" @click="closeQualityReport">
              关闭
            </button>
          </div>

          <div class="report-detail-grid">
            <div
              v-for="item in qualityReportDetailFacts"
              :key="item.label"
              class="fact-card fact-card--compact"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <div v-if="qualityProjectItems.length" class="report-section">
            <span>检测项目</span>
            <div class="pill-row quality-pill-row">
              <span v-for="item in qualityProjectItems" :key="item">{{ item }}</span>
            </div>
          </div>

          <div class="report-section">
            <span>重点信息 / 备注</span>
            <p>{{ qualitySummaryText }}</p>
          </div>

          <div class="report-dialog-actions">
            <button
              v-if="qualityReportFileUrl"
              class="report-button report-button--ghost"
              type="button"
              @click="openReportFile"
            >
              打开报告文件
            </button>
            <button class="report-button" type="button" @click="closeQualityReport">
              关闭
            </button>
          </div>
        </section>
      </div>
    </template>

    <div
      v-if="feedbackNotice"
      class="feedback-toast"
      :class="`feedback-toast--${feedbackNoticeType}`"
      data-testid="public-feedback-toast"
    >
      {{ feedbackNotice }}
    </div>

    <div
      v-if="feedbackDialogVisible"
      class="feedback-dialog-mask"
      data-testid="public-feedback-dialog"
      @click.self="closeFeedbackDialog"
    >
      <section
        class="feedback-dialog-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="feedback-dialog-title"
      >
        <div class="feedback-dialog-head">
          <h2 id="feedback-dialog-title">溯源信息反馈</h2>
          <button class="dialog-close-button" type="button" @click="closeFeedbackDialog">
            关闭
          </button>
        </div>

        <form class="feedback-form" @submit.prevent="submitFeedback">
          <div class="feedback-readonly-grid">
            <label
              v-for="item in feedbackReadonlyItems"
              :key="item.label"
              class="feedback-field"
            >
              <span>{{ item.label }}</span>
              <input :value="item.value" readonly>
            </label>
          </div>

          <label class="feedback-field">
            <span>反馈类型</span>
            <select v-model="feedbackForm.feedbackType" data-testid="public-feedback-type">
              <option v-for="item in feedbackTypeOptions" :key="item" :value="item">
                {{ item }}
              </option>
            </select>
          </label>

          <label class="feedback-field">
            <span>联系方式</span>
            <input v-model="feedbackForm.contact" placeholder="选填" maxlength="60">
          </label>

          <label class="feedback-field feedback-field--full">
            <span>反馈内容</span>
            <textarea
              v-model="feedbackForm.content"
              rows="5"
              maxlength="300"
              data-testid="public-feedback-content"
              @input="feedbackFormError = ''"
            />
          </label>
          <p v-if="feedbackFormError" class="feedback-form-error">{{ feedbackFormError }}</p>

          <div class="feedback-dialog-actions">
            <button class="report-button report-button--ghost" type="button" @click="closeFeedbackDialog">
              关闭
            </button>
            <button
              class="report-button"
              type="submit"
              data-testid="public-feedback-submit"
              :disabled="feedbackSubmitting"
            >
              {{ feedbackSubmitting ? '提交中' : '提交反馈' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.trace-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 18px 14px 40px;
}

.state-card,
.risk-banner,
.hero-card,
.card,
.latest-event-card,
.fact-card,
.info-item {
  border-radius: 24px;
  box-shadow: var(--trace-shadow);
}

.state-card,
.hero-card,
.card,
.latest-event-card,
.fact-card,
.info-item {
  background: var(--trace-surface);
}

.state-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 240px;
  padding: 28px 24px;
  text-align: center;
}

.state-card h1,
.card h2,
.timeline-body h3,
.hero-title-block h1,
.risk-banner h2,
.latest-event-card strong {
  margin: 0;
  color: var(--trace-text);
}

.state-card p,
.risk-banner p,
.section-copy,
.timeline-meta,
.timeline-summary,
.latest-event-card p,
.latest-event-card small,
.address-copy,
.section-head p {
  color: #4f6e8f;
  line-height: 1.7;
}

.state-eyebrow,
.eyebrow {
  margin: 0 0 8px;
  color: var(--trace-text-soft);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.state-eyebrow--muted {
  color: #6b86a4;
}

.error-card {
  border: 1px solid rgba(190, 70, 58, 0.16);
  background: rgba(255, 250, 249, 0.98);
}

.error-list {
  margin: 16px auto 0;
  padding-left: 20px;
  max-width: 520px;
  text-align: left;
  color: #6a4a45;
  line-height: 1.8;
}

.risk-banner {
  margin-bottom: 16px;
  padding: 20px;
}

.risk-banner.warning {
  background: #fff4dd;
  color: #8a5a12;
}

.risk-banner.pending {
  background: #eef6ff;
  color: #2d6eb2;
}

.risk-banner.danger {
  background: #fdeceb;
  color: #a0342c;
}

.risk-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 12px 0 8px;
  font-size: 13px;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(220px, 0.72fr) minmax(0, 1.28fr);
  gap: 22px;
  align-items: stretch;
  padding: 22px;
}

.hero-image-panel {
  display: grid;
  place-items: center;
  min-height: 278px;
  padding: 18px;
  border: 1px solid var(--trace-border);
  border-radius: 22px;
  background: var(--trace-surface-soft);
}

.product-image {
  width: min(100%, 260px);
  aspect-ratio: 1 / 1;
  border-radius: 20px;
  object-fit: cover;
  background: linear-gradient(160deg, #eef7ff, #dbeeff);
}

.hero-info-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.hero-title-block {
  min-width: 0;
  padding: 2px 0 14px;
}

.hero-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.hero-title-block h1 {
  font-size: 30px;
  line-height: 1.28;
}

.hero-info-grid,
.fact-grid,
.trust-list {
  display: grid;
  gap: 12px;
}

.hero-info-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-content: start;
}

.fact-card,
.latest-event-card,
.info-item {
  padding: 16px;
  border: 1px solid var(--trace-border);
  background: var(--trace-surface-soft);
}

.fact-card span,
.info-item span,
.latest-event-card span,
.section-head span,
.timeline-stage,
.timeline-top span {
  display: block;
  color: var(--trace-text-soft);
  font-size: 12px;
}

.fact-card strong,
.info-item strong {
  display: block;
  margin-top: 8px;
  color: var(--trace-text);
  line-height: 1.5;
}

.info-item strong {
  overflow-wrap: anywhere;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.quality-report-section {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.card {
  padding: 20px;
}

.verification-card {
  margin-top: 14px;
}

.section-head,
.timeline-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.fact-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.quality-report-card {
  width: 100%;
}

.quality-report-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.34fr);
  gap: 18px;
  align-items: stretch;
}

.quality-report-info {
  min-width: 0;
}

.quality-fact-grid,
.report-detail-grid {
  display: grid;
  gap: 12px;
}

.quality-fact-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 16px;
}

.quality-project-block,
.quality-note-block,
.report-section {
  margin-top: 16px;
}

.quality-project-block > span,
.quality-note-block > span,
.report-section > span,
.quality-report-action > span {
  display: block;
  color: var(--trace-text-soft);
  font-size: 12px;
}

.quality-note-block .section-copy,
.report-section p {
  margin: 8px 0 0;
}

.quality-pill-row {
  margin-top: 8px;
}

.quality-report-action {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--trace-border);
  border-radius: 22px;
  background: var(--trace-surface-soft);
}

.quality-report-action strong {
  margin-top: 8px;
  color: var(--trace-text);
  line-height: 1.5;
  word-break: break-word;
}

.quality-report-action small {
  margin-top: 6px;
  color: #4f6e8f;
  line-height: 1.6;
}

.report-button,
.dialog-close-button {
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: var(--trace-primary);
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}

.feedback-entry-button {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid var(--trace-border);
  border-radius: 999px;
  background: #ffffff;
  color: var(--trace-primary-deep);
  font-weight: 700;
  cursor: pointer;
}

.feedback-entry-button--state {
  margin: 20px auto 0;
}

.report-button {
  margin-top: 16px;
  width: fit-content;
}

.dialog-close-button,
.report-button--ghost {
  background: #ffffff;
  color: var(--trace-primary-deep);
  border-color: var(--trace-border);
}

.report-dialog-mask,
.feedback-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 38, 64, 0.36);
}

.feedback-dialog-mask {
  z-index: 34;
}

.report-dialog-card {
  width: min(720px, calc(100vw - 32px));
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 22px;
  border: 1px solid var(--trace-border);
  border-radius: 24px;
  background: var(--trace-surface);
  box-shadow: 0 24px 80px rgba(15, 38, 64, 0.22);
}

.feedback-dialog-card {
  width: min(620px, calc(100vw - 32px));
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 22px;
  border: 1px solid var(--trace-border);
  border-radius: 24px;
  background: var(--trace-surface);
  box-shadow: 0 24px 80px rgba(15, 38, 64, 0.22);
}

.report-dialog-head,
.report-dialog-actions,
.feedback-dialog-head,
.feedback-dialog-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-dialog-head h2,
.feedback-dialog-head h2 {
  margin: 0;
  color: var(--trace-text);
}

.feedback-form {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.feedback-readonly-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feedback-field {
  display: grid;
  gap: 8px;
}

.feedback-field span {
  color: var(--trace-text-soft);
  font-size: 12px;
}

.feedback-field input,
.feedback-field select,
.feedback-field textarea {
  width: 100%;
  border: 1px solid var(--trace-border);
  border-radius: 16px;
  background: var(--trace-surface-soft);
  color: var(--trace-text);
  outline: none;
}

.feedback-field input,
.feedback-field select {
  min-height: 44px;
  padding: 0 14px;
}

.feedback-field textarea {
  min-height: 118px;
  padding: 12px 14px;
  resize: vertical;
}

.feedback-field input[readonly] {
  color: #214c7c;
  font-weight: 700;
}

.feedback-field--full {
  grid-column: 1 / -1;
}

.feedback-form-error {
  margin: -4px 0 0;
  color: #be463a;
  font-size: 13px;
}

.feedback-dialog-actions {
  justify-content: flex-end;
}

.feedback-dialog-actions .report-button {
  margin-top: 0;
}

.feedback-dialog-actions .report-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.feedback-toast {
  position: fixed;
  top: 18px;
  left: 50%;
  z-index: 42;
  transform: translateX(-50%);
  width: min(420px, calc(100vw - 32px));
  padding: 13px 18px;
  border: 1px solid rgba(48, 149, 246, 0.2);
  border-radius: 999px;
  background: #ffffff;
  color: var(--trace-primary-deep);
  text-align: center;
  font-weight: 700;
  box-shadow: 0 16px 48px rgba(15, 38, 64, 0.16);
}

.feedback-toast--error {
  border-color: rgba(190, 70, 58, 0.2);
  color: #a0342c;
}

.report-detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.report-section p {
  color: #4f6e8f;
  line-height: 1.7;
}

.report-dialog-actions {
  justify-content: flex-end;
  margin-top: 18px;
}

.report-dialog-actions .report-button {
  margin-top: 0;
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.pill-row span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(48, 149, 246, 0.12);
  color: var(--trace-primary-deep);
  font-size: 13px;
}

.address-copy {
  margin: 16px 0 0;
}

.verification-layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.88fr) minmax(0, 1.12fr);
  gap: 14px;
  margin-top: 16px;
}

.verification-status {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--trace-border);
  border-radius: 22px;
  background: var(--trace-surface-soft);
}

.verification-status span {
  color: var(--trace-text-soft);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.verification-status strong {
  color: var(--trace-text);
  font-size: 30px;
  line-height: 1.08;
}

.verification-status p,
.verification-note {
  margin: 0;
  color: #4f6e8f;
  line-height: 1.7;
}

.verification-status.pass {
  background: linear-gradient(180deg, rgba(232, 247, 237, 0.98), rgba(244, 251, 246, 0.98));
  border-color: rgba(73, 166, 111, 0.18);
}

.verification-status.pass strong {
  color: #23784b;
}

.verification-status.fail {
  background: linear-gradient(180deg, rgba(253, 236, 235, 0.98), rgba(255, 247, 246, 0.98));
  border-color: rgba(190, 70, 58, 0.16);
}

.verification-status.fail strong {
  color: #a0342c;
}

.verification-status.pending {
  background: linear-gradient(180deg, rgba(238, 246, 255, 0.98), rgba(248, 252, 255, 0.98));
  border-color: rgba(48, 149, 246, 0.16);
}

.verification-status.pending strong {
  color: var(--trace-primary-deep);
}

.verification-facts {
  margin-top: 0;
}

.fact-card--compact strong {
  font-size: 16px;
}

.hash-text {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 14px;
  word-break: break-all;
}

.verification-note {
  margin-top: 14px;
}

.timeline-card {
  margin-top: 14px;
}

.latest-event-card {
  margin-top: 16px;
}

.timeline {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
}

.timeline-item {
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 12px;
  padding-bottom: 16px;
}

.timeline-marker {
  width: 14px;
  height: 14px;
  margin-top: 5px;
  border-radius: 999px;
  background: var(--trace-primary);
  box-shadow: 0 0 0 4px rgba(48, 149, 246, 0.14);
}

.timeline-image {
  width: 100%;
  margin-top: 12px;
  border-radius: 18px;
  object-fit: cover;
  max-height: 220px;
}

.toggle-button {
  margin-top: 8px;
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid var(--trace-border);
  border-radius: 999px;
  background: #ffffff;
  color: var(--trace-primary-deep);
}

@media (max-width: 820px) {
  .hero-card,
  .detail-grid,
  .quality-report-layout,
  .verification-layout {
    grid-template-columns: 1fr;
  }

  .hero-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feedback-readonly-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .trace-page {
    padding-inline: 12px;
  }

  .hero-info-grid,
  .fact-grid,
  .quality-fact-grid,
  .report-detail-grid {
    grid-template-columns: 1fr;
  }

  .quality-report-action,
  .report-dialog-actions,
  .feedback-dialog-actions {
    align-items: stretch;
  }

  .report-button,
  .dialog-close-button {
    width: 100%;
  }

  .report-dialog-head,
  .report-dialog-actions,
  .feedback-dialog-head,
  .feedback-dialog-actions,
  .hero-title-row {
    flex-direction: column;
  }

  .feedback-entry-button {
    width: 100%;
  }

  .product-image {
    width: min(100%, 220px);
  }

  .hero-title-block h1 {
    font-size: 26px;
  }
}
</style>
