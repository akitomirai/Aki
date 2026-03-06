/**
 * trace-web 路由：
 * - / 重定向到一个示例页（便于本地调试）
 * - /t/:qrToken 正式查询页
 */
import { createRouter, createWebHistory } from 'vue-router'
import TraceView from '../views/TraceView.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            redirect: '/t/demo'
        },
        {
            path: '/t/:qrToken',
            name: 'trace',
            component: () => import('../views/TraceView.vue')
        }
    ]
})

export default router