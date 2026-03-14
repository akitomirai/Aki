<template>
  <div class="product-manage">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>产品管理</span>
          <div class="header-ops">
            <el-button @click="loadProducts" :loading="loading">刷新</el-button>
            <el-button v-if="canManage" type="primary" @click="openCreateDialog">新增产品</el-button>
          </div>
        </div>
      </template>

      <el-table :data="productList" v-loading="loading" border stripe empty-text="暂无产品数据，可点击右上角新增产品">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="产品名称" min-width="180">
          <template #default="{ row }">
            {{ formatProductName(row.name) }}
          </template>
        </el-table-column>
        <el-table-column label="分类" width="160">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ formatCategory(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规格" width="140">
          <template #default="{ row }">
            {{ formatText(row.spec, '未设置') }}
          </template>
        </el-table-column>
        <el-table-column label="单位" width="120">
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
import { formatDateTime } from '../../utils/display'
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
  form.name = row.name || ''
  form.category = row.category || ''
  form.spec = row.spec || ''
  form.unit = row.unit || ''
  showDialog.value = true
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await listProductApi(roleCode.value)
    if (res.code === 0) {
      productList.value = res.data || []
      if (!productList.value.length) {
        ElMessage.info('未查询到数据')
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

const formatText = (value, fallback = '-') => {
  if (value == null) return fallback
  const text = String(value).trim()
  if (!text) return fallback
  if (text.includes('????') || text.includes('�')) return fallback
  return text
}

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
  align-items: center;
  justify-content: space-between;
}

.header-ops {
  display: flex;
  gap: 10px;
}
</style>
