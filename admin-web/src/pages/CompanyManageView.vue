<template>
  <div class="manage-page company-manage" data-testid="companies-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">{{ pageTitle }}</h1>
        <p class="manage-page-desc">{{ pageDesc }}</p>
      </div>
      <div class="manage-page-actions">
        <el-button data-testid="companies-refresh-button" @click="loadCompanies" :loading="loading">刷新</el-button>
        <el-button
          v-if="canCreateCompany"
          type="primary"
          data-testid="companies-open-create"
          @click="openCreateDialog"
        >
          新增企业
        </el-button>
      </div>
    </section>

    <el-card v-if="isEnterpriseAdmin" shadow="never" class="profile-banner" data-testid="companies-self-mode">
      <strong>当前为本企业资料模式</strong>
      <span>这里只展示并维护你所在企业的基础资料，不涉及全平台企业管理、状态调整或企业删除。</span>
    </el-card>

    <el-card v-if="isPlatformAdmin" shadow="never" class="manage-filter-card">
      <div class="manage-filter-grid company-filter-grid">
        <el-input
          v-model.trim="searchForm.keyword"
          clearable
          class="manage-filter-item"
          placeholder="按企业名称、联系人、电话或地址搜索"
          @keyup.enter="loadCompanies"
        />

        <el-select
          v-model="searchForm.status"
          clearable
          class="manage-filter-item"
          placeholder="状态"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <div class="summary-slot">
          <span class="manage-muted">当前共 {{ companies.length }} 家企业</span>
        </div>

        <el-button type="primary" @click="loadCompanies">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <div class="manage-summary">
      <button
        v-if="isPlatformAdmin"
        type="button"
        class="manage-summary-chip manage-summary-chip--interactive"
        :class="{ 'is-active': searchForm.status === '' }"
        data-testid="companies-summary-all"
        @click="handleSummaryChipClick('')"
      >
        <span>{{ summaryLabel }}</span>
        <strong>{{ companies.length }}</strong>
      </button>
      <div v-else class="manage-summary-chip">{{ summaryLabel }} <strong>{{ companies.length }}</strong></div>
      <button
        v-if="isPlatformAdmin"
        type="button"
        class="manage-summary-chip manage-summary-chip--interactive"
        :class="{ 'is-active': searchForm.status === 'ENABLED' }"
        data-testid="companies-summary-enabled"
        @click="handleSummaryChipClick('ENABLED')"
      >
        <span>启用中</span>
        <strong>{{ summary.enabled }}</strong>
      </button>
      <button
        v-if="isPlatformAdmin"
        type="button"
        class="manage-summary-chip manage-summary-chip--interactive"
        :class="{ 'is-active': searchForm.status === 'DISABLED' }"
        data-testid="companies-summary-disabled"
        @click="handleSummaryChipClick('DISABLED')"
      >
        <span>已停用</span>
        <strong>{{ summary.disabled }}</strong>
      </button>
      <button
        v-if="isPlatformAdmin"
        type="button"
        class="manage-summary-chip manage-summary-chip--interactive"
        :class="{ 'is-active': searchForm.status === 'ARCHIVED' }"
        data-testid="companies-summary-archived"
        @click="handleSummaryChipClick('ARCHIVED')"
      >
        <span>已归档</span>
        <strong>{{ summary.archived }}</strong>
      </button>
    </div>

    <el-card shadow="never" class="manage-table-card">
      <template #header>
        <div class="manage-table-header">
          <div>
            <p class="manage-table-title">{{ tableTitle }}</p>
            <p class="manage-table-tip">{{ tableTip }}</p>
          </div>
        </div>
      </template>

      <el-table :data="companies" v-loading="loading" border stripe empty-text="暂无企业数据">
        <el-table-column label="企业名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <strong>{{ textOf(row.name, '未命名企业') }}</strong>
              <span>{{ textOf(row.licenseNo, '未填写许可证号') }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="联系人" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ textOf(row.contactPerson, '未填写') }}
          </template>
        </el-table-column>

        <el-table-column label="联系电话" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ textOf(row.contactPhone, '未填写') }}
          </template>
        </el-table-column>

        <el-table-column label="地址" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ textOf(row.address, '未填写地址') }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              effect="plain"
              size="small"
              class="manage-status-tag"
              :class="statusClass(row.status)"
            >
              {{ statusText(row.statusLabel || row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="产品数" width="90">
          <template #default="{ row }">
            {{ row.productCount ?? 0 }}
          </template>
        </el-table-column>

        <el-table-column label="批次数" width="90">
          <template #default="{ row }">
            {{ row.batchCount ?? 0 }}
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button
                type="primary"
                link
                class="table-action-link"
                :data-testid="`company-edit-${row.id}`"
                @click="openEditDialog(row)"
              >
                编辑
              </el-button>
              <el-dropdown v-if="canManageCompanyStatus" @command="(command) => handleMoreCommand(row, command)">
                <el-button link class="table-action-link">
                  更多操作
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="row.status === 'ENABLED'" command="disable">停用</el-dropdown-item>
                    <el-dropdown-item v-else command="enable">启用</el-dropdown-item>
                    <el-dropdown-item v-if="row.status !== 'ARCHIVED'" command="archive">归档</el-dropdown-item>
                    <el-dropdown-item command="delete" :disabled="!row.canDelete">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="showDialog"
      :title="dialogTitle"
      width="720px"
      @closed="resetForm"
    >
      <el-form :model="form" label-position="top" class="dialog-form dialog-form--grouped" data-testid="company-form-dialog">
        <div class="dialog-section-title">基础信息</div>
        <el-form-item label="企业名称（必填）" required>
          <el-input v-model.trim="form.name" maxlength="64" show-word-limit placeholder="请输入企业名称" data-testid="company-form-name" />
        </el-form-item>
        <el-form-item label="许可证号（选填）">
          <el-input v-model.trim="form.licenseNo" maxlength="64" show-word-limit placeholder="便于备案和回查" data-testid="company-form-license" />
        </el-form-item>

        <div class="dialog-section-title">联系信息</div>
        <el-form-item label="联系人（必填）" required>
          <el-input v-model.trim="form.contactPerson" maxlength="32" show-word-limit placeholder="请输入联系人姓名" data-testid="company-form-contact-person" />
        </el-form-item>
        <el-form-item label="联系电话（必填）" required>
          <el-input v-model.trim="form.contactPhone" maxlength="32" show-word-limit placeholder="至少保留一个可回拨号码" data-testid="company-form-contact-phone" />
        </el-form-item>
        <el-form-item label="联系地址（必填）" required class="full-row">
          <el-input
            v-model.trim="form.address"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写园区、仓库或办公地点"
            data-testid="company-form-address"
          />
        </el-form-item>

        <template v-if="canManageCompanyStatus">
          <div class="dialog-section-title">状态设置</div>
          <el-form-item label="当前状态">
            <el-select v-model="form.status" placeholder="请选择状态">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" data-testid="company-form-submit" @click="handleSubmit">
          {{ dialogMode === 'create' ? '确认新增' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { getCompanyList, createCompany, updateCompany, updateCompanyStatus, deleteCompany } from '../api/master-data'
import { normalizeDisplayText } from '../utils/display'
import { extractErrorMessage } from '../utils/feedback'

const authStore = useAuthStore()
const roleCode = computed(() => authStore.user?.roleCode || '')
const isPlatformAdmin = computed(() => roleCode.value === 'PLATFORM_ADMIN')
const isEnterpriseAdmin = computed(() => roleCode.value === 'ENTERPRISE_ADMIN')
const canCreateCompany = computed(() => isPlatformAdmin.value)
const canManageCompanyStatus = computed(() => isPlatformAdmin.value)

const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)

const companies = ref([])

const searchForm = reactive({
  keyword: '',
  status: ''
})

const form = reactive({
  name: '',
  licenseNo: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  status: 'ENABLED'
})

const statusOptions = [
  { value: 'ENABLED', label: '启用中' },
  { value: 'DISABLED', label: '已停用' },
  { value: 'ARCHIVED', label: '已归档' }
]

const pageTitle = computed(() => isEnterpriseAdmin.value ? '本企业资料' : '企业管理')
const pageDesc = computed(() => {
  if (isEnterpriseAdmin.value) {
    return '这里只展示你所在企业的基础资料，你可以维护名称、联系人、联系电话和地址，但不能新建、删除或切换到其他企业。'
  }
  return '统一维护企业基础档案，支撑后续产品归属、批次建档和追溯信息关联。'
})
const tableTitle = computed(() => isEnterpriseAdmin.value ? '本企业资料卡片' : '企业档案')
const tableTip = computed(() => {
  if (isEnterpriseAdmin.value) {
    return '当前仅展示并维护你所在企业这一条资料，保存后会同步影响产品归属、批次建档和回查信息。'
  }
  return '优先按关键字和状态筛选，再执行编辑、停用、归档和删除操作。'
})
const summaryLabel = computed(() => isEnterpriseAdmin.value ? '当前可维护企业' : '当前企业总数')
const dialogTitle = computed(() => {
  if (dialogMode.value === 'create') {
    return '新增企业'
  }
  return isEnterpriseAdmin.value ? '编辑本企业资料' : '编辑企业'
})

function isSuccessResponse(res) {
  return res?.success === true || res?.code === 0 || String(res?.code) === '0'
}

const summary = computed(() => companies.value.reduce((result, item) => {
  const key = String(item.status || '').toUpperCase()
  if (key === 'DISABLED') result.disabled += 1
  else if (key === 'ARCHIVED') result.archived += 1
  else result.enabled += 1
  return result
}, {
  enabled: 0,
  disabled: 0,
  archived: 0
}))

function textOf(value, fallback = '-') {
  return normalizeDisplayText(value, fallback)
}

function statusText(value) {
  const normalized = String(value || '').trim().toUpperCase()
  if (normalized === 'ENABLED' || normalized === 'ACTIVE') return '启用中'
  if (normalized === 'DISABLED' || normalized === 'INACTIVE') return '已停用'
  if (normalized === 'ARCHIVED') return '已归档'
  return normalizeDisplayText(value, '未知状态')
}

function statusClass(value) {
  const normalized = String(value || '').trim().toUpperCase()
  if (normalized === 'DISABLED' || normalized === 'INACTIVE') return 'is-disabled'
  if (normalized === 'ARCHIVED') return 'is-archived'
  return 'is-enabled'
}

function handleSummaryChipClick(status) {
  searchForm.status = status
  loadCompanies()
}

function validateCompanyForm() {
  if (!form.name.trim()) {
    return '请先填写企业名称，后续产品和批次都会挂到这个企业下。'
  }
  if (!form.contactPerson.trim()) {
    return '请填写联系人，方便后续批次、质检或风险问题回查。'
  }
  if (!form.contactPhone.trim()) {
    return '请填写联系电话，至少保留一个能联系到企业的号码。'
  }
  if (form.contactPhone.trim().length < 6) {
    return '联系电话看起来太短了，请再核对一次。'
  }
  if (!form.address.trim()) {
    return '请填写联系地址，至少写到园区、仓库或办公地点。'
  }
  return ''
}

async function loadCompanies() {
  loading.value = true
  try {
    const params = isPlatformAdmin.value
      ? {
          keyword: searchForm.keyword || undefined,
          status: searchForm.status || undefined
        }
      : {}
    const res = await getCompanyList(params)
    if (isSuccessResponse(res)) {
      companies.value = res.data || []
    } else {
      ElMessage.error(res.message || '企业列表加载失败')
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '企业列表加载失败'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.licenseNo = ''
  form.contactPerson = ''
  form.contactPhone = ''
  form.address = ''
  form.status = 'ENABLED'
}

function openCreateDialog() {
  if (!canCreateCompany.value) {
    return
  }
  dialogMode.value = 'create'
  resetForm()
  showDialog.value = true
}

function openEditDialog(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.name = textOf(row.name, '')
  form.licenseNo = textOf(row.licenseNo, '')
  form.contactPerson = textOf(row.contactPerson, '')
  form.contactPhone = textOf(row.contactPhone, '')
  form.address = textOf(row.address, '')
  form.status = String(row.status || 'ENABLED').toUpperCase()
  showDialog.value = true
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = ''
  loadCompanies()
}

async function handleSubmit() {
  const validationMessage = validateCompanyForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name,
      licenseNo: form.licenseNo || undefined,
      contactPerson: form.contactPerson,
      contactPhone: form.contactPhone,
      address: form.address,
      status: form.status
    }

    const res = dialogMode.value === 'create'
      ? await createCompany(payload)
      : await updateCompany(editingId.value, payload)

    if (isSuccessResponse(res)) {
      ElMessage.success(dialogMode.value === 'create' ? '企业已创建，可继续到产品管理补产品。' : '企业资料已更新。')
      showDialog.value = false
      await loadCompanies()
    } else {
      ElMessage.error(res.message || '企业保存失败')
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '企业保存失败'))
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(row, status, actionLabel) {
  try {
    await ElMessageBox.confirm(
      `确认将“${textOf(row.name, '该企业')}”设为${actionLabel}吗？`,
      `${actionLabel}企业`,
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )

    const res = await updateCompanyStatus(row.id, status)
    if (isSuccessResponse(res)) {
      ElMessage.success(`企业已${actionLabel}`)
      await loadCompanies()
    } else {
      ElMessage.error(res.message || `${actionLabel}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(extractErrorMessage(error, `${actionLabel}失败`))
    }
  }
}

async function handleDelete(row) {
  if (!row.canDelete) {
    ElMessage.warning('该企业已关联产品或批次，暂不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认删除“${textOf(row.name, '该企业')}”吗？删除后无法恢复。`,
      '删除企业',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )

    const res = await deleteCompany(row.id)
    if (isSuccessResponse(res)) {
      ElMessage.success('企业已删除')
      await loadCompanies()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(extractErrorMessage(error, '删除失败'))
    }
  }
}

function handleMoreCommand(row, command) {
  if (command === 'disable') {
    handleStatusChange(row, 'DISABLED', '停用')
    return
  }
  if (command === 'enable') {
    handleStatusChange(row, 'ENABLED', '启用')
    return
  }
  if (command === 'archive') {
    handleStatusChange(row, 'ARCHIVED', '归档')
    return
  }
  if (command === 'delete') {
    handleDelete(row)
  }
}

onMounted(() => {
  loadCompanies()
})
</script>

<style scoped>
.company-manage {
  padding: 20px;
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

.company-filter-grid {
  grid-template-columns: minmax(0, 2fr) minmax(180px, 1fr) minmax(180px, 1fr) auto auto;
}

.summary-slot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 8px;
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.name-cell strong {
  color: var(--admin-text);
  font-size: 14px;
}

.name-cell span {
  color: var(--admin-text-soft);
  font-size: 12px;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-action-link {
  padding: 0;
}

.dialog-form :deep(.el-select),
.dialog-form :deep(.el-input),
.dialog-form :deep(.el-textarea) {
  width: 100%;
}

.dialog-section-title {
  color: var(--admin-text);
}

.dialog-form--grouped {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.dialog-form--grouped :deep(.el-form-item) {
  margin-bottom: 18px;
}

.dialog-form--grouped :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: var(--admin-text);
  font-weight: 600;
  line-height: 1.3;
}

.dialog-section-title,
.full-row {
  grid-column: 1 / -1;
}

.dialog-section-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .company-manage {
    padding: 14px;
  }

  .summary-slot {
    justify-content: flex-start;
    padding-right: 0;
  }

  .dialog-form--grouped {
    grid-template-columns: 1fr;
  }
}
</style>
