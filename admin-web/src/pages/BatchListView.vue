<script setup>
import { onMounted, ref } from 'vue'
import { getBatchList } from '../api/batch'

const batches = ref([])
const loading = ref(true)

onMounted(async () => {
  const response = await getBatchList()
  batches.value = response.data
  loading.value = false
})

const statusMap = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  FROZEN: '已冻结',
  RECALLED: '已召回'
}
</script>

<template>
  <div class="page-shell">
    <header class="page-header">
      <div>
        <p class="eyebrow">批次中心</p>
        <h1>批次管理</h1>
        <p>以批次为核心聚合信息，把录入、质检、二维码和发布动作尽量放在一条线内完成。</p>
      </div>
      <div class="header-actions">
        <RouterLink to="/dashboard">返回总览</RouterLink>
      </div>
    </header>

    <section class="table-panel">
      <div class="panel-head">
        <h2>批次列表</h2>
        <span>{{ batches.length }} 个批次</span>
      </div>

      <div v-if="loading" class="placeholder">正在加载批次数据...</div>

      <div v-else class="batch-grid">
        <article v-for="batch in batches" :key="batch.id" class="batch-card">
          <div class="card-head">
            <div>
              <p class="batch-code">{{ batch.batchCode }}</p>
              <h3>{{ batch.productName }}</h3>
            </div>
            <span class="status">{{ statusMap[batch.status] || batch.status }}</span>
          </div>

          <dl>
            <div>
              <dt>企业</dt>
              <dd>{{ batch.companyName }}</dd>
            </div>
            <div>
              <dt>当前节点</dt>
              <dd>{{ batch.currentNode }}</dd>
            </div>
            <div>
              <dt>质检</dt>
              <dd>{{ batch.qualityStatus }}</dd>
            </div>
            <div>
              <dt>二维码</dt>
              <dd>{{ batch.qrStatus }}</dd>
            </div>
          </dl>

          <div class="tags">
            <span v-for="tag in batch.quickTags" :key="tag">{{ tag }}</span>
          </div>

          <RouterLink class="detail-link" :to="`/batches/${batch.id}`">进入批次工作台</RouterLink>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 20px 40px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
}

.eyebrow {
  margin: 0 0 8px;
  color: #8f6b11;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 12px;
}

h1 {
  margin: 0 0 10px;
  font-size: 34px;
}

.page-header p:last-child {
  margin: 0;
  max-width: 760px;
  color: #426055;
  line-height: 1.8;
}

.header-actions a,
.detail-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 18px;
  border-radius: 999px;
  background: #234438;
  color: #f6efe0;
  font-weight: 700;
}

.table-panel {
  margin-top: 24px;
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 40px rgba(31, 62, 47, 0.08);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.batch-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.batch-card {
  padding: 22px;
  border-radius: 22px;
  background: #f7fbf7;
  border: 1px solid rgba(49, 88, 68, 0.1);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.batch-code {
  margin: 0 0 6px;
  color: #688476;
  font-size: 13px;
  letter-spacing: 0.05em;
}

h3 {
  margin: 0;
  font-size: 24px;
}

.status {
  height: fit-content;
  padding: 7px 12px;
  border-radius: 999px;
  background: #e4efe8;
  color: #234438;
  font-size: 13px;
  font-weight: 700;
}

dl {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin: 18px 0;
}

dt {
  color: #6c8378;
  font-size: 13px;
}

dd {
  margin: 6px 0 0;
  line-height: 1.6;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.tags span {
  padding: 7px 10px;
  border-radius: 999px;
  background: #eef3de;
  color: #57652d;
  font-size: 12px;
}

.placeholder {
  padding: 24px 0;
  color: #5d7268;
}

@media (max-width: 900px) {
  .page-header,
  .batch-grid,
  dl {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
