<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCompanyOptions } from '../../api/batch'
import { getOperationLogs } from '../../api/log'
import { useAuthStore } from '../../stores/auth'
import { getFriendlyErrorMessage } from '../../utils/batchExperience'

const DEFAULT_PAGE_SIZE = 10

const authStore = useAuthStore()

const loading = ref(false)
const companyLoading = ref(false)
const message = ref('')
const messageType = ref('info')
const rows = ref([])
const statsRows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const companyOptions = ref([])
const filters = ref(createFilters())
const detailDialog = ref(createDetailDialogState())
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]

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
const pageCount = computed(() => Math.max(1, Math.ceil(Number(total.value || 0) / Number(pageSize.value || DEFAULT_PAGE_SIZE))))
const pageSummary = computed(() => {
  if (!total.value) {
    return '当前没有可展示的操作日志。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(total.value, page.value * pageSize.value)
  return `共 ${total.value} 条日志，当前显示 ${from}-${to} 条。`
})
const successCount = computed(() => statsRows.value.filter((item) => String(item.result || '').toUpperCase() === 'SUCCESS').length)
const failedCount = computed(() => statsRows.value.filter((item) => String(item.result || '').toUpperCase() === 'FAILED').length)
const visibleCount = computed(() => rows.value.length)

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
    const [response, summaryResponse] = await Promise.all([
      getOperationLogs(cleanObject({
        actionType: filters.value.actionType || undefined,
        operatorKeyword: filters.value.operatorKeyword || undefined,
        roleCode: filters.value.roleCode || undefined,
        companyId: companyFilterEnabled.value ? normalizeCompanyId(filters.value.companyId) : undefined,
        result: filters.value.result || undefined,
        dateFrom: filters.value.dateFrom || undefined,
        dateTo: filters.value.dateTo || undefined,
        page: targetPage,
        pageSize: pageSize.value
      })),
      getOperationLogs({
        page: 1,
        pageSize: 1000
      })
    ])
    const payload = response.data ?? {}
    rows.value = payload.items ?? []
    total.value = Number(payload.total || 0)
    page.value = Number(payload.page || targetPage || 1)
    pageSize.value = Number(payload.pageSize || pageSize.value || DEFAULT_PAGE_SIZE)
    statsRows.value = summaryResponse.data?.items ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '操作日志加载失败，请稍后重试。'), 'error')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = createFilters()
  pageSize.value = DEFAULT_PAGE_SIZE
  fetchRows(1)
}

function handlePageSizeChange(value) {
  const nextPageSize = Number(value || DEFAULT_PAGE_SIZE)
  if (nextPageSize === pageSize.value) return
  pageSize.value = nextPageSize
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
  <div class="page-shell">
  <div class="manage-page logs-manage" data-testid="logs-page">
    <el-card v-if="isEnterpriseAdmin" shadow="never" class="profile-banner" data-testid="logs-self-mode">
      <strong>当前为本企业日志模式</strong>
      <span>{{ scopedModeHint }}</span>
    </el-card>

    <el-card shadow="never" class="manage-filter-card">
      <div class="logs-filter-layout">
        <div class="manage-filter-grid logs-filter-row logs-filter-row--primary">
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
        </div>

        <div class="manage-filter-grid logs-filter-row logs-filter-row--secondary">
          <el-input v-model="filters.dateFrom" class="manage-filter-item" data-testid="logs-filter-date-from" type="date" />
          <el-input v-model="filters.dateTo" class="manage-filter-item" data-testid="logs-filter-date-to" type="date" />
          <div class="logs-filter-trailing">
            <el-button type="primary" class="logs-filter-button" data-testid="logs-search-button" @click="fetchRows(1)">查询</el-button>
            <el-button class="logs-filter-button" data-testid="logs-reset-button" @click="resetFilters">重置</el-button>
            <div class="logs-page-size-control">
              <span class="manage-muted">每页显示</span>
              <el-select
                :model-value="pageSize"
                class="logs-page-size-select"
                data-testid="logs-page-size"
                @change="handlePageSizeChange"
              >
                <el-option v-for="item in pageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
            <div class="summary-slot logs-filter-summary">
              <span class="manage-muted">{{ pageSummary }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <div class="manage-summary-row">
      <div class="manage-summary">
        <div class="manage-summary-chip">
          <span>当前页日志</span>
          <strong>{{ visibleCount }}</strong>
        </div>
        <button
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': filters.result === 'SUCCESS' }"
          data-testid="logs-summary-success"
          @click="filters.result = filters.result === 'SUCCESS' ? '' : 'SUCCESS'; fetchRows(1)"
        >
          <span>成功</span>
          <strong>{{ successCount }}</strong>
        </button>
        <button
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': filters.result === 'FAILED' }"
          data-testid="logs-summary-failed"
          @click="filters.result = filters.result === 'FAILED' ? '' : 'FAILED'; fetchRows(1)"
        >
          <span>失败</span>
          <strong>{{ failedCount }}</strong>
        </button>
      </div>

      <div class="manage-summary-actions">
        <el-button data-testid="logs-refresh-button" @click="fetchRows(page)" :loading="loading">刷新</el-button>
      </div>
    </div>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section class="panel ledger-panel logs-ledger-panel">
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">日志台账</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div>
          <h3>正在加载操作日志...</h3>
        </div>
      </div>

      <div v-else-if="!rows.length" class="empty-state">
        <div>
          <h3>当前筛选下没有日志</h3>
        </div>
      </div>

      <div v-else class="table-scroll-shell ledger-table-shell logs-table-shell">
        <div class="ledger-table-head logs-table-head">
          <span>操作人 / 时间</span>
          <span>角色 / 企业</span>
          <span>操作类型</span>
          <span>操作对象</span>
          <span>结果</span>
          <span>摘要 / 查看</span>
        </div>

        <div class="ledger-row-list" data-testid="logs-table">
          <article
            v-for="row in rows"
            :key="row.id"
            class="ledger-row logs-row"
          >
            <div class="row-main">
              <strong>{{ row.operatorName || '系统用户' }}</strong>
              <span>{{ row.createdAt || '暂无时间' }}</span>
            </div>

            <div class="row-meta">
              <strong>{{ row.roleName || '系统用户' }}</strong>
              <small>{{ companyText(row) }}</small>
            </div>

            <div class="row-status">
              <span class="ledger-status-pill" :class="actionTone(row.actionType)">
                {{ row.actionTypeLabel || '系统操作' }}
              </span>
            </div>

            <div class="row-meta">
              <strong>{{ row.targetDisplay || '系统对象' }}</strong>
              <small>{{ row.targetTypeLabel || '系统对象' }}</small>
            </div>

            <div class="row-status">
              <span class="ledger-status-pill" :class="resultTone(row.result)">
                {{ row.resultLabel || '成功' }}
              </span>
            </div>

            <div class="row-actions">
              <div class="logs-summary-block">
                <span>{{ summaryPreview(row) || '暂无摘要' }}</span>
                <button class="text-button primary-text" :data-testid="`logs-detail-${row.id}`" @click="openDetailDialog(row)">查看详情</button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div class="toolbar logs-pagination">
        <span class="list-summary">第 {{ page }} / {{ pageCount }} 页</span>
        <div class="toolbar-actions">
          <el-button data-testid="logs-prev-page" :disabled="loading || page <= 1" @click="goPrevPage">上一页</el-button>
          <el-button data-testid="logs-next-page" :disabled="loading || page >= pageCount" @click="goNextPage">下一页</el-button>
        </div>
      </div>
    </section>

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
  </div>
</template>

<style scoped>
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

.logs-filter-layout {
  display: grid;
  gap: 14px;
}

.logs-filter-row {
  display: grid;
  gap: 12px;
}

.logs-filter-row--primary {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.logs-filter-row--secondary {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: center;
}

.logs-filter-trailing {
  grid-column: 3 / 6;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.logs-filter-button {
  width: 120px;
  margin: 0;
}

.summary-slot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 40px;
  text-align: right;
}

.logs-filter-summary {
  margin-left: auto;
  min-width: 0;
}

.logs-page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 188px;
}

.logs-page-size-select {
  width: 128px;
}

.logs-ledger-panel {
  margin-top: 0;
}

.logs-table-head,
.logs-row {
  grid-template-columns:
    minmax(190px, 0.9fr)
    minmax(220px, 1fr)
    minmax(170px, 0.8fr)
    minmax(220px, 1fr)
    minmax(120px, 0.5fr)
    minmax(300px, 1.2fr);
}

.logs-summary-block {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.logs-summary-block span {
  color: var(--admin-text);
  line-height: 1.6;
}

.logs-pagination {
  margin-top: 18px;
  padding: 0 28px;
}

.logs-pagination .list-summary {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
}

.logs-pagination .toolbar-actions {
  align-items: center;
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
  .logs-filter-row--primary,
  .logs-filter-row--secondary,
  .logs-detail-grid {
    grid-template-columns: 1fr;
  }

  .logs-filter-trailing {
    grid-column: auto;
    flex-wrap: wrap;
  }

  .logs-page-size-control {
    min-width: 0;
  }

  .summary-slot {
    justify-content: flex-start;
    text-align: left;
  }

  .logs-filter-button {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .logs-filter-trailing {
    flex-direction: column;
    align-items: stretch;
  }

  .logs-page-size-control {
    justify-content: space-between;
  }

  .logs-page-size-select {
    width: 100%;
  }

  .logs-filter-summary {
    margin-left: 0;
  }

  .logs-summary-block {
    align-items: flex-start;
  }
}
</style>
<style src="../../assets/styles/admin-task-pages.css" scoped></style>
