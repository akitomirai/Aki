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
const companyFilterEnabled = computed(() => isPlatformAdmin.value)
const currentCompanyName = computed(() => authStore.user?.companyName || '当前企业')
const pageCount = computed(() => Math.max(1, Math.ceil(Number(total.value || 0) / Number(pageSize.value || 20))))
const pageSummary = computed(() => {
  if (!total.value) {
    return '当前没有可展示的操作日志。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(total.value, page.value * pageSize.value)
  return `共 ${total.value} 条操作日志，当前显示 ${from}-${to} 条。`
})

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
  { value: 'TRACE_RECORD_SUBMIT', label: '提交现场记录' },
  { value: 'TRACE_IMAGE_UPLOAD', label: '图片上传成功' }
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
  if (loading.value || page.value <= 1) {
    return
  }
  fetchRows(page.value - 1)
}

function goNextPage() {
  if (loading.value || page.value >= pageCount.value) {
    return
  }
  fetchRows(page.value + 1)
}

function companyText(item) {
  return item.companyName || '平台主管范围'
}

function actionTone(actionType) {
  const code = String(actionType || '').toUpperCase()
  if (code.startsWith('RISK_')) {
    return 'frozen'
  }
  if (code.startsWith('USER_') || code.startsWith('AUTH_')) {
    return 'draft'
  }
  return 'published'
}

function resultTone(result) {
  return String(result || '').toUpperCase() === 'FAILED' ? 'recalled' : 'published'
}

function roleTone(roleCode) {
  return {
    PLATFORM_ADMIN: 'published',
    ENTERPRISE_ADMIN: 'draft',
    OPERATOR: 'published',
    REGULATOR: 'frozen'
  }[String(roleCode || '').toUpperCase()] || 'draft'
}

function summaryPreview(item) {
  const text = String(item.summary || '')
  if (text.length <= 44) {
    return text
  }
  return `${text.slice(0, 44)}...`
}
</script>

<template>
  <div class="page-shell" data-testid="logs-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">操作日志</h1>
        <p class="manage-page-subtitle">
          统一查看用户管理、批次分配、质检、二维码、发布、风险处理和现场作业提交的关键留痕，方便管理员快速回查谁在什么时间做了什么。
        </p>
      </div>
      <div class="manage-page-actions">
        <button class="ghost" data-testid="logs-refresh-button" :disabled="loading" @click="fetchRows(page)">刷新</button>
      </div>
    </section>

    <section class="panel">
      <div class="filter-grid logs-filter-grid">
        <label>
          <span>操作类型</span>
          <select v-model="filters.actionType" data-testid="logs-filter-action">
            <option v-for="item in actionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>

        <label>
          <span>操作人</span>
          <input
            v-model.trim="filters.operatorKeyword"
            data-testid="logs-filter-operator"
            type="text"
            placeholder="输入操作人姓名"
          >
        </label>

        <label>
          <span>角色</span>
          <select v-model="filters.roleCode" data-testid="logs-filter-role">
            <option v-for="item in roleOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>

        <label v-if="companyFilterEnabled">
          <span>所属企业</span>
          <select v-model="filters.companyId" data-testid="logs-filter-company" :disabled="companyLoading">
            <option value="">全部企业</option>
            <option v-for="item in companyOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <label v-else>
          <span>所属企业</span>
          <input :value="currentCompanyName" type="text" disabled>
        </label>

        <label>
          <span>结果</span>
          <select v-model="filters.result" data-testid="logs-filter-result">
            <option v-for="item in resultOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>

        <label>
          <span>开始日期</span>
          <input v-model="filters.dateFrom" data-testid="logs-filter-date-from" type="date">
        </label>

        <label>
          <span>结束日期</span>
          <input v-model="filters.dateTo" data-testid="logs-filter-date-to" type="date">
        </label>

        <label>
          <span>分页</span>
          <input :value="`第 ${page} / ${pageCount} 页`" type="text" disabled>
        </label>
      </div>

      <div class="toolbar">
        <span class="list-summary">{{ pageSummary }}</span>
        <div class="toolbar-actions">
          <button class="primary" data-testid="logs-search-button" :disabled="loading" @click="fetchRows(1)">查询</button>
          <button class="ghost" data-testid="logs-reset-button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section v-if="loading" class="panel empty-state">
      <div>
        <h3>正在加载操作日志...</h3>
        <p class="empty-copy">请稍等，系统正在汇总关键后台动作和留痕结果。</p>
      </div>
    </section>

    <section v-else-if="!rows.length" class="panel empty-state">
      <div>
        <h3>当前筛选下没有日志</h3>
        <p class="empty-copy">可以调整操作类型、时间范围、企业或操作人后再查看。</p>
      </div>
    </section>

    <section v-else class="panel">
      <div class="todo-table-head logs-head">
        <span>操作时间</span>
        <span>操作人</span>
        <span>角色</span>
        <span>所属企业</span>
        <span>操作类型</span>
        <span>操作对象</span>
        <span>结果</span>
        <span>摘要</span>
      </div>

      <div class="todo-row-list">
        <article
          v-for="item in rows"
          :key="item.id"
          class="todo-row logs-row"
          :data-testid="`logs-row-${item.id}`"
        >
          <div class="row-meta">
            <strong>{{ item.createdAt || '暂无时间' }}</strong>
            <small>日志 ID {{ item.id }}</small>
          </div>

          <div class="row-main">
            <strong>{{ item.operatorName }}</strong>
            <small>{{ item.operatorUserId ? `用户 ID ${item.operatorUserId}` : '系统动作' }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="roleTone(item.roleCode)">{{ item.roleName }}</span>
            <small>{{ item.roleCode || 'SYSTEM' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ companyText(item) }}</strong>
            <small>{{ item.companyId ? `企业 ID ${item.companyId}` : '平台范围日志' }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="actionTone(item.actionType)">{{ item.actionTypeLabel }}</span>
            <small>{{ item.actionType }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ item.targetDisplay }}</strong>
            <small>{{ item.targetTypeLabel }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="resultTone(item.result)">{{ item.resultLabel }}</span>
            <small>{{ item.result }}</small>
          </div>

          <div class="row-actions logs-summary-cell">
            <span class="summary-copy">{{ summaryPreview(item) || '暂无摘要' }}</span>
            <button class="text-button" :data-testid="`logs-detail-${item.id}`" @click="openDetailDialog(item)">查看详情</button>
          </div>
        </article>
      </div>

      <div class="toolbar logs-pagination">
        <span class="list-summary">第 {{ page }} / {{ pageCount }} 页</span>
        <div class="toolbar-actions">
          <button class="ghost" data-testid="logs-prev-page" :disabled="loading || page <= 1" @click="goPrevPage">上一页</button>
          <button class="ghost" data-testid="logs-next-page" :disabled="loading || page >= pageCount" @click="goNextPage">下一页</button>
        </div>
      </div>
    </section>

    <div v-if="detailDialog.visible" class="dialog-mask" @click.self="closeDetailDialog">
      <section class="dialog-card logs-detail-dialog" data-testid="logs-detail-dialog">
        <div class="dialog-head">
          <div>
            <h3>日志详情</h3>
            <p>查看该条关键后台动作的完整摘要与对象信息。</p>
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
            <span>原始编码</span>
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

<style src="../../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.logs-filter-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.logs-head,
.logs-row {
  grid-template-columns:
    minmax(0, 0.95fr)
    minmax(0, 0.85fr)
    minmax(0, 0.8fr)
    minmax(0, 0.9fr)
    minmax(0, 0.9fr)
    minmax(0, 1.2fr)
    minmax(0, 0.8fr)
    minmax(0, 1.2fr);
}

.logs-summary-cell {
  align-items: flex-start;
  justify-content: space-between;
}

.summary-copy {
  color: var(--admin-text);
  line-height: 1.7;
}

.logs-pagination {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--admin-border);
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
  .logs-detail-grid,
  .logs-row {
    grid-template-columns: 1fr;
  }
}
</style>
