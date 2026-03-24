<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceDetail } from '../api/trace'

const route = useRoute()
const detail = ref(null)
const loading = ref(true)

async function loadDetail(token) {
  loading.value = true
  const response = await getTraceDetail(token)
  detail.value = response.data
  loading.value = false
}

onMounted(() => loadDetail(route.params.token))
watch(() => route.params.token, (token) => loadDetail(token))
</script>

<template>
  <div class="trace-page">
    <div v-if="loading" class="loading-card">正在加载追溯信息...</div>

    <template v-else-if="detail">
      <header class="hero-card">
        <p class="eyebrow">扫码即看</p>
        <h1>{{ detail.summary.productName }}</h1>
        <p class="batch-code">批次号：{{ detail.summary.batchCode }}</p>
        <div class="summary-grid">
          <div>
            <span>企业</span>
            <strong>{{ detail.summary.companyName }}</strong>
          </div>
          <div>
            <span>产地</span>
            <strong>{{ detail.summary.originPlace }}</strong>
          </div>
          <div>
            <span>状态</span>
            <strong>{{ detail.summary.statusLabel }}</strong>
          </div>
          <div>
            <span>质检</span>
            <strong>{{ detail.summary.qualityLabel }}</strong>
          </div>
        </div>
        <p class="slogan">{{ detail.summary.slogan }}</p>
      </header>

      <section v-if="detail.exception.hasException" class="warning-card" :class="detail.exception.level">
        <h2>异常状态提醒</h2>
        <p>{{ detail.exception.message }}</p>
        <small>{{ detail.exception.suggestion }}</small>
      </section>

      <section class="card">
        <div class="section-head">
          <h2>关键时间线</h2>
          <span>{{ detail.timeline.length }} 个节点</span>
        </div>
        <ol class="timeline">
          <li v-for="item in detail.timeline" :key="`${item.stageName}-${item.time}`">
            <div class="dot" />
            <div class="line-body">
              <h3>{{ item.stageName }} · {{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <small>{{ item.time }} · {{ item.location }}</small>
            </div>
          </li>
        </ol>
      </section>

      <section class="card-grid">
        <article class="card">
          <div class="section-head">
            <h2>质检信息</h2>
            <span>{{ detail.quality.status }}</span>
          </div>
          <p>{{ detail.quality.agency }}</p>
          <p>{{ detail.quality.reportNo || '暂无报告编号' }}</p>
          <p>{{ detail.quality.reportTime || '暂无检测时间' }}</p>
          <div class="pill-list">
            <span v-for="item in detail.quality.highlights" :key="item">{{ item }}</span>
          </div>
        </article>

        <article class="card">
          <div class="section-head">
            <h2>企业信息</h2>
            <span>可联系</span>
          </div>
          <p>{{ detail.company.name }}</p>
          <p>许可证：{{ detail.company.licenseNo }}</p>
          <p>联系人：{{ detail.company.contactName }} / {{ detail.company.contactPhone }}</p>
          <p>{{ detail.company.address }}</p>
        </article>
      </section>

      <section class="card">
        <div class="section-head">
          <h2>消费者查看建议</h2>
          <span>首屏即懂</span>
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
  padding: 18px 14px 40px;
}

.hero-card,
.card,
.loading-card,
.warning-card {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 36px rgba(25, 55, 44, 0.08);
}

.hero-card {
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(245, 210, 127, 0.4), transparent 28%),
    linear-gradient(160deg, #17362e, #285245 70%, #eff6eb 180%);
  color: #fff7ea;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f5d283;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 12px;
}

h1 {
  margin: 0;
  font-size: 30px;
}

.batch-code,
.slogan {
  color: rgba(255, 247, 234, 0.88);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin: 18px 0;
}

.summary-grid div {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
}

.summary-grid span,
.section-head span {
  display: block;
  color: #d4dfd7;
  font-size: 12px;
}

.summary-grid strong {
  display: block;
  margin-top: 6px;
}

.warning-card {
  margin-top: 16px;
  padding: 18px;
}

.warning-card.warning {
  background: #fff7e8;
  color: #7a4d00;
}

.warning-card.danger {
  background: #fdeceb;
  color: #8b241f;
}

.card {
  margin-top: 16px;
  padding: 20px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-head h2 {
  margin: 0 0 14px;
  color: #183228;
}

.section-head span {
  color: #5d786e;
}

.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
}

.timeline li {
  display: grid;
  grid-template-columns: 22px 1fr;
  gap: 12px;
  padding-bottom: 16px;
}

.dot {
  width: 14px;
  height: 14px;
  margin-top: 4px;
  border-radius: 999px;
  background: #2d6b56;
  box-shadow: 0 0 0 4px rgba(45, 107, 86, 0.12);
}

.line-body h3 {
  margin: 0 0 8px;
  color: #19362c;
}

.line-body p,
.line-body small,
.card p,
.tips-list li {
  color: #446055;
  line-height: 1.7;
}

.pill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill-list span {
  padding: 7px 10px;
  border-radius: 999px;
  background: #edf3de;
  color: #566327;
  font-size: 12px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
}

@media (max-width: 680px) {
  .summary-grid,
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
