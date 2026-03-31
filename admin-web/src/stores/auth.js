/**
 * 认证状态仓库：
 * - 保存 token 和用户信息
 * - 统一管理修改密码弹层
 */
import { defineStore } from 'pinia'
import { normalizeUser } from '../utils/access'

function parseStoredUser() {
  try {
    return normalizeUser(JSON.parse(localStorage.getItem('admin_user') || '{}'))
  } catch (error) {
    return {}
  }
}

function persistUser(user) {
  localStorage.setItem('admin_user', JSON.stringify(user))
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    user: parseStoredUser(),
    passwordDialogVisible: false,
    passwordDialogForced: false
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },

  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = normalizeUser(user)
      this.passwordDialogVisible = Boolean(this.user?.needChangePassword)
      this.passwordDialogForced = Boolean(this.user?.needChangePassword)
      localStorage.setItem('admin_token', token)
      persistUser(this.user)
    },

    updateUser(user) {
      this.user = normalizeUser({
        ...(this.user || {}),
        ...(user || {})
      })
      persistUser(this.user)
      if (this.user?.needChangePassword) {
        this.passwordDialogVisible = true
        this.passwordDialogForced = true
      } else {
        this.passwordDialogVisible = false
        this.passwordDialogForced = false
      }
    },

    openPasswordDialog(forced = false) {
      if (!this.isAuthenticated) {
        return
      }
      this.passwordDialogVisible = true
      this.passwordDialogForced = Boolean(forced || this.user?.needChangePassword || this.passwordDialogForced)
    },

    closePasswordDialog(force = false) {
      if (this.passwordDialogForced && !force) {
        return
      }
      this.passwordDialogVisible = false
      this.passwordDialogForced = false
    },

    forceClosePasswordDialog() {
      this.passwordDialogVisible = false
      this.passwordDialogForced = false
    },

    logout() {
      this.token = ''
      this.user = {}
      this.forceClosePasswordDialog()
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
    }
  }
})
