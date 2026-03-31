<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { changePasswordApi } from './api/auth'
import PasswordChangeDialog from './components/PasswordChangeDialog.vue'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()
const passwordSaving = ref(false)

const passwordDialogVisible = computed({
  get: () => authStore.passwordDialogVisible,
  set: (value) => {
    if (!value) {
      authStore.closePasswordDialog()
    }
  }
})

watch(
  () => [authStore.isAuthenticated, Boolean(authStore.user?.needChangePassword)],
  ([isAuthenticated, needChangePassword]) => {
    if (!isAuthenticated) {
      authStore.forceClosePasswordDialog()
      return
    }
    if (needChangePassword) {
      authStore.openPasswordDialog(true)
    }
  },
  { immediate: true }
)

async function handleChangePassword(payload) {
  passwordSaving.value = true
  try {
    const response = await changePasswordApi(payload)
    authStore.updateUser(response.data)
    authStore.closePasswordDialog(true)
    ElMessage.success(response.message || '密码修改成功。')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '密码修改失败，请稍后重试。')
  } finally {
    passwordSaving.value = false
  }
}
</script>

<template>
  <div class="app-shell">
    <router-view />
    <PasswordChangeDialog
      v-model="passwordDialogVisible"
      :forced="authStore.passwordDialogForced"
      :loading="passwordSaving"
      @submit="handleChangePassword"
    />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}
</style>
