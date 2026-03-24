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
        <p class="eyebrow">Lightweight, direct, scan-first</p>
        <h1>Agricultural traceability admin</h1>
        <p class="lead">
          The project now stays focused on the batch workflow, public QR trace page,
          and the minimum master data needed to keep the demo close to a real system.
        </p>
      </div>
      <nav class="hero-actions">
        <RouterLink to="/batches">Batch center</RouterLink>
        <RouterLink to="/companies">Companies</RouterLink>
        <RouterLink to="/products">Products</RouterLink>
      </nav>
    </header>

    <section v-if="loading" class="panel">Loading overview...</section>

    <template v-else-if="overview">
      <section class="stats-grid">
        <article class="stat-card">
          <span>Total batches</span>
          <strong>{{ overview.totalBatches }}</strong>
        </article>
        <article class="stat-card">
          <span>Published batches</span>
          <strong>{{ overview.publishedBatches }}</strong>
        </article>
        <article class="stat-card">
          <span>Draft batches</span>
          <strong>{{ overview.draftBatches }}</strong>
        </article>
        <article class="stat-card danger">
          <span>Risk batches</span>
          <strong>{{ overview.riskBatches }}</strong>
        </article>
      </section>

      <section class="content-grid">
        <article class="panel">
          <h2>Current main flow</h2>
          <p>{{ overview.coreFlowMessage }}</p>
        </article>
        <article class="panel">
          <h2>This round focus</h2>
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
  flex-wrap: wrap;
  align-items: end;
  justify-content: end;
  gap: 12px;
}

.hero-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 148px;
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
