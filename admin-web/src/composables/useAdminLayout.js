import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

export function useAdminLayout() {
    const route = useRoute()
    const router = useRouter()
    const authStore = useAuthStore()

    const activeMenu = computed(() => {
        if (route.path.startsWith('/batches')) return '/batches'
        if (route.path.startsWith('/products')) return '/products'
        if (route.path.startsWith('/quality')) return '/quality'
        if (route.path.startsWith('/feedback')) return '/feedback'
        if (route.path.startsWith('/companies')) return '/companies'
        if (route.path.startsWith('/users')) return '/users'
        if (route.path.startsWith('/logs')) return '/logs'
        return '/dashboard'
    })

    const roleCode = computed(() => authStore.user?.roleCode || '')

    const canAccessBatch = computed(() => {
        const role = roleCode.value
        return role === 'PLATFORM_ADMIN' ||
            role === 'ENTERPRISE_ADMIN' ||
            role === 'ENTERPRISE_USER' ||
            role === 'ADMIN' ||
            role === 'REGULATOR'
    })

    const canAccessQuality = computed(() => {
        const role = roleCode.value
        return role === 'PLATFORM_ADMIN' ||
            role === 'ENTERPRISE_ADMIN' ||
            role === 'ENTERPRISE_USER' ||
            role === 'ADMIN' ||
            role === 'REGULATOR'
    })

    const canAccessFeedback = computed(() => {
        const role = roleCode.value
        return role === 'PLATFORM_ADMIN' ||
            role === 'ENTERPRISE_ADMIN' ||
            role === 'ENTERPRISE_USER' ||
            role === 'ADMIN' ||
            role === 'REGULATOR'
    })

    const isAdmin = computed(() => {
        const role = roleCode.value
        // 平台管理员、企业管理员、通用管理员、监管人员 均可看到日志（监管人员目前只有这一个角色）
        return role === 'PLATFORM_ADMIN' || 
               role === 'ENTERPRISE_ADMIN' || 
               role === 'ADMIN' || 
               role === 'REGULATOR'
    })

    const roleName = computed(() => {
        const role = roleCode.value
        if (role === 'PLATFORM_ADMIN') return '平台管理员'
        if (role === 'ENTERPRISE_ADMIN' || role === 'ADMIN') return '企业管理员'
        if (role === 'ENTERPRISE_USER') return '企业操作员'
        if (role === 'REGULATOR') return '监管人员'
        return '普通用户'
    })

    const isPlatformAdmin = computed(() => roleCode.value === 'PLATFORM_ADMIN')

    function logout() {
        authStore.logout()
        router.push('/login')
    }

    return {
        activeMenu,
        canAccessBatch,
        canAccessQuality,
        canAccessFeedback,
        isAdmin,
        isPlatformAdmin,
        roleName,
        logout
    }
}
