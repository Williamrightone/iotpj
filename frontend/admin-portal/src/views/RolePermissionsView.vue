<script setup>
import { ref, computed, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import api from '../api'

const auth = useAuthStore()
const ui   = useUiStore()

const features    = ref([])
const permissions = ref({})  // { ADMIN: [featureId,...], MAINTAINER: [...], VIEWER: [...] }
const loading     = ref(false)
const saving      = ref('')   // 目前儲存中的角色

const ROLES = [
  { key: 'ADMIN',      label: '管理員',   color: '#5B21B6' },
  { key: 'MAINTAINER', label: '維護人員', color: '#1D4ED8' },
  { key: 'VIEWER',     label: '檢視人員', color: '#374151' },
]

// ── Computed ───────────────────────────────────────────────
const tree = computed(() => {
  const roots = features.value.filter(f => !f.parentId)
  const result = []
  for (const root of roots) {
    result.push(root)
    const kids = features.value.filter(f => f.parentId === root.featureId)
    for (const kid of kids) result.push(kid)
  }
  return result
})

function isChecked(role, featureId) {
  return (permissions.value[role] || []).includes(featureId)
}

function togglePermission(role, featureId) {
  if (!auth.isAdmin) return
  const list = permissions.value[role] || []
  if (list.includes(featureId)) {
    permissions.value[role] = list.filter(id => id !== featureId)
  } else {
    permissions.value[role] = [...list, featureId]
  }
}

// ── API ────────────────────────────────────────────────────
async function fetchAll() {
  loading.value = true
  try {
    const [f, p] = await Promise.all([api.listFeatures(), api.getRolePermissions()])
    features.value    = f || []
    permissions.value = p || {}
    // 確保每個角色都有陣列
    for (const r of ROLES) {
      if (!Array.isArray(permissions.value[r.key])) {
        permissions.value[r.key] = []
      }
    }
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    loading.value = false
  }
}

async function saveRole(role) {
  saving.value = role
  try {
    await api.updateRolePermissions(role, permissions.value[role] || [])
    ui.showToast(`${ROLES.find(r => r.key === role)?.label} 權限已儲存`)
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    saving.value = ''
  }
}

onMounted(fetchAll)
</script>

<template>
  <Sidebar />
  <div class="main">
    <div class="topbar">
      <div>
        <div class="topbar-title">角色權限</div>
        <div class="topbar-sub">設定各角色可存取的功能項目</div>
      </div>
      <button class="btn btn-ghost btn-sm" @click="fetchAll">重新整理</button>
    </div>

    <div v-if="loading" class="empty-state">
      <div class="loading-spinner" style="margin: 0 auto 8px"></div>
      <p>載入中...</p>
    </div>

    <template v-else>
      <!-- 說明 -->
      <div v-if="!auth.isAdmin" class="panel" style="margin-bottom: 16px">
        <div class="panel-body" style="color: var(--text-sec); font-size: 13px">
          ⚠ 您目前為唯讀模式，僅管理員可修改角色權限設定。
        </div>
      </div>

      <!-- 三欄權限表 -->
      <div class="perm-grid">
        <div v-for="role in ROLES" :key="role.key" class="perm-col">
          <div class="perm-col-header" :style="{ color: role.color }">
            {{ role.label }}
            <span class="text-sec text-sm" style="font-weight: normal; margin-left: 4px">
              （{{ (permissions[role.key] || []).length }} 項）
            </span>
          </div>

          <div v-if="!tree.length" class="text-sec text-sm">尚無功能項目</div>

          <div
            v-for="f in tree"
            :key="f.featureId"
            class="perm-item"
            :class="{ child: !!f.parentId }"
            @click="togglePermission(role.key, f.featureId)"
          >
            <input
              class="perm-check"
              type="checkbox"
              :checked="isChecked(role.key, f.featureId)"
              :disabled="!auth.isAdmin"
              @click.stop="togglePermission(role.key, f.featureId)"
            />
            <div class="perm-name">
              <div>{{ f.featureName }}</div>
              <div class="perm-code">{{ f.featureCode }}</div>
            </div>
            <span v-if="!f.isActive" class="badge badge-gray text-sm" style="font-size: 10px; padding: 1px 5px;">停用</span>
          </div>

          <div v-if="auth.isAdmin" style="margin-top: 16px">
            <button
              class="btn btn-primary btn-sm"
              style="width: 100%"
              :disabled="saving === role.key"
              @click="saveRole(role.key)"
            >
              {{ saving === role.key ? '儲存中...' : '儲存' + role.label + '權限' }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.perm-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
</style>
