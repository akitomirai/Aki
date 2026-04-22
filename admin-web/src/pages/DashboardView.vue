<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getBatchList } from '../api/batch'
import { getProductList } from '../api/master-data'
import { hasRoleAccess } from '../utils/access'

const authStore = useAuthStore()
const loading = ref(true)
const batches = ref([])
const products = ref([])

const quickEntries = computed(() => {
  const roleCode = authStore.user?.roleCode
  const entries = [
    { to: '/batches', title: '批次管理' }
  ]

  if (hasRoleAccess(roleCode, ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'])) {
    entries.push({ to: '/products', title: '产品管理' })
  }

  if (hasRoleAccess(roleCode, ['PLATFORM_ADMIN'])) {
    entries.push({ to: '/companies', title: '企业管理' })
  }

  if (hasRoleAccess(roleCode, ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR'])) {
    entries.push({ to: '/risk', title: '风险处理' })
  }

  return entries
})

const productCount = computed(() => products.value.length)
const pendingQualityCount = computed(() => {
  return batches.value.filter((item) => String(item.qualityStatusCode || 'PENDING').toUpperCase() === 'PENDING').length
})
const publishedBatchCount = computed(() => {
  return batches.value.filter((item) => String(item.status || '').toUpperCase() === 'PUBLISHED').length
})
const riskBatchCount = computed(() => {
  return batches.value.filter((item) => ['FROZEN', 'RECALLED'].includes(String(item.status || '').toUpperCase())).length
})

const recentActivities = computed(() => {
  return [...batches.value]
    .sort((left, right) => {
      const leftTime = new Date(left.lastUpdatedAt || left.latestTraceTime || left.marketDate || 0).getTime()
      const rightTime = new Date(right.lastUpdatedAt || right.latestTraceTime || right.marketDate || 0).getTime()
      return rightTime - leftTime || (right.id ?? 0) - (left.id ?? 0)
    })
    .slice(0, 6)
    .map((item) => ({
      id: item.id,
      batchCode: item.batchCode,
      productName: item.productName,
      companyName: item.companyName,
      statusLabel: item.statusLabel || '状态待确认'
    }))
})

const statusDistributionRows = computed(() => ([
  { label: '草稿批次', value: batches.value.filter((item) => String(item.status || '').toUpperCase() === 'DRAFT').length, tone: 'warning' },
  { label: '已发布批次', value: publishedBatchCount.value, tone: 'info' },
  { label: '风险批次', value: riskBatchCount.value, tone: 'danger' },
  { label: '待质检批次', value: pendingQualityCount.value, tone: 'normal' }
]))

async function loadDashboard() {
  loading.value = true
  try {
    const [productResponse, batchResponse] = await Promise.all([
      getProductList(),
      getBatchList()
    ])
    products.value = productResponse.data ?? []
    batches.value = batchResponse.data ?? []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="dashboard-page" data-testid="dashboard-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">首页总览</h1>
      </div>
      <div class="manage-page-actions">
        <el-button :loading="loading" @click="loadDashboard">刷新</el-button>
      </div>
    </section>

    <section v-if="loading" class="section-card loading-card">
      正在加载首页数据...
    </section>

    <template v-else>
      <section class="stats-grid">
        <article class="stat-card">
          <span>产品总数</span>
          <strong>{{ productCount }}</strong>
        </article>
        <article class="stat-card">
          <span>批次总数</span>
          <strong>{{ batches.length }}</strong>
        </article>
        <article class="stat-card">
          <span>待质检批次</span>
          <strong>{{ pendingQualityCount }}</strong>
        </article>
        <article class="stat-card">
          <span>已发布批次</span>
          <strong>{{ publishedBatchCount }}</strong>
        </article>
        <article class="stat-card risk">
          <span>风险批次</span>
          <strong>{{ riskBatchCount }}</strong>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="section-card">
          <div class="section-head">
            <h2>状态分布</h2>
          </div>
          <div class="overview-list">
            <div v-for="item in statusDistributionRows" :key="item.label" class="overview-row">
              <span>{{ item.label }}</span>
              <strong :class="item.tone">{{ item.value }}</strong>
            </div>
          </div>
        </article>

        <article class="section-card">
          <div class="section-head">
            <h2>最近动态</h2>
          </div>
          <ul v-if="recentActivities.length" class="item-list">
            <li v-for="item in recentActivities" :key="item.id">
              <div>
                <strong>{{ item.batchCode }}</strong>
                <span>{{ item.productName }} / {{ item.companyName }}</span>
              </div>
              <div class="item-meta">
                <em :class="{ 'risk-text': ['已冻结', '已召回'].includes(item.statusLabel) }">{{ item.statusLabel }}</em>
                <RouterLink :to="`/batches/${item.id}`">查看工作台</RouterLink>
              </div>
            </li>
          </ul>
          <p v-else class="empty-text">当前没有最近动态。</p>
        </article>

        <article class="section-card quick-entry-card">
          <div class="section-head">
            <h2>快捷入口</h2>
          </div>
          <div class="quick-grid">
            <RouterLink v-for="entry in quickEntries" :key="entry.to" :to="entry.to" class="quick-link">
              <strong>{{ entry.title }}</strong>
              <span>进入</span>
            </RouterLink>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: var(--admin-page-section-gap);
  max-width: 1220px;
  margin: 0 auto;
  padding: var(--admin-page-shell-padding);
}

.section-card,
.stat-card {
  border: 1px solid var(--admin-border);
  border-radius: 20px;
  background: var(--admin-surface);
  box-shadow: var(--admin-shadow);
}

.loading-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  margin-top: 18px;
  color: var(--admin-text-soft);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 22px;
}

.stat-card span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 14px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  color: var(--admin-text);
  font-size: 34px;
}

.stat-card.risk strong {
  color: var(--admin-danger-text);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.section-card {
  padding: 20px;
}

.quick-entry-card {
  grid-column: 1 / -1;
}

.section-head {
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 0;
  color: var(--admin-text);
  font-size: 18px;
}

.item-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
}

.item-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(56, 134, 217, 0.1);
  border-radius: 16px;
  background: var(--admin-surface-soft);
}

.item-list strong,
.overview-row strong,
.quick-link strong {
  display: block;
  color: var(--admin-text);
}

.item-list span,
.quick-link span,
.empty-text {
  color: var(--admin-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.item-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  text-align: right;
}

.item-meta em {
  font-style: normal;
  color: var(--admin-text-soft);
  font-size: 12px;
}

.item-meta a {
  color: var(--admin-primary-deep);
  font-size: 13px;
  font-weight: 600;
}

.risk-text {
  color: var(--admin-danger-text) !important;
}

.overview-list {
  display: grid;
  gap: 12px;
}

.overview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--admin-surface-soft);
}

.overview-row span {
  color: var(--admin-text-soft);
  font-size: 13px;
}

.overview-row strong.info {
  color: var(--admin-primary-deep);
}

.overview-row strong.warning {
  color: #9a6512;
}

.overview-row strong.danger {
  color: var(--admin-danger-text);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-link {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 88px;
  padding: 16px;
  border: 1px solid rgba(56, 134, 217, 0.1);
  border-radius: 16px;
  background: var(--admin-surface-soft);
}

.quick-link strong {
  font-size: 15px;
}

.empty-text {
  margin: 0;
}

@media (max-width: 1080px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .dashboard-page {
    padding: var(--admin-page-shell-padding-mobile);
  }

  .dashboard-grid,
  .stats-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .item-list li {
    flex-direction: column;
    align-items: flex-start;
  }

  .item-meta {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
