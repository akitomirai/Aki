import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import {
  ALL_ADMIN_ROLES,
  getDefaultRouteByRole,
  getRoleName,
  hasRoleAccess
} from '../utils/access'

const batchReadRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
const dashboardRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN']
const qualityManageRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN']
const qualityReadRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
const riskRoles = ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'REGULATOR']
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
          title: '首页总览',
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
          title: '质检查看',
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

  if (to.matched.some((record) => record.meta?.guestOnly)) {
    if (isAuthenticated) {
      return getDefaultRouteByRole(roleCode)
    }
    return true
  }

  if (requiresAuth && !isAuthenticated) {
    return {
      path: '/login',
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
