<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePasswordApi, getProfileApi, updateProfileApi } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import { getFriendlyErrorMessage } from '../utils/batchExperience'

const authStore = useAuthStore()

const loading = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profileForm = reactive({
  username: '',
  realName: '',
  phone: '',
  roleName: '',
  companyName: '',
  passwordUpdatedAt: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const displayName = computed(() => profileForm.realName || profileForm.username || '当前用户')
const displayInitial = computed(() => String(displayName.value || 'U').trim().charAt(0).toUpperCase())
const displayCompany = computed(() => profileForm.companyName || '平台直属账号')
const displayPasswordUpdatedAt = computed(() => profileForm.passwordUpdatedAt || '暂未记录')

function applyProfile(user = {}) {
  const merged = {
    ...(authStore.user || {}),
    ...(user || {})
  }
  authStore.updateUser(merged)
  profileForm.username = merged.username || ''
  profileForm.realName = merged.realName || ''
  profileForm.phone = merged.phone || ''
  profileForm.roleName = merged.roleName || ''
  profileForm.companyName = merged.companyName || ''
  profileForm.passwordUpdatedAt = merged.passwordUpdatedAt || ''
}

function resetPasswordForm() {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function loadProfile() {
  loading.value = true
  try {
    const response = await getProfileApi()
    applyProfile(response.data || {})
  } catch (error) {
    applyProfile(authStore.user || {})
    ElMessage.error(getFriendlyErrorMessage(error, '个人资料加载失败，请稍后重试。'))
  } finally {
    loading.value = false
  }
}

async function handleProfileSave() {
  profileSaving.value = true
  try {
    const response = await updateProfileApi({
      realName: String(profileForm.realName || '').trim(),
      phone: String(profileForm.phone || '').trim()
    })
    applyProfile(response.data || {})
    ElMessage.success(response.message || '个人资料已更新')
  } catch (error) {
    ElMessage.error(getFriendlyErrorMessage(error, '个人资料更新失败，请稍后重试。'))
  } finally {
    profileSaving.value = false
  }
}

async function handlePasswordSave() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整的密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  passwordSaving.value = true
  try {
    const response = await changePasswordApi({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    applyProfile(response.data || {})
    resetPasswordForm()
    ElMessage.success(response.message || '密码修改成功')
  } catch (error) {
    ElMessage.error(getFriendlyErrorMessage(error, '密码修改失败，请稍后重试。'))
  } finally {
    passwordSaving.value = false
  }
}

onMounted(() => {
  applyProfile(authStore.user || {})
  loadProfile()
})
</script>

<template>
  <div class="profile-page manage-page" data-testid="profile-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">个人资料</h1>
        <p class="manage-page-desc">在这里统一维护显示名、手机号和登录密码。</p>
      </div>
      <div class="manage-page-actions">
        <el-button :loading="loading" @click="loadProfile">刷新</el-button>
      </div>
    </section>

    <el-card shadow="never" class="manage-filter-card profile-summary-card" v-loading="loading">
      <div class="profile-summary">
        <div class="profile-hero">
          <div class="profile-avatar">{{ displayInitial }}</div>
          <div class="profile-hero-copy">
            <h2>{{ displayName }}</h2>
            <p>{{ profileForm.username || '未登录用户' }}</p>
          </div>
        </div>

        <div class="profile-meta-grid">
          <article class="profile-meta-item">
            <span>当前角色</span>
            <strong>{{ profileForm.roleName || '系统用户' }}</strong>
          </article>
          <article class="profile-meta-item">
            <span>所属企业</span>
            <strong>{{ displayCompany }}</strong>
          </article>
          <article class="profile-meta-item">
            <span>最近改密</span>
            <strong>{{ displayPasswordUpdatedAt }}</strong>
          </article>
        </div>
      </div>
    </el-card>

    <section class="profile-grid">
      <el-card shadow="never" class="manage-table-card profile-card" v-loading="loading">
        <template #header>
          <div class="manage-table-header">
            <div>
              <p class="manage-table-title">编辑资料</p>
              <p class="manage-table-tip">手机号和显示名都可以在这里维护。</p>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="profile-form" @submit.prevent>
          <div class="profile-form-grid">
            <el-form-item label="用户名">
              <el-input :model-value="profileForm.username" disabled />
            </el-form-item>

            <el-form-item label="姓名 / 显示名">
              <el-input
                v-model="profileForm.realName"
                maxlength="64"
                clearable
                placeholder="未填写时将默认显示用户名"
              />
            </el-form-item>

            <el-form-item label="手机号">
              <el-input
                v-model="profileForm.phone"
                maxlength="32"
                clearable
                placeholder="请输入手机号"
              />
            </el-form-item>

            <el-form-item label="当前角色">
              <el-input :model-value="profileForm.roleName || '系统用户'" disabled />
            </el-form-item>

            <el-form-item label="所属企业" class="profile-form-item--full">
              <el-input :model-value="displayCompany" disabled />
            </el-form-item>
          </div>

          <div class="profile-card-actions">
            <el-button @click="loadProfile">重置</el-button>
            <el-button type="primary" :loading="profileSaving" @click="handleProfileSave">
              更新资料
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card shadow="never" class="manage-table-card profile-card" v-loading="loading">
        <template #header>
          <div class="manage-table-header">
            <div>
              <p class="manage-table-title">修改密码</p>
              <p class="manage-table-tip">修改完成后会立即同步到当前登录状态。</p>
            </div>
          </div>
        </template>

        <el-form label-position="top" class="profile-form" @submit.prevent>
          <el-form-item label="当前密码">
            <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <el-form-item label="新密码">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="至少 6 位"
            />
          </el-form-item>

          <el-form-item label="确认新密码">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            />
          </el-form-item>

          <p class="profile-form-hint">最近一次修改时间：{{ displayPasswordUpdatedAt }}</p>

          <div class="profile-card-actions">
            <el-button @click="resetPasswordForm">清空</el-button>
            <el-button type="primary" :loading="passwordSaving" @click="handlePasswordSave">
              修改密码
            </el-button>
          </div>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

.profile-summary-card,
.profile-card {
  border: 1px solid var(--admin-border) !important;
  border-radius: 20px !important;
  background: var(--admin-surface) !important;
  box-shadow: var(--admin-shadow) !important;
}

.profile-summary {
  display: grid;
  gap: 20px;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--admin-primary) 0%, var(--admin-primary-deep) 100%);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.profile-hero-copy h2 {
  margin: 0;
  color: var(--admin-text);
  font-size: 24px;
}

.profile-hero-copy p {
  margin: 6px 0 0;
  color: var(--admin-text-soft);
  font-size: 14px;
}

.profile-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.profile-meta-item {
  padding: 16px;
  border-radius: 16px;
  background: var(--admin-surface-soft);
}

.profile-meta-item span {
  display: block;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.profile-meta-item strong {
  display: block;
  margin-top: 8px;
  color: var(--admin-text);
  font-size: 18px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 16px;
}

.profile-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.profile-form-item--full {
  grid-column: 1 / -1;
}

.profile-form-hint {
  margin: 0;
  color: var(--admin-text-soft);
  font-size: 13px;
}

.profile-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 960px) {
  .profile-grid,
  .profile-meta-grid,
  .profile-form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 18px 14px 32px;
  }

  .profile-card-actions {
    justify-content: stretch;
    flex-direction: column;
  }
}
</style>
