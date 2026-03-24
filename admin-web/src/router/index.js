import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    component: () => import('../pages/DashboardView.vue')
  },
  {
    path: '/batches',
    component: () => import('../pages/BatchListView.vue')
  },
  {
    path: '/companies',
    component: () => import('../pages/CompanyManageView.vue')
  },
  {
    path: '/products',
    component: () => import('../pages/ProductManageView.vue')
  },
  {
    path: '/batches/:id',
    component: () => import('../pages/BatchWorkbenchView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
