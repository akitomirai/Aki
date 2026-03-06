import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import BatchManageView from '../views/BatchManageView.vue'
import BatchDetailView from '../views/BatchDetailView.vue'
import QualityManageView from '../views/QualityManageView.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: LoginView,
            meta: { public: true }
        },
        {
            path: '/',
            redirect: '/dashboard'
        },
        {
            path: '/dashboard',
            name: 'dashboard',
            component: DashboardView
        },
        {
            path: '/batch',
            name: 'batch',
            component: BatchManageView
        },
        {
            path: '/batch/:id',
            name: 'batch-detail',
            component: BatchDetailView
        },
        {
            path: '/quality',
            name: 'quality',
            component: QualityManageView
        }
    ]
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('admin_token')

    if (to.meta.public) {
        next()
        return
    }

    if (!token) {
        next('/login')
        return
    }

    next()
})

export default router