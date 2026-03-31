<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  forced: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      resetForm()
    }
  }
)

function resetForm() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

function handleClose() {
  if (props.forced || props.loading) {
    return
  }
  dialogVisible.value = false
}

function handleSubmit() {
  emit('submit', {
    currentPassword: form.currentPassword,
    newPassword: form.newPassword,
    confirmPassword: form.confirmPassword
  })
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    class="password-dialog"
    width="min(460px, calc(100vw - 24px))"
    :show-close="!forced"
    :close-on-click-modal="!forced"
    :close-on-press-escape="!forced"
    append-to-body
    align-center
    destroy-on-close
  >
    <template #header>
      <div class="password-dialog-head" data-testid="change-password-dialog">
        <h3>修改密码</h3>
        <p>
          {{ forced ? '首次登录或管理员重置密码后，需要先修改密码才能继续使用系统。' : '请输入当前密码并设置新的登录密码。' }}
        </p>
      </div>
    </template>

    <div class="password-dialog-body">
      <label class="password-field">
        <span>原密码</span>
        <input
          v-model.trim="form.currentPassword"
          data-testid="change-password-current"
          type="password"
          autocomplete="current-password"
          placeholder="请输入当前密码"
        >
      </label>

      <label class="password-field">
        <span>新密码</span>
        <input
          v-model.trim="form.newPassword"
          data-testid="change-password-new"
          type="password"
          autocomplete="new-password"
          placeholder="至少 6 位"
        >
      </label>

      <label class="password-field">
        <span>确认新密码</span>
        <input
          v-model.trim="form.confirmPassword"
          data-testid="change-password-confirm"
          type="password"
          autocomplete="new-password"
          placeholder="请再次输入新密码"
        >
      </label>
    </div>

    <template #footer>
      <div class="password-dialog-actions">
        <button
          v-if="!forced"
          class="ghost-button"
          data-testid="change-password-cancel"
          :disabled="loading"
          @click="handleClose"
        >
          取消
        </button>
        <button
          class="primary-button"
          data-testid="change-password-submit"
          :disabled="loading"
          @click="handleSubmit"
        >
          {{ loading ? '正在保存...' : '确认修改' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.password-dialog-head h3 {
  margin: 0;
  font-size: 20px;
  color: var(--admin-text);
}

.password-dialog-head p {
  margin: 8px 0 0;
  color: var(--admin-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.password-dialog-body {
  display: grid;
  gap: 14px;
}

.password-field {
  display: grid;
  gap: 8px;
}

.password-field span {
  font-size: 13px;
  color: var(--admin-text-soft);
}

.password-field input {
  width: 100%;
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  background: #fff;
  color: var(--admin-text);
}

.password-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.primary-button,
.ghost-button {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  cursor: pointer;
  border: 1px solid transparent;
}

.primary-button {
  border-color: var(--admin-primary);
  background: linear-gradient(135deg, var(--admin-primary) 0%, var(--admin-primary-deep) 100%);
  color: #fff;
}

.ghost-button {
  border-color: var(--admin-border);
  background: #fff;
  color: var(--admin-primary-deep);
}
</style>
