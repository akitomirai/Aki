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

const listSummary = computed(() => {
  if (isPlatformAdmin.value) {
    return `共 ${rows.value.length} 个用户，当前显示 ${visibleRows.value.length} 个。`
  }
  return `当前仅展示本企业用户，共 ${rows.value.length} 个，当前显示 ${visibleRows.value.length} 个。`
})

const companyFilterEnabled = computed(() => isPlatformAdmin.value)
const currentCompanyName = computed(() => authStore.user?.companyName || '当前企业')
const companyFieldRequired = computed(() => roleNeedsCompany(dialog.value.form.roleCode))
const companyFieldDisabled = computed(() => !companyFieldRequired.value || isEnterpriseAdmin.value)

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
  return Number(status) === 1 ? 'published' : 'recalled'
}

function resolveRoleTone(roleCode) {
  return {
    PLATFORM_ADMIN: 'published',
    ENTERPRISE_ADMIN: 'draft',
    OPERATOR: 'published',
    REGULATOR: 'frozen'
  }[String(roleCode || '').toUpperCase()] || 'draft'
}

function resolvePasswordTone(item) {
  return item.needChangePassword ? 'draft' : 'published'
}

function companyText(item) {
  return item.companyName || '平台直属'
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
      companyId: companyFilterEnabled.value ? normalizeCompanyId(filters.value.companyId) || undefined : undefined
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
  <div class="page-shell" data-testid="users-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">用户管理</h1>
        <p class="manage-page-subtitle">
          在后台统一维护平台管理员、企业管理员、现场操作员和监管人员，账号启停、角色归属、首次登录改密和管理员重置密码都从这里处理。
        </p>
      </div>
      <div class="manage-page-actions">
        <button class="ghost" data-testid="users-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
        <button class="primary" data-testid="users-open-create" @click="openCreateDialog">新增用户</button>
      </div>
    </section>

    <section class="panel todo-tabs-panel">
      <div class="todo-tabs">
        <button
          v-for="tab in roleTabs"
          :key="tab.value"
          type="button"
          class="todo-tab"
          :class="{ active: activeRoleTab === tab.value }"
          :data-testid="`users-tab-${tab.value}`"
          @click="activeRoleTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <strong>{{ roleCounts[tab.value] ?? 0 }}</strong>
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="filter-grid">
        <label>
          <span>关键词</span>
          <input
            v-model.trim="filters.keyword"
            data-testid="users-filter-keyword"
            type="text"
            placeholder="输入用户名或姓名"
          >
        </label>

        <label v-if="companyFilterEnabled">
          <span>所属企业</span>
          <select v-model="filters.companyId" data-testid="users-filter-company" :disabled="companyLoading">
            <option value="">全部企业</option>
            <option v-for="item in companyOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <label v-else>
          <span>所属企业</span>
          <input :value="currentCompanyName" type="text" disabled>
        </label>

        <label>
          <span>用户状态</span>
          <select v-model="filters.status" data-testid="users-filter-status">
            <option value="">全部状态</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
          </select>
        </label>

        <label>
          <span>当前角色筛选</span>
          <input :value="roleTabs.find((item) => item.value === activeRoleTab)?.label || '全部用户'" type="text" disabled>
        </label>
      </div>

      <div class="toolbar">
        <span class="list-summary">{{ listSummary }}</span>
        <div class="toolbar-actions">
          <button class="primary" data-testid="users-search-button" :disabled="loading" @click="fetchRows">查询</button>
          <button class="ghost" data-testid="users-reset-button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section v-if="loading" class="panel empty-state">
      <div>
        <h3>正在加载用户列表...</h3>
        <p class="empty-copy">请稍等，系统正在汇总角色、企业、账号状态和密码状态。</p>
      </div>
    </section>

    <section v-else-if="!visibleRows.length" class="panel empty-state">
      <div>
        <h3>当前筛选下没有用户</h3>
        <p class="empty-copy">可以调整角色、企业、状态或关键词后再查看。</p>
      </div>
    </section>

    <section v-else class="panel">
      <div class="todo-table-head user-head">
        <span>用户名</span>
        <span>姓名</span>
        <span>角色</span>
        <span>所属企业</span>
        <span>状态</span>
        <span>密码状态</span>
        <span>最近更新时间</span>
        <span>操作</span>
      </div>

      <div class="todo-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="todo-row user-row"
          :data-testid="`users-row-${item.id}`"
        >
          <div class="row-main">
            <strong>{{ item.username }}</strong>
            <small>ID {{ item.id }}</small>
          </div>

          <div class="row-main">
            <strong>{{ item.realName }}</strong>
            <small>{{ item.username }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="resolveRoleTone(item.roleCode)">{{ item.roleName }}</span>
            <small>{{ item.roleCode }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ companyText(item) }}</strong>
            <small>{{ item.companyId ? `企业 ID ${item.companyId}` : '平台直属账号' }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="resolveStatusTone(item.status)">{{ item.statusLabel }}</span>
            <small>{{ Number(item.status) === 1 ? '可登录，可参与新分配' : '不可登录，不可参与新分配' }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="resolvePasswordTone(item)">{{ item.passwordStatusLabel }}</span>
            <small>{{ item.passwordUpdatedAt || '尚未设置' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ item.updatedAt || '暂无更新' }}</strong>
            <small>最近更新时间</small>
          </div>

          <div class="row-actions">
            <button class="text-button" :data-testid="`user-edit-${item.id}`" @click="openEditDialog(item)">编辑</button>
            <button
              class="text-button"
              :data-testid="`user-reset-password-${item.id}`"
              :disabled="Number(authStore.user?.id) === Number(item.id)"
              @click="openResetPasswordDialog(item)"
            >
              重置密码
            </button>
            <button
              class="text-button"
              :data-testid="`user-toggle-${item.id}`"
              :disabled="Number(authStore.user?.id) === Number(item.id)"
              @click="toggleUserStatus(item)"
            >
              {{ Number(item.status) === 1 ? '停用' : '启用' }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card user-dialog" data-testid="user-form-dialog">
        <div class="dialog-head">
          <div>
            <h3>{{ dialog.mode === 'create' ? '新增用户' : '编辑用户' }}</h3>
            <p>
              {{ dialog.mode === 'create'
                ? '新建账号后会自动标记为“首次登录需改密”，交付给同事后需要先完成改密。'
                : '可修改显示名、角色和企业归属，保存后列表会立即刷新。' }}
            </p>
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
            <small class="field-note">
              <template v-if="!companyFieldRequired">平台管理员和监管人员默认不绑定企业。</template>
              <template v-else-if="isEnterpriseAdmin">企业管理员只能管理本企业用户：{{ currentCompanyName }}</template>
              <template v-else>企业管理员和现场操作员必须绑定所属企业。</template>
            </small>
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
            <p>为 {{ resetDialog.displayName }} 设置新的登录密码。重置后，该用户下次登录必须先修改密码。</p>
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

<style src="../../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.user-head,
.user-row {
  grid-template-columns:
    minmax(0, 1fr)
    minmax(0, 1.1fr)
    minmax(0, 0.9fr)
    minmax(0, 1fr)
    minmax(0, 0.95fr)
    minmax(0, 1fr)
    minmax(0, 1fr)
    minmax(0, 1.2fr);
}

.user-dialog,
.reset-password-dialog {
  width: min(760px, 100%);
}

.field-note {
  line-height: 1.6;
}

@media (max-width: 760px) {
  .user-row {
    grid-template-columns: 1fr;
  }
}
</style>
