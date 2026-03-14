<template>
  <div class="dashboard">
    <el-row :gutter="20" class="dashboard-cards">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="card-title">在管批次数</div>
          <div class="card-value">{{ batchCount }}</div>
          <div class="card-desc">当前系统可管理的批次总数</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card clickable-card" shadow="hover" @click="goBatches">
          <div class="card-title">已生成追溯码</div>
          <div class="card-value">{{ qrCount }}</div>
          <div class="card-desc">点击可前往批次管理查看追溯入口</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="card-title">
            累计扫码次数（PV）
            <el-tooltip content="消费者累计访问次数" placement="top">
              <el-icon class="tip-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="card-value">{{ pvCount }}</div>
          <div class="card-desc">PV：累计访问次数</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="card-title">
            累计访客人数（UV）
            <el-tooltip content="消费者去重访问人数" placement="top">
              <el-icon class="tip-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="card-value">{{ uvCount }}</div>
          <div class="card-desc">UV：去重后的访问人数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="dashboard-section" shadow="hover">
      <template #header>
        <div class="section-header">
          <span>公开追溯快捷入口</span>
          <el-tag size="small" type="success">消费者扫码页</el-tag>
        </div>
      </template>
      <div class="trace-entry">
        <el-input v-model="traceToken" placeholder="请输入追溯 token，例如 test-token-2026" class="trace-token-input" />
        <el-button type="primary" @click="openTraceLink">查看公开追溯页</el-button>
        <el-button @click="copyTraceLink">复制追溯链接</el-button>
      </div>
      <div class="trace-link-tip">
        当前链接：{{ traceLink }}
      </div>
    </el-card>

    <el-card class="dashboard-section">
      <template #header>
        <span>系统溯源流程</span>
      </template>
      <ol class="dashboard-list">
        <li>企业创建产品批次</li>
        <li>录入生产事件</li>
        <li>上传质检报告</li>
        <li>生成溯源二维码</li>
        <li>消费者扫码查看溯源信息</li>
      </ol>
    </el-card>

    <el-card class="dashboard-section">
      <template #header>
        <span>系统功能</span>
      </template>
      <ul class="dashboard-list">
        <li>批次管理</li>
        <li>生产事件记录</li>
        <li>二维码生成与追溯</li>
        <li>扫码访问统计（PV / UV）</li>
        <li>质检报告管理</li>
        <li>文件上传与存证</li>
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
import { buildTraceLink } from '../../utils/display'

const { batchCount, qrCount, pvCount, uvCount } = useDashboard()
const router = useRouter()
const traceToken = ref('test-token-2026')

const traceLink = computed(() => buildTraceLink(traceToken.value))

const goBatches = () => {
  router.push('/batches')
}

const openTraceLink = () => {
  if (!traceToken.value) {
    ElMessage.warning('请先输入追溯 token')
    return
  }
  window.open(traceLink.value, '_blank')
}

const copyTraceLink = async () => {
  if (!traceToken.value) {
    ElMessage.warning('请先输入追溯 token')
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
