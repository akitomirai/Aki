<template>
  <div class="manage-page company-manage">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">企业管理</h1>
        <p class="manage-page-desc">统一维护企业基础档案，支持后续产品归属、批次建档和追溯信息关联。</p>
      </div>
      <div class="manage-page-actions">
        <el-button @click="loadCompanies" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">新增企业</el-button>
      </div>
    </section>

    <el-card shadow="never" class="manage-filter-card">
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
      <div class="manage-summary-chip">启用中<strong>{{ summary.enabled }}</strong></div>
      <div class="manage-summary-chip">已停用<strong>{{ summary.disabled }}</strong></div>
      <div class="manage-summary-chip">已归档<strong>{{ summary.archived }}</strong></div>
    </div>

    <el-card shadow="never" class="manage-table-card">
      <template #header>
        <div class="manage-table-header">
          <div>
            <p class="manage-table-title">企业档案</p>
            <p class="manage-table-tip">优先按关键词和状态筛选，再执行编辑、停用、归档和删除操作。</p>
          </div>
        </div>
      </template>

      <el-table :data="companies" v-loading="loading" border stripe empty-text="暂无企业数据">
        <el-table-column label="企业名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <strong>{{ textOf(row.name, '未命名企业') }}</strong>
              <span>{{ textOf(row.licenseNo, '未填写营业执照号') }}</span>
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

        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button type="primary" link class="table-action-link" @click="openEditDialog(row)">编辑</el-button>
              <el-dropdown @command="(command) => handleMoreCommand(row, command)">
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
      :title="dialogMode === 'create' ? '新增企业' : '编辑企业'"
      width="560px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="92px" class="dialog-form">
        <el-form-item label="企业名称" required>
          <el-input v-model.trim="form.name" maxlength="64" show-word-limit placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="营业执照号">
          <el-input v-model.trim="form.licenseNo" maxlength="64" show-word-limit placeholder="可选，便于备案管理" />
        </el-form-item>
        <el-form-item label="联系人" required>
          <el-input v-model.trim="form.contactPerson" maxlength="32" show-word-limit placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model.trim="form.contactPhone" maxlength="32" show-word-limit placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系地址" required>
          <el-input
            v-model.trim="form.address"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入联系地址"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogMode === 'create' ? '确认新增' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCompanyList, createCompany, updateCompany, updateCompanyStatus, deleteCompany } from '../api/master-data'
import { normalizeDisplayText } from '../utils/display'
import { extractErrorMessage } from '../utils/feedback'

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

async function loadCompanies() {
  loading.value = true
  try {
    const res = await getCompanyList({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined
    })
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
  if (!form.name || !form.contactPerson || !form.contactPhone || !form.address) {
    ElMessage.warning('请补全企业名称、联系人、电话和地址')
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
      ElMessage.success(dialogMode.value === 'create' ? '企业已新增' : '企业已更新')
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

@media (max-width: 768px) {
  .company-manage {
    padding: 14px;
  }

  .summary-slot {
    justify-content: flex-start;
    padding-right: 0;
  }
}
</style>
