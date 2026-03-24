/**
 * trace-web 入口：
 * - 注册 Vue Router
 * - 注册 Element Plus
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')