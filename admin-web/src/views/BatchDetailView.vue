<template>
  <AdminLayout>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>批次基本信息</span>
          </template>

          <div v-if="batch">
            <p><strong>ID：</strong>{{ batch.id }}</p>
            <p><strong>批次编码：</strong>{{ batch.batchCode }}</p>
            <p><strong>产品ID：</strong>{{ batch.productId }}</p>
            <p><strong>企业ID：</strong>{{ batch.companyId }}</p>
            <p><strong>产地：</strong>{{ batch.originPlace }}</p>
            <p><strong>状态：</strong>{{ batch.status }}</p>
            <p><strong>备注：</strong>{{ batch.remark }}</p>
          </div>

          <el-empty v-else description="暂无批次信息" />
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>质检校验状态</span>
          </template>

          <el-tag v-if="qualityVerified === true" type="success" size="large">
            校验通过
          </el-tag>

          <el-tag v-else-if="qualityVerified === false" type="danger" size="large">
            校验失败
          </el-tag>

          <el-tag v-else type="info" size="large">
            暂无数据
          </el-tag>
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>新增事件</span>
          </template>

          <el-form :model="eventForm" label-width="80px">
            <el-form-item label="环节">
              <el-select v-model="eventForm.stage" style="width: 100%;">
                <el-option label="生产" value="PRODUCE" />
                <el-option label="加工" value="PROCESS" />
                <el-option label="检验" value="INSPECT" />
                <el-option label="仓储" value="WAREHOUSE" />
                <el-option label="物流" value="LOGISTICS" />
                <el-option label="销售" value="SALE" />
              </el-select>
            </el-form-item>

            <el-form-item label="地点">
              <el-input v-model="eventForm.location" placeholder="如：江西赣州-基地A" />
            </el-form-item>

            <el-form-item label="内容JSON">
              <el-input
                  v-model="eventJsonText"
                  type="textarea"
                  :rows="8"
                  placeholder='例如：{"fields":{"workType":"施肥","amount":"10kg"}}'
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="eventLoading" @click="handleCreateEvent">
                新增事件
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>生成二维码</span>
          </template>

          <el-form :model="qrForm" label-width="80px">
            <el-form-item label="数量">
              <el-input-number v-model="qrForm.count" :min="1" :max="20" style="width: 100%;" />
            </el-form-item>

            <el-form-item label="过期天数">
              <el-input-number v-model="qrForm.expireDays" :min="0" :max="3650" style="width: 100%;" />
            </el-form-item>

            <el-form-item label="备注">
              <el-input v-model="qrForm.remark" placeholder="如：演示用二维码" />
            </el-form-item>

            <el-form-item>
              <el-button type="success" :loading="qrLoading" @click="handleGenerateQr">
                生成二维码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <span>二维码列表（PV / UV）</span>
          </template>

          <el-table :data="qrList" border style="width: 100%; margin-bottom: 20px;">
            <el-table-column prop="id" label="二维码ID" min-width="180" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="pv" label="PV" width="80" />
            <el-table-column prop="uv" label="UV" width="80" />
            <el-table-column prop="remark" label="备注" min-width="120" />

            <el-table-column label="操作" min-width="520">
              <template #default="{ row }">
                <div class="qr-action-row">
                  <el-button size="small" @click="handleCopyQrLink(row)">
                    复制链接
                  </el-button>
                  <el-button size="small" @click="handleCopyQrToken(row)">
                    复制Token
                  </el-button>
                  <el-button size="small" @click="handleShowQr(row)">
                    查看码
                  </el-button>
                  <el-button size="small" @click="handleShowTrend(row)">
                    查看趋势
                  </el-button>
                  <el-button size="small" type="primary" plain @click="handleOpenQrLink(row)">
                    打开
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="qrList.length === 0" description="暂无二维码" />
        </el-card>
        <el-card style="margin-top: 20px;">
          <template #header>
            <span>溯源事件时间轴</span>
          </template>

          <el-timeline v-if="events.length > 0">
            <el-timeline-item
                v-for="item in events"
                :key="item.id"
                :timestamp="item.eventTime"
                placement="top"
            >
              <el-card>
                <p><strong>环节：</strong>{{ item.stage }}</p>
                <p><strong>地点：</strong>{{ item.location || '-' }}</p>
                <pre class="json-box">{{ formatJson(item.contentJson) }}</pre>
              </el-card>
            </el-timeline-item>
          </el-timeline>

          <el-empty v-else description="暂无事件记录" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
        v-model="qrDialogVisible"
        title="二维码预览"
        width="360px"
        destroy-on-close
    >
      <div class="qr-dialog-body">
        <img v-if="qrImageUrl" :src="qrImageUrl" alt="二维码" class="qr-image" />
        <el-empty v-else description="二维码生成中" />

        <div class="qr-link-box" v-if="currentQrLink">
          {{ currentQrLink }}
        </div>

        <div class="qr-actions">
          <el-button @click="handleCopyCurrentQrLink">复制链接</el-button>
          <el-button type="primary" @click="handleDownloadQr">下载 PNG</el-button>
        </div>
      </div>
    </el-dialog>
    <el-dialog
        v-model="trendDialogVisible"
        title="二维码趋势"
        width="720px"
        destroy-on-close
        @opened="handleTrendDialogOpened"
    >
      <div class="trend-header">
        <span>二维码ID：{{ currentTrendQrId || '-' }}</span>
        <el-button size="small" @click="loadQrTrend">刷新</el-button>
      </div>

      <div v-if="trendLoading" style="padding: 20px;">
        <el-skeleton :rows="4" animated />
      </div>

      <el-empty
          v-show="!trendLoading && trendData.length === 0"
          description="暂无趋势数据"
      />

      <div
          v-show="!trendLoading && trendData.length > 0"
          ref="trendChartRef"
          class="trend-chart"
      ></div>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
/**
 * 批次详情页：
 * - 批次信息
 * - 质检校验状态
 * - 二维码列表（PV/UV）
 * - 事件时间轴
 * - 新增事件
 * - 生成二维码
 *
 * 重要约定：
 * - 前端 ID 一律按 String 处理，避免 JS 大整数精度丢失
 */
import * as echarts from 'echarts'
import { onMounted, ref, reactive, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '../components/AdminLayout.vue'

import { getBatchDetailApi } from '../api/batch'
import { listEventApi, createEventApi } from '../api/event'
import { listQrApi, generateQrApi, getQrTrendApi } from '../api/qr'
import { verifyQualityApi } from '../api/quality'
import QRCode from 'qrcode'

const route = useRoute()

const batch = ref(null)
const events = ref([])
const qrList = ref([])
const qualityVerified = ref(null)
const eventLoading = ref(false)
const qrLoading = ref(false)
const qrDialogVisible = ref(false)
const qrImageUrl = ref('')
const currentQrLink = ref('')
const currentQrId = ref('')
const trendDialogVisible = ref(false)
const trendLoading = ref(false)
const currentTrendQrId = ref('')
const trendData = ref([])
const trendChartRef = ref(null)

let trendChartInstance = null

const eventForm = reactive({
  stage: 'PRODUCE',
  location: ''
})

const eventJsonText = ref('{\n  "fields": {\n    "workType": "施肥",\n    "amount": "10kg"\n  }\n}')

const qrForm = reactive({
  count: 1,
  expireDays: 30,
  remark: '详情页生成'
})

async function loadBatch() {
  try {
    const res = await getBatchDetailApi(route.params.id)
    if (res.code === 0) {
      batch.value = res.data
    } else {
      ElMessage.error(res.message || '加载批次详情失败')
    }
  } catch (error) {
    ElMessage.error('加载批次详情失败')
  }
}

async function loadEvents() {
  try {
    const res = await listEventApi(route.params.id)
    if (res.code === 0) {
      events.value = res.data || []
    } else {
      ElMessage.error(res.message || '加载事件失败')
    }
  } catch (error) {
    ElMessage.error('加载事件失败')
  }
}

async function loadQrList() {
  try {
    const res = await listQrApi(route.params.id)
    if (res.code === 0) {
      qrList.value = res.data || []
    } else {
      ElMessage.error(res.message || '加载二维码失败')
    }
  } catch (error) {
    ElMessage.error('加载二维码失败')
  }
}

async function loadQualityVerify() {
  try {
    const res = await verifyQualityApi(route.params.id)
    if (res.code === 0) {
      qualityVerified.value = res.data.qualityVerified
    } else {
      ElMessage.error(res.message || '加载质检状态失败')
    }
  } catch (error) {
    ElMessage.error('加载质检状态失败')
  }
}

/**
 * 新增事件
 * 注意：batchId 保持字符串，不做 Number() 转换
 */
async function handleCreateEvent() {
  let content
  try {
    content = JSON.parse(eventJsonText.value)
  } catch (e) {
    ElMessage.error('内容JSON格式不正确')
    return
  }

  eventLoading.value = true
  try {
    const res = await createEventApi({
      batchId: route.params.id,
      stage: eventForm.stage,
      location: eventForm.location,
      content,
      attachments: []
    })

    if (res.code === 0) {
      ElMessage.success('事件新增成功')
      eventForm.location = ''
      await loadEvents()
    } else {
      ElMessage.error(res.message || '新增事件失败')
    }
  } catch (error) {
    ElMessage.error('新增事件失败')
  } finally {
    eventLoading.value = false
  }
}

/**
 * 生成二维码
 * 注意：batchId 保持字符串，不做 Number() 转换
 */
async function handleGenerateQr() {
  qrLoading.value = true
  try {
    const res = await generateQrApi({
      batchId: route.params.id,
      count: qrForm.count,
      expireDays: qrForm.expireDays,
      remark: qrForm.remark
    })

    if (res.code === 0) {
      ElMessage.success(`成功生成 ${res.data?.length || 0} 个二维码`)
      await loadQrList()
    } else {
      ElMessage.error(res.message || '生成二维码失败')
    }
  } catch (error) {
    ElMessage.error('生成二维码失败')
  } finally {
    qrLoading.value = false
  }
}

/**
 * 构造前台查询页链接
 * 说明：改为 trace-web 页面地址，而不是后端 JSON 接口地址
 */
const TRACE_ORIGIN = import.meta.env.VITE_TRACE_WEB_ORIGIN || 'http://localhost:5173'

function buildQrLink(row) {
  return `${TRACE_ORIGIN}/t/${row.qrToken}`
}

/**
 * 复制二维码查询链接
 */
async function handleCopyQrLink(row) {
  const link = buildQrLink(row)
  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success('二维码查询链接已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

/**
 * 打开二维码查询链接
 */
function handleOpenQrLink(row) {
  const link = buildQrLink(row)
  window.open(link, '_blank')
}

function formatJson(jsonStr) {
  if (!jsonStr) return '-'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch (e) {
    return jsonStr
  }
}

/**
 * 复制 qrToken（便于调试）
 */
async function handleCopyQrToken(row) {
  try {
    await navigator.clipboard.writeText(row.qrToken)
    ElMessage.success('二维码Token已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

/**
 * 显示二维码弹窗，并生成二维码图片
 */
async function handleShowQr(row) {
  const link = buildQrLink(row)
  currentQrLink.value = link
  currentQrId.value = row.id
  qrImageUrl.value = ''
  qrDialogVisible.value = true

  try {
    qrImageUrl.value = await QRCode.toDataURL(link, {
      width: 240,
      margin: 2
    })
  } catch (e) {
    ElMessage.error('二维码生成失败')
  }
}

/**
 * 复制弹窗中的当前二维码链接
 */
async function handleCopyCurrentQrLink() {
  if (!currentQrLink.value) return
  try {
    await navigator.clipboard.writeText(currentQrLink.value)
    ElMessage.success('链接已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

/**
 * 下载当前二维码 PNG
 */
function handleDownloadQr() {
  if (!qrImageUrl.value) {
    ElMessage.warning('二维码尚未生成完成')
    return
  }

  const a = document.createElement('a')
  a.href = qrImageUrl.value
  a.download = `qr-${currentQrId.value || 'code'}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
/**
 * 打开趋势弹窗
 */
async function handleShowTrend(row) {
  currentTrendQrId.value = row.id
  trendDialogVisible.value = true
  await loadQrTrend()
}

/**
 * 弹窗打开完成后：
 * - 确保 DOM 已经渲染
 * - 如果已有数据，重绘一次图表
 */
async function handleTrendDialogOpened() {
  await nextTick()
  renderTrendChart()
}

/**
 * 加载二维码趋势数据
 */
async function loadQrTrend() {
  if (!currentTrendQrId.value) return

  trendLoading.value = true
  try {
    const res = await getQrTrendApi(currentTrendQrId.value, 7)
    if (res.code === 0) {
      trendData.value = res.data || []
      await nextTick()
      renderTrendChart()
    } else {
      ElMessage.error(res.message || '加载趋势失败')
    }
  } catch (error) {
    ElMessage.error('加载趋势失败')
  } finally {
    trendLoading.value = false
    await nextTick()
    renderTrendChart()
  }
}

/**
 * 获取/初始化图表实例
 */
function ensureTrendChartInstance() {
  if (!trendChartRef.value) return null

  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChartRef.value)
  }

  return trendChartInstance
}

/**
 * 渲染趋势图
 * 兼容两种后端返回：
 * 1) [{ day, pv }]
 * 2) [{ day, pv, uv }]
 */
function renderTrendChart() {
  if (!trendChartRef.value || trendData.value.length === 0) return

  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChartRef.value)
  }

  const xData = trendData.value.map(item => item.day)
  const pvData = trendData.value.map(item => Number(item.pv || 0))
  const hasUv = trendData.value.some(item => item.uv !== undefined && item.uv !== null)
  const uvData = trendData.value.map(item => Number(item.uv || 0))

  const series = [
    {
      name: 'PV',
      type: 'line',
      smooth: true,
      data: pvData
    }
  ]

  if (hasUv) {
    series.push({
      name: 'UV',
      type: 'line',
      smooth: true,
      data: uvData
    })
  }

  trendChartInstance.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: hasUv ? ['PV', 'UV'] : ['PV']
    },
    xAxis: {
      type: 'category',
      data: xData
    },
    yAxis: {
      type: 'value'
    },
    series
  })
}

onMounted(() => {
  loadBatch()
  loadEvents()
  loadQrList()
  loadQualityVerify()
})
</script>

<style scoped>
.json-box {
  margin-top: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.qr-action-row {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  white-space: nowrap;
  flex-wrap: nowrap;
}

.qr-action-row .el-button {
  flex-shrink: 0;
}

/* 可选：隐藏滚动条更美观 */
.qr-action-row::-webkit-scrollbar {
  display: none;
}
.qr-dialog-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.qr-image {
  width: 240px;
  height: 240px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  padding: 8px;
}

.qr-link-box {
  width: 100%;
  max-height: 80px;
  overflow: auto;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 12px;
  word-break: break-all;
}

.qr-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.trend-chart {
  width: 100%;
  height: 360px;
}
</style>