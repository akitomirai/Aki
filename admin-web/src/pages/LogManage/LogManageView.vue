<template>
  <div class="log-manage">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>系统操作日志</span>
          <div class="header-ops">
            <el-input
              v-model="query.module"
              placeholder="模块代码"
              style="width: 150px; margin-right: 10px"
              clearable
              @clear="handleSearch"
            />
            <el-input
              v-model="query.operatorName"
              placeholder="操作人"
              style="width: 150px; margin-right: 10px"
              clearable
              @clear="handleSearch"
            />
            <el-select
              v-model="query.resultStatus"
              placeholder="结果"
              style="width: 120px; margin-right: 10px"
              clearable
              @clear="handleSearch"
            >
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAIL" />
            </el-select>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </div>
        </div>
      </template>

      <el-table :data="logList" v-loading="loading" border stripe empty-text="暂无操作日志">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="roleCode" label="角色" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ getRoleText(row.roleCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120">
          <template #default="{ row }">
            <div class="col-with-sub">
              <span>{{ getLogModuleText(row.module) }}</span>
              <small>{{ row.module || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="150">
          <template #default="{ row }">
            <div class="col-with-sub">
              <span>{{ getLogActionText(row.action) }}</span>
              <small>{{ row.action || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="requestPath" label="请求路径" min-width="200">
          <template #default="{ row }">
            <el-tooltip :content="row.requestPath" placement="top">
              <span class="ellipsis-text">{{ row.requestPath || '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="方法" width="80">
          <template #default="{ row }">
            <el-tag :type="getMethodTag(row.requestMethod)" size="small">
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultStatus" label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="getLogResultTagType(row.resultStatus)" size="small">
              {{ getLogResultText(row.resultStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createdAt" label="操作时间" width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="760px"
      append-to-body
    >
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ getRoleText(detail.roleCode) }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ getLogResultText(detail.resultStatus) }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ getLogModuleText(detail.module) }}</el-descriptions-item>
          <el-descriptions-item label="操作">{{ getLogActionText(detail.action) }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ detail.requestMethod }}</el-descriptions-item>
          <el-descriptions-item label="请求路径">{{ detail.requestPath }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ detail.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="请求参数" :span="2">
            <pre style="white-space: pre-wrap;">{{ detail.requestParams || '-' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="User-Agent" :span="2">
            <pre style="white-space: pre-wrap;">{{ detail.userAgent || '-' }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLogDetailApi, pageLogApi } from '../../api/log'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'
import {
  getLogActionText,
  getLogModuleText,
  getLogResultTagType,
  getLogResultText,
  getRoleText
} from '../../utils/display'
import { extractErrorMessage } from '../../utils/feedback'

const authStore = useAuthStore()
const loading = ref(false)
const logList = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const query = reactive({
  current: 1,
  size: 10,
  module: '',
  operatorName: '',
  resultStatus: ''
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await pageLogApi(query, authStore.user?.roleCode)
    if (res.code === 0) {
      logList.value = res.data?.records || []
      total.value = Number(res.data?.total || 0)
      if (!logList.value.length) {
        ElMessage.info('未查询到数据')
      }
    } else {
      ElMessage.error(`查询失败：${res.message || '日志列表加载失败'}`)
    }
  } catch (error) {
    ElMessage.error(`加载失败：${extractErrorMessage(error, '日志列表加载失败')}`)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.current = 1
  loadLogs()
}

const resetSearch = () => {
  query.module = ''
  query.operatorName = ''
  query.resultStatus = ''
  handleSearch()
}

const getMethodTag = (method) => {
  const map = {
    'GET': 'info',
    'POST': 'success',
    'PUT': 'warning',
    'DELETE': 'danger'
  }
  return map[method] || 'info'
}

const openDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await getLogDetailApi(row.id, authStore.user?.roleCode)
    if (res.code === 0) {
      detail.value = res.data
    } else {
      ElMessage.error(`查询失败：${res.message || '日志详情加载失败'}`)
    }
  } catch (error) {
    ElMessage.error(`加载失败：${extractErrorMessage(error, '日志详情加载失败')}`)
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.log-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.ellipsis-text {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-with-sub {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.col-with-sub small {
  color: #9ca3af;
  font-size: 11px;
}
</style>
