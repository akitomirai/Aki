import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import {
  ALL_ADMIN_ROLES,
  getDefaultRouteByRole,
  getRoleName,
  hasRoleAccess,
  isOperator
} from '../utils/access'
import { isMobileDevice } from '../utils/device'

const batchReadRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
const dashboardRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
const qualityManageRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN']
const qualityReadRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
const riskRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
const feedbackRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
const fieldRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'OPERATOR']
const userManageRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN']

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/Login/LoginView.vue'),
    meta: {
      guestOnly: true,
      title: '登录'
    }
  },
  {
    path: '/mobile-login',
    name: 'mobile-login',
    component: () => import('../pages/Login/LoginView.vue'),
    meta: {
      guestOnly: true,
      title: '移动端登录',
      mobileLogin: true
    }
  },
  {
    path: '/register',
    redirect: '/login'
  },
  {
    path: '/field-entry',
    name: 'field-entry',
    component: () => import('../pages/FieldEntryView.vue'),
    meta: {
      requiresAuth: true,
      title: '现场作业',
      roles: fieldRoles
    }
  },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout/AdminLayout.vue'),
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../pages/DashboardView.vue'),
        meta: {
          requiresAuth: true,
          title: '数据统计分析管理',
          headerTitle: '首页',
          hideHeaderTitle: true,
          roles: dashboardRoles
        }
      },
      {
        path: 'products',
        name: 'products',
        component: () => import('../pages/ProductManageView.vue'),
        meta: {
          requiresAuth: true,
          title: '产品管理',
          roles: ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN']
        }
      },
      {
        path: 'companies',
        name: 'companies',
        component: () => import('../pages/CompanyManageView.vue'),
        meta: {
          requiresAuth: true,
          title: '企业管理',
          roles: ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN']
        }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../pages/UserManage/UserManageView.vue'),
        meta: {
          requiresAuth: true,
          title: '用户管理',
          roles: userManageRoles
        }
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('../pages/LogManage/LogManageView.vue'),
        meta: {
          requiresAuth: true,
          title: '操作日志',
          roles: userManageRoles
        }
      },
      {
        path: 'batches',
        name: 'batches',
        component: () => import('../pages/BatchListView.vue'),
        meta: {
          requiresAuth: true,
          title: '批次管理',
          roles: batchReadRoles
        }
      },
      {
        path: 'qr',
        name: 'qr-publish-entry',
        component: () => import('../pages/QrPublishManageView.vue'),
        meta: {
          requiresAuth: true,
          title: '二维码与发布',
          roles: qualityManageRoles
        }
      },
      {
        path: 'quality',
        name: 'quality-entry',
        component: () => import('../pages/QualityTodoView.vue'),
        meta: {
          requiresAuth: true,
          title: '质检待办',
          roles: qualityReadRoles
        }
      },
      {
        path: 'risk',
        name: 'risk-entry',
        component: () => import('../pages/RiskTodoView.vue'),
        meta: {
          requiresAuth: true,
          title: '风险处理',
          roles: riskRoles
        }
      },
      {
        path: 'feedback',
        name: 'feedback-entry',
        component: () => import('../pages/FeedbackManage/FeedbackManageView.vue'),
        meta: {
          requiresAuth: true,
          title: '反馈处理',
          roles: feedbackRoles
        }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../pages/ProfileView.vue'),
        meta: {
          requiresAuth: true,
          title: '个人资料',
          roles: ALL_ADMIN_ROLES
        }
      },
      {
        path: 'batches/:id',
        name: 'batch-workbench',
        component: () => import('../pages/BatchWorkbenchView.vue'),
        meta: {
          requiresAuth: true,
          title: '批次工作台',
          roles: batchReadRoles
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function resolveRouteRoles(to) {
  const matchedRoles = [...to.matched]
    .reverse()
    .find((record) => Array.isArray(record.meta?.roles))
    ?.meta?.roles

  if (to.path === '/batches' && ['READY'].includes(String(to.query.mode || '').toUpperCase())) {
    return qualityReadRoles
  }
  if (to.path === '/batches' && ['RISK'].includes(String(to.query.mode || '').toUpperCase())) {
    return riskRoles
  }

  return matchedRoles ?? []
}

function resolveRouteTitle(to) {
  if (to.path === '/batches' && String(to.query.mode || '').toUpperCase() === 'READY') {
    return '质检待办'
  }
  if (to.path === '/batches' && String(to.query.mode || '').toUpperCase() === 'RISK') {
    return '风险处理'
  }
  return [...to.matched].reverse().find((record) => record.meta?.title)?.meta?.title || '当前页面'
}

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const isAuthenticated = authStore.isAuthenticated
  const roleCode = authStore.user?.roleCode
  const requiresAuth = to.matched.some((record) => record.meta?.requiresAuth)
  const isMobileLoginPath = to.path === '/mobile-login'

  if (isAuthenticated && isOperator(roleCode) && !isMobileDevice()) {
    authStore.logout()
    ElMessage.warning('现场操作员仅支持移动端登录，请使用移动端入口。')
    return isMobileLoginPath
      ? true
      : {
          path: '/mobile-login',
          query: {
            redirect: '/field-entry'
          }
        }
  }

  if (to.matched.some((record) => record.meta?.guestOnly)) {
    if (isAuthenticated) {
      return getDefaultRouteByRole(roleCode)
    }
    return true
  }

  if (requiresAuth && !isAuthenticated) {
    return {
      path: to.path === '/field-entry' ? '/mobile-login' : '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  const allowedRoles = resolveRouteRoles(to)
  if (isAuthenticated && allowedRoles.length && !hasRoleAccess(roleCode, allowedRoles)) {
    ElMessage.warning(`当前账号为${getRoleName(roleCode)}，不能访问“${resolveRouteTitle(to)}”，已为你切换到可用页面。`)
    return getDefaultRouteByRole(roleCode)
  }

  return true
})

export default router
