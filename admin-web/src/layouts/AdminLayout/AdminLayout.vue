<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">农产品溯源后台</div>

      <el-menu
          class="menu"
          :default-active="activeMenu"
          router
          background-color="transparent"
          text-color="#dbeafe"
          active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">首页</el-menu-item>
        <el-menu-item v-if="canAccessBatch" index="/batches">批次管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/products">产品管理</el-menu-item>
        <el-menu-item v-if="isPlatformAdmin" index="/companies">企业管理</el-menu-item>
        <el-menu-item v-if="canAccessQuality" index="/quality">质检报告</el-menu-item>
        <el-menu-item v-if="canAccessFeedback" index="/feedback">消费者反馈</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">用户管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/logs">操作日志</el-menu-item>
      </el-menu>
    </aside>

    <div class="main">
      <header class="header">
        <div class="header-title">管理后台 ({{ roleName }})</div>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useAdminLayout } from '../../composables/useAdminLayout'

const { activeMenu, canAccessBatch, canAccessQuality, canAccessFeedback, isAdmin, isPlatformAdmin, roleName, logout } = useAdminLayout()
</script>

<style src="./admin-layout.css" scoped></style>
