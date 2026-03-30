<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceDetail } from '../api/trace'

const route = useRoute()

const detail = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const showFullTimeline = ref(false)

const verdict = computed(() => {
  if (!detail.value) {
    return {
      title: '正在加载查询结果',
      copy: '正在读取批次追溯信息。'
    }
  }

  if (detail.value.risk?.hasRisk) {
    return {
      title: riskStageLabel(detail.value.risk.status),
      copy: detail.value.risk.reason || '当前批次存在异常，请先关注风险提示。'
    }
  }

  if ((detail.value.summary?.qualityResult || '').includes('合格') || /pass/i.test(detail.value.summary?.qualityResult || '')) {
    return {
      title: '当前批次可正常查询',
      copy: '已展示企业信息、批次状态和最近质检结论，可继续查看关键过程。'
    }
  }

  return {
    title: '请先关注质检结论',
    copy: '当前还没有完整的质检说明，建议结合最新记录一起查看。'
  }
})

const visibleTimeline = computed(() => {
  const timeline = detail.value?.timeline ?? []
  return showFullTimeline.value ? timeline : timeline.slice(0, 4)
})

const latestTimelineItem = computed(() => {
  const timeline = detail.value?.timeline ?? []
  return timeline.length ? timeline[timeline.length - 1] : null
})

const qualityHighlights = computed(() => detail.value?.quality?.highlights ?? [])

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
    errorMessage.value = error?.response?.data?.message || error?.message || '追溯页加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function riskClass(risk) {
  return {
    pending: 'pending',
    warning: 'warning',
    danger: 'danger'
  }[risk?.riskLevel] ?? 'warning'
}

function riskStageLabel(status) {
  return {
    FROZEN: '当前已暂停流通',
    RECALLED: '当前已召回',
    PROCESSING: '当前正在处理中',
    RECTIFIED: '企业已完成整改'
  }[status] ?? '当前存在风险提示'
}

function consumerStatusLabel(statusLabel) {
  return {
    草稿: '尚未正式对外发布',
    已发布: '已公开展示',
    已冻结: '已暂停流通',
    已召回: '已召回'
  }[statusLabel] ?? statusLabel
}

function shortSummary(text) {
  if (!text) {
    return '该节点已留痕。'
  }
  return text.length > 52 ? `${text.slice(0, 52)}...` : text
}
</script>

<template>
  <div class="trace-page" data-testid="public-trace-page">
    <div v-if="loading" class="loading-card">正在加载追溯详情...</div>

    <div v-else-if="errorMessage" class="error-card">
      {{ errorMessage }}
    </div>

    <template v-else-if="detail">
      <section
        v-if="detail.risk?.hasRisk"
        class="risk-banner"
        data-testid="public-risk-banner"
        :class="riskClass(detail.risk)"
      >
        <p class="risk-tag">风险提醒</p>
        <h2>{{ riskStageLabel(detail.risk.status) }}</h2>
        <p>{{ detail.risk.reason }}</p>
        <div class="risk-meta">
          <span>当前阶段：{{ riskStageLabel(detail.risk.status) }}</span>
          <span>最近更新：{{ detail.risk.updatedAt || '暂无' }}</span>
        </div>
        <small>{{ detail.risk.tip }}</small>
      </section>

      <section class="result-card" data-testid="public-summary">
        <div class="result-head">
          <div class="product-block">
            <img class="product-image" :src="detail.summary.productImageUrl" :alt="detail.summary.productName">
            <div class="product-copy">
              <p class="eyebrow">查询结果</p>
              <h1 data-testid="public-product-name">{{ detail.summary.productName }}</h1>
              <p class="verdict-title">{{ verdict.title }}</p>
              <p class="verdict-copy">{{ verdict.copy }}</p>
            </div>
          </div>

          <div class="result-side">
            <article class="result-pill">
              <span>当前状态</span>
              <strong data-testid="public-status">{{ consumerStatusLabel(detail.summary.statusLabel) }}</strong>
            </article>
            <article class="result-pill highlight">
              <span>质检结论</span>
              <strong data-testid="public-quality">{{ detail.summary.qualityResult }}</strong>
            </article>
          </div>
        </div>

        <div class="summary-grid">
          <div>
            <span>企业</span>
            <strong data-testid="public-company">{{ detail.summary.companyName }}</strong>
          </div>
          <div>
            <span>批次号</span>
            <strong data-testid="public-batch-code">{{ detail.summary.batchCode }}</strong>
          </div>
          <div>
            <span>产地</span>
            <strong data-testid="public-origin">{{ detail.summary.originPlace }}</strong>
          </div>
          <div>
            <span>生产日期</span>
            <strong>{{ detail.summary.productionDate || '暂无' }}</strong>
          </div>
          <div>
            <span>公开时间</span>
            <strong>{{ detail.summary.publishedAt || '尚未公开' }}</strong>
          </div>
          <div>
            <span>查询说明</span>
            <strong>{{ detail.summary.slogan }}</strong>
          </div>
        </div>
      </section>

      <section class="detail-grid">
        <article class="card">
          <div class="section-head">
            <h2>基础信息</h2>
            <span>企业与批次</span>
          </div>

          <div class="info-grid">
            <div>
              <span>企业名称</span>
              <strong>{{ detail.company.name }}</strong>
            </div>
            <div>
              <span>许可证号</span>
              <strong>{{ detail.company.licenseNo || '待补充' }}</strong>
            </div>
            <div>
              <span>联系人</span>
              <strong>{{ detail.company.contactName || '待补充' }}</strong>
            </div>
            <div>
              <span>联系电话</span>
              <strong>{{ detail.company.contactPhone || '待补充' }}</strong>
            </div>
          </div>

          <p class="section-copy">{{ detail.company.address || '企业地址待补充。' }}</p>
        </article>

        <article class="card">
          <div class="section-head">
            <h2>质检与最近记录</h2>
            <span>{{ detail.quality.resultLabel }}</span>
          </div>

          <p class="section-copy">{{ detail.quality.summary }}</p>

          <div class="pill-row">
            <span v-for="item in qualityHighlights" :key="item">{{ item }}</span>
          </div>

          <div class="recent-card">
            <span>最近记录</span>
            <strong>{{ latestTimelineItem?.title || '暂无记录' }}</strong>
            <p>{{ latestTimelineItem?.time || '暂无时间' }} · {{ latestTimelineItem?.location || '地点待补充' }}</p>
            <small>{{ shortSummary(latestTimelineItem?.summary) }}</small>
          </div>
        </article>
      </section>

      <section class="card" data-testid="public-timeline">
        <div class="section-head">
          <h2>追溯时间线</h2>
          <span>{{ detail.timeline.length }} 个节点</span>
        </div>

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
              <p class="timeline-meta">{{ item.location || '地点已留痕' }}</p>
              <p class="timeline-summary">{{ shortSummary(item.summary) }}</p>
              <img v-if="item.imageUrl" class="timeline-image" :src="item.imageUrl" :alt="item.title">
            </div>
          </li>
        </ol>

        <button
          v-if="detail.timeline.length > 4"
          class="toggle-button"
          @click="showFullTimeline = !showFullTimeline"
        >
          {{ showFullTimeline ? '收起更多节点' : '查看更多关键节点' }}
        </button>
      </section>
    </template>
  </div>
</template>

<style scoped>
.trace-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 18px 14px 40px;
}

.loading-card,
.error-card,
.risk-banner,
.result-card,
.card {
  border-radius: 24px;
  box-shadow: var(--trace-shadow);
}

.loading-card,
.error-card,
.result-card,
.card {
  background: var(--trace-surface);
}

.loading-card,
.error-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 20px;
  color: var(--trace-text-soft);
  text-align: center;
}

.risk-banner {
  margin-bottom: 14px;
  padding: 18px;
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

.risk-tag,
.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.risk-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 12px 0 8px;
  font-size: 13px;
}

.result-card {
  padding: 20px;
}

.result-head {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: 16px;
}

.product-block {
  display: grid;
  grid-template-columns: 136px 1fr;
  gap: 16px;
  align-items: start;
}

.product-image {
  width: 136px;
  height: 136px;
  border-radius: 22px;
  object-fit: cover;
  background: linear-gradient(160deg, #eef7ff, #dbeeff);
}

.product-copy {
  min-width: 0;
}

.eyebrow {
  color: var(--trace-text-soft);
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 10px;
  color: var(--trace-text);
  font-size: 30px;
}

.verdict-title {
  margin-bottom: 8px;
  color: var(--trace-primary-deep);
  font-size: 19px;
  font-weight: 700;
}

.verdict-copy,
.section-copy,
.timeline-meta,
.timeline-summary,
.recent-card p,
.recent-card small {
  color: #4a6b90;
  line-height: 1.7;
}

.result-side,
.summary-grid,
.info-grid {
  display: grid;
  gap: 12px;
}

.result-pill,
.summary-grid div,
.info-grid div,
.recent-card {
  padding: 14px;
  border-radius: 18px;
  background: var(--trace-surface-soft);
  border: 1px solid var(--trace-border);
}

.result-pill.highlight {
  background: var(--trace-primary-soft);
}

.result-pill span,
.summary-grid span,
.info-grid span,
.recent-card span,
.section-head span {
  display: block;
  color: var(--trace-text-soft);
  font-size: 12px;
}

.result-pill strong,
.summary-grid strong,
.info-grid strong,
.recent-card strong {
  display: block;
  margin-top: 6px;
  color: var(--trace-text);
  line-height: 1.5;
}

.summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.card {
  margin-top: 14px;
  padding: 20px;
}

.detail-grid .card {
  margin-top: 0;
}

.section-head,
.timeline-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-head h2,
.timeline-body h3 {
  margin-bottom: 0;
  color: var(--trace-text);
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
  background: var(--trace-primary-soft);
  color: var(--trace-primary-deep);
  font-size: 13px;
}

.recent-card {
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

.timeline-stage,
.timeline-top span {
  color: var(--trace-text-soft);
  font-size: 12px;
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

@media (max-width: 720px) {
  .result-head,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 540px) {
  .trace-page {
    padding-inline: 12px;
  }

  .product-block,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .product-image {
    width: 100%;
    height: 220px;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
