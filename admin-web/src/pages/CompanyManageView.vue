<template>
  <div class="page-shell">
  <div class="manage-page company-manage" data-testid="companies-page">
    <el-card v-if="isEnterpriseAdmin" shadow="never" class="profile-banner" data-testid="companies-self-mode">
      <strong>当前为本企业资料模式</strong>
      <span>这里只显示并维护你所在企业的基础资料，不涉及全平台企业管理。</span>
    </el-card>

    <el-card v-if="isPlatformAdmin" shadow="never" class="manage-filter-card">
      <div class="manage-filter-grid company-filter-grid">
        <el-input
          v-model.trim="searchForm.keyword"
          clearable
          class="manage-filter-item"
          placeholder="按企业名称、联系人、电话或地址搜索"
          @keyup.enter="handleSearch"
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

        <div class="company-page-size-control">
          <span class="manage-muted">每页显示</span>
          <el-select
            :model-value="pageSize"
            class="company-page-size-select"
            data-testid="companies-page-size"
            @change="handlePageSizeChange"
          >
            <el-option v-for="item in pageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>

        <div class="summary-slot">
          <span class="manage-muted">{{ listSummary }}</span>
        </div>

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <div class="manage-summary-row">
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
          <strong>{{ allCompanies.length }}</strong>
        </button>
        <div v-else class="manage-summary-chip">{{ summaryLabel }} <strong>{{ allCompanies.length }}</strong></div>

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

      <div class="manage-summary-actions">
        <el-button data-testid="companies-refresh-button" :loading="loading" @click="loadCompanies">刷新</el-button>
        <el-button
          v-if="canCreateCompany"
          type="primary"
          data-testid="companies-open-create"
          @click="openCreateDialog"
        >
          新增企业
        </el-button>
      </div>
    </div>

    <section class="panel ledger-panel company-ledger-panel">
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">{{ tableTitle }}</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div>
          <h3>正在加载企业列表...</h3>
        </div>
      </div>

      <div v-else-if="!companies.length" class="empty-state">
        <div>
          <h3>当前筛选下没有企业数据</h3>
          <div class="toolbar-actions">
            <button v-if="canCreateCompany" class="primary" @click="openCreateDialog">新增企业</button>
          </div>
        </div>
      </div>

      <div v-else class="table-scroll-shell ledger-table-shell company-table-shell">
        <div class="ledger-table-head company-table-head">
          <span>企业</span>
          <span>联系人 / 电话</span>
          <span>地址</span>
          <span>状态</span>
          <span>产品 / 批次</span>
          <span>操作</span>
        </div>

        <div class="ledger-row-list">
          <article
            v-for="row in visibleCompanies"
            :key="row.id"
            class="ledger-row company-row"
            :data-testid="`company-row-${row.id}`"
          >
            <div class="row-main">
              <strong>{{ textOf(row.name, '未命名企业') }}</strong>
              <span>{{ textOf(row.licenseNo, '未填写许可证号') }}</span>
            </div>

            <div class="row-meta">
              <strong>{{ textOf(row.contactPerson, '未填写') }}</strong>
              <small>{{ textOf(row.contactPhone, '未填写') }}</small>
            </div>

            <div class="row-meta">
              <strong>{{ textOf(row.address, '未填写地址') }}</strong>
            </div>

            <div class="row-status">
              <span class="ledger-status-pill" :class="statusClass(row.status)">
                {{ statusText(row.statusLabel || row.status) }}
              </span>
            </div>

            <div class="row-meta">
              <strong>{{ row.productCount ?? 0 }} 个产品</strong>
              <small>{{ row.batchCount ?? 0 }} 个批次</small>
            </div>

            <div class="row-actions">
              <div class="ledger-actions-scroll">
                <button class="text-button primary-text" :data-testid="`company-edit-${row.id}`" @click="openEditDialog(row)">编辑</button>
                <button
                  v-if="canManageCompanyStatus"
                  class="text-button"
                  :data-testid="`company-toggle-${row.id}`"
                  @click="handleStatusChange(row, row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED', row.status === 'ENABLED' ? '停用' : '启用')"
                >
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </button>
                <button
                  v-if="canManageCompanyStatus && row.status !== 'ARCHIVED'"
                  class="text-button"
                  :data-testid="`company-archive-${row.id}`"
                  @click="handleStatusChange(row, 'ARCHIVED', '归档')"
                >
                  归档
                </button>
                <button
                  v-if="canManageCompanyStatus"
                  class="text-button manage-danger-link"
                  :data-testid="`company-delete-${row.id}`"
                  :disabled="!row.canDelete"
                  @click="handleDelete(row)"
                >
                  删除
                </button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div class="toolbar company-pagination">
        <span class="list-summary">第 {{ page }} / {{ pageCount }} 页</span>
        <div class="toolbar-actions">
          <el-button data-testid="companies-prev-page" :disabled="loading || page <= 1" @click="goPrevPage">上一页</el-button>
          <el-button data-testid="companies-next-page" :disabled="loading || page >= pageCount" @click="goNextPage">下一页</el-button>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="showDialog"
      :title="dialogTitle"
      width="720px"
      @closed="resetForm"
    >
      <el-form :model="form" label-position="top" class="dialog-form dialog-form--grouped" data-testid="company-form-dialog">
        <div class="dialog-section-title">基础信息</div>

        <el-form-item label="企业名称（必填）" required>
          <el-input
            v-model.trim="form.name"
            maxlength="64"
            show-word-limit
            placeholder="请输入企业名称"
            data-testid="company-form-name"
          />
        </el-form-item>

        <el-form-item label="许可证号（选填）">
          <el-input
            v-model.trim="form.licenseNo"
            maxlength="64"
            show-word-limit
            placeholder="便于备案和回查"
            data-testid="company-form-license"
          />
        </el-form-item>

        <div class="dialog-section-title">联系信息</div>

        <el-form-item label="联系人（必填）" required>
          <el-input
            v-model.trim="form.contactPerson"
            maxlength="32"
            show-word-limit
            placeholder="请输入联系人姓名"
            data-testid="company-form-contact-person"
          />
        </el-form-item>

        <el-form-item label="联系电话（必填）" required>
          <el-input
            v-model.trim="form.contactPhone"
            maxlength="32"
            show-word-limit
            placeholder="至少保留一个可回拨号码"
            data-testid="company-form-contact-phone"
          />
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
const DEFAULT_PAGE_SIZE = 10
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
const allCompanies = ref([])
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]

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

const pageTitle = computed(() => (isEnterpriseAdmin.value ? '本企业资料' : '企业管理'))
const pageDesc = computed(() => {
  if (isEnterpriseAdmin.value) {
    return '这里只显示你所在企业的基础资料，你可以维护名称、联系人、电话和地址。'
  }
  return '统一维护企业基础档案，支持后续产品归属、批次建档和追溯信息关联。'
})
const tableTitle = computed(() => (isEnterpriseAdmin.value ? '本企业资料卡片' : '企业档案'))
const tableTip = computed(() => {
  if (isEnterpriseAdmin.value) {
    return '当前仅维护你所在企业这一条资料。'
  }
  return '优先按关键词和状态筛选，再执行编辑、启用、停用、归档和删除。'
})
const summaryLabel = computed(() => (isEnterpriseAdmin.value ? '当前可维护企业' : '当前企业总数'))
const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增企业' : (isEnterpriseAdmin.value ? '编辑本企业资料' : '编辑企业')))

function isSuccessResponse(res) {
  return res?.success === true || res?.code === 0 || String(res?.code) === '0'
}

const summary = computed(() => allCompanies.value.reduce((result, item) => {
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

const pageCount = computed(() => Math.max(1, Math.ceil(Number(companies.value.length || 0) / Number(pageSize.value || DEFAULT_PAGE_SIZE))))
const visibleCompanies = computed(() => {
  const fromIndex = (page.value - 1) * pageSize.value
  return companies.value.slice(fromIndex, fromIndex + pageSize.value)
})
const listSummary = computed(() => {
  if (!companies.value.length) {
    return '当前没有企业数据。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(companies.value.length, page.value * pageSize.value)
  return `当前共 ${companies.value.length} 家企业，当前显示 ${from}-${to} 家。`
})

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
  page.value = 1
  loadCompanies()
}

function handlePageSizeChange(value) {
  const nextPageSize = Number(value || DEFAULT_PAGE_SIZE)
  if (nextPageSize === pageSize.value) return
  pageSize.value = nextPageSize
  page.value = 1
}

function validateCompanyForm() {
  if (!form.name.trim()) {
    return '请填写企业名称。'
  }
  if (!form.contactPerson.trim()) {
    return '请填写联系人。'
  }
  if (!form.contactPhone.trim()) {
    return '请填写联系电话。'
  }
  if (form.contactPhone.trim().length < 6) {
    return '联系电话看起来太短了，请再核对一次。'
  }
  if (!form.address.trim()) {
    return '请填写联系地址。'
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
    const [listRes, summaryRes] = await Promise.all([
      getCompanyList(params),
      getCompanyList()
    ])

    if (isSuccessResponse(listRes)) {
      companies.value = listRes.data || []
      if (page.value > pageCount.value) {
        page.value = pageCount.value
      }
    } else {
      ElMessage.error(listRes.message || '企业列表加载失败')
    }

    if (isSuccessResponse(summaryRes)) {
      allCompanies.value = summaryRes.data || []
    } else {
      ElMessage.error(summaryRes.message || '企业统计加载失败')
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
  if (!canCreateCompany.value) return
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
  page.value = 1
  pageSize.value = DEFAULT_PAGE_SIZE
  loadCompanies()
}

function handleSearch() {
  page.value = 1
  loadCompanies()
}

function goPrevPage() {
  if (loading.value || page.value <= 1) return
  page.value -= 1
}

function goNextPage() {
  if (loading.value || page.value >= pageCount.value) return
  page.value += 1
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
      ElMessage.success(dialogMode.value === 'create' ? '企业已创建。' : '企业资料已更新。')
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
    ElMessage.warning('该企业已关联产品或批次，暂不能删除。')
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

onMounted(async () => {
  await loadCompanies()
})
</script>

<style scoped>
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
  grid-template-columns: minmax(280px, 1.4fr) minmax(140px, 0.72fr) minmax(188px, max-content) minmax(210px, 1fr) auto auto;
}

.summary-slot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 8px;
}

.company-page-size-control {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 188px;
}

.company-page-size-select {
  width: 128px;
}

.company-pagination {
  margin-top: 18px;
  padding: 0 28px;
}

.company-pagination .list-summary {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
}

.company-pagination .toolbar-actions {
  align-items: center;
}

.company-ledger-panel {
  margin-top: 0;
}

.company-table-head,
.company-row {
  grid-template-columns:
    minmax(240px, 1fr)
    minmax(190px, 0.82fr)
    minmax(280px, 1.2fr)
    minmax(120px, 0.5fr)
    minmax(150px, 0.65fr)
    minmax(250px, max-content);
}

.dialog-form :deep(.el-select),
.dialog-form :deep(.el-input) {
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

@media (max-width: 1080px) {
  .company-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .company-page-size-control {
    min-width: 0;
  }

  .summary-slot {
    justify-content: flex-start;
    padding-right: 0;
  }
}

@media (max-width: 768px) {
  .company-filter-grid,
  .dialog-form--grouped {
    grid-template-columns: 1fr;
  }

  .company-page-size-control {
    justify-content: space-between;
  }

  .company-page-size-select {
    width: 100%;
  }
}
</style>
<style src="../assets/styles/admin-task-pages.css" scoped></style>
