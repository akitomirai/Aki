<template>
  <div class="layout" data-testid="admin-layout">
    <aside class="sidebar">
      <div class="brand">
        <h1>农产品追溯后台</h1>
      </div>

      <nav class="nav-groups" aria-label="后台菜单">
        <section
          v-for="section in menuSections"
          :key="section.title"
          class="nav-group"
          :class="{ 'nav-group--account': section.type === 'account' }"
        >
          <p class="nav-group-title">{{ section.title }}</p>
          <RouterLink
            v-for="item in section.items"
            :key="item.key"
            :to="item.to"
            class="nav-link"
            :class="{ active: activeMenu === item.key }"
          >
            {{ item.label }}
          </RouterLink>
        </section>
      </nav>
    </aside>

    <div class="main">
      <header class="header">
        <h2>{{ pageTitle }}</h2>
        <div class="header-actions">
          <div class="identity">
            <strong>{{ displayName }}</strong>
            <span>{{ roleName }}</span>
          </div>
          <RouterLink class="header-secondary-button" to="/profile">
            个人资料
          </RouterLink>
          <button class="logout-button" type="button" @click="logout">退出登录</button>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useAdminLayout } from '../../composables/useAdminLayout'

const { activeMenu, displayName, logout, menuSections, pageTitle, roleName } = useAdminLayout()
</script>

<style src="./admin-layout.css" scoped></style>
