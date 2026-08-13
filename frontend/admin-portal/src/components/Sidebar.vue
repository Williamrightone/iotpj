<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()
const ui     = useUiStore()

const menuItems = [
  { section: '概覽' },
  { label: '儀表板', icon: '◈', path: '/dashboard' },
  { section: '系統管理' },
  { label: '使用者管理', icon: '◉', path: '/users',            adminOnly: true },
  { label: '功能管理',   icon: '◆', path: '/features' },
  { label: '角色權限',   icon: '◇', path: '/role-permissions' },
]

async function handleLogout() {
  ui.loading = true
  await auth.logout()
  ui.loading = false
  router.push('/login')
}
</script>

<template>
  <nav class="sidebar">
    <div class="sidebar-logo">
      <div class="sidebar-logo-title">willThx IoT</div>
      <div class="sidebar-logo-sub">Admin Portal</div>
    </div>

    <template v-for="item in menuItems" :key="item.label || item.section">
      <div v-if="item.section" class="sidebar-section">{{ item.section }}</div>
      <template v-else-if="!item.adminOnly || auth.isAdmin">
        <router-link
          :to="item.path"
          class="sidebar-item"
          :class="{ active: route.path === item.path }"
        >
          <span class="sidebar-icon">{{ item.icon }}</span>
          {{ item.label }}
        </router-link>
      </template>
    </template>

    <div class="sidebar-bottom">
      <div class="sidebar-user">
        <div class="sidebar-avatar">{{ (auth.displayName || '?').charAt(0).toUpperCase() }}</div>
        <div class="sidebar-user-info">
          <div class="sidebar-user-name">{{ auth.displayName }}</div>
          <div class="sidebar-user-role">{{ auth.user?.role || '' }}</div>
        </div>
      </div>
      <button class="sidebar-logout" @click="handleLogout">
        <span>→</span> 登出
      </button>
    </div>
  </nav>
</template>
