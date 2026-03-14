import { createRouter, createWebHistory } from 'vue-router'
import TraceDetailView from '../pages/TraceDetail'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/t/demo'
    },
    {
      path: '/t/:qrToken',
      name: 'TraceDetail',
      component: TraceDetailView
    }
  ]
})

export default router
