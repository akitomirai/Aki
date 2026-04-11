<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceDetail } from '../api/trace'

const route = useRoute()

const detail = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const showFullTimeline = ref(false)

const summary = computed(() => detail.value?.summary ?? {})
const company = computed(() => detail.value?.company ?? {})
const quality = computed(() => detail.value?.quality ?? {})
const risk = computed(() => detail.value?.risk ?? {})
const timelineItems = computed(() => detail.value?.timeline ?? [])
const latestTimelineItem = computed(() => {
  const timeline = timelineItems.value
  return timeline.length ? timeline[timeline.length - 1] : null
})

const visibleTimeline = computed(() => {
  return showFullTimeline.value ? timelineItems.value : timelineItems.value.slice(0, 4)
})

const qualityHighlights = computed(() => quality.value.highlights ?? [])

const publicStatusText = computed(() => summary.value.statusLabel || '状态待确认')
const publicQualityText = computed(() => quality.value.resultLabel || summary.value.qualityResult || '待补质检')
const publicPublishedAtText = computed(() => {
  const publishedAt = summary.value.publishedAt
  if (publishedAt) {
    return publishedAt
  }
  if (publicStatusText.value === '草稿') {
    return '尚未公开'
  }
  if (['已冻结', '已召回'].includes(publicStatusText.value)) {
    return '已公开，当前附带风险提示'
  }
  return '已公开，时间待补录'
})

const publicSloganText = computed(() => {
  return localizeVisibleText(summary.value.slogan) || '扫码后可直接查看批次状态、质检结论和关键追溯节点。'
})

const verdict = computed(() => {
  if (!detail.value) {
    return {
      title: '正在读取查询结果',
      copy: '系统正在核对当前批次的状态、质检和关键节点。'
    }
  }

  if (risk.value.hasRisk) {
    return {
      title: risk.value.statusLabel || '当前批次存在风险提示',
      copy: localizeVisibleText(risk.value.reason) || '当前批次存在异常，请先查看风险提示再决定是否继续使用。'
    }
  }

  if ((publicQualityText.value || '').includes('合格') || /pass/i.test(publicQualityText.value || '')) {
    return {
      title: '当前批次状态清晰，可放心继续查看',
      copy: '首屏保留了消费者最关心的状态、质检和关键节点，继续下滑即可回看完整过程。'
    }
  }

  return {
    title: '请先关注质检结论',
    copy: '当前还没有明确的合格结论，建议结合最近记录与企业说明一起查看。'
  }
})

const trustSignals = computed(() => [
  {
    label: '主体企业',
    value: summary.value.companyName || company.value.name || '企业信息待补充'
  },
  {
    label: '最近质检',
    value: publicQualityText.value
  },
  {
    label: '公开时间',
    value: publicPublishedAtText.value
  }
])

const consumerFacts = computed(() => [
  {
    label: '生产日期',
    value: summary.value.productionDate || '待补充'
  },
  {
    label: '产地',
    value: localizeVisibleText(summary.value.originPlace) || '产地待补充'
  },
  {
    label: '查询说明',
    value: publicSloganText.value
  }
])

const qualitySummaryText = computed(() => {
  return localizeVisibleText(quality.value.summary) || '当前暂无更多质检补充说明。'
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

function localizeVisibleText(text) {
  const value = String(text || '').trim()
  if (!value) {
    return ''
  }
  return {
    'Xinfeng Orchard Base': '江西省赣州市信丰果园基地',
    'Wuyuan Tea Base': '江西省上饶市婺源县茶园基地',
    'Public trace page is available for this batch.': '当前批次已开放公开查询，可继续查看关键追溯节点。',
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
        <div class="hero-card__main">
          <div class="hero-product">
            <img class="product-image" :src="summary.productImageUrl" :alt="summary.productName">
            <div class="hero-copy">
              <p class="eyebrow">消费者追溯</p>
              <h1 data-testid="public-product-name">{{ summary.productName }}</h1>
              <p class="verdict-title">{{ verdict.title }}</p>
              <p class="verdict-copy">{{ verdict.copy }}</p>
            </div>
          </div>

          <div class="hero-kpi-grid">
            <article class="kpi-card">
              <span>当前状态</span>
              <strong data-testid="public-status">{{ publicStatusText }}</strong>
            </article>
            <article class="kpi-card kpi-card--accent">
              <span>质检结论</span>
              <strong data-testid="public-quality">{{ publicQualityText }}</strong>
            </article>
            <article class="kpi-card">
              <span>主体企业</span>
              <strong data-testid="public-company">{{ summary.companyName || company.name }}</strong>
            </article>
            <article class="kpi-card">
              <span>批次编号</span>
              <strong data-testid="public-batch-code">{{ summary.batchCode }}</strong>
            </article>
            <article class="kpi-card">
              <span>产地</span>
              <strong data-testid="public-origin">{{ localizeVisibleText(summary.originPlace) || '产地待补充' }}</strong>
            </article>
            <article class="kpi-card">
              <span>公开时间</span>
              <strong>{{ publicPublishedAtText }}</strong>
            </article>
          </div>
        </div>

        <aside class="trust-card">
          <p class="state-eyebrow state-eyebrow--muted">可信依据</p>
          <ul class="trust-list">
            <li v-for="item in trustSignals" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </li>
          </ul>
        </aside>
      </section>

      <section class="detail-grid">
        <article class="card">
          <div class="section-head">
            <div>
              <h2>消费者最关心的信息</h2>
              <p>首屏只保留状态、来源和使用判断，方便扫完码后快速理解。</p>
            </div>
            <span>扫码首屏</span>
          </div>

          <div class="fact-grid">
            <div v-for="item in consumerFacts" :key="item.label" class="fact-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </article>

        <article class="card">
          <div class="section-head">
            <div>
              <h2>企业与检测说明</h2>
              <p>弱化后台字段感，只保留能支撑“可信度”的关键信息。</p>
            </div>
            <span>可信说明</span>
          </div>

          <div class="fact-grid">
            <div class="fact-card">
              <span>企业备案</span>
              <strong>{{ company.name || summary.companyName || '企业信息待补充' }}</strong>
            </div>
            <div class="fact-card">
              <span>许可证号</span>
              <strong>{{ company.licenseNo || '待补充' }}</strong>
            </div>
          </div>

          <p class="section-copy">{{ qualitySummaryText }}</p>

          <div v-if="qualityHighlights.length" class="pill-row">
            <span v-for="item in qualityHighlights" :key="item">{{ item }}</span>
          </div>

          <p class="address-copy">{{ localizeVisibleText(company.address) || '企业地址待补充。' }}</p>
        </article>
      </section>

      <section class="card timeline-card" data-testid="public-timeline">
        <div class="section-head">
          <div>
            <h2>关键追溯过程</h2>
            <p>先看最近动态，再按时间回看关键节点，手机端也能顺着往下滑动查看。</p>
          </div>
          <span>{{ timelineItems.length }} 个节点</span>
        </div>

        <article class="latest-event-card">
          <span>最近动态</span>
          <strong>{{ latestTimelineItem?.title || '暂无追溯记录' }}</strong>
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
    </template>
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
.trust-card,
.latest-event-card,
.fact-card,
.kpi-card {
  border-radius: 24px;
  box-shadow: var(--trace-shadow);
}

.state-card,
.hero-card,
.card,
.trust-card,
.latest-event-card,
.fact-card,
.kpi-card {
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
.hero-copy h1,
.risk-banner h2,
.latest-event-card strong {
  margin: 0;
  color: var(--trace-text);
}

.state-card p,
.risk-banner p,
.verdict-copy,
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
  grid-template-columns: 1.55fr 0.85fr;
  gap: 16px;
  padding: 20px;
}

.hero-product {
  display: grid;
  grid-template-columns: 148px 1fr;
  gap: 18px;
  align-items: start;
}

.product-image {
  width: 148px;
  height: 148px;
  border-radius: 24px;
  object-fit: cover;
  background: linear-gradient(160deg, #eef7ff, #dbeeff);
}

.hero-copy {
  min-width: 0;
}

.hero-copy h1 {
  margin-bottom: 10px;
  font-size: 30px;
}

.verdict-title {
  margin: 0 0 8px;
  color: var(--trace-primary-deep);
  font-size: 20px;
  font-weight: 700;
}

.hero-kpi-grid,
.fact-grid,
.trust-list {
  display: grid;
  gap: 12px;
}

.hero-kpi-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.kpi-card,
.fact-card,
.latest-event-card {
  padding: 16px;
  border: 1px solid var(--trace-border);
  background: var(--trace-surface-soft);
}

.kpi-card--accent {
  background: rgba(48, 149, 246, 0.1);
}

.kpi-card span,
.fact-card span,
.latest-event-card span,
.section-head span,
.timeline-stage,
.timeline-top span {
  display: block;
  color: var(--trace-text-soft);
  font-size: 12px;
}

.kpi-card strong,
.fact-card strong {
  display: block;
  margin-top: 8px;
  color: var(--trace-text);
  line-height: 1.5;
}

.trust-card {
  padding: 18px;
  border: 1px solid rgba(48, 149, 246, 0.14);
  background: linear-gradient(180deg, rgba(248, 252, 255, 0.98), rgba(241, 248, 255, 0.98));
}

.trust-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.trust-list li {
  padding: 14px 0;
  border-bottom: 1px solid rgba(48, 149, 246, 0.08);
}

.trust-list li:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.trust-list span {
  display: block;
  color: var(--trace-text-soft);
  font-size: 12px;
}

.trust-list strong {
  display: block;
  margin-top: 8px;
  color: var(--trace-text);
  line-height: 1.6;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.card {
  padding: 20px;
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
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .trace-page {
    padding-inline: 12px;
  }

  .hero-product,
  .hero-kpi-grid,
  .fact-grid {
    grid-template-columns: 1fr;
  }

  .product-image {
    width: 100%;
    height: 220px;
  }

  .hero-copy h1 {
    font-size: 26px;
  }
}
</style>
