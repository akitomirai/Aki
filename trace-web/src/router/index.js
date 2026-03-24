import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/t/test-token-2026'
  },
  {
    path: '/t/:token',
    component: () => import('../views/TraceDetailView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
