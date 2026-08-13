<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()
const ui     = useUiStore()

const ICON_MAP = {
  DASHBOARD:      '◈',
  USER_LIST:      '◉',
  SYS_FEATURES:   '◆',
  SYS_ROLE_PERMS: '◇',
}

// 將平坦陣列組成 [{ group, children[] }]，依 sortOrder 排序
const menuGroups = computed(() => {
  const features = auth.user?.features || []
  const parents  = features
    .filter(f => f.parentId === null)
    .sort((a, b) => a.sortOrder - b.sortOrder)

  return parents.map(parent => ({
    group:    parent,
    children: features
      .filter(f => f.parentId === parent.featureId)
      .sort((a, b) => a.sortOrder - b.sortOrder),
  }))
})

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

    <template v-for="{ group, children } in menuGroups" :key="group.featureId">
      <div class="sidebar-section">{{ group.featureName }}</div>
      <router-link
        v-for="child in children"
        :key="child.featureId"
        :to="child.route"
        class="sidebar-item"
        :class="{ active: route.path === child.route }"
      >
        <span class="sidebar-icon">{{ ICON_MAP[child.featureCode] || '▸' }}</span>
        {{ child.featureName }}
      </router-link>
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
