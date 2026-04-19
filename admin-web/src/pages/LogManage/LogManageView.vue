<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminListPagination from '../../components/AdminListPagination.vue'
import AdminListTemplate from '../../components/AdminListTemplate.vue'
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
    <AdminListTemplate
      template-class="logs-card-stack"
      filter-card-class="logs-filter-card"
      ledger-card-class="logs-ledger-panel logs-ledger-card"
    >
      <template #summary>
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
      </template>

      <template #actions>
        <el-button data-testid="logs-refresh-button" :loading="loading" @click="fetchRows(page)">刷新</el-button>
      </template>

      <template #banner>
        <el-card v-if="isEnterpriseAdmin" shadow="never" class="profile-banner" data-testid="logs-self-mode">
          <strong>当前为本企业日志模式</strong>
          <span>{{ scopedModeHint }}</span>
        </el-card>
      </template>

      <template #filterPrimary>
        <div class="logs-filter-layout">
          <div class="manage-filter-grid logs-filter-grid">
            <label class="manage-filter-field logs-filter-field logs-filter-field--action">
              <span class="manage-filter-field__label">操作类型</span>
              <el-select v-model="filters.actionType" class="manage-filter-item" data-testid="logs-filter-action" placeholder="操作类型">
                <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>

            <label class="manage-filter-field logs-filter-field logs-filter-field--operator">
              <span class="manage-filter-field__label">操作人</span>
              <el-input
                v-model.trim="filters.operatorKeyword"
                class="manage-filter-item"
                data-testid="logs-filter-operator"
                placeholder="输入操作人姓名"
                @keyup.enter="fetchRows(1)"
              />
            </label>

            <label class="manage-filter-field logs-filter-field logs-filter-field--role">
              <span class="manage-filter-field__label">角色</span>
              <el-select v-model="filters.roleCode" class="manage-filter-item" data-testid="logs-filter-role" placeholder="角色">
                <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>

            <label class="manage-filter-field logs-filter-field logs-filter-field--company">
              <span class="manage-filter-field__label">所属企业</span>
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
            </label>

            <label class="manage-filter-field logs-filter-field logs-filter-field--result">
              <span class="manage-filter-field__label">结果</span>
              <el-select v-model="filters.result" class="manage-filter-item" data-testid="logs-filter-result" placeholder="结果">
                <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>

            <label class="manage-filter-field logs-filter-field logs-filter-field--date">
              <span class="manage-filter-field__label">开始日期</span>
              <el-input v-model="filters.dateFrom" class="manage-filter-item" data-testid="logs-filter-date-from" type="date" />
            </label>

            <label class="manage-filter-field logs-filter-field logs-filter-field--date">
              <span class="manage-filter-field__label">结束日期</span>
              <el-input v-model="filters.dateTo" class="manage-filter-item" data-testid="logs-filter-date-to" type="date" />
            </label>

            <label class="manage-filter-field logs-filter-field logs-filter-field--page-size">
              <span class="manage-filter-field__label">每页显示</span>
              <el-select
                :model-value="pageSize"
                class="manage-filter-item logs-page-size-select"
                data-testid="logs-page-size"
                @change="handlePageSizeChange"
              >
                <el-option v-for="item in pageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>
          </div>
        </div>
      </template>

      <template #filterSecondary>
        <div class="logs-filter-toolbar">
          <div class="logs-filter-actions">
            <el-button type="primary" data-testid="logs-search-button" @click="fetchRows(1)">查询</el-button>
            <el-button data-testid="logs-reset-button" @click="resetFilters">重置</el-button>
          </div>

          <div class="logs-list-summary" role="status" aria-live="polite">
            <span class="manage-muted">{{ pageSummary }}</span>
          </div>
        </div>
      </template>

      <template #message>
        <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>
      </template>

      <template #ledger>
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

          <div class="ledger-row-list logs-row-list" data-testid="logs-table">
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

        <AdminListPagination
          :summary="`第 ${page} / ${pageCount} 页`"
          :prev-disabled="loading || page <= 1"
          :next-disabled="loading || page >= pageCount"
          prev-testid="logs-prev-page"
          next-testid="logs-next-page"
          @prev="goPrevPage"
          @next="goNextPage"
        />
      </template>
    </AdminListTemplate>

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
.logs-manage {
  font-family: "PingFang SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif;
  --logs-filter-text-inset: 14px;
  --logs-grid-inline-padding: 18px;
  --logs-grid-column-gap: 16px;
  --logs-filter-group-left-shift: 8px;
  --logs-action-filter-width: 172px;
  --logs-operator-filter-width: 216px;
  --logs-role-filter-width: 136px;
  --logs-company-filter-width: 208px;
  --logs-result-filter-width: 120px;
  --logs-date-filter-width: 168px;
  --logs-page-size-width: 148px;
  --logs-filter-card-border: rgba(56, 134, 217, 0.14);
  --logs-filter-card-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 251, 255, 0.94) 100%);
  --logs-filter-card-shadow: 0 16px 34px rgba(45, 113, 194, 0.1);
  --logs-filter-control-height: 40px;
  --logs-filter-control-radius: 12px;
}

.profile-banner {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
  border-radius: 20px;
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

.logs-card-stack {
  display: grid;
  gap: 16px;
}

:deep(.logs-filter-card),
:deep(.logs-ledger-card) {
  position: relative;
  overflow: hidden;
}

:deep(.logs-filter-card) {
  padding: 18px 22px 0;
  border-color: var(--logs-filter-card-border) !important;
  background: var(--logs-filter-card-bg) !important;
  box-shadow: var(--logs-filter-card-shadow) !important;
}

.logs-filter-layout {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 18px;
  margin-left: calc(-1 * var(--logs-filter-group-left-shift));
  padding: 0 12px 0 calc(var(--logs-grid-inline-padding) - var(--logs-filter-text-inset));
  flex-wrap: wrap;
}

.logs-filter-grid {
  grid-template-columns:
    minmax(var(--logs-action-filter-width), max-content)
    minmax(var(--logs-operator-filter-width), max-content)
    minmax(var(--logs-role-filter-width), max-content)
    minmax(var(--logs-company-filter-width), max-content)
    minmax(var(--logs-result-filter-width), max-content);
  align-items: end;
  column-gap: 12px;
  row-gap: 12px;
  padding: 0;
  flex: 0 1 auto;
  min-width: 0;
}

.logs-filter-field {
  gap: 8px;
  width: 100%;
  justify-self: start;
}

.logs-filter-field--action {
  max-width: var(--logs-action-filter-width);
}

.logs-filter-field--operator {
  max-width: var(--logs-operator-filter-width);
}

.logs-filter-field--role {
  max-width: var(--logs-role-filter-width);
}

.logs-filter-field--company {
  max-width: var(--logs-company-filter-width);
}

.logs-filter-field--result {
  max-width: var(--logs-result-filter-width);
}

.logs-filter-field--date {
  max-width: var(--logs-date-filter-width);
}

.logs-filter-field--page-size {
  max-width: var(--logs-page-size-width);
}

.logs-filter-grid .manage-filter-field__label {
  padding-inline-start: var(--logs-filter-text-inset);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.logs-filter-grid :deep(.manage-filter-item .el-input__wrapper),
.logs-filter-grid :deep(.manage-filter-item .el-select__wrapper) {
  min-height: var(--logs-filter-control-height);
  padding-inline-start: var(--logs-filter-text-inset);
  padding-inline-end: 14px;
  border-radius: var(--logs-filter-control-radius);
  background: #fff;
  box-shadow: 0 0 0 1px rgba(56, 134, 217, 0.14) inset !important;
}

.logs-filter-grid :deep(.manage-filter-item .el-input__inner),
.logs-filter-grid :deep(.manage-filter-item .el-select__selected-item),
.logs-filter-grid :deep(.manage-filter-item .el-select__placeholder) {
  text-align: left;
}

.logs-filter-grid :deep(.manage-filter-item .el-select__placeholder),
.logs-filter-grid :deep(.manage-filter-item .el-input__inner::placeholder) {
  color: var(--admin-text-faint);
}

.logs-page-size-select {
  width: 100%;
}

.logs-page-size-select :deep(.el-select__wrapper) {
  min-height: var(--logs-filter-control-height);
}

.logs-filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 14px;
  margin-top: 14px;
  margin-left: calc(-1 * var(--logs-filter-group-left-shift));
  padding: 0 12px 18px var(--logs-grid-inline-padding);
  min-width: 0;
  flex-wrap: wrap;
}

.logs-filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.logs-filter-actions :deep(.el-button) {
  min-height: 38px;
  padding-inline: 16px;
  border-radius: 12px;
}

.logs-list-summary {
  margin: 0;
  padding: 0;
}

.logs-list-summary .manage-muted {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.logs-ledger-panel {
  margin-top: 0;
}

:deep(.logs-ledger-panel .panel-heading) {
  padding-left: 28px;
}

:deep(.logs-ledger-card) {
  padding: 20px 22px 18px;
  border: 1px solid rgba(56, 134, 217, 0.14) !important;
  border-radius: 24px !important;
  background: #fff !important;
  box-shadow: 0 18px 42px rgba(45, 113, 194, 0.08) !important;
}

.logs-table-shell {
  --ledger-grid-columns:
    minmax(190px, 0.9fr)
    minmax(220px, 1fr)
    minmax(170px, 0.8fr)
    minmax(220px, 1fr)
    minmax(120px, 0.5fr)
    minmax(300px, 1.2fr);
  --ledger-column-gap: var(--logs-grid-column-gap);
  --ledger-inline-padding: var(--logs-grid-inline-padding);
  --ledger-min-width: 1440px;
}

.logs-table-head {
  color: #6f86a4;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  background: #fff;
}

.logs-row-list,
.logs-row-list.ledger-row-list {
  display: grid;
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 26px;
  background: #fff;
}

:deep(.logs-ledger-card .table-scroll-shell) {
  background: #fff !important;
}

:deep(.logs-ledger-card .admin-list-pagination) {
  background: #fff !important;
}

:deep(.logs-ledger-card .table-scroll-shell .logs-table-head) {
  background: #fff !important;
}

:deep(.logs-ledger-card .panel-heading),
:deep(.logs-ledger-card .panel-heading > div) {
  background: #fff;
}

.logs-row,
.logs-row.ledger-row {
  background: #fff;
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

@media (max-width: 1280px) {
  .logs-filter-grid {
    grid-template-columns:
      minmax(var(--logs-action-filter-width), max-content)
      minmax(var(--logs-operator-filter-width), max-content)
      minmax(var(--logs-role-filter-width), max-content);
  }
}

@media (max-width: 900px) {
  .logs-filter-grid,
  .logs-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .logs-filter-layout,
  .logs-filter-toolbar {
    margin-left: 0;
    padding-left: 0;
    padding-right: 0;
  }

  .logs-filter-actions {
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .logs-filter-grid,
  .logs-detail-grid {
    grid-template-columns: 1fr;
  }

  .logs-filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .logs-filter-actions {
    width: 100%;
  }

  .logs-filter-actions :deep(.el-button) {
    flex: 1 1 auto;
  }

  .logs-list-summary {
    width: 100%;
  }

  .logs-summary-block {
    align-items: flex-start;
  }
}
</style>
<style src="../../assets/styles/admin-task-pages.css" scoped></style>
