/**
 * 认证状态仓库：
 * - 保存 token
 * - 登录成功后持久化到 localStorage
 * - 退出登录时清理
 */
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('admin_token') || ''
    }),

    actions: {
        setToken(token) {
            this.token = token
            localStorage.setItem('admin_token', token)
        },

        logout() {
            this.token = ''
            localStorage.removeItem('admin_token')
        }
    }
})