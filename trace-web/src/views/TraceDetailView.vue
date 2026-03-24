<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceDetail } from '../api/trace'

const route = useRoute()

const detail = ref(null)
const loading = ref(true)
const errorMessage = ref('')

onMounted(() => {
  loadDetail(route.params.token)
})

watch(
  () => route.params.token,
  (token) => {
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
    errorMessage.value = error?.response?.data?.message || error?.message || 'Failed to load the trace page.'
  } finally {
    loading.value = false
  }
}

function riskClass(risk) {
  return {
    warning: 'warning',
    danger: 'danger'
  }[risk?.riskLevel] ?? 'warning'
}
</script>

<template>
  <div class="trace-page">
    <div v-if="loading" class="loading-card">Loading trace detail...</div>

    <div v-else-if="errorMessage" class="error-card">
      {{ errorMessage }}
    </div>

    <template v-else-if="detail">
      <section
        v-if="detail.risk?.hasRisk"
        class="risk-banner"
        :class="riskClass(detail.risk)"
      >
        <p class="risk-tag">Risk alert</p>
        <h2>{{ detail.risk.title }}</h2>
        <p>{{ detail.risk.reason }}</p>
        <div class="risk-meta">
          <span>Status: {{ detail.risk.status }}</span>
          <span>Updated: {{ detail.risk.updatedAt || 'N/A' }}</span>
        </div>
        <small>{{ detail.risk.tip }}</small>
      </section>

      <header class="hero-card">
        <div class="hero-image-wrap">
          <img class="hero-image" :src="detail.summary.productImageUrl" :alt="detail.summary.productName">
        </div>

        <div class="hero-copy">
          <p class="eyebrow">Scan and read</p>
          <h1>{{ detail.summary.productName }}</h1>
          <p class="batch-code">Batch: {{ detail.summary.batchCode }}</p>
          <p class="slogan">{{ detail.summary.slogan }}</p>

          <div class="summary-grid">
            <div>
              <span>Company</span>
              <strong>{{ detail.summary.companyName }}</strong>
            </div>
            <div>
              <span>Origin</span>
              <strong>{{ detail.summary.originPlace }}</strong>
            </div>
            <div>
              <span>Status</span>
              <strong>{{ detail.summary.statusLabel }}</strong>
            </div>
            <div>
              <span>Quality</span>
              <strong>{{ detail.summary.qualityResult }}</strong>
            </div>
            <div>
              <span>Production date</span>
              <strong>{{ detail.summary.productionDate }}</strong>
            </div>
            <div>
              <span>Market date</span>
              <strong>{{ detail.summary.marketDate || 'Not published' }}</strong>
            </div>
          </div>
        </div>
      </header>

      <section class="card-grid">
        <article class="card">
          <div class="section-head">
            <h2>Quality summary</h2>
            <span>{{ detail.quality.resultLabel }}</span>
          </div>
          <p class="section-copy">{{ detail.quality.summary }}</p>
          <div class="info-list">
            <div>
              <span>Agency</span>
              <strong>{{ detail.quality.agency || 'Not provided' }}</strong>
            </div>
            <div>
              <span>Report no</span>
              <strong>{{ detail.quality.reportNo || 'Not provided' }}</strong>
            </div>
            <div>
              <span>Report time</span>
              <strong>{{ detail.quality.reportTime || 'Not provided' }}</strong>
            </div>
          </div>
          <div class="pill-row">
            <span v-for="item in detail.quality.highlights" :key="item">{{ item }}</span>
          </div>
        </article>

        <article class="card">
          <div class="section-head">
            <h2>Company</h2>
            <span>Contact</span>
          </div>
          <div class="info-list">
            <div>
              <span>Name</span>
              <strong>{{ detail.company.name }}</strong>
            </div>
            <div>
              <span>License no</span>
              <strong>{{ detail.company.licenseNo || 'Not provided' }}</strong>
            </div>
            <div>
              <span>Contact</span>
              <strong>{{ detail.company.contactName || 'Not provided' }}</strong>
            </div>
            <div>
              <span>Phone</span>
              <strong>{{ detail.company.contactPhone || 'Not provided' }}</strong>
            </div>
          </div>
          <p class="section-copy">{{ detail.company.address }}</p>
        </article>
      </section>

      <section class="card">
        <div class="section-head">
          <h2>Key timeline</h2>
          <span>{{ detail.timeline.length }} nodes</span>
        </div>

        <ol class="timeline">
          <li v-for="item in detail.timeline" :key="`${item.stageCode}-${item.time}`" class="timeline-item">
            <div class="timeline-marker" />
            <div class="timeline-body">
              <div class="timeline-top">
                <div>
                  <p class="timeline-stage">{{ item.stageName }}</p>
                  <h3>{{ item.title }}</h3>
                </div>
                <span>{{ item.time }}</span>
              </div>
              <p class="timeline-meta">{{ item.location }}</p>
              <p class="timeline-summary">{{ item.summary }}</p>
              <img v-if="item.imageUrl" class="timeline-image" :src="item.imageUrl" :alt="item.title">
            </div>
          </li>
        </ol>
      </section>

      <section class="card">
        <div class="section-head">
          <h2>How to read this page</h2>
          <span>3-second scan</span>
        </div>
        <ul class="tips-list">
          <li v-for="item in detail.consumerTips" :key="item">{{ item }}</li>
        </ul>
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

.batch-code,
.slogan {
  color: rgba(255, 247, 234, 0.9);
}

.slogan {
  margin-bottom: 18px;
  line-height: 1.7;
}

.summary-grid,
.info-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-grid div,
.info-list div {
  padding: 14px;
  border-radius: 18px;
}

.summary-grid div {
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
.summary-grid span,
.info-list span {
  display: block;
  color: #688176;
  font-size: 12px;
}

.summary-grid strong,
.info-list strong {
  display: block;
  margin-top: 6px;
  line-height: 1.5;
}

.section-copy,
.timeline-summary,
.timeline-meta,
.tips-list li {
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

@media (min-width: 760px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
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
