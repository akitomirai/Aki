<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { createUser, getUserList, resetUserPassword, updateUser, updateUserStatus } from '../../api/user'
import { getCompanyOptions } from '../../api/batch'
import { useAuthStore } from '../../stores/auth'
import { getFriendlyErrorMessage } from '../../utils/batchExperience'

const authStore = useAuthStore()
const DEFAULT_PAGE_SIZE = 10

const loading = ref(false)
const saving = ref(false)
const resetSaving = ref(false)
const companyLoading = ref(false)
const message = ref('')
const messageType = ref('info')
const rows = ref([])
const allRows = ref([])
const companyOptions = ref([])
const activeRoleTab = ref('ALL')
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const filters = ref(createFilters())
const isPlatformAdmin = computed(() => String(authStore.user?.roleCode || '').toUpperCase() === 'PLATFORM_ADMIN')
const isEnterpriseAdmin = computed(() => String(authStore.user?.roleCode || '').toUpperCase() === 'ENTERPRISE_ADMIN')
const dialog = ref(createDialogState())
const resetDialog = ref(createResetDialogState())
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]

const roleTabs = computed(() => {
  const base = [{ value: 'ALL', label: '全部用户' }]
  if (isPlatformAdmin.value) {
    return [
      ...base,
      { value: 'PLATFORM_ADMIN', label: '平台管理员' },
      { value: 'ENTERPRISE_ADMIN', label: '企业管理员' },
      { value: 'OPERATOR', label: '现场操作员' },
      { value: 'REGULATOR', label: '监管人员' }
    ]
  }
  return [
    ...base,
    { value: 'ENTERPRISE_ADMIN', label: '企业管理员' },
    { value: 'OPERATOR', label: '现场操作员' }
  ]
})

const roleOptions = computed(() => roleTabs.value.filter((item) => item.value !== 'ALL'))

const formCompanyOptions = computed(() => {
  if (isPlatformAdmin.value) {
    return companyOptions.value
  }
  const currentCompanyId = Number(authStore.user?.companyId || 0)
  return companyOptions.value.filter((item) => Number(item.id) === currentCompanyId)
})

const filteredRows = computed(() => {
  if (activeRoleTab.value === 'ALL') {
    return rows.value
  }
  return rows.value.filter((item) => item.roleCode === activeRoleTab.value)
})

const pageCount = computed(() => Math.max(1, Math.ceil(Number(filteredRows.value.length || 0) / Number(pageSize.value || DEFAULT_PAGE_SIZE))))

const visibleRows = computed(() => {
  const fromIndex = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(fromIndex, fromIndex + pageSize.value)
})

const roleCounts = computed(() => {
  const counts = allRows.value.reduce((acc, item) => {
    const key = String(item.roleCode || '')
    acc.ALL += 1
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, { ALL: 0 })

  for (const tab of roleTabs.value) {
    counts[tab.value] = counts[tab.value] || 0
  }
  return counts
})

const enabledCount = computed(() => allRows.value.filter((item) => Number(item.status) === 1).length)
const disabledCount = computed(() => allRows.value.filter((item) => Number(item.status) !== 1).length)
const passwordPendingCount = computed(() => allRows.value.filter((item) => item.needChangePassword).length)
const currentCompanyName = computed(() => authStore.user?.companyName || '当前企业')
const companyFieldRequired = computed(() => roleNeedsCompany(dialog.value.form.roleCode))
const companyFieldDisabled = computed(() => !companyFieldRequired.value || isEnterpriseAdmin.value)
const usernameFieldDisabled = computed(() => (
  dialog.value.mode === 'edit' && Number(dialog.value.editingId) === Number(authStore.user?.id)
))

const listSummary = computed(() => {
  if (!filteredRows.value.length) {
    return '当前没有可展示的用户。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(filteredRows.value.length, page.value * pageSize.value)
  if (isPlatformAdmin.value) {
    return `共 ${filteredRows.value.length} 个用户，当前显示 ${from}-${to} 个。`
  }
  return `当前仅展示本企业用户，共 ${filteredRows.value.length} 个，当前显示 ${from}-${to} 个。`
})

onMounted(async () => {
  await Promise.all([loadCompanyOptions(), fetchRows()])
})

watch(
  () => dialog.value.form.roleCode,
  (roleCode) => {
    if (roleNeedsCompany(roleCode)) {
      if (isEnterpriseAdmin.value) {
        dialog.value.form.companyId = normalizeCompanyId(authStore.user?.companyId)
      }
      return
    }
    dialog.value.form.companyId = ''
  }
)

watch(activeRoleTab, () => {
  page.value = 1
})

watch(filteredRows, () => {
  if (page.value > pageCount.value) {
    page.value = pageCount.value
  }
})

function createFilters() {
  return {
    keyword: '',
    companyId: '',
    status: ''
  }
}

function createDialogForm() {
  return {
    username: '',
    password: '',
    realName: '',
    roleCode: 'OPERATOR',
    companyId: isEnterpriseAdmin.value ? normalizeCompanyId(authStore.user?.companyId) : ''
  }
}

function createDialogState() {
  return {
    visible: false,
    mode: 'create',
    editingId: null,
    form: createDialogForm()
  }
}

function createResetDialogState() {
  return {
    visible: false,
    userId: null,
    displayName: '',
    password: '123456'
  }
}

function roleNeedsCompany(roleCode) {
  return ['ENTERPRISE_ADMIN', 'OPERATOR'].includes(String(roleCode || '').toUpperCase())
}

function normalizeCompanyId(value) {
  if (value === null || value === undefined || value === '') {
    return ''
  }
  return Number(value)
}

function cleanObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, item]) => item !== '' && item !== null && item !== undefined)
  )
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function clearMessage() {
  message.value = ''
}

function resolveStatusTone(status) {
  return Number(status) === 1 ? 'is-enabled' : 'is-disabled'
}

function resolveRoleTone(roleCode) {
  return {
    PLATFORM_ADMIN: 'is-enabled',
    ENTERPRISE_ADMIN: 'is-archived',
    OPERATOR: 'is-enabled',
    REGULATOR: 'is-disabled'
  }[String(roleCode || '').toUpperCase()] || 'is-archived'
}

function resolvePasswordTone(item) {
  return item.needChangePassword ? 'is-archived' : 'is-enabled'
}

function companyText(item) {
  return item.companyName || '平台主账号'
}

function handleStatusChipClick(status) {
  filters.value.status = filters.value.status === status ? '' : status
  page.value = 1
  fetchRows()
}

function resetFilters() {
  filters.value = createFilters()
  activeRoleTab.value = 'ALL'
  page.value = 1
  pageSize.value = DEFAULT_PAGE_SIZE
  fetchRows()
}

function handlePageSizeChange(value) {
  const nextPageSize = Number(value || DEFAULT_PAGE_SIZE)
  if (nextPageSize === pageSize.value) return
  pageSize.value = nextPageSize
  page.value = 1
}

async function loadCompanyOptions() {
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

async function fetchRows() {
  loading.value = true
  clearMessage()
  try {
    const [listResponse, summaryResponse] = await Promise.all([
      getUserList(cleanObject({
        keyword: filters.value.keyword || undefined,
        status: filters.value.status === '' ? undefined : Number(filters.value.status),
        companyId: isPlatformAdmin.value ? normalizeCompanyId(filters.value.companyId) || undefined : undefined
      })),
      getUserList()
    ])
    rows.value = listResponse.data ?? []
    allRows.value = summaryResponse.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '用户列表加载失败，请稍后重试。'), 'error')
  } finally {
    loading.value = false
  }
}

function goPrevPage() {
  if (loading.value || page.value <= 1) return
  page.value -= 1
}

function goNextPage() {
  if (loading.value || page.value >= pageCount.value) return
  page.value += 1
}

function openCreateDialog() {
  clearMessage()
  dialog.value = {
    visible: true,
    mode: 'create',
    editingId: null,
    form: createDialogForm()
  }
}

function openEditDialog(item) {
  clearMessage()
  dialog.value = {
    visible: true,
    mode: 'edit',
    editingId: item.id,
    form: {
      username: item.username,
      password: '',
      realName: item.realName,
      roleCode: item.roleCode,
      companyId: normalizeCompanyId(item.companyId)
    }
  }
}

function closeDialog() {
  if (saving.value) return
  dialog.value = createDialogState()
}

function openResetPasswordDialog(item) {
  clearMessage()
  resetDialog.value = {
    visible: true,
    userId: item.id,
    displayName: item.realName || item.username,
    password: '123456'
  }
}

function closeResetPasswordDialog() {
  if (resetSaving.value) return
  resetDialog.value = createResetDialogState()
}

function normalizeUserPayload() {
  const form = dialog.value.form
  const payload = {
    username: String(form.username || '').trim(),
    realName: String(form.realName || '').trim(),
    roleCode: String(form.roleCode || '').trim(),
    companyId: companyFieldRequired.value ? normalizeCompanyId(form.companyId) : null
  }
  if (dialog.value.mode === 'create') {
    payload.password = String(form.password || '').trim()
  }
  return payload
}

async function submitDialog() {
  saving.value = true
  clearMessage()
  try {
    const payload = normalizeUserPayload()
    if (dialog.value.mode === 'create') {
      await createUser(payload)
      showMessage('用户已创建，首次登录需先修改密码。', 'success')
    } else {
      await updateUser(dialog.value.editingId, payload)
      showMessage('用户资料已更新。', 'success')
    }
    dialog.value = createDialogState()
    await Promise.all([loadCompanyOptions(), fetchRows()])
  } catch (error) {
    const fallback = dialog.value.mode === 'create'
      ? '用户创建失败，请稍后重试。'
      : '用户更新失败，请稍后重试。'
    showMessage(getFriendlyErrorMessage(error, fallback), 'error')
  } finally {
    saving.value = false
  }
}

async function toggleUserStatus(item) {
  const nextStatus = Number(item.status) === 1 ? 0 : 1
  const nextLabel = nextStatus === 1 ? '启用' : '停用'
  const confirmed = window.confirm(`确认${nextLabel}用户“${item.realName || item.username}”吗？`)
  if (!confirmed) return

  clearMessage()
  try {
    await updateUserStatus(item.id, nextStatus)
    showMessage(`用户已${nextLabel}。`, 'success')
    await fetchRows()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, `用户${nextLabel}失败，请稍后重试。`), 'error')
  }
}

async function submitResetPassword() {
  resetSaving.value = true
  clearMessage()
  try {
    await resetUserPassword(resetDialog.value.userId, {
      newPassword: String(resetDialog.value.password || '').trim()
    })
    showMessage('密码已重置，用户下次登录需先修改密码。', 'success')
    resetDialog.value = createResetDialogState()
    await fetchRows()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '密码重置失败，请稍后重试。'), 'error')
  } finally {
    resetSaving.value = false
  }
}
</script>

<template>
  <div class="page-shell">
  <div class="manage-page user-manage" data-testid="users-page">
    <el-card shadow="never" class="manage-filter-card">
      <div class="manage-filter-grid user-filter-grid">
        <el-input
          v-model.trim="filters.keyword"
          clearable
          class="manage-filter-item"
          data-testid="users-filter-keyword"
          placeholder="输入用户名或姓名"
          @keyup.enter="fetchRows"
        />

        <el-select
          v-if="isPlatformAdmin"
          v-model="filters.companyId"
          clearable
          filterable
          class="manage-filter-item"
          data-testid="users-filter-company"
          placeholder="所属企业"
          :loading="companyLoading"
        >
          <el-option value="" label="全部企业" />
          <el-option v-for="item in companyOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>

        <el-input
          v-else
          class="manage-filter-item"
          :model-value="currentCompanyName"
          disabled
        />

        <el-select
          v-model="filters.status"
          class="manage-filter-item"
          data-testid="users-filter-status"
          placeholder="用户状态"
        >
          <el-option value="" label="全部状态" />
          <el-option value="1" label="启用" />
          <el-option value="0" label="停用" />
        </el-select>

        <el-button type="primary" data-testid="users-search-button" @click="fetchRows">查询</el-button>
        <el-button data-testid="users-reset-button" @click="resetFilters">重置</el-button>
        <div class="user-page-size-control">
          <span class="manage-muted">每页显示</span>
          <el-select
            :model-value="pageSize"
            class="user-page-size-select"
            data-testid="users-page-size"
            @change="handlePageSizeChange"
          >
            <el-option v-for="item in pageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="summary-slot">
          <span class="manage-muted">{{ listSummary }}</span>
        </div>
      </div>
    </el-card>

    <div class="manage-summary-row">
      <div class="manage-summary">
        <button
          v-for="tab in roleTabs"
          :key="tab.value"
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': activeRoleTab === tab.value }"
          :data-testid="`users-tab-${tab.value}`"
          @click="activeRoleTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <strong>{{ roleCounts[tab.value] ?? 0 }}</strong>
        </button>

        <button
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': filters.status === '1' }"
          data-testid="users-summary-enabled"
          @click="handleStatusChipClick('1')"
        >
          <span>启用中</span>
          <strong>{{ enabledCount }}</strong>
        </button>

        <button
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': filters.status === '0' }"
          data-testid="users-summary-disabled"
          @click="handleStatusChipClick('0')"
        >
          <span>已停用</span>
          <strong>{{ disabledCount }}</strong>
        </button>

        <div class="manage-summary-chip">
          <span>待改密码</span>
          <strong>{{ passwordPendingCount }}</strong>
        </div>
      </div>

      <div class="manage-summary-actions">
        <el-button data-testid="users-refresh-button" :loading="loading" @click="fetchRows">刷新</el-button>
        <el-button type="primary" data-testid="users-open-create" @click="openCreateDialog">新增用户</el-button>
      </div>
    </div>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section class="panel ledger-panel user-ledger-panel">
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">用户台账</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div>
          <h3>正在加载用户列表...</h3>
        </div>
      </div>

      <div v-else-if="!visibleRows.length" class="empty-state">
        <div>
          <h3>当前筛选下没有用户</h3>
          <div class="toolbar-actions">
            <button class="primary" @click="openCreateDialog">新增用户</button>
          </div>
        </div>
      </div>

      <div v-else class="table-scroll-shell ledger-table-shell user-table-shell">
        <div class="ledger-table-head user-table-head">
          <span>用户</span>
          <span>角色 / 企业</span>
          <span>状态</span>
          <span>密码状态</span>
          <span>最近更新时间</span>
          <span>操作</span>
        </div>

        <div class="ledger-row-list" data-testid="users-table">
          <article
            v-for="row in visibleRows"
            :key="row.id"
            class="ledger-row user-row"
            :data-testid="`users-row-${row.id}`"
          >
            <div class="row-main">
              <strong>{{ row.realName || row.username }}</strong>
              <span class="ledger-code">{{ row.username }}</span>
            </div>

            <div class="row-meta">
              <strong>{{ row.roleName }}</strong>
              <small>{{ companyText(row) }}</small>
            </div>

            <div class="row-status">
              <span class="ledger-status-pill" :class="resolveStatusTone(row.status)">
                {{ row.statusLabel }}
              </span>
            </div>

            <div class="row-status">
              <span class="ledger-status-pill" :class="resolvePasswordTone(row)">
                {{ row.passwordStatusLabel }}
              </span>
            </div>

            <div class="row-meta">
              <strong>{{ row.updatedAt || '暂无更新' }}</strong>
              <small>{{ row.passwordUpdatedAt || '暂无改密记录' }}</small>
            </div>

            <div class="row-actions">
              <div class="ledger-actions-scroll">
                <button class="text-button primary-text" :data-testid="`user-edit-${row.id}`" @click="openEditDialog(row)">编辑</button>
                <button
                  class="text-button"
                  :data-testid="`user-reset-password-${row.id}`"
                  :disabled="Number(authStore.user?.id) === Number(row.id)"
                  @click="openResetPasswordDialog(row)"
                >
                  重置密码
                </button>
                <button
                  class="text-button"
                  :data-testid="`user-toggle-${row.id}`"
                  :disabled="Number(authStore.user?.id) === Number(row.id)"
                  @click="toggleUserStatus(row)"
                >
                  {{ Number(row.status) === 1 ? '停用' : '启用' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div class="toolbar user-pagination">
        <span class="list-summary">第 {{ page }} / {{ pageCount }} 页</span>
        <div class="toolbar-actions">
          <el-button data-testid="users-prev-page" :disabled="loading || page <= 1" @click="goPrevPage">上一页</el-button>
          <el-button data-testid="users-next-page" :disabled="loading || page >= pageCount" @click="goNextPage">下一页</el-button>
        </div>
      </div>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card user-dialog" data-testid="user-form-dialog">
        <div class="dialog-head">
          <div>
            <h3>{{ dialog.mode === 'create' ? '新增用户' : '编辑用户' }}</h3>
          </div>
          <button class="dialog-close-button" type="button" aria-label="关闭" :disabled="saving" @click="closeDialog">×</button>
        </div>

        <div class="form-grid user-form-grid">
          <label>
            <span>用户名</span>
            <input
              v-model.trim="dialog.form.username"
              data-testid="user-form-username"
              type="text"
              :disabled="usernameFieldDisabled"
              placeholder="例如 operator_d"
            >
          </label>

          <label v-if="dialog.mode === 'create'">
            <span>{{ dialog.mode === 'create' ? '初始密码' : '密码' }}</span>
            <input
              v-model.trim="dialog.form.password"
              data-testid="user-form-password"
              type="password"
              :placeholder="dialog.mode === 'create' ? '至少 6 位' : '请使用“重置密码”修改密码'"
            >
          </label>

          <label>
            <span>姓名 / 显示名</span>
            <input
              v-model.trim="dialog.form.realName"
              data-testid="user-form-real-name"
              type="text"
              placeholder="例如 Field Operator D"
            >
          </label>

          <label>
            <span>角色</span>
            <select v-model="dialog.form.roleCode" data-testid="user-form-role">
              <option v-for="item in roleOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>

          <label :class="{ 'full-width': dialog.mode === 'create' }">
            <span>所属企业</span>
            <select
              v-model="dialog.form.companyId"
              data-testid="user-form-company"
              :disabled="companyFieldDisabled || companyLoading"
            >
              <option value="">{{ companyFieldRequired ? '请选择所属企业' : '当前角色无需绑定企业' }}</option>
              <option v-for="item in formCompanyOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>
        </div>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="dialog-secondary-button" data-testid="user-form-cancel" :disabled="saving" @click="closeDialog">取消</button>
          <button class="dialog-primary-button" data-testid="user-form-submit" :disabled="saving" @click="submitDialog">
            {{ saving ? '正在保存...' : dialog.mode === 'create' ? '确认创建' : '确认保存' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="resetDialog.visible" class="dialog-mask" @click.self="closeResetPasswordDialog">
      <section class="dialog-card reset-password-dialog" data-testid="user-reset-password-dialog">
        <div class="dialog-head">
          <div>
            <h3>重置密码</h3>
            <p>当前用户：{{ resetDialog.displayName }}</p>
          </div>
          <button class="dialog-close-button" type="button" aria-label="关闭" :disabled="resetSaving" @click="closeResetPasswordDialog">×</button>
        </div>

        <div class="form-grid">
          <label class="full-width">
            <span>新密码</span>
            <input
              v-model.trim="resetDialog.password"
              data-testid="user-reset-password-value"
              type="password"
              placeholder="至少 6 位"
            >
          </label>
        </div>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="dialog-secondary-button" data-testid="user-reset-password-cancel" :disabled="resetSaving" @click="closeResetPasswordDialog">取消</button>
          <button class="dialog-primary-button" data-testid="user-reset-password-submit" :disabled="resetSaving" @click="submitResetPassword">
            {{ resetSaving ? '正在重置...' : '确认重置' }}
          </button>
        </div>
      </section>
    </div>
  </div>
  </div>
</template>

<style scoped>
.user-filter-grid {
  grid-template-columns: minmax(0, 1.6fr) minmax(180px, 1fr) minmax(180px, 1fr) auto auto minmax(180px, max-content) minmax(240px, 1fr);
}

.summary-slot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 8px;
}

.user-page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 188px;
}

.user-page-size-select {
  width: 128px;
}

.user-ledger-panel {
  margin-top: 0;
}

.user-pagination {
  margin-top: 18px;
  padding: 0 28px;
}

.user-pagination .list-summary {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
}

.user-pagination .toolbar-actions {
  align-items: center;
}

.user-table-head,
.user-row {
  grid-template-columns:
    minmax(220px, 1fr)
    minmax(220px, 0.95fr)
    minmax(120px, 0.52fr)
    minmax(150px, 0.65fr)
    minmax(180px, 0.8fr)
    minmax(250px, max-content);
}

.dialog-card.user-dialog {
  width: min(calc(100vw - 40px), 420px);
  max-width: 420px;
  padding: 20px 18px;
}

.dialog-card.reset-password-dialog {
  width: min(calc(100vw - 40px), 400px);
  max-width: 400px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(12, 24, 43, 0.42);
}

.dialog-card {
  width: min(calc(100vw - 40px), 760px);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 20px 18px;
  border: 1px solid rgba(56, 134, 217, 0.12);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow:
    0 22px 54px rgba(45, 113, 194, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-head h3 {
  margin: 0;
  color: var(--admin-text);
}

.dialog-head p {
  margin: 6px 0 0;
  color: var(--admin-text-soft);
}

.dialog-close-button,
.dialog-primary-button,
.dialog-secondary-button {
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
}

.dialog-close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  min-width: 36px;
  min-height: 36px;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #8e9caf;
  font-size: 26px;
  line-height: 1;
  box-shadow: none;
}

.dialog-primary-button {
  min-height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, var(--admin-primary) 0%, var(--admin-primary-deep) 100%);
  color: #fff;
  box-shadow: 0 12px 22px rgba(48, 149, 246, 0.16);
}

.dialog-secondary-button {
  min-height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid rgba(56, 134, 217, 0.16);
  background: #fff;
  color: var(--admin-primary-deep);
}

.dialog-close-button:hover:not(:disabled) {
  color: var(--admin-text);
  background: rgba(120, 146, 173, 0.1);
  transform: none;
}

.dialog-primary-button:hover:not(:disabled),
.dialog-secondary-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.dialog-close-button:disabled,
.dialog-primary-button:disabled,
.dialog-secondary-button:disabled {
  opacity: 0.58;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.dialog-card label {
  display: block;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.dialog-card.user-dialog .user-form-grid {
  grid-template-columns: minmax(0, 1fr);
  gap: 14px;
}

.dialog-card label span {
  display: block;
  margin-bottom: 8px;
  color: var(--admin-text-soft);
  font-size: 14px;
}

.dialog-card input,
.dialog-card select {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid var(--admin-border);
  border-radius: 16px;
  background: var(--admin-surface-soft);
  color: var(--admin-text);
}

.full-width {
  grid-column: 1 / -1;
}

@media (max-width: 960px) {
  .user-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .user-page-size-control {
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .dialog-mask {
    padding: 14px;
  }

  .summary-slot {
    justify-content: flex-start;
    padding-right: 0;
  }

  .user-page-size-control {
    justify-content: space-between;
  }

  .user-page-size-select {
    width: 100%;
  }

  .form-grid,
  .user-filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
<style src="../../assets/styles/admin-task-pages.css" scoped></style>
