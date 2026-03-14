<template>
  <div class="user-manage">
    <el-card shadow="never" class="table-card">
      <el-tabs v-model="activeTab">
        <!-- 用户列表 Tab -->
        <el-tab-pane label="用户列表" name="users">
          <div class="header-ops">
            <el-input
              v-model="userQuery.username"
              placeholder="用户名"
              style="width: 180px; margin-right: 10px"
              clearable
              @clear="loadUsers"
            />
            <el-button type="primary" @click="loadUsers">搜索</el-button>
            <el-button type="success" @click="showInviteDialog = true">新增邀请</el-button>
          </div>
          
          <el-table :data="userList" v-loading="loadingUsers" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="150" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="roleCode" label="角色" width="150">
              <template #default="{ row }">
                <el-tag size="small">{{ row.roleCode }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="1"
                  :inactive-value="0"
                  :disabled="isSelf(row) || loadingUsers"
                  @change="(val) => handleStatusChange(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="180" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="!isSelf(row)"
                  type="warning"
                  link
                  @click="handleResetPassword(row)"
                >重置密码</el-button>
                <el-button
                  v-if="!isSelf(row)"
                  type="danger"
                  link
                  @click="handleDelete(row)"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 已邀请列表 Tab -->
        <el-tab-pane label="已邀请列表" name="invites">
          <div class="header-ops">
            <el-button type="success" @click="showInviteDialog = true">新增邀请</el-button>
          </div>
          <el-table :data="inviteList" v-loading="loadingInvites" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="code" label="邀请码" width="120">
              <template #default="{ row }">
                <code style="font-weight: bold; color: #409EFF">{{ row.code }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="roleCode" label="预设角色" width="150" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getInviteStatusTag(row.status)" size="small">
                  {{ getInviteStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="expireAt" label="过期时间" width="180" />
            <el-table-column prop="createdAt" label="生成时间" width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增邀请对话框 -->
    <el-dialog
      v-model="showInviteDialog"
      title="新增用户邀请"
      width="400px"
      @closed="resetInviteForm"
    >
      <el-form :model="inviteForm" label-width="80px" v-if="!generatedInvite">
        <el-form-item label="邀请角色">
          <el-select v-model="inviteForm.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in availableRoles"
              :key="role.value"
              :label="role.label"
              :value="role.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="inviteForm.expireAt"
            type="datetime"
            placeholder="选择过期时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>

      <div v-else class="invite-result">
        <div class="result-item">
          <span class="label">邀请码：</span>
          <span class="code">{{ generatedInvite.code }}</span>
        </div>
        <div class="result-item qrcode-wrap">
          <canvas ref="qrCodeCanvas"></canvas>
        </div>
        <div class="result-item">
          <el-input v-model="registerUrl" readonly size="small">
            <template #append>
              <el-button @click="copyUrl">复制链接</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showInviteDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreateInvite" v-if="!generatedInvite">
            生成邀请
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch, computed, nextTick } from 'vue'
import { listUserApi, listInviteApi, createInviteApi, updateUserStatusApi, deleteUserApi, resetUserPasswordApi } from '../../api/user'
import { useAuthStore } from '../../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'

const authStore = useAuthStore()

const isSelf = (row) => {
  return row.id === authStore.user?.id
}

const handleStatusChange = async (row, val) => {
  try {
    const res = await updateUserStatusApi(row.id, val, authStore.user?.roleCode)
    if (res.code === 0) {
      ElMessage.success(`${val === 1 ? '启用' : '禁用'}成功`)
    } else {
      // 失败了要回滚状态
      row.status = val === 1 ? 0 : 1
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    row.status = val === 1 ? 0 : 1
    ElMessage.error('网络请求失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${row.username}" 吗？此操作不可撤销。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await deleteUserApi(row.id, authStore.user?.roleCode)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除操作失败')
    }
  }
}

const handleResetPassword = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定将用户 "${row.username}" 的密码重置为系统默认密码吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await resetUserPasswordApi(row.id, authStore.user?.roleCode)
    if (res.code === 0) {
      ElMessage.success('重置成功（已重置为系统默认密码）')
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置操作失败')
    }
  }
}
const activeTab = ref('users')
const loadingUsers = ref(false)
const loadingInvites = ref(false)
const userList = ref([])
const inviteList = ref([])

const userQuery = reactive({
  username: ''
})

// 邀请码生成相关
const showInviteDialog = ref(false)
const submitting = ref(false)
const generatedInvite = ref(null)
const qrCodeCanvas = ref(null)
const inviteForm = reactive({
  roleCode: '',
  expireAt: ''
})

const availableRoles = computed(() => {
  const role = authStore.user?.roleCode
  if (role === 'PLATFORM_ADMIN') {
    return [
      { label: '企业管理员', value: 'ENTERPRISE_ADMIN' },
      { label: '企业普通用户', value: 'ENTERPRISE_USER' }
    ]
  } else if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') {
    return [
      { label: '企业普通用户', value: 'ENTERPRISE_USER' },
      { label: '企业管理员', value: 'ENTERPRISE_ADMIN' }
    ]
  } else if (role === 'REGULATOR') {
    return [
      { label: '监管人员', value: 'REGULATOR' }
    ]
  }
  return []
})

const registerUrl = computed(() => {
  if (!generatedInvite.value) return ''
  const baseUrl = window.location.origin
  return `${baseUrl}/#/register?inviteCode=${generatedInvite.value.code}`
})

const handleCreateInvite = async () => {
  if (!inviteForm.roleCode) {
    ElMessage.warning('请选择角色')
    return
  }
  
  submitting.value = true
  try {
    const res = await createInviteApi(inviteForm, authStore.user?.roleCode)
    if (res.code === 0) {
      generatedInvite.value = { code: res.data }
      nextTick(() => {
        if (qrCodeCanvas.value) {
          QRCode.toCanvas(qrCodeCanvas.value, registerUrl.value, { width: 200 }, (error) => {
            if (error) console.error('QR Code generation failed:', error)
          })
        }
      })
      if (activeTab.value === 'invites') loadInvites()
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (error) {
    ElMessage.error('请求生成接口失败')
  } finally {
    submitting.value = false
  }
}

const resetInviteForm = () => {
  inviteForm.roleCode = ''
  inviteForm.expireAt = ''
  generatedInvite.value = null
}

const copyUrl = () => {
  navigator.clipboard.writeText(registerUrl.value).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  })
}

const loadUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await listUserApi(userQuery, authStore.user?.roleCode)
    if (res.code === 0) {
      userList.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

const loadInvites = async () => {
  loadingInvites.value = true
  try {
    const res = await listInviteApi(authStore.user?.roleCode)
    if (res.code === 0) {
      inviteList.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取邀请列表失败')
  } finally {
    loadingInvites.value = false
  }
}

const getInviteStatusTag = (status) => {
  const map = {
    'UNUSED': 'primary',
    'USED': 'success',
    'EXPIRED': 'info',
    'DISABLED': 'danger'
  }
  return map[status] || 'info'
}

const getInviteStatusText = (status) => {
  const map = {
    'UNUSED': '未使用',
    'USED': '已使用',
    'EXPIRED': '已过期',
    'DISABLED': '已禁用'
  }
  return map[status] || status
}

watch(activeTab, (newTab) => {
  if (newTab === 'users') loadUsers()
  else if (newTab === 'invites') loadInvites()
})

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-manage {
  padding: 20px;
}
.header-ops {
  margin-bottom: 20px;
  display: flex;
}
.invite-result {
  text-align: center;
}
.result-item {
  margin-bottom: 15px;
}
.result-item .label {
  font-weight: bold;
}
.result-item .code {
  font-size: 20px;
  color: #409EFF;
  letter-spacing: 2px;
}
.qrcode-wrap {
  display: flex;
  justify-content: center;
}
</style>
