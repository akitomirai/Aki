<template>
  <div class="product-manage">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="header-title">产品管理</div>
            <div class="header-subtitle">统一管理产品基础信息，支持快速筛选与编辑。</div>
          </div>
          <div class="header-ops">
            <el-button class="header-btn" @click="loadProducts" :loading="loading">刷新</el-button>
            <el-button v-if="canManage" class="header-btn" type="primary" @click="openCreateDialog">新增产品</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
          v-model.trim="keyword"
          clearable
          class="search-item keyword-input"
          placeholder="搜索产品名称 / 分类 / 规格 / 单位"
        />
        <el-select v-model="selectedCategory" clearable class="search-item category-select" placeholder="按分类筛选">
          <el-option
            v-for="item in categoryOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
        <el-button class="search-btn" type="primary" @click="handleSearch">查询</el-button>
        <el-button class="search-btn" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="displayList" v-loading="loading" border stripe empty-text="暂无产品数据">
        <el-table-column label="产品信息" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <div class="cell-main" :title="formatProductName(row.name)">{{ formatProductName(row.name) }}</div>
              <div class="cell-sub">产品ID：{{ row.id ?? '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分类" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="soft-tag">{{ formatCategory(row.category) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="规格" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span :title="formatText(row.spec, '未设置规格')">{{ formatText(row.spec, '未设置规格') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="单位" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span :title="formatUnit(row.unit)">{{ formatUnit(row.unit) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column v-if="canManage" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="showDialog"
      :title="dialogMode === 'create' ? '新增产品' : '编辑产品'"
      width="420px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model.trim="form.name" placeholder="请输入产品名称" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model.trim="form.category" placeholder="可选" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model.trim="form.spec" placeholder="可选" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model.trim="form.unit" placeholder="可选" maxlength="16" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { listProductApi, createProductApi, updateProductApi } from '../../api/product'
import { formatDateTime, normalizeDisplayText } from '../../utils/display'
import { extractErrorMessage } from '../../utils/feedback'

const authStore = useAuthStore()
const roleCode = computed(() => authStore.user?.roleCode || '')
const canManage = computed(() => roleCode.value === 'PLATFORM_ADMIN')

const loading = ref(false)
const productList = ref([])

const keyword = ref('')
const selectedCategory = ref('')

const showDialog = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const editingId = ref(null)
const form = reactive({
  name: '',
  category: '',
  spec: '',
  unit: ''
})

const formatText = (value, fallback = '-') => normalizeDisplayText(value, fallback)
const formatProductName = (value) => formatText(value, '未命名产品')
const formatCategory = (value) => formatText(value, '未分类')
const formatUnit = (value) => formatText(value, '未设置')

const categoryOptions = computed(() => {
  const set = new Set()
  productList.value.forEach((item) => {
    const text = normalizeDisplayText(item?.category, '')
    if (text) set.add(text)
  })
  return Array.from(set)
})

const displayList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const category = selectedCategory.value
  return (productList.value || []).filter((item) => {
    if (category && formatCategory(item.category) !== category) return false
    if (!kw) return true
    const text = [
      formatProductName(item.name),
      formatCategory(item.category),
      formatText(item.spec, ''),
      formatText(item.unit, '')
    ].join(' ').toLowerCase()
    return text.includes(kw)
  })
})

const resetForm = () => {
  editingId.value = null
  form.name = ''
  form.category = ''
  form.spec = ''
  form.unit = ''
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  resetForm()
  showDialog.value = true
}

const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.name = normalizeDisplayText(row.name, '')
  form.category = normalizeDisplayText(row.category, '')
  form.spec = normalizeDisplayText(row.spec, '')
  form.unit = normalizeDisplayText(row.unit, '')
  showDialog.value = true
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await listProductApi(roleCode.value)
    if (res.code === 0) {
      productList.value = res.data || []
      if (!productList.value.length) {
        ElMessage.info('暂无产品数据')
      }
    } else {
      ElMessage.error(res.message || '产品列表加载失败')
    }
  } catch (e) {
    ElMessage.error(extractErrorMessage(e, '产品列表加载失败'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {}

const handleReset = () => {
  keyword.value = ''
  selectedCategory.value = ''
}

const handleSubmit = async () => {
  if (!form.name) {
    ElMessage.warning('产品名称不能为空')
    return
  }
  submitting.value = true
  try {
    let res
    if (dialogMode.value === 'create') {
      res = await createProductApi(form)
    } else {
      res = await updateProductApi(editingId.value, form)
    }

    if (res.code === 0) {
      ElMessage.success(dialogMode.value === 'create' ? '新增成功' : '更新成功')
      showDialog.value = false
      await loadProducts()
    } else {
      ElMessage.error(res.message || '产品保存失败')
    }
  } catch (e) {
    ElMessage.error(extractErrorMessage(e, '产品保存失败'))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.product-manage {
  padding: 20px;
}

.table-card {
  border-radius: 14px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.header-title {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.header-subtitle {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.header-ops {
  display: flex;
  gap: 10px;
}

.header-btn {
  min-width: 92px;
}

.search-bar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-item {
  height: 32px;
}

.keyword-input {
  width: 320px;
}

.category-select {
  width: 180px;
}

.search-btn {
  min-width: 76px;
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cell-main {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}

.cell-sub {
  color: #909399;
  font-size: 12px;
}

.soft-tag {
  max-width: 136px;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 960px) {
  .card-header {
    flex-direction: column;
  }

  .search-bar {
    align-items: stretch;
  }

  .keyword-input,
  .category-select {
    width: 100%;
  }
}
</style>
