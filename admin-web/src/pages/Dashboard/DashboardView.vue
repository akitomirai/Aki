<template>
  <div class="dashboard">
    <el-row :gutter="20" class="dashboard-cards">
      <el-col v-for="card in statCards" :key="card.key" :span="6">
        <el-card
          class="stat-card"
          :class="{ 'clickable-card': card.clickable }"
          shadow="hover"
          @click="card.clickable ? card.action() : undefined"
        >
          <div class="card-title">
            <span>{{ card.title }}</span>
            <el-tooltip v-if="card.tooltip" :content="card.tooltip" placement="top">
              <el-icon class="tip-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="card-value">{{ card.value }}</div>
          <div class="card-desc">{{ card.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="dashboard-section trace-shortcut-card" shadow="hover">
      <div class="trace-shortcut">
        <div class="trace-shortcut__content">
          <div class="trace-shortcut__title-row">
            <div>
              <p class="trace-shortcut__eyebrow">公开追溯页快捷入口</p>
              <h3 class="trace-shortcut__title">输入追溯码后，直接打开消费者可见页面</h3>
            </div>
            <el-tag type="success" effect="dark">答辩演示入口</el-tag>
          </div>
          <p class="trace-shortcut__desc">
            可用于老师现场演示扫码结果。输入 token 后可直接打开公开页，或复制完整追溯链接。
          </p>
          <div class="trace-shortcut__actions">
            <el-input
              v-model="traceToken"
              placeholder="请输入追溯码 token，例如 test-token-2026"
              clearable
              class="trace-token-input"
            />
            <el-button type="primary" @click="openTraceLink">打开公开追溯页</el-button>
            <el-button @click="copyTraceLink">复制追溯链接</el-button>
          </div>
          <div class="trace-shortcut__link">
            <span class="trace-shortcut__label">当前链接</span>
            <el-tooltip :content="traceLink || '请输入追溯码后生成链接'" placement="top">
              <span class="trace-shortcut__value">{{ traceLink || '请输入追溯码后生成链接' }}</span>
            </el-tooltip>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="dashboard-section" shadow="hover">
      <template #header>
        <div class="section-header">
          <span>系统溯源流程</span>
          <el-tag size="small" type="primary" effect="plain">老师快速理解</el-tag>
        </div>
      </template>
      <ol class="dashboard-list">
        <li>企业创建产品批次并录入基础信息</li>
        <li>按业务过程补充生产、运输、加工等节点</li>
        <li>上传质检与监管信息，形成可展示档案</li>
        <li>生成追溯码并对外发布</li>
        <li>消费者或老师通过追溯页查看完整档案</li>
      </ol>
    </el-card>

    <el-card class="dashboard-section" shadow="hover">
      <template #header>
        <span>当前系统覆盖能力</span>
      </template>
      <ul class="dashboard-list">
        <li>批次与产品管理</li>
        <li>业务节点记录</li>
        <li>追溯码生成与公开展示</li>
        <li>扫码访问统计（PV / UV）</li>
        <li>质检报告与监管记录管理</li>
        <li>文件上传与业务资料存证</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { useDashboard } from '../../composables/useDashboard'
import { buildTraceLink, normalizeDisplayNumber } from '../../utils/display'

const { batchCount, qrCount, pvCount, uvCount } = useDashboard()
const router = useRouter()
const traceToken = ref('test-token-2026')

const traceLink = computed(() => buildTraceLink(traceToken.value.trim()))

const goBatches = () => {
  router.push('/batches')
}

const formatCount = (value) => normalizeDisplayNumber(value, '0')

const statCards = computed(() => [
  {
    key: 'batch',
    title: '在管批次数',
    value: formatCount(batchCount.value),
    desc: Number(batchCount.value || 0) > 0 ? '当前系统内可继续维护的批次总数。' : '当前还没有批次数据，可先从产品或批次管理开始录入。'
  },
  {
    key: 'qr',
    title: '已生成追溯码',
    value: formatCount(qrCount.value),
    desc: Number(qrCount.value || 0) > 0 ? '已可用于公开查询或打印展示的追溯码数量。' : '暂未生成追溯码，批次发布后即可继续生成。',
    clickable: true,
    action: goBatches
  },
  {
    key: 'pv',
    title: '累计扫码次数 PV',
    value: formatCount(pvCount.value),
    desc: Number(pvCount.value || 0) > 0 ? '表示追溯页被打开的总次数，重复访问也会累计。' : '暂时还没有扫码访问记录，后续扫码后会自动累计。',
    tooltip: 'PV 表示页面总访问次数，同一人重复打开也会累计。'
  },
  {
    key: 'uv',
    title: '累计访客人数 UV',
    value: formatCount(uvCount.value),
    desc: Number(uvCount.value || 0) > 0 ? '表示去重后的访客人数，更接近实际查看人数。' : '暂时还没有访客数据，后续扫码后会自动统计。',
    tooltip: 'UV 表示去重后的访问人数，更适合说明有多少人查看过。'
  }
])

const openTraceLink = () => {
  if (!traceToken.value.trim()) {
    ElMessage.warning('请先输入追溯码 token')
    return
  }
  window.open(traceLink.value, '_blank')
}

const copyTraceLink = async () => {
  if (!traceToken.value.trim()) {
    ElMessage.warning('请先输入追溯码 token')
    return
  }
  try {
    await navigator.clipboard.writeText(traceLink.value)
    ElMessage.success('追溯链接已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style src="./dashboard.css" scoped></style>
