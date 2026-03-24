<script setup>
import { onMounted, ref } from 'vue'
import { getDashboardOverview } from '../api/dashboard'

const overview = ref(null)
const loading = ref(true)

onMounted(async () => {
  const response = await getDashboardOverview()
  overview.value = response.data
  loading.value = false
})
</script>

<template>
  <div class="page-shell">
    <header class="hero">
      <div>
        <p class="eyebrow">轻量、直观、扫码即看</p>
        <h1>农产品追溯管理端</h1>
        <p class="lead">
          本轮项目方向已经从功能堆砌收敛为批次主线，优先保证企业建档、批次工作台和公开追溯页顺畅。
        </p>
      </div>
      <nav class="hero-actions">
        <RouterLink to="/batches">进入批次管理</RouterLink>
      </nav>
    </header>

    <section v-if="loading" class="panel">正在加载概览数据...</section>

    <template v-else-if="overview">
      <section class="stats-grid">
        <article class="stat-card">
          <span>批次总数</span>
          <strong>{{ overview.totalBatches }}</strong>
        </article>
        <article class="stat-card">
          <span>已发布批次</span>
          <strong>{{ overview.publishedBatches }}</strong>
        </article>
        <article class="stat-card">
          <span>草稿批次</span>
          <strong>{{ overview.draftBatches }}</strong>
        </article>
        <article class="stat-card danger">
          <span>风险批次</span>
          <strong>{{ overview.riskBatches }}</strong>
        </article>
      </section>

      <section class="content-grid">
        <article class="panel">
          <h2>当前主流程</h2>
          <p>{{ overview.coreFlowMessage }}</p>
        </article>
        <article class="panel">
          <h2>本轮建设重点</h2>
          <ul>
            <li v-for="item in overview.currentFocus" :key="item">{{ item }}</li>
          </ul>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.page-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 20px 40px;
}

.hero {
  display: grid;
  grid-template-columns: 1.6fr 0.8fr;
  gap: 24px;
  padding: 28px;
  border-radius: 28px;
  background: rgba(22, 48, 43, 0.92);
  color: #f7f3e8;
  box-shadow: 0 30px 60px rgba(25, 47, 37, 0.18);
}

.eyebrow {
  margin: 0 0 12px;
  color: #f3d17b;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 12px;
}

h1 {
  margin: 0 0 14px;
  font-size: 40px;
}

.lead {
  margin: 0;
  font-size: 16px;
  line-height: 1.8;
  max-width: 680px;
}

.hero-actions {
  display: flex;
  align-items: end;
  justify-content: end;
}

.hero-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 168px;
  padding: 14px 18px;
  border-radius: 999px;
  background: #f3d17b;
  color: #22352e;
  font-weight: 700;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-top: 24px;
}

.stat-card,
.panel {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(12px);
  box-shadow: 0 16px 36px rgba(38, 70, 52, 0.08);
}

.stat-card {
  padding: 22px;
}

.stat-card span {
  display: block;
  color: #557062;
  font-size: 14px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 34px;
}

.danger strong {
  color: #a63d2a;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
  margin-top: 18px;
}

.panel {
  padding: 24px;
}

.panel h2 {
  margin: 0 0 14px;
}

.panel p,
.panel li {
  line-height: 1.8;
  color: #334b40;
}

.panel ul {
  margin: 0;
  padding-left: 18px;
}

@media (max-width: 900px) {
  .hero,
  .content-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  h1 {
    font-size: 32px;
  }

  .hero-actions {
    justify-content: start;
  }
}
</style>
