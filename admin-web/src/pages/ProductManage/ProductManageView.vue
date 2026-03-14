<template>
  <div class="product-manage">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="header-title">产品管理</div>
            <div class="header-subtitle">统一补齐空值兜底与列宽展示，提升答辩观感。</div>
          </div>
          <div class="header-ops">
            <el-button class="header-btn" @click="loadProducts" :loading="loading">刷新</el-button>
            <el-button v-if="canManage" class="header-btn" type="primary" @click="openCreateDialog">新增产品</el-button>
          </div>
        </div>
      </template>

      <el-table :data="productList" v-loading="loading" border stripe empty-text="暂无产品数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="产品名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-main">{{ formatProductName(row.name) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ formatCategory(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规格" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatText(row.spec, '未设置规格') }}
          </template>
        </el-table-column>
        <el-table-column label="单位" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatUnit(row.unit) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="120" fixed="right">
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
          <el-input v-model="form.name" placeholder="请输入产品名称" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="可选" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" placeholder="可选" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="可选" maxlength="16" show-word-limit />
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
import { onMounted, ref, reactive, computed } from 'vue'
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
      ElMessage.error(`查询失败：${res.message || '产品列表加载失败'}`)
    }
  } catch (e) {
    ElMessage.error(`加载失败：${extractErrorMessage(e, '产品列表加载失败')}`)
  } finally {
    loading.value = false
  }
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
      ElMessage.error(`保存失败：${res.message || '产品保存失败'}`)
    }
  } catch (e) {
    ElMessage.error(`保存失败：${extractErrorMessage(e, '产品保存失败')}`)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProducts()
})

const formatText = (value, fallback = '-') => normalizeDisplayText(value, fallback)
const formatProductName = (value) => formatText(value, '未命名产品')
const formatCategory = (value) => formatText(value, '未分类')
const formatUnit = (value) => formatText(value, '未设置')
</script>

<style scoped>
.product-manage {
  padding: 20px;
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
  min-width: 88px;
}

.cell-main {
  color: #111827;
  font-weight: 600;
}

@media (max-width: 960px) {
  .card-header {
    flex-direction: column;
  }
}
</style>
