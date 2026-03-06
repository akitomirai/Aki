<template>
  <AdminLayout>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>新建批次</span>
          </template>

          <el-form :model="form" label-width="90px">
            <el-form-item label="产品ID">
              <el-input-number v-model="form.productId" :min="1" style="width: 100%;" />
            </el-form-item>

            <el-form-item label="企业ID">
              <el-input-number v-model="form.companyId" :min="1" style="width: 100%;" />
            </el-form-item>

            <el-form-item label="产地">
              <el-input v-model="form.originPlace" placeholder="如：江西赣州" />
            </el-form-item>

            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleCreate">
                创建批次
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <span>批次列表</span>
          </template>

          <el-table :data="tableData" border style="width: 100%;"@row-click="handleRowClick">
            <el-table-column prop="id" label="ID" min-width="180" />
            <el-table-column prop="batchCode" label="批次编码" min-width="180" />
            <el-table-column prop="productId" label="产品ID" width="90" />
            <el-table-column prop="companyId" label="企业ID" width="90" />
            <el-table-column prop="originPlace" label="产地" min-width="120" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>

          <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
            <el-pagination
                background
                layout="total, prev, pager, next"
                :total="total"
                :current-page="current"
                :page-size="size"
                @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </AdminLayout>
</template>

<script setup>
/**
 * 批次管理页：
 * - 左侧创建批次
 * - 右侧批次分页表格
 */
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '../components/AdminLayout.vue'
import { createBatchApi, pageBatchApi } from '../api/batch'
import { useRouter } from 'vue-router'

const loading = ref(false)

const current = ref(1)
const size = ref(10)
const total = ref(0)
const tableData = ref([])
const router = useRouter()

const form = reactive({
  productId: 1,
  companyId: 1,
  originPlace: '江西赣州',
  remark: '前端创建测试'
})

/**
 * 加载批次分页数据
 */
async function loadTable() {
  try {
    const res = await pageBatchApi({
      current: current.value,
      size: size.value
    })

    if (res.code === 0) {
      tableData.value = res.data.records || []
      total.value = Number(res.data.total || 0)
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    ElMessage.error('加载批次列表失败')
  }
}

/**
 * 创建批次
 */
async function handleCreate() {
  loading.value = true
  try {
    const res = await createBatchApi({
      productId: form.productId,
      companyId: form.companyId,
      originPlace: form.originPlace,
      remark: form.remark
    })

    if (res.code === 0) {
      ElMessage.success('批次创建成功')
      current.value = 1
      await loadTable()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '请求失败')
  } finally {
    loading.value = false
  }
}

/**
 * 分页切换
 */
async function handlePageChange(page) {
  current.value = page
  await loadTable()
}

/**
 * 点击表格行，跳转到批次详情
 */
async function handleRowClick(row) {
  router.push(`/batch/${row.id}`)
}

onMounted(() => {
  loadTable()
})
</script>