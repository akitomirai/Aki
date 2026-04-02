<template>
  <div class="manage-page product-manage" data-testid="products-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">{{ pageTitle }}</h1>
        <p class="manage-page-desc">{{ pageDesc }}</p>
      </div>
      <div class="manage-page-actions">
        <el-button data-testid="products-refresh-button" @click="loadProducts" :loading="loading">刷新</el-button>
        <el-button v-if="canManage" type="primary" data-testid="products-open-create" @click="openCreateDialog">新增产品</el-button>
      </div>
    </section>

    <el-card v-if="isEnterpriseAdmin" shadow="never" class="profile-banner" data-testid="products-self-mode">
      <strong>当前为本企业产品模式</strong>
      <span>{{ scopedModeHint }}</span>
    </el-card>

    <el-card shadow="never" class="manage-filter-card">
      <div class="manage-filter-grid">
        <el-select
          v-model="searchForm.companyId"
          clearable
          filterable
          class="manage-filter-item"
          placeholder="选择企业"
          :disabled="isCompanyLocked"
          data-testid="products-filter-company"
        >
          <el-option
            v-for="item in companyOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>

        <el-input
          v-model.trim="searchForm.keyword"
          clearable
          class="manage-filter-item"
          placeholder="按产品名称、编码、分类、规格搜索"
          @keyup.enter="loadProducts"
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
          <span class="manage-muted">{{ summaryLabel }} {{ productList.length }} 个产品</span>
        </div>

        <el-button type="primary" @click="loadProducts">查询</el-button>
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
            <p class="manage-table-title">{{ tableTitle }}</p>
            <p class="manage-table-tip">{{ tableTip }}</p>
          </div>
        </div>
      </template>

      <el-table
        :data="productList"
        v-loading="loading"
        border
        stripe
        empty-text="暂无产品数据"
        data-testid="products-table"
      >
        <el-table-column label="产品名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell" :data-testid="`products-row-${row.id}`">
              <strong>{{ textOf(row.productName, '未命名产品') }}</strong>
              <span>{{ textOf(row.productCode, '未设置产品编码') }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="所属企业" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ textOf(row.companyName, '未关联企业') }}
          </template>
        </el-table-column>

        <el-table-column label="分类" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ textOf(row.category, '未分类') }}
          </template>
        </el-table-column>

        <el-table-column label="产地" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ textOf(row.originPlace, '未填写产地') }}
          </template>
        </el-table-column>

        <el-table-column label="规格 / 单位" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ joinSpec(row.specification, row.unit) }}
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

        <el-table-column label="关联批次" width="100">
          <template #default="{ row }">
            {{ row.batchCount ?? 0 }}
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="260" fixed="right" v-if="canManage">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button type="primary" link class="table-action-link" :data-testid="`products-edit-${row.id}`" @click="openEditDialog(row)">编辑</el-button>
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
      :title="dialogMode === 'create' ? '新增产品' : '编辑产品'"
      width="700px"
      @closed="resetForm"
    >
      <div class="dialog-intro-card">
        <strong>{{ dialogMode === 'create' ? '先把产品归到企业下，再继续去批次管理建批次。' : '当前正在调整产品资料，保存后会同步回到产品台账。' }}</strong>
        <span>产品名称、企业归属和产地会直接影响批次建档、工作台展示和公开查询。</span>
      </div>

      <el-form :model="form" label-width="126px" class="dialog-form dialog-form--grouped" data-testid="products-form-dialog">
        <div class="dialog-section-title">归属关系</div>
        <el-form-item label="所属企业（必填）" required>
          <el-select
            v-model="form.companyId"
            filterable
            placeholder="请选择企业"
            :disabled="isCompanyLocked"
            data-testid="products-form-company"
          >
            <el-option
              v-for="item in companyOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="产品名称（必填）" required>
          <el-input v-model.trim="form.productName" maxlength="64" show-word-limit placeholder="请输入产品名称" data-testid="products-form-name" />
        </el-form-item>
        <el-form-item label="产品编码（选填）">
          <el-input v-model.trim="form.productCode" maxlength="64" show-word-limit placeholder="可选，便于内部台账和打印标识" data-testid="products-form-code" />
        </el-form-item>
        <div class="dialog-section-title">产品资料</div>
        <el-form-item label="产品分类（必填）" required>
          <el-input v-model.trim="form.category" maxlength="32" show-word-limit placeholder="如水果、茶叶、粮油" data-testid="products-form-category" />
        </el-form-item>
        <el-form-item label="产地（必填）" required>
          <el-input v-model.trim="form.originPlace" maxlength="128" show-word-limit placeholder="请输入主要产地" data-testid="products-form-origin" />
        </el-form-item>
        <el-form-item label="规格（选填）">
          <el-input v-model.trim="form.specification" maxlength="64" show-word-limit placeholder="可选，如 5kg / 箱" data-testid="products-form-specification" />
        </el-form-item>
        <el-form-item label="计量单位（选填）">
          <el-input v-model.trim="form.unit" maxlength="16" show-word-limit placeholder="可选，如 箱、斤、袋" data-testid="products-form-unit" />
        </el-form-item>
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
        <div class="full-row dialog-status-note">
          <strong>{{ statusText(form.status) }}</strong>
          <span>{{ productStatusHint(form.status) }}</span>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" data-testid="products-form-submit" @click="handleSubmit">
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
import { getCompanyList, getProductList, createProduct, updateProduct, updateProductStatus, deleteProduct } from '../api/master-data'
import { normalizeDisplayText } from '../utils/display'
import { extractErrorMessage } from '../utils/feedback'

const authStore = useAuthStore()
const roleCode = computed(() => authStore.user?.roleCode || '')
const isPlatformAdmin = computed(() => roleCode.value === 'PLATFORM_ADMIN')
const isEnterpriseAdmin = computed(() => roleCode.value === 'ENTERPRISE_ADMIN')
const canManage = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'].includes(roleCode.value))
const isCompanyLocked = computed(() => roleCode.value === 'ENTERPRISE_ADMIN' && Boolean(authStore.user?.companyId))
const currentCompanyName = computed(() => authStore.user?.companyName || '当前企业')

const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)

const productList = ref([])
const companyOptions = ref([])

const searchForm = reactive({
  companyId: null,
  keyword: '',
  status: ''
})

const form = reactive({
  companyId: null,
  productName: '',
  productCode: '',
  category: '',
  originPlace: '',
  specification: '',
  unit: '',
  status: 'ENABLED'
})

const statusOptions = [
  { value: 'ENABLED', label: '启用中' },
  { value: 'DISABLED', label: '已停用' },
  { value: 'ARCHIVED', label: '已归档' }
]

const pageTitle = computed(() => isEnterpriseAdmin.value ? '本企业产品' : '产品管理')
const pageDesc = computed(() => {
  if (isEnterpriseAdmin.value) {
    return `这里只展示并维护 ${currentCompanyName.value} 的产品资料，企业归属已固定，不能切换到其他企业。`
  }
  return '维护产品基础资料，供企业建批次、补录追溯和公开查询统一调用。'
})
const scopedModeHint = computed(() => `当前账号只维护 ${currentCompanyName.value} 的产品，前后端都会按本企业范围校验，不能把产品挂到其他企业。`)
const tableTitle = computed(() => isEnterpriseAdmin.value ? '本企业产品台账' : '产品台账')
const tableTip = computed(() => {
  if (isEnterpriseAdmin.value) {
    return '当前仅展示本企业产品，企业归属已经固定；你可以维护本企业产品资料，但不能切换到其他企业。'
  }
  return '优先按企业、状态和关键词筛选，右侧直接执行编辑、停用、归档和删除。'
})
const summaryLabel = computed(() => isEnterpriseAdmin.value ? '当前可维护' : '当前共')

function isSuccessResponse(res) {
  return res?.success === true || res?.code === 0 || String(res?.code) === '0'
}

const summary = computed(() => productList.value.reduce((result, item) => {
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

function productStatusHint(status) {
  const normalized = String(status || '').trim().toUpperCase()
  if (normalized === 'DISABLED') {
    return '已停用的产品会保留历史台账，后续建批次前建议先确认是否继续使用。'
  }
  if (normalized === 'ARCHIVED') {
    return '已归档的产品主要用于历史回查，不建议继续作为日常建档入口。'
  }
  return '启用中的产品可继续用于批次建档、工作台回查和公开展示。'
}

function joinSpec(specification, unit) {
  const specText = textOf(specification, '')
  const unitText = textOf(unit, '')
  if (specText && unitText) return `${specText} / ${unitText}`
  return specText || unitText || '未填写'
}

function validateProductForm() {
  if (!form.companyId) {
    return '请先选择所属企业，产品必须明确归属企业后才能建批次。'
  }
  if (!form.productName.trim()) {
    return '请填写产品名称，后续批次会沿用这个名称展示。'
  }
  if (!form.category.trim()) {
    return '请补充产品分类，方便后台筛选和台账回查。'
  }
  if (!form.originPlace.trim()) {
    return '请填写主要产地，后续批次和公开页都会用到。'
  }
  return ''
}

async function loadCompanyOptions() {
  if (!canManage.value) return
  try {
    const res = await getCompanyList()
    if (isSuccessResponse(res)) {
      companyOptions.value = res.data || []
      if (isCompanyLocked.value && !searchForm.companyId) {
        searchForm.companyId = authStore.user.companyId
      }
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '企业选项加载失败'))
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductList({
      companyId: searchForm.companyId || undefined,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined
    })
    if (isSuccessResponse(res)) {
      productList.value = res.data || []
    } else {
      ElMessage.error(res.message || '产品列表加载失败')
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '产品列表加载失败'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.companyId = isCompanyLocked.value ? authStore.user.companyId : null
  form.productName = ''
  form.productCode = ''
  form.category = ''
  form.originPlace = ''
  form.specification = ''
  form.unit = ''
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
  form.companyId = row.companyId ?? (isCompanyLocked.value ? authStore.user.companyId : null)
  form.productName = textOf(row.productName, '')
  form.productCode = textOf(row.productCode, '')
  form.category = textOf(row.category, '')
  form.originPlace = textOf(row.originPlace, '')
  form.specification = textOf(row.specification, '')
  form.unit = textOf(row.unit, '')
  form.status = String(row.status || 'ENABLED').toUpperCase()
  showDialog.value = true
}

function handleReset() {
  searchForm.companyId = isCompanyLocked.value ? authStore.user.companyId : null
  searchForm.keyword = ''
  searchForm.status = ''
  loadProducts()
}

async function handleSubmit() {
  const validationMessage = validateProductForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  submitting.value = true
  try {
    const payload = {
      companyId: form.companyId,
      productName: form.productName,
      productCode: form.productCode || undefined,
      category: form.category,
      originPlace: form.originPlace,
      coverImage: null,
      specification: form.specification || undefined,
      unit: form.unit || undefined,
      status: form.status
    }

    const res = dialogMode.value === 'create'
      ? await createProduct(payload)
      : await updateProduct(editingId.value, payload)

    if (isSuccessResponse(res)) {
      ElMessage.success(dialogMode.value === 'create' ? '产品已创建，可直接去批次管理建批次。' : '产品资料已更新。')
      showDialog.value = false
      await loadProducts()
    } else {
      ElMessage.error(res.message || '产品保存失败')
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '产品保存失败'))
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(row, status, actionLabel) {
  try {
    await ElMessageBox.confirm(
      `确认将“${textOf(row.productName, '该产品')}”设为${actionLabel}吗？`,
      `${actionLabel}产品`,
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )

    const res = await updateProductStatus(row.id, status)
    if (isSuccessResponse(res)) {
      ElMessage.success(`产品已${actionLabel}`)
      await loadProducts()
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
    ElMessage.warning('该产品已关联批次，暂不能删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除“${textOf(row.productName, '该产品')}”吗？删除后无法恢复。`,
      '删除产品',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    const res = await deleteProduct(row.id)
    if (isSuccessResponse(res)) {
      ElMessage.success('产品已删除')
      await loadProducts()
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

onMounted(async () => {
  if (isCompanyLocked.value && authStore.user?.companyId) {
    searchForm.companyId = authStore.user.companyId
  }
  await loadCompanyOptions()
  resetForm()
  await loadProducts()
})
</script>

<style scoped>
.product-manage {
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
  gap: 16px;
  white-space: nowrap;
}

.table-action-link {
  padding: 0;
  font-weight: 600;
}

.dialog-form :deep(.el-select),
.dialog-form :deep(.el-input) {
  width: 100%;
}

.dialog-intro-card,
.dialog-status-note {
  border: 1px solid var(--admin-border);
  border-radius: 18px;
  background: var(--admin-surface-soft);
}

.dialog-intro-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  margin-bottom: 18px;
}

.dialog-intro-card strong,
.dialog-status-note strong,
.dialog-section-title {
  color: var(--admin-text);
}

.dialog-intro-card span,
.dialog-status-note span {
  color: var(--admin-text-soft);
  line-height: 1.6;
}

.dialog-form--grouped {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.dialog-form--grouped :deep(.el-form-item) {
  margin-bottom: 18px;
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

.dialog-status-note {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
}

@media (max-width: 768px) {
  .product-manage {
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
