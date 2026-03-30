import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getRoleName, hasRoleAccess } from '../utils/access'

export function useAdminLayout() {
  const route = useRoute()
  const router = useRouter()
  const authStore = useAuthStore()

  const roleCode = computed(() => authStore.user?.roleCode || '')
  const roleName = computed(() => authStore.user?.roleName || getRoleName(roleCode.value))
  const displayName = computed(() => authStore.user?.realName || authStore.user?.username || '登录用户')

  const pageTitle = computed(() => {
    const matched = [...route.matched].reverse().find((item) => item.meta?.title)
    return matched?.meta?.title || '后台管理'
  })

  const activeMenu = computed(() => {
    if (route.path.startsWith('/products')) return '/products'
    if (route.path.startsWith('/companies')) return '/companies'
    if (route.path.startsWith('/batches')) {
      const mode = String(route.query.mode || '').toUpperCase()
      if (mode === 'READY') return '/quality'
      if (mode === 'RISK') return '/risk'
      return '/batches'
    }
    return '/dashboard'
  })

  const menuSections = computed(() => {
    const role = roleCode.value
    const sections = [
      {
        title: '工作台',
        items: [
          { key: '/dashboard', label: '首页总览', to: '/dashboard' }
        ]
      }
    ]

    const baseDataItems = []
    if (hasRoleAccess(role, ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN'])) {
      baseDataItems.push({ key: '/products', label: '产品管理', to: '/products' })
    }
    if (hasRoleAccess(role, ['PLATFORM_ADMIN'])) {
      baseDataItems.push({ key: '/companies', label: '企业管理', to: '/companies' })
    }
    if (baseDataItems.length) {
      sections.push({
        title: '基础资料',
        items: baseDataItems
      })
    }

    sections.push({
      title: '业务管理',
      items: [
        { key: '/batches', label: '批次管理', to: '/batches' }
      ]
    })

    const qualityItems = []
    if (hasRoleAccess(role, ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR'])) {
      qualityItems.push({ key: '/quality', label: '质检待办', to: '/quality' })
      qualityItems.push({ key: '/risk', label: '风险处理', to: '/risk' })
    }
    if (qualityItems.length) {
      sections.push({
        title: '质量与风险',
        items: qualityItems
      })
    }

    return sections
  })

  function logout() {
    authStore.logout()
    router.replace('/login')
  }

  return {
    activeMenu,
    displayName,
    logout,
    menuSections,
    pageTitle,
    roleCode,
    roleName
  }
}
