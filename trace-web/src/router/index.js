import { createRouter, createWebHistory } from 'vue-router'

const demoToken = import.meta.env.VITE_TRACE_DEMO_TOKEN || 'demo-normal-2026'

const routes = [
  {
    path: '/',
    redirect: `/t/${demoToken}`
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
