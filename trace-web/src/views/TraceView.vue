<template>
  <div class="page">
    <div class="container">
      <el-card v-if="loading" class="card">
        <el-skeleton :rows="6" animated />
      </el-card>

      <template v-else>
        <el-card v-if="errorMessage" class="card">
          <el-result icon="error" title="查询失败" :sub-title="errorMessage" />
        </el-card>

        <template v-else-if="detail">
          <!-- 顶部摘要 -->
          <el-card class="card hero-card">
            <div class="hero-title">农产品溯源信息</div>
            <div class="hero-subtitle">
              批次编码：{{ detail.timeline?.batch?.batchCode || '-' }}
            </div>

            <div class="hero-grid">
              <div class="hero-item">
                <div class="label">产地</div>
                <div class="value">{{ detail.timeline?.batch?.originPlace || '-' }}</div>
              </div>
              <div class="hero-item">
                <div class="label">状态</div>
                <div class="value">{{ detail.timeline?.batch?.status || '-' }}</div>
              </div>
              <div class="hero-item">
                <div class="label">PV</div>
                <div class="value">{{ detail.pv ?? 0 }}</div>
              </div>
              <div class="hero-item">
                <div class="label">UV</div>
                <div class="value">{{ detail.uv ?? 0 }}</div>
              </div>
            </div>
          </el-card>

          <!-- 质检校验 -->
          <el-card class="card">
            <template #header>
              <span>质检校验状态</span>
            </template>

            <el-tag v-if="detail.qualityVerified === true" type="success" size="large">
              校验通过
            </el-tag>
            <el-tag v-else-if="detail.qualityVerified === false" type="danger" size="large">
              校验失败
            </el-tag>
            <el-tag v-else type="info" size="large">
              暂无数据
            </el-tag>
          </el-card>

          <!-- 基本信息 -->
          <el-card class="card">
            <template #header>
              <span>批次基本信息</span>
            </template>

            <div class="info-list">
              <div class="info-row">
                <span class="info-label">批次编码</span>
                <span class="info-value">{{ detail.timeline?.batch?.batchCode || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">产品ID</span>
                <span class="info-value">{{ detail.timeline?.batch?.productId || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">企业ID</span>
                <span class="info-value">{{ detail.timeline?.batch?.companyId || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">产地</span>
                <span class="info-value">{{ detail.timeline?.batch?.originPlace || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">状态</span>
                <span class="info-value">{{ detail.timeline?.batch?.status || '-' }}</span>
              </div>
            </div>
          </el-card>

          <!-- 时间轴 -->
          <el-card class="card">
            <template #header>
              <span>溯源时间轴</span>
            </template>

            <el-timeline v-if="(detail.timeline?.events || []).length > 0">
              <el-timeline-item
                  v-for="item in detail.timeline.events"
                  :key="item.id"
                  :timestamp="item.eventTime"
                  placement="top"
              >
                <el-card class="event-card">
                  <div class="event-head">
                    <span class="event-stage">{{ stageText(item.stage) }}</span>
                    <span class="event-location">{{ item.location || '未知地点' }}</span>
                  </div>

                  <!-- fields 友好展示 -->
                  <div v-if="extractFields(item).length > 0" class="field-list">
                    <div
                        v-for="field in extractFields(item)"
                        :key="field.key"
                        class="field-row"
                    >
                      <span class="field-key">{{ field.key }}</span>
                      <span class="field-value">{{ field.value }}</span>
                    </div>
                  </div>

                  <!-- INSPECT 报告按钮 -->
                  <div v-if="getReportUrl(item)" class="report-actions">
                    <el-button type="primary" size="small" @click="openReport(item)">
                      查看质检报告
                    </el-button>
                  </div>

                  <!-- 调试/兜底：原始 JSON -->
                  <details class="raw-json">
                    <summary>查看原始数据</summary>
                    <pre class="json-box">{{ formatJson(item.content) }}</pre>
                  </details>
                </el-card>
              </el-timeline-item>
            </el-timeline>

            <el-empty v-else description="暂无溯源事件" />
          </el-card>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup>
/**
 * 消费者查询页（优化版）：
 * - 更适合手机展示
 * - fields 按键值渲染
 * - INSPECT 事件支持“查看报告”
 */
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceByTokenApi } from '../api/trace'

const route = useRoute()

const loading = ref(true)
const errorMessage = ref('')
const detail = ref(null)

async function loadData() {
  loading.value = true
  errorMessage.value = ''

  try {
    const qrToken = route.params.qrToken
    const res = await getTraceByTokenApi(qrToken)

    if (res.code === 0) {
      detail.value = res.data
    } else {
      errorMessage.value = res.message || '查询失败'
    }
  } catch (error) {
    errorMessage.value = '查询请求失败'
  } finally {
    loading.value = false
  }
}

function stageText(stage) {
  const map = {
    PRODUCE: '生产',
    PROCESS: '加工',
    INSPECT: '检验',
    WAREHOUSE: '仓储',
    LOGISTICS: '物流',
    SALE: '销售'
  }
  return map[stage] || stage
}

/**
 * 从事件内容中提取 fields，转成前端可渲染数组
 */
function extractFields(item) {
  const fields = item?.content?.fields
  if (!fields || typeof fields !== 'object') return []

  return Object.keys(fields).map((key) => ({
    key: fieldText(key),
    value: stringifyValue(fields[key])
  }))
}

/**
 * 字段名中文化（可继续补充）
 */
function fieldText(key) {
  const map = {
    workType: '操作类型',
    material: '投入品',
    amount: '数量',
    operatorName: '操作人',
    remark: '备注',
    carrier: '承运方',
    trackingNo: '物流单号',
    status: '状态',
    reportId: '报告ID',
    reportNo: '报告编号',
    agency: '检测机构',
    result: '检测结果',
    reportFileUrl: '报告地址'
  }
  return map[key] || key
}

function stringifyValue(value) {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

/**
 * 如果是 INSPECT 事件，尝试获取报告链接
 */
function getReportUrl(item) {
  return item?.content?.fields?.reportFileUrl || ''
}

function openReport(item) {
  const url = getReportUrl(item)
  if (!url) return

  // 后端本地文件映射是 /files/**，这里补上服务地址
  const fullUrl = url.startsWith('http') ? url : `http://localhost:8080${url}`
  window.open(fullUrl, '_blank')
}

function formatJson(obj) {
  if (!obj) return '-'
  try {
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return String(obj)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 12px;
}

.container {
  max-width: 720px;
  margin: 0 auto;
}

.card {
  margin-bottom: 12px;
  border-radius: 12px;
}

.hero-card {
  background: linear-gradient(135deg, #ffffff, #f7fbff);
}

.hero-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
}

.hero-subtitle {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.hero-item {
  padding: 12px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
}

.label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ebeef5;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #303133;
  text-align: right;
  word-break: break-all;
}

.event-card {
  border-radius: 10px;
}

.event-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.event-stage {
  font-weight: 700;
  color: #303133;
}

.event-location {
  font-size: 12px;
  color: #909399;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  background: #f9fafc;
  border-radius: 8px;
}

.field-key {
  color: #606266;
  font-size: 13px;
}

.field-value {
  color: #303133;
  font-size: 13px;
  text-align: right;
  word-break: break-all;
}

.report-actions {
  margin-top: 12px;
}

.raw-json {
  margin-top: 12px;
}

.raw-json summary {
  cursor: pointer;
  color: #409eff;
  font-size: 13px;
}

.json-box {
  margin-top: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 480px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .event-head,
  .field-row,
  .info-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-value,
  .field-value {
    text-align: left;
  }
}
</style>