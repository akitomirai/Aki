<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { getTraceDetail, submitTraceFeedback } from '../api/trace'

const route = useRoute()
const feedbackTypeOptions = ['信息不一致', '质量疑问', '二维码无法识别', '页面显示异常', '其他']

const detail = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const traceQrPreview = ref('')
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

const traceImageAssets = Object.freeze({
  orchard: '/images/trace/orange-orchard.jpg',
  packing: '/images/trace/orange-packing.jpg',
  lab: '/images/trace/food-lab.jpg',
  fruit: '/images/trace/orange-fruit.jpg'
})

const visibleTimeline = computed(() => {
  if (risk.value?.hasRisk) {
    return timelineItems.value.slice(0, 2)
  }
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

const routeTraceToken = computed(() => String(route.params.token || '').trim())

const traceCodeText = computed(() => {
  return detail.value?.qrToken || routeTraceToken.value || '-'
})

const traceQrContentText = computed(() => buildTracePageUrl(routeTraceToken.value))
const traceQrDisplayText = computed(() => {
  return extractTraceCodeFromQrContent(traceQrContentText.value) || traceQrContentText.value || '-'
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

const heroImageUrl = computed(() => resolveTraceImage(summary.value.productImageUrl, 'orchard'))

const heroMetrics = computed(() => [
  {
    label: '批次状态',
    value: displayValue(publicStatusText.value)
  },
  {
    label: '质检结论',
    value: displayValue(publicQualityText.value)
  },
  {
    label: '追溯节点',
    value: `${timelineItems.value.length || 0} 个`
  },
  {
    label: 'Hash 校验',
    value: displayValue(verificationStatusText.value)
  }
])

const heroStoryFacts = computed(() => compactFacts([
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
  }
]))

const traceIntroText = computed(() => {
  const origin = localizeVisibleText(summary.value.originPlace)
  const companyName = summary.value.companyName || company.value.name
  if (origin && companyName) {
    return `${displayValue(summary.value.productName)}来自${origin}，由${companyName}完成批次建档与公开追溯。`
  }
  if (origin) {
    return `${displayValue(summary.value.productName)}来自${origin}，关键流转信息已公开留痕。`
  }
  return localizeVisibleText(summary.value.slogan) || '从基地、流通到质检报告，关键节点已公开留痕。'
})

const traceBriefItems = computed(() => [
  publicQualityText.value ? `质检${publicQualityText.value}` : '',
  timelineItems.value.length ? `${timelineItems.value.length} 个节点` : '',
  verificationStatusText.value ? verificationStatusText.value : ''
].filter(Boolean))

const storyMetaItems = computed(() => compactFacts([
  {
    label: '批次',
    value: summary.value.batchCode
  },
  {
    label: '企业',
    value: summary.value.companyName || company.value.name
  },
  {
    label: '产地',
    value: localizeVisibleText(summary.value.originPlace)
  },
  {
    label: '日期',
    value: summary.value.productionDate
  }
]))

const riskFactItems = computed(() => compactFacts([
  {
    label: '产品',
    value: summary.value.productName
  },
  {
    label: '批次',
    value: summary.value.batchCode
  },
  {
    label: '企业',
    value: summary.value.companyName || company.value.name
  },
  {
    label: '质检',
    value: publicQualityText.value
  },
  {
    label: '更新时间',
    value: risk.value.updatedAt
  },
  {
    label: '追溯码',
    value: traceCodeText.value
  }
]))

const riskTipText = computed(() => {
  return localizeVisibleText(risk.value?.tip) || '请优先关注企业说明和风险处置结果，必要时可通过反馈入口补充问题线索。'
})

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
  if (/尚未发布|等待企业完成发布|未发布/i.test(message)) {
    return {
      eyebrow: '暂未发布',
      title: '追溯码暂未开放',
      copy: '企业已生成二维码，但还没有完成公开发布审核。',
      tips: []
    }
  }
  if (/不存在|未找到|无效|失效|not found|invalid/i.test(message)) {
    return {
      eyebrow: '未查到结果',
      title: '暂时无法查询',
      copy: '该追溯码未匹配到公开记录。',
      tips: []
    }
  }

  return {
    eyebrow: '页面暂不可用',
    title: '暂时无法打开',
    copy: '当前服务未返回公开信息。',
    tips: []
  }
})

onMounted(() => {
  generateTraceQrPreview(routeTraceToken.value)
  loadDetail(route.params.token)
})

watch(
  () => route.params.token,
  (token) => {
    generateTraceQrPreview(String(token || '').trim())
    showFullTimeline.value = false
    reportDialogVisible.value = false
    feedbackDialogVisible.value = false
    feedbackFormError.value = ''
    loadDetail(token)
  }
)

async function generateTraceQrPreview(token) {
  const url = buildTracePageUrl(token)
  if (!url) {
    traceQrPreview.value = ''
    return
  }

  const size = 272
  const canvas = document.createElement('canvas')
  await QRCode.toCanvas(canvas, url, {
    width: size,
    margin: 1,
    errorCorrectionLevel: 'H',
    color: {
      dark: '#1f3f68',
      light: '#ffffff'
    }
  })
  await drawQrCenterLogo(canvas, size)
  traceQrPreview.value = canvas.toDataURL('image/png')
}

function buildTracePageUrl(token) {
  const value = String(token || '').trim()
  if (!value || typeof window === 'undefined') {
    return ''
  }
  return `${window.location.origin}/t/${encodeURIComponent(value)}`
}

function extractTraceCodeFromQrContent(content) {
  const value = String(content || '').trim()
  if (!value) {
    return ''
  }
  try {
    const url = new URL(value)
    const segments = url.pathname.split('/').filter(Boolean)
    const traceIndex = segments.findIndex((segment) => segment === 't')
    const code = traceIndex >= 0 ? segments[traceIndex + 1] : ''
    return code ? decodeURIComponent(code) : ''
  } catch (error) {
    return ''
  }
}

async function drawQrCenterLogo(canvas, size) {
  const context = canvas.getContext('2d')
  if (!context) {
    return
  }
  try {
    const logo = await loadImage('/images/brand/system-icon.jpg')
    const plateSize = Math.round(size * 0.24)
    const logoSize = Math.round(size * 0.19)
    const plateX = Math.round((size - plateSize) / 2)
    const logoX = Math.round((size - logoSize) / 2)
    context.save()
    context.shadowColor = 'rgba(31, 63, 104, 0.18)'
    context.shadowBlur = 10
    context.fillStyle = '#ffffff'
    drawRoundRect(context, plateX, plateX, plateSize, plateSize, 14)
    context.fill()
    context.restore()
    context.drawImage(logo, logoX, logoX, logoSize, logoSize)
  } catch (error) {
    // The QR code remains usable if the small brand icon fails to load.
  }
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = src
  })
}

function drawRoundRect(context, x, y, width, height, radius) {
  context.beginPath()
  context.moveTo(x + radius, y)
  context.arcTo(x + width, y, x + width, y + height, radius)
  context.arcTo(x + width, y + height, x, y + height, radius)
  context.arcTo(x, y + height, x, y, radius)
  context.arcTo(x, y, x + width, y, radius)
  context.closePath()
}

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

function openFeedbackDialog(defaultType = '') {
  feedbackForm.value = createFeedbackForm(defaultType || (errorMessage.value ? '二维码无法识别' : '信息不一致'))
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
    批次: 'public-batch-code',
    主体企业: 'public-company',
    企业: 'public-company',
    产地: 'public-origin',
    日期: 'public-production-date',
    发布时间: 'public-published-at',
    质检结论: 'public-quality',
    批次状态: 'public-status'
  }[label]
}
function resolveTraceImage(url, fallbackKey = 'orchard') {
  const value = String(url || '').trim()
  if (value && /\/images\/products\/(?!orange-batch\.svg)/i.test(value)) {
    return value
  }
  if (value && !/data:image\/svg\+xml|\.svg(?:$|\?)|placeholder|default|orange/i.test(value)) {
    return value
  }
  return traceImageAssets[fallbackKey] || traceImageAssets.orchard
}

function getTimelineImage(item, index) {
  const text = `${item?.stageCode || ''} ${item?.stageName || ''} ${item?.title || ''}`.toLowerCase()
  if (/quality|qa|test|check|lab|检测|检验|质检|报告/.test(text)) {
    return resolveTraceImage(item?.imageUrl, 'lab')
  }
  if (/pack|warehouse|storehouse|包装|分拣|仓|入库|出库/.test(text)) {
    return resolveTraceImage(item?.imageUrl, 'packing')
  }
  if (/transport|delivery|market|sell|logistics|运输|配送|流通|销售|上架/.test(text)) {
    return resolveTraceImage(item?.imageUrl, 'fruit')
  }
  if (/plant|grow|farm|orchard|harvest|produce|种植|基地|果园|采收|生产/.test(text)) {
    return resolveTraceImage(item?.imageUrl, 'orchard')
  }
  return resolveTraceImage(item?.imageUrl, ['orchard', 'fruit', 'lab'][index % 3])
}
</script>

<template>
  <div class="trace-page" data-testid="public-trace-page">
    <section v-if="loading" class="state-card loading-card">
      <p class="state-eyebrow">正在查询</p>
      <h1>正在读取追溯信息</h1>
      <p>系统正在核对当前批次的状态、质检结论和关键节点，请稍候。</p>
    </section>

    <section v-else-if="errorMessage" class="exception-query-layout" data-testid="public-error-state">
      <article class="exception-query-card">
        <div class="exception-qr-shell">
          <img v-if="traceQrPreview" :src="traceQrPreview" alt="当前追溯二维码">
        </div>
        <p class="exception-qr-code-text">{{ traceQrDisplayText }}</p>
      </article>

      <article class="state-card error-card compact-state-card exception-result-card">
        <p class="state-eyebrow">{{ errorState.eyebrow }}</p>
        <h1 data-testid="public-error-title">{{ errorState.title }}</h1>
        <p data-testid="public-error-copy">{{ errorState.copy }}</p>
        <ul v-if="errorState.tips.length" class="error-list" data-testid="public-error-tips">
          <li v-for="tip in errorState.tips" :key="tip">{{ tip }}</li>
        </ul>
      </article>
    </section>

    <template v-else-if="detail">
      <section
        v-if="risk.hasRisk"
        class="exception-query-layout"
        data-testid="public-risk-banner"
      >
        <article class="exception-query-card">
          <div class="exception-qr-shell">
            <img v-if="traceQrPreview" :src="traceQrPreview" alt="当前追溯二维码">
          </div>
          <p class="exception-qr-code-text">{{ traceQrDisplayText }}</p>
        </article>

        <article class="state-card error-card compact-state-card risk-state-card exception-result-card">
          <p class="state-eyebrow">风险提示</p>
          <h1>{{ risk.statusLabel || publicStatusText }}</h1>
          <p>{{ localizeVisibleText(risk.reason) || '该批次当前存在风险提示。' }}</p>
          <div class="risk-fact-list">
            <div v-for="item in riskFactItems" :key="item.label" class="risk-fact-row">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <p class="risk-tip-text">{{ riskTipText }}</p>
          <button
            class="feedback-entry-button feedback-entry-button--state"
            type="button"
            data-testid="public-feedback-entry"
            @click="openFeedbackDialog('质量疑问')"
          >
            信息反馈
          </button>
        </article>
      </section>

      <section v-if="!risk.hasRisk" class="hero-card trace-story-hero" data-testid="public-summary">
        <div class="hero-image-panel">
          <img
            class="product-image"
            :src="heroImageUrl"
            :alt="summary.productName || '产品图片'"
          >
          <div class="hero-image-caption">
            <span>{{ displayValue(summary.originPlace || company.name) }}</span>
            <strong>{{ displayValue(publicQualityText) }}</strong>
          </div>
        </div>

        <div class="hero-info-panel">
          <div class="hero-title-row">
            <div class="hero-title-block">
              <h1 data-testid="public-product-name">{{ displayValue(summary.productName) }}</h1>
              <p class="hero-subtitle">
                {{ traceIntroText }}
              </p>
            </div>
          </div>

          <div class="trace-brief-row">
            <span v-for="item in traceBriefItems" :key="item">{{ item }}</span>
          </div>

          <div class="story-meta-list">
            <div v-for="item in storyMetaItems" :key="item.label" class="story-meta-item">
              <strong
                :data-testid="heroFactTestId(item.label)"
              >
                {{ item.value }}
              </strong>
            </div>
          </div>
        </div>
      </section>

      <section v-if="!risk.hasRisk" class="card timeline-card" data-testid="public-timeline">
        <div class="section-head">
          <div>
            <h2>{{ risk.hasRisk ? '已公开追溯节点' : '关键追溯节点时间线' }}</h2>
          </div>
          <span v-if="!risk.hasRisk">{{ timelineItems.length }} 个节点</span>
        </div>

        <article v-if="!risk.hasRisk" class="latest-event-card recent-card">
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
              <img class="timeline-image" :src="getTimelineImage(item, index)" :alt="item.title">
            </div>
          </li>
        </ol>

        <button
          v-if="!risk.hasRisk && timelineItems.length > 4"
          class="toggle-button"
          @click="showFullTimeline = !showFullTimeline"
        >
          {{ showFullTimeline ? '收起完整过程' : '展开更多追溯节点' }}
        </button>
      </section>

      <section v-if="!risk.hasRisk" class="quality-report-section">
        <article class="card quality-report-card" data-testid="public-quality-summary">
          <div class="quality-report-layout" :class="{ 'quality-report-layout--simple': risk.hasRisk }">
            <div class="quality-media">
              <img :src="traceImageAssets.lab" alt="质检场景">
              <span>检测与报告</span>
            </div>
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

              <div v-if="!risk.hasRisk && qualityProjectItems.length" class="quality-project-block">
                <span>检测项目</span>
                <div class="pill-row quality-pill-row">
                  <span v-for="item in qualityProjectItems" :key="item">{{ item }}</span>
                </div>
              </div>

              <div v-if="!risk.hasRisk" class="quality-note-block">
                <span>重点信息 / 备注</span>
                <p class="section-copy">{{ qualitySummaryText }}</p>
              </div>
            </div>

            <aside v-if="!risk.hasRisk" class="quality-report-action">
              <span>质检报告</span>
              <strong>{{ displayValue(quality.reportNo) }}</strong>
              <small>检测时间：{{ displayValue(quality.reportTime) }}</small>
              <button class="report-button" type="button" @click="openQualityReport">
                查看质检报告
              </button>
            </aside>
          </div>
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

.compact-state-card {
  min-height: 220px;
  margin-top: 34px;
  padding: 34px 26px;
}

.compact-state-card h1 {
  font-size: 32px;
  line-height: 1.25;
}

.compact-state-card p {
  max-width: 320px;
  margin: 14px auto 0;
}

.risk-state-card {
  min-height: 250px;
  margin-top: 0;
  padding: 34px 30px;
  text-align: left;
}

.risk-state-card h1 {
  color: #7d2f28;
}

.risk-state-card p {
  max-width: none;
  margin: 12px 0 0;
}

.risk-fact-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
  margin-top: 22px;
  border-top: 1px solid rgba(190, 70, 58, 0.12);
  border-bottom: 1px solid rgba(190, 70, 58, 0.12);
}

.risk-fact-row {
  min-width: 0;
  padding: 12px 0;
  border-top: 1px solid rgba(190, 70, 58, 0.08);
}

.risk-fact-row:nth-child(-n + 2) {
  border-top: 0;
}

.risk-fact-row span {
  display: block;
  color: #8b655f;
  font-size: 12px;
}

.risk-fact-row strong {
  display: block;
  margin-top: 5px;
  overflow: hidden;
  color: var(--trace-text);
  font-size: 15px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-tip-text {
  color: #6f5751;
}

.risk-state-card .feedback-entry-button {
  margin-top: 20px;
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

.exception-query-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
  margin-top: 34px;
}

.exception-query-card {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--trace-border);
  border-radius: 24px;
  background: #ffffff;
  box-shadow: var(--trace-shadow);
}

.exception-qr-shell {
  display: grid;
  place-items: center;
  min-height: 302px;
  border: 1px solid rgba(31, 63, 104, 0.12);
  border-radius: 18px;
  background: #ffffff;
}

.exception-qr-shell img {
  width: min(100%, 272px);
  aspect-ratio: 1 / 1;
  object-fit: contain;
}

.exception-qr-code-text {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid rgba(31, 63, 104, 0.12);
  border-radius: 14px;
  background: #f8fbff;
  color: var(--trace-primary-deep);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  text-align: center;
  overflow-wrap: anywhere;
}

.exception-result-card {
  margin-top: 0;
  min-height: 0;
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

.risk-summary-card {
  margin-bottom: 14px;
  padding: 18px 20px;
  border: 1px solid rgba(190, 70, 58, 0.14);
  border-radius: 22px;
  background: rgba(255, 250, 249, 0.96);
  box-shadow: var(--trace-shadow);
}

.risk-summary-card h1 {
  margin: 0;
  color: #7d2f28;
  font-size: 28px;
  line-height: 1.25;
}

.risk-summary-card p:not(.state-eyebrow) {
  margin: 10px 0 0;
  color: #6f5751;
  line-height: 1.7;
}

.risk-summary-card.warning {
  border-color: rgba(188, 127, 44, 0.18);
  background: #fff8ec;
}

.risk-summary-card.warning h1 {
  color: #8a5a12;
}

.risk-summary-card.pending {
  border-color: rgba(48, 149, 246, 0.16);
  background: #f4f9ff;
}

.risk-summary-card.pending h1 {
  color: #245f9a;
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
  .exception-query-layout,
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

  .exception-query-layout {
    margin-top: 18px;
  }

  .exception-query-card {
    max-width: 380px;
    width: 100%;
    justify-self: center;
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

  .exception-query-card {
    padding: 14px;
  }

  .exception-qr-shell {
    min-height: 268px;
  }

  .exception-qr-shell img {
    width: min(100%, 240px);
  }

  .exception-qr-code-text {
    font-size: 14px;
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
.trace-page {
  max-width: 980px;
}

.trace-story-hero {
  overflow: hidden;
  grid-template-columns: minmax(300px, 0.92fr) minmax(0, 1.08fr);
  gap: 0;
  padding: 0;
  border: 1px solid rgba(55, 128, 84, 0.12);
  background: #ffffff;
}

.trace-story-hero .hero-image-panel {
  position: relative;
  display: block;
  min-height: 100%;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: #f4f8ee;
}

.trace-story-hero .product-image {
  width: 100%;
  height: 100%;
  min-height: 360px;
  aspect-ratio: auto;
  border-radius: 0;
  object-fit: cover;
  background: #f4f8ee;
}

.hero-image-caption {
  position: absolute;
  right: 16px;
  bottom: 16px;
  left: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 18px;
  background: rgba(23, 50, 35, 0.62);
  color: #ffffff;
  backdrop-filter: blur(10px);
}

.hero-image-caption span,
.hero-image-caption strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-image-caption span {
  opacity: 0.88;
  font-size: 12px;
}

.hero-image-caption strong {
  font-size: 14px;
}

.trace-story-hero .hero-info-panel {
  justify-content: center;
  padding: 28px;
}

.trace-story-hero--risk .hero-info-panel {
  padding: 22px;
}

.trace-story-hero--risk .hero-title-block {
  padding-bottom: 12px;
}

.trace-story-hero--risk .hero-subtitle {
  display: none;
}

.trace-story-hero--risk .hero-info-grid {
  margin-top: 4px;
}

.trace-story-hero--risk .product-image {
  min-height: 300px;
}

.hero-subtitle {
  max-width: 520px;
  margin: 10px 0 0;
  color: #4c684f;
  line-height: 1.75;
}

.trace-brief-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0 18px;
}

.trace-brief-row span {
  max-width: 100%;
  padding: 7px 12px;
  border: 1px solid rgba(66, 138, 88, 0.14);
  border-radius: 999px;
  background: #f6faf3;
  color: #315b3b;
  font-size: 13px;
  line-height: 1.4;
}

.story-meta-list {
  display: grid;
  gap: 10px;
}

.story-meta-item {
  padding: 12px 0;
  border-bottom: 1px solid rgba(66, 138, 88, 0.12);
}

.story-meta-item:last-child {
  border-bottom: 0;
}

.story-meta-item strong {
  display: block;
  overflow-wrap: anywhere;
  color: var(--trace-text);
  font-size: 16px;
  line-height: 1.55;
}

.timeline {
  display: grid;
  gap: 16px;
}

.timeline-item {
  position: relative;
  display: block;
  padding: 0 0 0 22px;
}

.timeline-item::before {
  position: absolute;
  top: 18px;
  bottom: -18px;
  left: 6px;
  width: 2px;
  border-radius: 999px;
  background: #d7ead5;
  content: '';
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-marker {
  position: absolute;
  top: 18px;
  left: 0;
  z-index: 1;
  background: #36a15c;
  box-shadow: 0 0 0 5px rgba(54, 161, 92, 0.16);
}

.timeline-body {
  overflow: hidden;
  border: 1px solid rgba(66, 138, 88, 0.12);
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(25, 67, 44, 0.08);
}

.timeline-top,
.timeline-meta,
.timeline-summary {
  padding-right: 16px;
  padding-left: 16px;
}

.timeline-top {
  padding-top: 16px;
}

.timeline-summary {
  margin-bottom: 14px;
}

.timeline-image {
  display: block;
  margin-top: 0;
  border-radius: 0;
  aspect-ratio: 16 / 9;
  max-height: none;
}

.quality-report-layout {
  grid-template-columns: minmax(210px, 0.38fr) minmax(0, 1fr) minmax(210px, 0.32fr);
}

.quality-report-layout--simple {
  grid-template-columns: minmax(190px, 0.42fr) minmax(0, 1fr);
}

.quality-report-layout--simple .quality-fact-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quality-media {
  position: relative;
  overflow: hidden;
  min-height: 230px;
  border-radius: 22px;
  background: #edf6ef;
}

.quality-media img {
  width: 100%;
  height: 100%;
  min-height: 230px;
  object-fit: cover;
}

.quality-media span {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(23, 50, 35, 0.66);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 820px) {
  .trace-story-hero,
  .quality-report-layout {
    grid-template-columns: 1fr;
  }

  .trace-story-hero .product-image {
    min-height: auto;
    aspect-ratio: 4 / 3;
  }

  .trace-story-hero .hero-info-panel {
    padding: 20px;
  }
}

@media (max-width: 560px) {
  .trace-page {
    padding-inline: 10px;
  }

  .compact-state-card {
    min-height: 190px;
    margin-top: 22px;
    padding: 28px 22px;
  }

  .compact-state-card h1 {
    font-size: 28px;
  }

  .risk-summary-card {
    padding: 16px;
  }

  .risk-summary-card h1 {
    font-size: 24px;
  }

  .trace-brief-row {
    gap: 7px;
  }

  .trace-brief-row span {
    padding: 6px 10px;
    font-size: 12px;
  }

  .hero-image-caption {
    right: 10px;
    bottom: 10px;
    left: 10px;
    padding: 10px 12px;
  }

  .trace-story-hero .hero-title-row {
    gap: 10px;
  }

  .trace-story-hero .feedback-entry-button {
    width: auto;
    align-self: flex-start;
  }

  .trace-story-hero--risk .product-image {
    min-height: auto;
    aspect-ratio: 4 / 3;
  }

  .trace-story-hero--risk .hero-info-panel {
    padding: 16px;
  }

  .trace-story-hero--risk .hero-title-block h1 {
    font-size: 24px;
  }

  .trace-story-hero--risk .hero-info-grid {
    grid-template-columns: 1fr;
  }

  .trace-story-hero--risk .info-item {
    padding: 12px;
  }

  .risk-fact-list {
    grid-template-columns: 1fr;
  }

  .risk-fact-row:nth-child(2) {
    border-top: 1px solid rgba(190, 70, 58, 0.08);
  }

  .timeline-top {
    flex-direction: column;
    gap: 4px;
  }

  .quality-media {
    min-height: 190px;
  }

  .quality-media img {
    min-height: 190px;
  }
}
</style>
