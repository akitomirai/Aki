<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { createUser, getUserList, resetUserPassword, updateUser, updateUserStatus } from '../../api/user'
import { getCompanyOptions } from '../../api/batch'
import { useAuthStore } from '../../stores/auth'
import { getFriendlyErrorMessage } from '../../utils/batchExperience'

const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const resetSaving = ref(false)
const companyLoading = ref(false)
const message = ref('')
const messageType = ref('info')
const rows = ref([])
const companyOptions = ref([])
const activeRoleTab = ref('ALL')
const filters = ref(createFilters())
const isPlatformAdmin = computed(() => String(authStore.user?.roleCode || '').toUpperCase() === 'PLATFORM_ADMIN')
const isEnterpriseAdmin = computed(() => String(authStore.user?.roleCode || '').toUpperCase() === 'ENTERPRISE_ADMIN')
const dialog = ref(createDialogState())
const resetDialog = ref(createResetDialogState())

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

const visibleRows = computed(() => {
  if (activeRoleTab.value === 'ALL') {
    return rows.value
  }
  return rows.value.filter((item) => item.roleCode === activeRoleTab.value)
})

const roleCounts = computed(() => {
  const counts = rows.value.reduce((acc, item) => {
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

const enabledCount = computed(() => rows.value.filter((item) => Number(item.status) === 1).length)
const disabledCount = computed(() => rows.value.filter((item) => Number(item.status) !== 1).length)
const passwordPendingCount = computed(() => rows.value.filter((item) => item.needChangePassword).length)
const currentCompanyName = computed(() => authStore.user?.companyName || '当前企业')
const companyFieldRequired = computed(() => roleNeedsCompany(dialog.value.form.roleCode))
const companyFieldDisabled = computed(() => !companyFieldRequired.value || isEnterpriseAdmin.value)

const listSummary = computed(() => {
  if (isPlatformAdmin.value) {
    return `共 ${rows.value.length} 个用户，当前显示 ${visibleRows.value.length} 个。`
  }
  return `当前仅展示本企业用户，共 ${rows.value.length} 个，当前显示 ${visibleRows.value.length} 个。`
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
  return item.companyName || '平台主管账号'
}

function handleStatusChipClick(status) {
  filters.value.status = filters.value.status === status ? '' : status
  fetchRows()
}

function resetFilters() {
  filters.value = createFilters()
  activeRoleTab.value = 'ALL'
  fetchRows()
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
    const response = await getUserList(cleanObject({
      keyword: filters.value.keyword || undefined,
      status: filters.value.status === '' ? undefined : Number(filters.value.status),
      companyId: isPlatformAdmin.value ? normalizeCompanyId(filters.value.companyId) || undefined : undefined
    }))
    rows.value = response.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '用户列表加载失败，请稍后重试。'), 'error')
  } finally {
    loading.value = false
  }
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
  if (saving.value) {
    return
  }
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
  if (resetSaving.value) {
    return
  }
  resetDialog.value = createResetDialogState()
}

function normalizeUserPayload() {
  const form = dialog.value.form
  const payload = {
    realName: String(form.realName || '').trim(),
    roleCode: String(form.roleCode || '').trim(),
    companyId: companyFieldRequired.value ? normalizeCompanyId(form.companyId) : null
  }
  if (dialog.value.mode === 'create') {
    payload.username = String(form.username || '').trim()
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
  if (!confirmed) {
    return
  }

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
  <div class="manage-page user-manage" data-testid="users-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">用户管理</h1>
        <p class="manage-page-desc">统一维护平台管理员、企业管理员、现场操作员和监管人员账号，先筛选，再执行编辑、重置密码和启停。</p>
      </div>
      <div class="manage-page-actions">
        <el-button data-testid="users-refresh-button" @click="fetchRows" :loading="loading">刷新</el-button>
        <el-button type="primary" data-testid="users-open-create" @click="openCreateDialog">新增用户</el-button>
      </div>
    </section>

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

        <div class="summary-slot">
          <span class="manage-muted">{{ listSummary }}</span>
        </div>

        <el-button type="primary" data-testid="users-search-button" @click="fetchRows">查询</el-button>
        <el-button data-testid="users-reset-button" @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <el-card shadow="never" class="manage-table-card">
      <template #header>
        <div class="manage-table-header">
          <div>
            <p class="manage-table-title">用户台账</p>
            <p class="manage-table-tip">列表按用户名、角色、企业和状态统一回看，右侧直接执行编辑、重置密码和启停。</p>
          </div>
        </div>
      </template>

      <el-table
        :data="visibleRows"
        v-loading="loading"
        border
        stripe
        empty-text="当前筛选下没有用户"
        data-testid="users-table"
      >
        <el-table-column label="用户名" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell" :data-testid="`users-row-${row.id}`">
              <strong>{{ row.username }}</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="姓名" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.realName || '未填写' }}
          </template>
        </el-table-column>

        <el-table-column label="角色" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="manage-status-tag" :class="resolveRoleTone(row.roleCode)">
              {{ row.roleName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="所属企业" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <strong>{{ companyText(row) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="manage-status-tag" :class="resolveStatusTone(row.status)">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="密码状态" min-width="130">
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="manage-status-tag" :class="resolvePasswordTone(row)">
              {{ row.passwordStatusLabel }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最近更新时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.updatedAt || '暂无更新' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button type="primary" link class="table-action-link" :data-testid="`user-edit-${row.id}`" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                type="primary"
                link
                class="table-action-link"
                :data-testid="`user-reset-password-${row.id}`"
                :disabled="Number(authStore.user?.id) === Number(row.id)"
                @click="openResetPasswordDialog(row)"
              >
                重置密码
              </el-button>
              <el-button
                type="primary"
                link
                class="table-action-link"
                :data-testid="`user-toggle-${row.id}`"
                :disabled="Number(authStore.user?.id) === Number(row.id)"
                @click="toggleUserStatus(row)"
              >
                {{ Number(row.status) === 1 ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card user-dialog" data-testid="user-form-dialog">
        <div class="dialog-head">
          <div>
            <h3>{{ dialog.mode === 'create' ? '新增用户' : '编辑用户' }}</h3>
          </div>
          <button class="ghost" :disabled="saving" @click="closeDialog">关闭</button>
        </div>

        <div class="form-grid">
          <label>
            <span>用户名</span>
            <input
              v-model.trim="dialog.form.username"
              data-testid="user-form-username"
              type="text"
              :disabled="dialog.mode === 'edit'"
              placeholder="例如 operator_d"
            >
          </label>

          <label>
            <span>{{ dialog.mode === 'create' ? '初始密码' : '密码' }}</span>
            <input
              v-model.trim="dialog.form.password"
              data-testid="user-form-password"
              type="password"
              :disabled="dialog.mode === 'edit'"
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

          <label class="full-width">
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
          <button class="ghost" data-testid="user-form-cancel" :disabled="saving" @click="closeDialog">取消</button>
          <button class="primary" data-testid="user-form-submit" :disabled="saving" @click="submitDialog">
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
          </div>
          <button class="ghost" :disabled="resetSaving" @click="closeResetPasswordDialog">关闭</button>
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
          <button class="ghost" data-testid="user-reset-password-cancel" :disabled="resetSaving" @click="closeResetPasswordDialog">取消</button>
          <button class="primary" data-testid="user-reset-password-submit" :disabled="resetSaving" @click="submitResetPassword">
            {{ resetSaving ? '正在重置...' : '确认重置' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.user-manage {
  padding: 20px;
}

.user-filter-grid {
  grid-template-columns: minmax(0, 1.6fr) minmax(180px, 1fr) minmax(180px, 1fr) minmax(240px, 1fr) auto auto;
}

.summary-slot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 8px;
}

.name-cell,
.stack-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.name-cell strong,
.stack-cell strong {
  color: var(--admin-text);
  font-size: 14px;
}

.name-cell span,
.stack-cell span {
  color: var(--admin-text-soft);
  font-size: 12px;
  line-height: 1.5;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 14px;
  white-space: nowrap;
}

.table-action-link {
  padding: 0;
  font-weight: 600;
}

.user-dialog,
.reset-password-dialog {
  width: min(760px, 100%);
}

.dialog-head h3 {
  margin: 0;
  color: var(--admin-text);
}

.dialog-card label {
  display: block;
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
}

@media (max-width: 768px) {
  .user-manage {
    padding: 14px;
  }

  .summary-slot {
    justify-content: flex-start;
    padding-right: 0;
  }

  .user-filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
