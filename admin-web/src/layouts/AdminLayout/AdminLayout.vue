<template>
  <div class="layout" data-testid="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-shell">
        <div class="brand">
          <h1>农产品追溯后台</h1>
        </div>

        <nav class="nav-groups" aria-label="后台菜单">
          <section
            v-for="section in sidebarMenuSections"
            :key="section.title"
            class="nav-group"
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

        <section class="sidebar-account" data-testid="sidebar-account-section">
          <p class="nav-group-title">{{ accountMenuSection?.title || '账号设置' }}</p>
          <RouterLink
            v-for="item in accountMenuSection?.items || []"
            :key="item.key"
            :to="item.to"
            class="nav-link"
            :class="{ active: activeMenu === item.key }"
            data-testid="sidebar-profile-entry"
          >
            {{ item.label }}
          </RouterLink>
          <button
            class="nav-link nav-link--button"
            type="button"
            data-testid="sidebar-logout-entry"
            @click="logout"
          >
            退出登录
          </button>
        </section>
      </div>
    </aside>

    <div class="main">
      <header class="header">
        <h2 v-if="headerTitle">{{ headerTitle }}</h2>
        <div id="admin-header-actions" class="header-actions"></div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAdminLayout } from '../../composables/useAdminLayout'

const { activeMenu, logout, menuSections, pageTitle } = useAdminLayout()
const route = useRoute()
const sidebarMenuSections = computed(() => menuSections.value.filter((section) => section.type !== 'account'))
const accountMenuSection = computed(() => menuSections.value.find((section) => section.type === 'account'))
const showPageTitle = computed(() => route.meta?.hideHeaderTitle !== true)
const headerTitle = computed(() => route.meta?.headerTitle || (showPageTitle.value ? pageTitle.value : ''))
</script>

<style src="./admin-layout.css" scoped></style>
