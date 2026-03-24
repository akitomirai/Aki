<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getBatchDetail } from '../api/batch'

const route = useRoute()
const detail = ref(null)
const loading = ref(true)

async function loadDetail(id) {
  loading.value = true
  const response = await getBatchDetail(id)
  detail.value = response.data
  loading.value = false
}

onMounted(() => loadDetail(route.params.id))
watch(() => route.params.id, (id) => loadDetail(id))

const actionLabelMap = {
  ADD_TRACE: '补录过程',
  UPLOAD_QUALITY: '上传质检',
  GENERATE_QR: '生成二维码',
  PUBLISH: '发布',
  FREEZE: '冻结',
  RECALL: '召回'
}

const statusLabel = computed(() => {
  if (!detail.value) return ''
  return {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    FROZEN: '已冻结',
    RECALLED: '已召回'
  }[detail.value.batchStatus] || detail.value.batchStatus
})
</script>

<template>
  <div class="page-shell">
    <div v-if="loading" class="loading">正在加载批次工作台...</div>

    <template v-else-if="detail">
      <header class="hero">
        <div>
          <p class="eyebrow">批次工作台</p>
          <h1>{{ detail.productName }}</h1>
          <p class="code">{{ detail.batchCode }}</p>
          <p class="intro">{{ detail.publicRemark }}</p>
        </div>
        <aside class="hero-side">
          <span class="status-badge">{{ statusLabel }}</span>
          <p>当前节点：{{ detail.currentNode }}</p>
          <p>质检状态：{{ detail.qualityStatus }}</p>
          <p>产地：{{ detail.originPlace }}</p>
        </aside>
      </header>

      <section class="grid top-grid">
        <article class="panel">
          <h2>批次基本信息</h2>
          <ul class="info-list">
            <li><span>产品</span><strong>{{ detail.productName }}</strong></li>
            <li><span>品类</span><strong>{{ detail.productCategory }}</strong></li>
            <li><span>企业</span><strong>{{ detail.companyName }}</strong></li>
            <li><span>许可证</span><strong>{{ detail.companyLicenseNo }}</strong></li>
            <li><span>联系人</span><strong>{{ detail.companyContactName }} / {{ detail.companyContactPhone }}</strong></li>
            <li><span>生产日期</span><strong>{{ detail.productionDate }}</strong></li>
          </ul>
        </article>

        <article class="panel">
          <h2>二维码状态</h2>
          <template v-if="detail.qrInfo">
            <p class="qr-status">状态：{{ detail.qrInfo.status }}</p>
            <p class="link-row">公开链接：<a :href="detail.qrInfo.publicUrl" target="_blank">{{ detail.qrInfo.publicUrl }}</a></p>
            <p>生成时间：{{ detail.qrInfo.generatedAt }}</p>
            <p>最近扫码：{{ detail.qrInfo.lastScanAt || '暂无' }}</p>
          </template>
          <p v-else>尚未生成二维码，无法进入公开追溯页。</p>
        </article>
      </section>

      <section class="grid">
        <article class="panel">
          <div class="section-head">
            <h2>追溯记录</h2>
            <span>{{ detail.traceRecords.length }} 条</span>
          </div>
          <ol class="timeline">
            <li v-for="record in detail.traceRecords" :key="record.id">
              <div>
                <h3>{{ record.stageName }} · {{ record.title }}</h3>
                <p>{{ record.summary }}</p>
              </div>
              <small>{{ record.eventTime }} · {{ record.location }}</small>
            </li>
          </ol>
        </article>

        <article class="panel">
          <div class="section-head">
            <h2>质检与留痕</h2>
            <span>{{ detail.qualityReports.length }} 份</span>
          </div>
          <div v-if="detail.qualityReports.length" class="stack">
            <div v-for="report in detail.qualityReports" :key="report.id" class="stack-card">
              <h3>{{ report.reportNo }}</h3>
              <p>{{ report.agency }} · {{ report.result }}</p>
              <p>{{ report.reportTime }}</p>
              <div class="tag-list">
                <span v-for="item in report.highlights" :key="item">{{ item }}</span>
              </div>
            </div>
          </div>
          <p v-else>暂未上传质检报告。</p>
        </article>
      </section>

      <section class="grid">
        <article class="panel">
          <div class="section-head">
            <h2>状态流转</h2>
            <span>{{ detail.statusHistory.length }} 次</span>
          </div>
          <ul class="history-list">
            <li v-for="item in detail.statusHistory" :key="`${item.status}-${item.operatedAt}`">
              <strong>{{ item.status }}</strong>
              <span>{{ item.operatedAt }} · {{ item.operatorName }}</span>
              <p>{{ item.reason }}</p>
            </li>
          </ul>
        </article>

        <article class="panel">
          <div class="section-head">
            <h2>可执行动作</h2>
            <span>少步骤入口</span>
          </div>
          <div class="action-list">
            <button v-for="action in detail.actions" :key="action.code" :disabled="!action.enabled">
              {{ actionLabelMap[action.code] || action.label }}
            </button>
          </div>
          <p class="muted">
            当前轮次先把数据结构和操作入口聚合好，后续再接表单弹窗和真正的提交动作。
          </p>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.page-shell {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 20px 44px;
}

.loading {
  padding: 28px;
}

.hero {
  display: grid;
  grid-template-columns: 1.6fr 0.8fr;
  gap: 18px;
  padding: 28px;
  border-radius: 28px;
  background: linear-gradient(135deg, #1f4033, #325b47 62%, #c7b57d 150%);
  color: #fff7e9;
}

.eyebrow,
.code {
  margin: 0 0 8px;
  color: #f4d68b;
}

h1 {
  margin: 0;
  font-size: 36px;
}

.intro {
  margin-top: 16px;
  max-width: 680px;
  line-height: 1.8;
}

.hero-side {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.08);
}

.status-badge {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(244, 214, 139, 0.18);
  color: #fbe2a6;
  font-weight: 700;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
  margin-top: 18px;
}

.top-grid {
  margin-top: 24px;
}

.panel {
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 18px 36px rgba(31, 62, 47, 0.08);
}

.panel h2,
.panel h3 {
  margin-top: 0;
}

.info-list,
.history-list {
  display: grid;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.info-list li {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(34, 68, 56, 0.08);
}

.info-list span {
  color: #6a8377;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.timeline {
  margin: 0;
  padding-left: 18px;
}

.timeline li {
  margin-bottom: 18px;
  line-height: 1.7;
}

.timeline h3 {
  margin-bottom: 6px;
}

.timeline p,
.timeline small,
.history-list span,
.muted,
.link-row {
  color: #496156;
}

.stack {
  display: grid;
  gap: 12px;
}

.stack-card {
  padding: 16px;
  border-radius: 18px;
  background: #f7fbf4;
}

.tag-list,
.action-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list span {
  padding: 7px 10px;
  border-radius: 999px;
  background: #e9f0d7;
  color: #5f6d29;
  font-size: 12px;
}

.action-list button {
  padding: 10px 14px;
  border: none;
  border-radius: 999px;
  background: #214237;
  color: #fff3df;
  cursor: pointer;
}

.action-list button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.history-list p {
  margin: 6px 0 0;
}

.qr-status {
  font-weight: 700;
}

@media (max-width: 900px) {
  .hero,
  .grid {
    grid-template-columns: 1fr;
  }

  .info-list li {
    flex-direction: column;
  }
}
</style>
