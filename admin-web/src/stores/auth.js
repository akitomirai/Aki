/**
 * 认证状态仓库：
 * - 保存 token 和 用户信息
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

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('admin_token') || '',
        user: parseStoredUser()
    }),

    getters: {
        isAuthenticated: (state) => Boolean(state.token)
    },

    actions: {
        setAuth(token, user) {
            this.token = token
            this.user = normalizeUser(user)
            localStorage.setItem('admin_token', token)
            localStorage.setItem('admin_user', JSON.stringify(this.user))
        },

        logout() {
            this.token = ''
            this.user = {}
            localStorage.removeItem('admin_token')
            localStorage.removeItem('admin_user')
        }
    }
})
