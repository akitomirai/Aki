<template>
  <div class="company-manage">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>企业管理</span>
          <el-button :loading="loading" @click="loadCompanies">刷新</el-button>
        </div>
      </template>

      <el-table :data="companies" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="企业名称" min-width="180" />
        <el-table-column prop="licenseNo" label="营业执照号" min-width="160" />
        <el-table-column label="业务身份" min-width="280">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag
                v-for="role in row.bizRoles || []"
                :key="role"
                size="small"
              >
                {{ role }}
              </el-tag>
              <span v-if="!(row.bizRoles && row.bizRoles.length)">-</span>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑身份</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="编辑企业业务身份"
      width="520px"
      @closed="resetDialog"
    >
      <el-form>
        <el-form-item label="企业">
          <span>{{ currentCompany?.name || '-' }}</span>
        </el-form-item>
        <el-form-item label="业务身份">
          <el-checkbox-group v-model="editingRoles">
            <el-checkbox
              v-for="role in roleOptions"
              :key="role.value"
              :label="role.value"
            >
              {{ role.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listCompanyApi, updateCompanyBizRolesApi } from '../../api/company'

const companies = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const currentCompany = ref(null)
const editingRoles = ref([])

const roleOptions = [
  { label: '生产（PRODUCER）', value: 'PRODUCER' },
  { label: '种植/养殖（FARMER）', value: 'FARMER' },
  { label: '运输（TRANSPORTER）', value: 'TRANSPORTER' },
  { label: '加工（PROCESSOR）', value: 'PROCESSOR' },
  { label: '销售（SELLER）', value: 'SELLER' },
  { label: '仓储（WAREHOUSE）', value: 'WAREHOUSE' }
]

const loadCompanies = async () => {
  loading.value = true
  try {
    const res = await listCompanyApi()
    if (String(res.code) === '0') {
      companies.value = res.data || []
    } else {
      ElMessage.error(res.message || '加载企业列表失败')
    }
  } catch {
    ElMessage.error('加载企业列表失败')
  } finally {
    loading.value = false
  }
}

const openEdit = (row) => {
  currentCompany.value = row
  editingRoles.value = [...(row.bizRoles || [])]
  dialogVisible.value = true
}

const resetDialog = () => {
  currentCompany.value = null
  editingRoles.value = []
}

const saveRoles = async () => {
  if (!currentCompany.value?.id) return
  saving.value = true
  try {
    const res = await updateCompanyBizRolesApi(currentCompany.value.id, editingRoles.value)
    if (String(res.code) === '0') {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      await loadCompanies()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

loadCompanies()
</script>

<style scoped>
.company-manage {
  width: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
