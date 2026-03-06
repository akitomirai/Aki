<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">农产品溯源后台</div>

      <el-menu
          :default-active="activePath"
          router
          class="menu"
      >
        <el-menu-item index="/dashboard">首页</el-menu-item>
        <el-menu-item index="/batch">批次管理</el-menu-item>
        <el-menu-item index="/quality">质检报告</el-menu-item>
      </el-menu>
    </aside>

    <div class="main">
      <header class="header">
        <div>管理后台</div>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </header>

      <section class="content">
        <slot />
      </section>
    </div>
  </div>
</template>

<script setup>
/**
 * 后台通用布局组件
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activePath = computed(() => route.path)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  background: #f5f7fa;
}

.sidebar {
  width: 220px;
  background: #001529;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.menu {
  border-right: none;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.content {
  padding: 20px;
}
</style>