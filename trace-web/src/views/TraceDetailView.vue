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
      title: '正在加载',
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
      title: '当前批次状态正常',
      copy: '已看到企业信息、批次状态和最近质检结论，可继续查看关键过程。'
    }
  }

  return {
    title: '先关注质检与状态',
    copy: '当前还没有完整的质检结论，建议结合关键过程一起看。'
  }
})

const visibleTimeline = computed(() => {
  const timeline = detail.value?.timeline ?? []
  return showFullTimeline.value ? timeline : timeline.slice(0, 4)
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
  return text.length > 42 ? `${text.slice(0, 42)}...` : text
}
</script>

<template>
  <div class="trace-page">
    <div v-if="loading" class="loading-card">正在加载追溯详情...</div>

    <div v-else-if="errorMessage" class="error-card">
      {{ errorMessage }}
    </div>

    <template v-else-if="detail">
      <section
        v-if="detail.risk?.hasRisk"
        class="risk-banner"
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

      <header class="hero-card">
        <div class="hero-image-wrap">
          <img class="hero-image" :src="detail.summary.productImageUrl" :alt="detail.summary.productName">
        </div>

        <div class="hero-copy">
          <p class="eyebrow">扫码结果</p>
          <h1>{{ detail.summary.productName }}</h1>
          <p class="hero-verdict">{{ verdict.title }}</p>
          <p class="slogan">{{ verdict.copy }}</p>

          <div class="verdict-row">
            <article class="verdict-pill status">
              <span>当前状态</span>
              <strong>{{ consumerStatusLabel(detail.summary.statusLabel) }}</strong>
            </article>
            <article class="verdict-pill quality">
              <span>质检结论</span>
              <strong>{{ detail.summary.qualityResult }}</strong>
            </article>
          </div>

          <div class="summary-grid">
            <div>
              <span>企业</span>
              <strong>{{ detail.summary.companyName }}</strong>
            </div>
            <div>
              <span>批次号</span>
              <strong>{{ detail.summary.batchCode }}</strong>
            </div>
            <div>
              <span>产地</span>
              <strong>{{ detail.summary.originPlace }}</strong>
            </div>
            <div>
              <span>生产日期</span>
              <strong>{{ detail.summary.productionDate }}</strong>
            </div>
          </div>
        </div>
      </header>

      <section class="card-grid">
        <article class="card">
          <div class="section-head">
            <h2>看这页先看什么</h2>
            <span>3 秒阅读</span>
          </div>
          <ul class="tips-list compact">
            <li>先看状态和质检结论，判断当前是否可放心购买或食用。</li>
            <li>再看企业、批次号和产地，确认这是不是你手上的这批货。</li>
            <li>最后看关键过程，了解这批产品是怎么流转过来的。</li>
          </ul>
        </article>

        <article class="card">
          <div class="section-head">
            <h2>信任信息</h2>
            <span>现有信息直读</span>
          </div>
          <div class="info-list">
            <div>
              <span>企业名称</span>
              <strong>{{ detail.company.name }}</strong>
            </div>
            <div>
              <span>许可证号</span>
              <strong>{{ detail.company.licenseNo || '待补充' }}</strong>
            </div>
            <div>
              <span>检测机构</span>
              <strong>{{ detail.quality.agency || '待补充' }}</strong>
            </div>
            <div>
              <span>报告编号</span>
              <strong>{{ detail.quality.reportNo || '待补充' }}</strong>
            </div>
          </div>
        </article>
      </section>

      <section class="card">
        <div class="section-head">
          <h2>关键过程</h2>
          <span>{{ detail.timeline.length }} 个节点</span>
        </div>

        <ol class="timeline">
          <li v-for="item in visibleTimeline" :key="`${item.stageCode}-${item.time}`" class="timeline-item">
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

      <section class="card">
        <div class="section-head">
          <h2>质检摘要</h2>
          <span>{{ detail.quality.resultLabel }}</span>
        </div>
        <p class="section-copy">{{ detail.quality.summary }}</p>
        <div class="pill-row">
          <span v-for="item in detail.quality.highlights" :key="item">{{ item }}</span>
        </div>
        <p class="company-copy">{{ detail.company.address }}</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.trace-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 16px 14px 40px;
}

.loading-card,
.error-card,
.risk-banner,
.hero-card,
.card {
  border-radius: 24px;
  box-shadow: 0 18px 36px rgba(25, 55, 44, 0.08);
}

.loading-card,
.error-card,
.card {
  background: rgba(255, 255, 255, 0.94);
}

.loading-card,
.error-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 20px;
  color: #466257;
  text-align: center;
}

.risk-banner {
  margin-bottom: 14px;
  padding: 18px;
}

.risk-banner.warning {
  background: #fff5df;
  color: #7a4d00;
}

.risk-banner.pending {
  background: #eef5ff;
  color: #2a4f7f;
}

.risk-banner.danger {
  background: #fdeceb;
  color: #8b241f;
}

.risk-tag,
.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.risk-tag {
  color: currentColor;
  opacity: 0.78;
}

.risk-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 12px 0 8px;
  font-size: 13px;
}

.hero-card {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(245, 210, 127, 0.34), transparent 26%),
    linear-gradient(160deg, #17362e, #285245 70%, #eff6eb 180%);
  color: #fff7ea;
}

.hero-image-wrap {
  padding: 18px 18px 0;
}

.hero-image {
  width: 100%;
  height: min(48vw, 280px);
  max-height: 280px;
  object-fit: cover;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.16);
}

.hero-copy {
  padding: 18px 18px 24px;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 10px;
  font-size: 30px;
}

.hero-verdict {
  margin-bottom: 8px;
  font-size: 18px;
  font-weight: 700;
}

.slogan {
  margin-bottom: 18px;
  line-height: 1.7;
  color: rgba(255, 247, 234, 0.92);
}

.verdict-row,
.summary-grid,
.info-list {
  display: grid;
  gap: 12px;
}

.verdict-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 14px;
}

.verdict-pill,
.summary-grid div,
.info-list div {
  padding: 14px;
  border-radius: 18px;
}

.verdict-pill {
  background: rgba(255, 255, 255, 0.12);
}

.verdict-pill.status {
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.verdict-pill.quality {
  background: rgba(242, 204, 105, 0.18);
}

.summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-grid div,
.info-list div {
  background: rgba(255, 255, 255, 0.08);
}

.card {
  margin-top: 14px;
  padding: 20px;
}

.card-grid {
  display: grid;
  gap: 14px;
}

.section-head,
.timeline-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-head h2 {
  margin-bottom: 0;
  color: #183228;
}

.section-head span,
.verdict-pill span,
.summary-grid span,
.info-list span {
  display: block;
  color: #688176;
  font-size: 12px;
}

.verdict-pill strong,
.summary-grid strong,
.info-list strong {
  display: block;
  margin-top: 6px;
  line-height: 1.5;
}

.section-copy,
.timeline-summary,
.timeline-meta,
.tips-list li,
.company-copy {
  color: #446055;
  line-height: 1.7;
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
  background: rgba(45, 107, 86, 0.1);
  color: #225241;
  font-size: 13px;
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
  background: #2d6b56;
  box-shadow: 0 0 0 4px rgba(45, 107, 86, 0.12);
}

.timeline-stage {
  margin-bottom: 4px;
  color: #688176;
  font-size: 12px;
}

.timeline-body h3 {
  margin-bottom: 8px;
  color: #19362c;
}

.timeline-image {
  width: 100%;
  margin-top: 12px;
  border-radius: 18px;
  object-fit: cover;
  max-height: 220px;
}

.tips-list {
  margin: 18px 0 0;
  padding-left: 18px;
}

.tips-list.compact {
  margin-top: 12px;
}

.toggle-button {
  margin-top: 8px;
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

@media (min-width: 760px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .verdict-row,
  .summary-grid,
  .info-list {
    grid-template-columns: 1fr;
  }

  .hero-image {
    height: 220px;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
