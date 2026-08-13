<script setup>
import { ref, onMounted, computed } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { useAuthStore } from '../stores/auth'
import api from '../api'

const auth = useAuthStore()

const users   = ref([])
const loading = ref(false)

const features = computed(() => auth.user?.features || [])

const roleLabel = {
  ADMIN:      '系統管理員',
  MAINTAINER: '維護人員',
  VIEWER:     '檢視人員',
}

const navCards = computed(() => {
  const cards = [
    { label: '功能管理',   icon: '◆', path: '/features',         desc: '管理系統功能與選單設定',     color: '#2563EB' },
    { label: '角色權限',   icon: '◇', path: '/role-permissions', desc: '設定各角色可存取的功能項目', color: '#7C3AED' },
  ]
  if (auth.isAdmin) {
    cards.unshift({
      label: '使用者管理', icon: '◉', path: '/users',
      desc: '新增、停用及管理使用者帳號', color: '#0EA5E9'
    })
  }
  return cards
})

const now = new Date()
const dateStr = `${now.getFullYear()}/${String(now.getMonth()+1).padStart(2,'0')}/${String(now.getDate()).padStart(2,'0')}  ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`

onMounted(async () => {
  if (!auth.isAdmin) return
  loading.value = true
  try {
    users.value = await api.listUsers().catch(() => [])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <Sidebar />
  <div class="main">
    <div class="topbar">
      <div>
        <div class="topbar-title">儀表板</div>
        <div class="topbar-sub">{{ dateStr }}</div>
      </div>
    </div>

    <!-- 使用者資訊 -->
    <div class="user-card panel">
      <div class="panel-body user-card-body">
        <div class="user-big-avatar">{{ (auth.displayName || '?').charAt(0).toUpperCase() }}</div>
        <div class="user-card-info">
          <div class="user-card-name">{{ auth.displayName }}</div>
          <div class="user-card-account">{{ auth.user?.account }}</div>
          <div class="user-card-meta">
            <span class="badge" :class="{
              'badge-admin': auth.user?.role === 'ADMIN',
              'badge-maintainer': auth.user?.role === 'MAINTAINER',
              'badge-viewer': auth.user?.role === 'VIEWER'
            }">{{ roleLabel[auth.user?.role] || auth.user?.role }}</span>
            <span class="user-card-tenant text-sec text-sm">Tenant #{{ auth.user?.tenantId }}</span>
          </div>
          <div v-if="auth.user?.stationIds?.length" class="user-stations">
            <span class="text-sm text-sec">綁定站點：</span>
            <span v-for="s in auth.user.stationIds" :key="s" class="tag">{{ s }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 統計卡 (ADMIN) -->
    <div v-if="auth.isAdmin && !loading" class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">使用者總數</div>
        <div class="stat-value">{{ users.length }}</div>
        <div class="stat-sub">
          {{ users.filter(u => u.status === 'ACTIVE').length }} 啟用中
        </div>
      </div>
      <div class="stat-card" style="border-top-color: #7C3AED">
        <div class="stat-label">功能項目</div>
        <div class="stat-value">{{ features.length }}</div>
        <div class="stat-sub">
          {{ features.filter(f => f.isActive).length }} 已啟用
        </div>
      </div>
      <div class="stat-card" style="border-top-color: #10B981">
        <div class="stat-label">管理員人數</div>
        <div class="stat-value">{{ users.filter(u => u.role === 'ADMIN').length }}</div>
      </div>
      <div class="stat-card" style="border-top-color: #F59E0B">
        <div class="stat-label">停用帳號</div>
        <div class="stat-value">{{ users.filter(u => u.status === 'DISABLED').length }}</div>
      </div>
    </div>

    <!-- 快速導覽 -->
    <div class="panel">
      <div class="panel-header"><h2>功能導覽</h2></div>
      <div class="panel-body nav-grid">
        <router-link
          v-for="card in navCards"
          :key="card.path"
          :to="card.path"
          class="nav-card"
          :style="{ borderTopColor: card.color }"
        >
          <div class="nav-card-icon" :style="{ color: card.color }">{{ card.icon }}</div>
          <div class="nav-card-label">{{ card.label }}</div>
          <div class="nav-card-desc">{{ card.desc }}</div>
        </router-link>
      </div>
    </div>

    <!-- 我的可存取功能 -->
    <div v-if="auth.user?.features?.length" class="panel">
      <div class="panel-header"><h2>我的可存取功能</h2></div>
      <div class="panel-body">
        <div class="feat-tags">
          <span
            v-for="f in auth.user.features"
            :key="f.featureId"
            class="feat-tag"
          >{{ f.featureName }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-card-body {
  display: flex; align-items: center; gap: 24px;
}
.user-big-avatar {
  width: 64px; height: 64px; border-radius: 50%;
  background: var(--primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 800; flex-shrink: 0;
}
.user-card-name    { font-size: 20px; font-weight: 700; }
.user-card-account { font-size: 13px; color: var(--text-sec); margin-top: 2px; }
.user-card-meta    { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.user-card-tenant  { }
.user-stations     { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-top: 8px; }

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.nav-card {
  display: block;
  background: var(--bg);
  border-radius: 10px;
  padding: 20px;
  border-top: 3px solid var(--primary);
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.15s, transform 0.15s;
}
.nav-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  transform: translateY(-1px);
}
.nav-card-icon  { font-size: 24px; margin-bottom: 8px; }
.nav-card-label { font-size: 15px; font-weight: 700; margin-bottom: 4px; }
.nav-card-desc  { font-size: 12px; color: var(--text-sec); }

.feat-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.feat-tag {
  background: var(--primary-light);
  color: var(--primary);
  font-size: 12px; font-weight: 600;
  padding: 4px 10px; border-radius: 6px;
}
</style>
