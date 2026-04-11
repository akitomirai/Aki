<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCompanyOptions } from '../../api/batch'
import { getOperationLogs } from '../../api/log'
import { useAuthStore } from '../../stores/auth'
import { getFriendlyErrorMessage } from '../../utils/batchExperience'

const authStore = useAuthStore()

const loading = ref(false)
const companyLoading = ref(false)
const message = ref('')
const messageType = ref('info')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const companyOptions = ref([])
const filters = ref(createFilters())
const detailDialog = ref(createDetailDialogState())

const isPlatformAdmin = computed(() => String(authStore.user?.roleCode || '').toUpperCase() === 'PLATFORM_ADMIN')
const isEnterpriseAdmin = computed(() => String(authStore.user?.roleCode || '').toUpperCase() === 'ENTERPRISE_ADMIN')
const companyFilterEnabled = computed(() => isPlatformAdmin.value)
const currentCompanyName = computed(() => authStore.user?.companyName || '当前企业')
const pageTitle = computed(() => isEnterpriseAdmin.value ? '本企业操作日志' : '操作日志')
const pageDesc = computed(() => {
  if (isEnterpriseAdmin.value) {
    return `只查看 ${currentCompanyName.value} 的关键后台留痕，方便回查本企业内的资料维护、批次流转和风险处置动作。`
  }
  return '统一查看关键后台留痕，便于按时间、操作人和企业快速回看近期动作。'
})
const scopedModeHint = computed(() => `当前只展示 ${currentCompanyName.value} 的操作日志，所属企业范围已固定。`)
const pageCount = computed(() => Math.max(1, Math.ceil(Number(total.value || 0) / Number(pageSize.value || 20))))
const pageSummary = computed(() => {
  if (!total.value) {
    return '当前没有可展示的操作日志。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(total.value, page.value * pageSize.value)
  return `共 ${total.value} 条日志，当前显示 ${from}-${to} 条。`
})

const actionOptions = [
  { value: '', label: '全部操作类型' },
  { value: 'AUTH_LOGIN_SUCCESS', label: '登录成功' },
  { value: 'USER_CREATE', label: '新建用户' },
  { value: 'USER_UPDATE', label: '编辑用户' },
  { value: 'USER_ENABLE', label: '启用用户' },
  { value: 'USER_DISABLE', label: '停用用户' },
  { value: 'USER_RESET_PASSWORD', label: '重置密码' },
  { value: 'BATCH_ASSIGN', label: '分配操作员' },
  { value: 'BATCH_REASSIGN', label: '改派操作员' },
  { value: 'BATCH_UNASSIGN', label: '清空分配' },
  { value: 'QUALITY_UPLOAD', label: '上传质检' },
  { value: 'QR_GENERATE', label: '生成二维码' },
  { value: 'BATCH_PUBLISH', label: '发布批次' },
  { value: 'RISK_FREEZE', label: '冻结批次' },
  { value: 'RISK_COMMENT', label: '补处理说明' },
  { value: 'RISK_RECTIFICATION', label: '补整改记录' },
  { value: 'RISK_PROCESSING', label: '标记处理中' },
  { value: 'RISK_RECTIFIED', label: '标记已整改' },
  { value: 'RISK_RESUME_PUBLISH', label: '恢复发布' },
  { value: 'TRACE_RECORD_SUBMIT', label: '提交现场记录' }
]

const roleOptions = [
  { value: '', label: '全部角色' },
  { value: 'PLATFORM_ADMIN', label: '平台管理员' },
  { value: 'ENTERPRISE_ADMIN', label: '企业管理员' },
  { value: 'OPERATOR', label: '现场操作员' },
  { value: 'REGULATOR', label: '监管人员' }
]

const resultOptions = [
  { value: '', label: '全部结果' },
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAILED', label: '失败' }
]

onMounted(async () => {
  await Promise.all([loadCompanyOptions(), fetchRows(1)])
})

function createFilters() {
  return {
    actionType: '',
    operatorKeyword: '',
    roleCode: '',
    companyId: '',
    result: '',
    dateFrom: '',
    dateTo: ''
  }
}

function createDetailDialogState() {
  return {
    visible: false,
    item: null
  }
}

function cleanObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, item]) => item !== '' && item !== null && item !== undefined)
  )
}

function normalizeCompanyId(value) {
  if (value === '' || value === null || value === undefined) {
    return undefined
  }
  return Number(value)
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function clearMessage() {
  message.value = ''
}

async function loadCompanyOptions() {
  if (!companyFilterEnabled.value) {
    companyOptions.value = []
    return
  }
  companyLoading.value = true
  try {
    const response = await getCompanyOptions()
    companyOptions.value = response.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '企业选项加载失败，请稍后重试。'), 'error')
  } finally {
    companyLoading.value = false
  }
}

async function fetchRows(targetPage = page.value) {
  loading.value = true
  clearMessage()
  try {
    const response = await getOperationLogs(cleanObject({
      actionType: filters.value.actionType || undefined,
      operatorKeyword: filters.value.operatorKeyword || undefined,
      roleCode: filters.value.roleCode || undefined,
      companyId: companyFilterEnabled.value ? normalizeCompanyId(filters.value.companyId) : undefined,
      result: filters.value.result || undefined,
      dateFrom: filters.value.dateFrom || undefined,
      dateTo: filters.value.dateTo || undefined,
      page: targetPage,
      pageSize: pageSize.value
    }))
    const payload = response.data ?? {}
    rows.value = payload.items ?? []
    total.value = Number(payload.total || 0)
    page.value = Number(payload.page || targetPage || 1)
    pageSize.value = Number(payload.pageSize || pageSize.value || 20)
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '操作日志加载失败，请稍后重试。'), 'error')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = createFilters()
  fetchRows(1)
}

function openDetailDialog(item) {
  detailDialog.value = {
    visible: true,
    item
  }
}

function closeDetailDialog() {
  detailDialog.value = createDetailDialogState()
}

function goPrevPage() {
  if (loading.value || page.value <= 1) return
  fetchRows(page.value - 1)
}

function goNextPage() {
  if (loading.value || page.value >= pageCount.value) return
  fetchRows(page.value + 1)
}

function companyText(item) {
  return item.companyName || '平台范围'
}

function actionTone(actionType) {
  const code = String(actionType || '').toUpperCase()
  if (code.startsWith('RISK_')) return 'is-archived'
  if (code.startsWith('USER_') || code.startsWith('AUTH_')) return 'is-enabled'
  return 'is-disabled'
}

function resultTone(result) {
  return String(result || '').toUpperCase() === 'FAILED' ? 'is-disabled' : 'is-enabled'
}

function roleTone(roleCode) {
  return {
    PLATFORM_ADMIN: 'is-enabled',
    ENTERPRISE_ADMIN: 'is-archived',
    OPERATOR: 'is-enabled',
    REGULATOR: 'is-disabled'
  }[String(roleCode || '').toUpperCase()] || 'is-archived'
}

function summaryPreview(item) {
  const text = String(item.summary || '')
  if (text.length <= 52) {
    return text
  }
  return `${text.slice(0, 52)}...`
}
</script>

<template>
  <div class="manage-page logs-manage" data-testid="logs-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">{{ pageTitle }}</h1>
        <p class="manage-page-desc">{{ pageDesc }}</p>
      </div>
      <div class="manage-page-actions">
        <el-button data-testid="logs-refresh-button" @click="fetchRows(page)" :loading="loading">刷新</el-button>
      </div>
    </section>

    <el-card v-if="isEnterpriseAdmin" shadow="never" class="profile-banner" data-testid="logs-self-mode">
      <strong>当前为本企业日志模式</strong>
      <span>{{ scopedModeHint }}</span>
    </el-card>

    <el-card shadow="never" class="manage-filter-card">
      <div class="manage-filter-grid logs-filter-grid">
        <el-select v-model="filters.actionType" class="manage-filter-item" data-testid="logs-filter-action" placeholder="操作类型">
          <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-input
          v-model.trim="filters.operatorKeyword"
          class="manage-filter-item"
          data-testid="logs-filter-operator"
          placeholder="输入操作人姓名"
          @keyup.enter="fetchRows(1)"
        />

        <el-select v-model="filters.roleCode" class="manage-filter-item" data-testid="logs-filter-role" placeholder="角色">
          <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-select
          v-if="companyFilterEnabled"
          v-model="filters.companyId"
          class="manage-filter-item"
          data-testid="logs-filter-company"
          placeholder="所属企业"
          :loading="companyLoading"
          clearable
          filterable
        >
          <el-option value="" label="全部企业" />
          <el-option v-for="item in companyOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>

        <el-input
          v-else
          class="manage-filter-item"
          :model-value="currentCompanyName"
          data-testid="logs-company-fixed"
          disabled
        />

        <el-select v-model="filters.result" class="manage-filter-item" data-testid="logs-filter-result" placeholder="结果">
          <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-input v-model="filters.dateFrom" class="manage-filter-item" data-testid="logs-filter-date-from" type="date" />
        <el-input v-model="filters.dateTo" class="manage-filter-item" data-testid="logs-filter-date-to" type="date" />

        <div class="summary-slot">
          <span class="manage-muted">{{ pageSummary }}</span>
        </div>

        <el-button type="primary" data-testid="logs-search-button" @click="fetchRows(1)">查询</el-button>
        <el-button data-testid="logs-reset-button" @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <el-card shadow="never" class="manage-table-card">
      <template #header>
        <div class="manage-table-header">
          <div>
            <p class="manage-table-title">日志台账</p>
            <p class="manage-table-tip">按时间、操作人、企业、操作类型和结果回看关键后台动作，内部编码不直接暴露在主列表中。</p>
          </div>
        </div>
      </template>

      <el-table
        :data="rows"
        v-loading="loading"
        border
        stripe
        empty-text="当前筛选下没有日志"
        data-testid="logs-table"
      >
        <el-table-column label="操作时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.createdAt || '暂无时间' }}
          </template>
        </el-table-column>

        <el-table-column label="操作人" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.operatorName || '系统用户' }}
          </template>
        </el-table-column>

        <el-table-column label="角色" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="manage-status-tag" :class="roleTone(row.roleCode)">
              {{ row.roleName || '系统用户' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="所属企业" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ companyText(row) }}
          </template>
        </el-table-column>

        <el-table-column label="操作类型" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="manage-status-tag" :class="actionTone(row.actionType)">
              {{ row.actionTypeLabel || '系统操作' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作对象" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.targetDisplay || '系统对象' }}
          </template>
        </el-table-column>

        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="manage-status-tag" :class="resultTone(row.result)">
              {{ row.resultLabel || '成功' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="摘要" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="summary-cell">
              <span>{{ summaryPreview(row) || '暂无摘要' }}</span>
              <el-button type="primary" link class="table-action-link" :data-testid="`logs-detail-${row.id}`" @click="openDetailDialog(row)">
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="toolbar logs-pagination">
        <span class="list-summary">第 {{ page }} / {{ pageCount }} 页</span>
        <div class="toolbar-actions">
          <el-button data-testid="logs-prev-page" :disabled="loading || page <= 1" @click="goPrevPage">上一页</el-button>
          <el-button data-testid="logs-next-page" :disabled="loading || page >= pageCount" @click="goNextPage">下一页</el-button>
        </div>
      </div>
    </el-card>

    <div v-if="detailDialog.visible" class="dialog-mask" @click.self="closeDetailDialog">
      <section class="dialog-card logs-detail-dialog" data-testid="logs-detail-dialog">
        <div class="dialog-head">
          <div>
            <h3>日志详情</h3>
          </div>
          <button class="ghost" @click="closeDetailDialog">关闭</button>
        </div>

        <div class="overview-grid logs-detail-grid">
          <div>
            <span>操作时间</span>
            <strong>{{ detailDialog.item?.createdAt || '暂无时间' }}</strong>
          </div>
          <div>
            <span>操作人</span>
            <strong>{{ detailDialog.item?.operatorName || '系统用户' }}</strong>
          </div>
          <div>
            <span>角色</span>
            <strong>{{ detailDialog.item?.roleName || '系统用户' }}</strong>
          </div>
          <div>
            <span>所属企业</span>
            <strong>{{ companyText(detailDialog.item || {}) }}</strong>
          </div>
          <div>
            <span>操作类型</span>
            <strong>{{ detailDialog.item?.actionTypeLabel || '系统操作' }}</strong>
          </div>
          <div>
            <span>操作对象</span>
            <strong>{{ detailDialog.item?.targetDisplay || '系统对象' }}</strong>
          </div>
          <div>
            <span>操作结果</span>
            <strong>{{ detailDialog.item?.resultLabel || '成功' }}</strong>
          </div>
          <div>
            <span>内部编码</span>
            <strong>{{ detailDialog.item?.actionType || 'SYSTEM' }}</strong>
          </div>
        </div>

        <section class="panel logs-detail-summary">
          <span class="panel-label">完整摘要</span>
          <p>{{ detailDialog.item?.summary || '暂无摘要' }}</p>
        </section>
      </section>
    </div>
  </div>
</template>

<style scoped>
.logs-manage {
  padding: 20px;
}

.profile-banner {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
  border: 1px solid rgba(29, 111, 161, 0.16);
  background: linear-gradient(135deg, rgba(246, 251, 255, 0.96), rgba(236, 245, 255, 0.92));
}

.profile-banner strong {
  color: var(--admin-text);
  font-size: 15px;
}

.profile-banner span {
  color: var(--admin-text-soft);
  line-height: 1.7;
}

.logs-filter-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-slot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 8px;
}

.summary-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.summary-cell span {
  color: var(--admin-text);
  line-height: 1.6;
}

.table-action-link {
  padding: 0;
  font-weight: 600;
}

.logs-pagination {
  margin-top: 18px;
}

.logs-detail-dialog {
  width: min(860px, 100%);
}

.logs-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 18px 0;
}

.logs-detail-grid div {
  display: grid;
  gap: 8px;
}

.logs-detail-summary {
  padding: 18px 20px;
}

.logs-detail-summary p {
  margin: 10px 0 0;
  color: var(--admin-text);
  line-height: 1.8;
}

@media (max-width: 900px) {
  .logs-filter-grid,
  .logs-detail-grid {
    grid-template-columns: 1fr;
  }

  .summary-slot {
    justify-content: flex-start;
    padding-right: 0;
  }
}

@media (max-width: 768px) {
  .logs-manage {
    padding: 14px;
  }

  .summary-cell {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
