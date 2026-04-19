<template>
  <div class="page-shell">
  <div class="manage-page product-manage" data-testid="products-page">
    <AdminListTemplate
      template-class="product-card-stack"
      filter-card-class="product-filter-card"
      ledger-card-class="product-ledger-panel product-ledger-card"
    >
    <template #banner>
      <el-card v-if="isEnterpriseAdmin" shadow="never" class="profile-banner" data-testid="products-self-mode">
        <strong>当前为本企业产品模式</strong>
        <span>{{ scopedModeHint }}</span>
      </el-card>
    </template>

    <template #filterPrimary>
      <div class="product-filter-layout">
        <div class="manage-filter-grid product-filter-grid">
          <label class="manage-filter-field product-filter-field product-filter-field--company">
            <span class="manage-filter-field__label">企业</span>
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
          </label>

          <label class="manage-filter-field product-filter-field product-filter-field--keyword">
            <span class="manage-filter-field__label">关键词</span>
            <el-input
              v-model.trim="searchForm.keyword"
              clearable
              class="manage-filter-item"
              placeholder="按产品名称、编码、分类、规格搜索"
              @keyup.enter="handleSearch"
            />
          </label>

          <label class="manage-filter-field product-filter-field product-filter-field--status">
            <span class="manage-filter-field__label">状态</span>
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
        </label>
          <label class="manage-filter-field product-filter-field product-filter-field--page-size">
            <span class="manage-filter-field__label">每页显示</span>
            <el-select
              :model-value="pageSize"
              class="manage-filter-item product-page-size-select"
              data-testid="products-page-size"
              @change="handlePageSizeChange"
            >
              <el-option v-for="item in pageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
        </div>

      </div>
    </template>

    <template #filterSecondary>
      <div class="product-filter-toolbar">
        <div class="product-filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>

        <div class="product-list-summary" role="status" aria-live="polite">
          <span class="manage-muted">{{ listSummary }}</span>
        </div>
      </div>
    </template>

    <template #summary>
      <div class="manage-summary">
        <button
          v-for="chip in summaryChips"
          :key="chip.value"
          type="button"
          class="manage-summary-chip manage-summary-chip--interactive"
          :class="{ 'is-active': searchForm.status === chip.value }"
          :data-testid="`products-summary-${chip.value.toLowerCase()}`"
          @click="handleSummaryChipClick(chip.value)"
        >
          <span>{{ chip.label }}</span>
          <strong>{{ chip.count }}</strong>
        </button>
      </div>

    </template>

    <template #actions>
        <el-button data-testid="products-refresh-button" :loading="loading" @click="loadProducts">刷新</el-button>
        <el-button v-if="canManage" type="primary" data-testid="products-open-create" @click="openCreateDialog">新增产品</el-button>
    </template>

    <template #ledger>
      <div class="panel-heading">
        <div>
          <h2 class="panel-heading__title">{{ tableTitle }}</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div>
          <h3>正在加载产品列表...</h3>
        </div>
      </div>

      <div v-else-if="!productList.length" class="empty-state">
        <div>
          <h3>当前条件下没有产品数据</h3>
          <div class="toolbar-actions">
            <button v-if="canManage" class="primary" @click="openCreateDialog">新增产品</button>
          </div>
        </div>
      </div>

      <div v-else class="table-scroll-shell ledger-table-shell product-table-shell">
        <div class="ledger-table-head product-table-head">
          <span>产品</span>
          <span>企业</span>
          <span>分类 / 产地</span>
          <span>规格 / 单位</span>
          <span class="table-head-cell--center">状态</span>
          <span class="table-head-cell--center">批次</span>
          <span v-if="canManage">操作</span>
        </div>

        <div class="ledger-row-list product-row-list">
          <article
            v-for="row in visibleProductList"
            :key="row.id"
            class="ledger-row product-row"
            :data-testid="`products-row-${row.id}`"
          >
            <div class="row-main">
              <strong>{{ textOf(row.productName, '未命名产品') }}</strong>
              <span>{{ textOf(row.productCode, '未设置产品编码') }}</span>
            </div>

            <div class="row-meta">
              <strong>{{ textOf(row.companyName, '未关联企业') }}</strong>
            </div>

            <div class="row-meta">
              <strong>{{ textOf(row.category, '未分类') }}</strong>
              <small>{{ textOf(row.originPlace, '未填写产地') }}</small>
            </div>

            <div class="row-meta">
              <strong>{{ joinSpec(row.specification, row.unit) }}</strong>
            </div>

            <div class="row-status table-cell--center">
              <span class="ledger-status-pill" :class="statusClass(row.status)">{{ statusText(row.statusLabel || row.status) }}</span>
            </div>

            <div class="row-meta table-cell--center">
              <strong>{{ row.batchCount ?? 0 }}</strong>
            </div>

            <div v-if="canManage" class="row-actions table-cell--actions">
              <div class="row-actions-scroll ledger-actions-scroll">
                <button class="text-button primary-text" :data-testid="`products-edit-${row.id}`" @click="openEditDialog(row)">编辑</button>
                <button
                  class="text-button"
                  :data-testid="`products-toggle-${row.id}`"
                  @click="handleStatusChange(row, row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED', row.status === 'ENABLED' ? '停用' : '启用')"
                >
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </button>
                <button
                  v-if="row.status !== 'ARCHIVED'"
                  class="text-button"
                  :data-testid="`products-archive-${row.id}`"
                  @click="handleStatusChange(row, 'ARCHIVED', '归档')"
                >
                  归档
                </button>
                <button
                  class="text-button manage-danger-link"
                  :data-testid="`products-delete-${row.id}`"
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

      <AdminListPagination
        :summary="`第 ${page} / ${pageCount} 页`"
        :prev-disabled="loading || page <= 1"
        :next-disabled="loading || page >= pageCount"
        prev-testid="products-prev-page"
        next-testid="products-next-page"
        @prev="goPrevPage"
        @next="goNextPage"
      />
    </template>
    </AdminListTemplate>

    <el-dialog
      v-model="showDialog"
      :title="dialogMode === 'create' ? '新增产品' : '编辑产品'"
      width="760px"
      @closed="resetForm"
    >
      <el-form :model="form" label-position="top" class="dialog-form dialog-form--grouped" data-testid="products-form-dialog">
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
          <el-input
            v-model.trim="form.productName"
            maxlength="64"
            show-word-limit
            placeholder="请输入产品名称"
            data-testid="products-form-name"
          />
        </el-form-item>

        <el-form-item label="产品编码（选填）">
          <el-input
            v-model.trim="form.productCode"
            maxlength="64"
            show-word-limit
            placeholder="可选，便于内部台账和打印标识"
            data-testid="products-form-code"
          />
        </el-form-item>

        <div class="dialog-section-title">产品资料</div>

        <el-form-item label="产品分类（必填）" required>
          <el-input
            v-model.trim="form.category"
            maxlength="32"
            show-word-limit
            placeholder="如水果、茶叶、粮油"
            data-testid="products-form-category"
          />
        </el-form-item>

        <el-form-item label="产地（必填）" required>
          <el-input
            v-model.trim="form.originPlace"
            maxlength="128"
            show-word-limit
            placeholder="请输入主要产地"
            data-testid="products-form-origin"
          />
        </el-form-item>

        <el-form-item label="规格（选填）">
          <el-input
            v-model.trim="form.specification"
            maxlength="64"
            show-word-limit
            placeholder="可选，如 5kg / 箱"
            data-testid="products-form-specification"
          />
        </el-form-item>

        <el-form-item label="计量单位（选填）">
          <el-input
            v-model.trim="form.unit"
            maxlength="16"
            show-word-limit
            placeholder="可选，如 箱、斤、袋"
            data-testid="products-form-unit"
          />
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
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" data-testid="products-form-submit" @click="handleSubmit">
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
import AdminListTemplate from '../components/AdminListTemplate.vue'
import AdminListPagination from '../components/AdminListPagination.vue'
import { useAuthStore } from '../stores/auth'
import { getCompanyList, getProductList, createProduct, updateProduct, updateProductStatus, deleteProduct } from '../api/master-data'
import { normalizeDisplayText } from '../utils/display'
import { extractErrorMessage } from '../utils/feedback'

const authStore = useAuthStore()
const DEFAULT_PAGE_SIZE = 10
const roleCode = computed(() => authStore.user?.roleCode || '')
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
const allProductList = ref([])
const companyOptions = ref([])
const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const pageSizeOptions = [
  { value: 10, label: '10 条 / 页' },
  { value: 20, label: '20 条 / 页' },
  { value: 50, label: '50 条 / 页' },
  { value: 100, label: '100 条 / 页' }
]

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

const pageTitle = computed(() => (isEnterpriseAdmin.value ? '本企业产品' : '产品管理'))
const pageDesc = computed(() => {
  if (isEnterpriseAdmin.value) {
    return `这里只显示并维护 ${currentCompanyName.value} 的产品资料，企业归属已固定，不能切换到其他企业。`
  }
  return '维护产品基础资料，供企业建批次、补录追溯和公开查询统一调用。'
})

const scopedModeHint = computed(() => `当前账号只维护 ${currentCompanyName.value} 的产品，前后端都会按本企业范围校验。`)
const tableTitle = computed(() => (isEnterpriseAdmin.value ? '本企业产品台账' : '产品台账'))
const tableTip = computed(() => {
  if (isEnterpriseAdmin.value) {
    return '当前仅展示本企业产品，企业归属已固定。'
  }
  return '优先按企业、状态和关键词筛选，右侧可直接执行编辑、启用、停用、归档和删除。'
})
const summaryLabel = computed(() => (isEnterpriseAdmin.value ? '当前可维护' : '当前共'))

function isSuccessResponse(res) {
  return res?.success === true || res?.code === 0 || String(res?.code) === '0'
}

const summary = computed(() => allProductList.value.reduce((result, item) => {
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

const summaryChips = computed(() => ([
  { value: 'ENABLED', label: '启用中', count: summary.value.enabled },
  { value: 'DISABLED', label: '已停用', count: summary.value.disabled },
  { value: 'ARCHIVED', label: '已归档', count: summary.value.archived }
]))

const pageCount = computed(() => Math.max(1, Math.ceil(Number(productList.value.length || 0) / Number(pageSize.value || DEFAULT_PAGE_SIZE))))
const visibleProductList = computed(() => {
  const fromIndex = (page.value - 1) * pageSize.value
  return productList.value.slice(fromIndex, fromIndex + pageSize.value)
})
const listSummary = computed(() => {
  if (!productList.value.length) {
    return '当前没有产品数据。'
  }
  const from = (page.value - 1) * pageSize.value + 1
  const to = Math.min(productList.value.length, page.value * pageSize.value)
  return `当前共 ${productList.value.length} 个产品，当前显示 ${from}-${to} 个。`
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
  searchForm.status = searchForm.status === status ? '' : status
  page.value = 1
  loadProducts()
}

function handlePageSizeChange(value) {
  const nextPageSize = Number(value || DEFAULT_PAGE_SIZE)
  if (nextPageSize === pageSize.value) return
  pageSize.value = nextPageSize
  page.value = 1
}

function joinSpec(specification, unit) {
  const specText = textOf(specification, '')
  const unitText = textOf(unit, '')
  if (specText && unitText) return `${specText} / ${unitText}`
  return specText || unitText || '未填写'
}

function validateProductForm() {
  if (!form.companyId) {
    return '请先选择所属企业。'
  }
  if (!form.productName.trim()) {
    return '请填写产品名称。'
  }
  if (!form.category.trim()) {
    return '请补充产品分类。'
  }
  if (!form.originPlace.trim()) {
    return '请填写主要产地。'
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
    const [listRes, summaryRes] = await Promise.all([
      getProductList({
        companyId: searchForm.companyId || undefined,
        keyword: searchForm.keyword || undefined,
        status: searchForm.status || undefined
      }),
      getProductList({
        companyId: isCompanyLocked.value ? authStore.user?.companyId : undefined
      })
    ])

    if (isSuccessResponse(listRes)) {
      productList.value = listRes.data || []
      if (page.value > pageCount.value) {
        page.value = pageCount.value
      }
    } else {
      ElMessage.error(listRes.message || '产品列表加载失败')
    }

    if (isSuccessResponse(summaryRes)) {
      allProductList.value = summaryRes.data || []
    } else {
      ElMessage.error(summaryRes.message || '产品统计加载失败')
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
  page.value = 1
  pageSize.value = DEFAULT_PAGE_SIZE
  loadProducts()
}

function handleSearch() {
  page.value = 1
  loadProducts()
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
      ElMessage.success(dialogMode.value === 'create' ? '产品已创建。' : '产品资料已更新。')
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
    ElMessage.warning('该产品已关联批次，暂不能删除。')
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
  font-family: "PingFang SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif;
  --product-filter-text-inset: 14px;
  --product-grid-inline-padding: 18px;
  --product-grid-column-gap: 16px;
  --product-filter-group-left-shift: 8px;
  --product-ledger-columns:
    minmax(220px, 1.02fr)
    minmax(178px, 0.8fr)
    minmax(208px, 0.84fr)
    minmax(154px, 0.64fr)
    minmax(112px, 0.46fr)
    minmax(84px, 0.32fr)
    minmax(242px, 0.86fr);
  --product-company-filter-width: 168px;
  --product-keyword-filter-width: 280px;
  --product-status-filter-width: 116px;
  --product-page-size-width: 148px;
  --product-filter-card-border: rgba(56, 134, 217, 0.14);
  --product-filter-card-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 251, 255, 0.94) 100%);
  --product-filter-card-shadow: 0 16px 34px rgba(45, 113, 194, 0.1);
  --product-filter-control-height: 40px;
  --product-filter-control-radius: 12px;
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

.product-card-stack {
  display: grid;
  gap: 16px;
}

:deep(.product-filter-card),
:deep(.product-ledger-card) {
  position: relative;
  overflow: hidden;
}

:deep(.product-filter-card) {
  padding: 18px 22px 0;
  border-color: var(--product-filter-card-border) !important;
  background: var(--product-filter-card-bg) !important;
  box-shadow: var(--product-filter-card-shadow) !important;
}

.product-filter-layout {
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  gap: 18px;
  margin-left: calc(-1 * var(--product-filter-group-left-shift));
  padding: 0 12px 0 calc(var(--product-grid-inline-padding) - var(--product-filter-text-inset));
  flex-wrap: wrap;
}

.product-filter-grid {
  grid-template-columns:
    minmax(var(--product-company-filter-width), max-content)
    minmax(var(--product-keyword-filter-width), max-content)
    minmax(var(--product-status-filter-width), max-content)
    minmax(var(--product-page-size-width), max-content);
  align-items: end;
  column-gap: 12px;
  row-gap: 12px;
  padding: 0;
  flex: 0 1 auto;
  min-width: 0;
}

.product-filter-field {
  gap: 8px;
  width: 100%;
  justify-self: start;
}

.product-filter-field--company {
  max-width: var(--product-company-filter-width);
}

.product-filter-field--keyword {
  max-width: var(--product-keyword-filter-width);
}

.product-filter-field--status {
  max-width: var(--product-status-filter-width);
}

.product-filter-field--page-size {
  max-width: var(--product-page-size-width);
}

.product-filter-grid .manage-filter-field__label {
  padding-inline-start: var(--product-filter-text-inset);
  color: var(--admin-text-mid);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.product-filter-grid :deep(.manage-filter-item .el-input__wrapper),
.product-filter-grid :deep(.manage-filter-item .el-select__wrapper) {
  min-height: var(--product-filter-control-height);
  padding-inline-start: var(--product-filter-text-inset);
  padding-inline-end: 14px;
  border-radius: var(--product-filter-control-radius);
  background: #fff;
  box-shadow: 0 0 0 1px rgba(56, 134, 217, 0.14) inset !important;
}

.product-filter-grid :deep(.manage-filter-item .el-input__inner),
.product-filter-grid :deep(.manage-filter-item .el-select__selected-item),
.product-filter-grid :deep(.manage-filter-item .el-select__placeholder) {
  text-align: left;
}

.product-filter-grid :deep(.manage-filter-item .el-select__placeholder),
.product-filter-grid :deep(.manage-filter-item .el-input__inner::placeholder) {
  color: var(--admin-text-faint);
}

:deep(.product-ledger-panel .panel-heading) {
  padding-left: 28px;
}

.product-filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 14px;
  margin-top: 14px;
  margin-left: calc(-1 * var(--product-filter-group-left-shift));
  padding: 0 12px 18px var(--product-grid-inline-padding);
  min-width: 0;
  flex-wrap: wrap;
}

.product-list-summary {
  margin: 0;
  padding: 0;
}

.product-list-summary .manage-muted {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  color: var(--admin-text-soft);
  font-size: 13px;
}

:deep(.product-ledger-card) {
  padding: 20px 22px 18px;
  border: 1px solid rgba(56, 134, 217, 0.14) !important;
  border-radius: 24px !important;
  background: #fff !important;
  box-shadow: 0 18px 42px rgba(45, 113, 194, 0.08) !important;
}

.product-table-shell {
  --ledger-grid-columns: var(--product-ledger-columns);
  --ledger-column-gap: var(--product-grid-column-gap);
  --ledger-inline-padding: var(--product-grid-inline-padding);
  --ledger-min-width: 1500px;
}

.product-table-head {
  color: #6f86a4;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  background: #fff;
}

.product-row-list,
.product-row-list.ledger-row-list {
  display: grid;
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba(56, 134, 217, 0.14);
  border-radius: 26px;
  background: #fff;
}

:deep(.product-ledger-card .table-scroll-shell) {
  background: #fff !important;
}

:deep(.product-ledger-card .admin-list-pagination) {
  background: #fff !important;
}

:deep(.product-ledger-card .table-scroll-shell .product-table-head) {
  background: #fff !important;
}

:deep(.product-ledger-card .panel-heading),
:deep(.product-ledger-card .panel-heading > div) {
  background: #fff;
}

.product-row,
.product-row.ledger-row {
  align-items: center;
  border-top: 1px solid rgba(56, 134, 217, 0.1);
  background: #fff;
}

.product-row:first-child {
  border-top: 0;
}

.product-row:hover {
  background: rgba(48, 149, 246, 0.03);
}

.product-page-size-select {
  width: 100%;
}

.product-page-size-select :deep(.el-select__wrapper) {
  min-height: var(--product-filter-control-height);
}

.product-filter-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.product-filter-actions :deep(.el-button) {
  min-height: 38px;
  padding-inline: 16px;
  border-radius: 12px;
}

.panel-heading__title {
  font-size: 18px;
  font-weight: 700;
}

.row-main,
.row-meta {
  gap: 6px;
  min-width: 0;
}

.row-main strong {
  color: var(--admin-text);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: 0.01em;
}

.row-main span,
.row-meta small {
  color: var(--admin-text-soft);
  font-size: 13px;
  line-height: 1.5;
}

.row-meta strong {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
}

.row-status,
.row-actions {
  display: flex;
  align-items: center;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.row-actions-scroll {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.row-actions-scroll .text-button {
  min-height: 34px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
  color: #2f5f95;
}

.row-actions-scroll .primary-text {
  font-weight: 600;
}

.product-row .table-cell--center .ledger-status-pill {
  min-width: 92px;
}

.table-action-link {
  padding: 0;
  font-weight: 600;
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

@media (max-width: 1280px) {
  .product-filter-layout {
    align-items: flex-end;
    gap: 14px;
    margin-left: 0;
    padding-inline: 0;
  }

  .product-filter-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .product-filter-toolbar {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}

@media (max-width: 960px) {
  .product-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-filter-field--company,
  .product-filter-field--keyword,
  .product-filter-field--status {
    max-width: none;
  }
}

@media (max-width: 768px) {
  :deep(.product-ledger-panel) {
    padding: 16px;
  }

  :deep(.product-ledger-panel .panel-heading) {
    padding-left: 0;
  }

  .product-filter-grid,
  .dialog-form--grouped {
    grid-template-columns: 1fr;
  }

  :deep(.product-filter-card) {
    padding-bottom: 0;
  }

  .product-filter-toolbar {
    justify-content: flex-start;
    min-width: 0;
    width: 100%;
  }

  .product-page-size-select {
    width: 100%;
  }
}
</style>
<style src="../assets/styles/admin-task-pages.css" scoped></style>
