import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout'
import LoginView from '../pages/Login'
import RegisterView from '../pages/Register'
import DashboardView from '../pages/Dashboard'
import BatchManageView from '../pages/BatchManage'
import BatchDetailView from '../pages/BatchDetail'
import QualityManageView from '../pages/QualityManage'
import FeedbackManageView from '../pages/FeedbackManage'
import LogManageView from '../pages/LogManage'
import UserManageView from '../pages/UserManage'
import ProductManageView from '../pages/ProductManage'
import CompanyManageView from '../pages/CompanyManage'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    component: LoginView
  },
  {
    path: '/register',
    component: RegisterView
  },
  {
    path: '/',
    component: AdminLayout,
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        component: DashboardView
      },
      {
        path: 'batches',
        component: BatchManageView
      },
      {
        path: 'batches/:id',
        component: BatchDetailView
      },
      {
        path: 'quality',
        component: QualityManageView
      },
      {
        path: 'feedback',
        component: FeedbackManageView
      },
      {
        path: 'products',
        component: ProductManageView
      },
      {
        path: 'companies',
        component: CompanyManageView
      },
      {
        path: 'users',
        component: UserManageView
      },
      {
        path: 'logs',
        component: LogManageView
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.path !== '/login' && to.path !== '/register' && !authStore.token) {
    next('/login')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && authStore.token) {
    next('/dashboard')
    return
  }

  if (to.path.startsWith('/batches')) {
    const role = authStore.user?.roleCode || ''
    const allow =
      role === 'PLATFORM_ADMIN' ||
      role === 'ENTERPRISE_ADMIN' ||
      role === 'ENTERPRISE_USER' ||
      role === 'ADMIN' ||
      role === 'REGULATOR'

    if (!allow) {
      next('/dashboard')
      return
    }
  }

  if (to.path.startsWith('/quality')) {
    const role = authStore.user?.roleCode || ''
    const allow =
      role === 'PLATFORM_ADMIN' ||
      role === 'ENTERPRISE_ADMIN' ||
      role === 'ENTERPRISE_USER' ||
      role === 'ADMIN' ||
      role === 'REGULATOR'

    if (!allow) {
      next('/dashboard')
      return
    }
  }

  if (to.path.startsWith('/feedback')) {
    const role = authStore.user?.roleCode || ''
    const allow =
      role === 'PLATFORM_ADMIN' ||
      role === 'ENTERPRISE_ADMIN' ||
      role === 'ENTERPRISE_USER' ||
      role === 'ADMIN' ||
      role === 'REGULATOR'

    if (!allow) {
      next('/dashboard')
      return
    }
  }

  if (to.path.startsWith('/logs')) {
    const role = authStore.user?.roleCode || ''
    const allow =
      role === 'PLATFORM_ADMIN' ||
      role === 'ENTERPRISE_ADMIN' ||
      role === 'ADMIN' ||
      role === 'REGULATOR'

    if (!allow) {
      next('/dashboard')
      return
    }
  }

  if (to.path.startsWith('/companies')) {
    const role = authStore.user?.roleCode || ''
    if (role !== 'PLATFORM_ADMIN') {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
