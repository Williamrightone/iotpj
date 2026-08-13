<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../components/Sidebar.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import api from '../api'

const router = useRouter()
const auth   = useAuthStore()
const ui     = useUiStore()

// ── Guard ─────────────────────────────────────────────────
if (!auth.isAdmin) router.push('/dashboard')

// ── State ──────────────────────────────────────────────────
const users       = ref([])
const loading     = ref(false)
const searchText  = ref('')
const filterRole  = ref('')

// Modals
const showCreate  = ref(false)
const showEdit    = ref(false)
const showStation = ref(false)
const confirmTarget = ref(null)  // { userId, displayName }

// Forms
const createForm = ref(emptyCreate())
const editForm   = ref({})
const stationForm = ref({ userId: null, stationInput: '', tags: [] })

function emptyCreate() {
  return { account: '', displayName: '', role: 'VIEWER', password: '', stationIds: [] }
}

// ── Computed ───────────────────────────────────────────────
const filteredUsers = computed(() => {
  let list = users.value
  if (searchText.value) {
    const q = searchText.value.toLowerCase()
    list = list.filter(u =>
      u.account.toLowerCase().includes(q) ||
      u.displayName.toLowerCase().includes(q)
    )
  }
  if (filterRole.value) {
    list = list.filter(u => u.role === filterRole.value)
  }
  return list
})

// ── Helpers ────────────────────────────────────────────────
function roleBadge(role) {
  return { ADMIN: 'badge-admin', MAINTAINER: 'badge-maintainer', VIEWER: 'badge-viewer' }[role] || 'badge-gray'
}
function roleLabel(role) {
  return { ADMIN: '管理員', MAINTAINER: '維護人員', VIEWER: '檢視人員' }[role] || role
}
function fmtDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleDateString('zh-TW', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// ── Station tag input ──────────────────────────────────────
function stationKeydown(e) {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    addStationTag()
  } else if (e.key === 'Backspace' && !stationForm.value.stationInput) {
    stationForm.value.tags.pop()
  }
}
function addStationTag() {
  const val = stationForm.value.stationInput.trim()
  if (val && !stationForm.value.tags.includes(val)) {
    stationForm.value.tags.push(val)
  }
  stationForm.value.stationInput = ''
}
function removeStationTag(i) {
  stationForm.value.tags.splice(i, 1)
}

// Create form station tags
const createStationInput = ref('')
function createStationKeydown(e) {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    const val = createStationInput.value.trim()
    if (val && !createForm.value.stationIds.includes(val)) {
      createForm.value.stationIds.push(val)
    }
    createStationInput.value = ''
  } else if (e.key === 'Backspace' && !createStationInput.value) {
    createForm.value.stationIds.pop()
  }
}

// Edit form station tags
const editStationInput = ref('')
function editStationKeydown(e) {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    const val = editStationInput.value.trim()
    if (val && !editForm.value.stationIds?.includes(val)) {
      editForm.value.stationIds = [...(editForm.value.stationIds || []), val]
    }
    editStationInput.value = ''
  } else if (e.key === 'Backspace' && !editStationInput.value) {
    editForm.value.stationIds?.pop()
  }
}

// ── API ────────────────────────────────────────────────────
async function fetchUsers() {
  loading.value = true
  try {
    users.value = await api.listUsers()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.account || !createForm.value.displayName || !createForm.value.password) {
    ui.showToast('請填寫必要欄位')
    return
  }
  ui.loading = true
  try {
    await api.createUser({
      account:     createForm.value.account,
      displayName: createForm.value.displayName,
      role:        createForm.value.role,
      password:    createForm.value.password,
      stationIds:  createForm.value.stationIds,
    })
    ui.showToast('使用者已建立')
    showCreate.value = false
    createForm.value = emptyCreate()
    createStationInput.value = ''
    await fetchUsers()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function openEdit(user) {
  editForm.value = {
    userId:      user.userId,
    displayName: user.displayName,
    role:        user.role,
    stationIds:  [...(user.stationIds || [])],
  }
  editStationInput.value = ''
  showEdit.value = true
}

async function handleEdit() {
  if (!editForm.value.displayName) {
    ui.showToast('請填寫顯示名稱')
    return
  }
  ui.loading = true
  try {
    await api.updateUser(editForm.value.userId, {
      displayName: editForm.value.displayName,
      role:        editForm.value.role,
      stationIds:  editForm.value.stationIds || [],
    })
    ui.showToast('使用者已更新')
    showEdit.value = false
    await fetchUsers()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function openDisableConfirm(user) {
  confirmTarget.value = user
}

async function handleDisable() {
  if (!confirmTarget.value) return
  ui.loading = true
  try {
    await api.disableUser(confirmTarget.value.userId)
    ui.showToast(`使用者 ${confirmTarget.value.displayName} 已停用`)
    confirmTarget.value = null
    await fetchUsers()
  } catch (e) {
    ui.showToast(e.message)
    confirmTarget.value = null
  } finally {
    ui.loading = false
  }
}

async function openStations(user) {
  stationForm.value = { userId: user.userId, stationInput: '', tags: [] }
  showStation.value = true
  try {
    const ids = await api.getStations(user.userId)
    stationForm.value.tags = ids || []
  } catch (e) {
    ui.showToast(e.message)
  }
}

async function handleSaveStations() {
  if (stationForm.value.stationInput.trim()) {
    addStationTag()
  }
  ui.loading = true
  try {
    await api.updateStations(stationForm.value.userId, stationForm.value.tags)
    ui.showToast('站點已更新')
    showStation.value = false
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

onMounted(fetchUsers)
</script>

<template>
  <Sidebar />
  <div class="main">
    <div class="topbar">
      <div>
        <div class="topbar-title">使用者管理</div>
        <div class="topbar-sub">管理租戶內的使用者帳號與存取權限</div>
      </div>
      <div class="topbar-actions">
        <button class="btn btn-primary" @click="showCreate = true">+ 新增使用者</button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2>使用者清單（{{ filteredUsers.length }}）</h2>
        <div class="filter-bar" style="margin: 0">
          <input v-model="searchText" class="form-input" placeholder="搜尋帳號或名稱..." />
          <select v-model="filterRole" class="form-select">
            <option value="">所有角色</option>
            <option value="ADMIN">管理員</option>
            <option value="MAINTAINER">維護人員</option>
            <option value="VIEWER">檢視人員</option>
          </select>
          <button class="btn btn-ghost btn-sm" @click="fetchUsers">重新整理</button>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div class="loading-spinner" style="margin: 0 auto 8px"></div>
        <p>載入中...</p>
      </div>

      <div v-else-if="!filteredUsers.length" class="empty-state">
        <div class="empty-icon">◉</div>
        <p>{{ searchText || filterRole ? '沒有符合條件的使用者' : '尚無使用者' }}</p>
      </div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>帳號</th>
              <th>顯示名稱</th>
              <th>角色</th>
              <th>狀態</th>
              <th>最後登入</th>
              <th>建立時間</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.userId">
              <td>
                <span style="font-family: monospace; font-size: 12px;">{{ u.account }}</span>
              </td>
              <td>{{ u.displayName }}</td>
              <td>
                <span class="badge" :class="roleBadge(u.role)">{{ roleLabel(u.role) }}</span>
              </td>
              <td>
                <span class="badge" :class="u.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                  {{ u.status === 'ACTIVE' ? '啟用' : '停用' }}
                </span>
              </td>
              <td class="text-sec">{{ fmtDate(u.lastLoginAt) }}</td>
              <td class="text-sec">{{ fmtDate(u.createdAt) }}</td>
              <td>
                <div class="td-actions">
                  <button class="btn btn-ghost btn-sm" @click="openEdit(u)">編輯</button>
                  <button class="btn btn-outline btn-sm" @click="openStations(u)">站點</button>
                  <button
                    v-if="u.status === 'ACTIVE' && u.userId !== auth.user?.userId"
                    class="btn btn-danger btn-sm"
                    @click="openDisableConfirm(u)"
                  >停用</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- ── Create Modal ─────────────────────────────────── -->
  <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
    <div class="modal">
      <div class="modal-header">
        <h3>新增使用者</h3>
        <button class="modal-close" @click="showCreate = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="form-row">
          <div class="form-group">
            <label>Email 帳號 <span class="required">*</span></label>
            <input v-model="createForm.account" class="form-input" type="email" placeholder="user@example.com" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>顯示名稱 <span class="required">*</span></label>
            <input v-model="createForm.displayName" class="form-input" placeholder="王小明" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>角色 <span class="required">*</span></label>
            <select v-model="createForm.role" class="form-select">
              <option value="ADMIN">管理員</option>
              <option value="MAINTAINER">維護人員</option>
              <option value="VIEWER">檢視人員</option>
            </select>
          </div>
          <div class="form-group">
            <label>初始密碼 <span class="required">*</span></label>
            <input v-model="createForm.password" class="form-input" type="password" placeholder="••••••••" />
          </div>
        </div>
        <div class="form-group">
          <label>綁定站點（按 Enter 或逗號新增）</label>
          <div class="tags-wrap" @click="$el.querySelector('.tag-input')?.focus()">
            <span v-for="(s, i) in createForm.stationIds" :key="i" class="tag">
              {{ s }}
              <button class="tag-remove" @click="createForm.stationIds.splice(i, 1)">×</button>
            </span>
            <input
              v-model="createStationInput"
              class="tag-input"
              placeholder="輸入站點 ID"
              @keydown="createStationKeydown"
            />
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-ghost" @click="showCreate = false">取消</button>
        <button class="btn btn-primary" @click="handleCreate">建立</button>
      </div>
    </div>
  </div>

  <!-- ── Edit Modal ───────────────────────────────────── -->
  <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
    <div class="modal">
      <div class="modal-header">
        <h3>編輯使用者</h3>
        <button class="modal-close" @click="showEdit = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="form-group" style="margin-bottom: 14px">
          <label>顯示名稱 <span class="required">*</span></label>
          <input v-model="editForm.displayName" class="form-input" />
        </div>
        <div class="form-group" style="margin-bottom: 14px">
          <label>角色</label>
          <select v-model="editForm.role" class="form-select" :disabled="editForm.userId === auth.user?.userId">
            <option value="ADMIN">管理員</option>
            <option value="MAINTAINER">維護人員</option>
            <option value="VIEWER">檢視人員</option>
          </select>
          <p v-if="editForm.userId === auth.user?.userId" class="text-sec text-sm" style="margin-top: 4px">
            不可修改自己的角色
          </p>
        </div>
        <div class="form-group">
          <label>綁定站點（按 Enter 或逗號新增）</label>
          <div class="tags-wrap">
            <span v-for="(s, i) in editForm.stationIds" :key="i" class="tag">
              {{ s }}
              <button class="tag-remove" @click="editForm.stationIds.splice(i, 1)">×</button>
            </span>
            <input
              v-model="editStationInput"
              class="tag-input"
              placeholder="輸入站點 ID"
              @keydown="editStationKeydown"
            />
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-ghost" @click="showEdit = false">取消</button>
        <button class="btn btn-primary" @click="handleEdit">儲存</button>
      </div>
    </div>
  </div>

  <!-- ── Stations Modal ───────────────────────────────── -->
  <div v-if="showStation" class="modal-overlay" @click.self="showStation = false">
    <div class="modal">
      <div class="modal-header">
        <h3>管理站點綁定</h3>
        <button class="modal-close" @click="showStation = false">✕</button>
      </div>
      <div class="modal-body">
        <p class="text-sec text-sm" style="margin-bottom: 12px">按 Enter 或逗號新增站點 ID，點擊 × 移除</p>
        <div class="tags-wrap">
          <span v-for="(s, i) in stationForm.tags" :key="i" class="tag">
            {{ s }}
            <button class="tag-remove" @click="removeStationTag(i)">×</button>
          </span>
          <input
            v-model="stationForm.stationInput"
            class="tag-input"
            placeholder="輸入站點 ID"
            @keydown="stationKeydown"
          />
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-ghost" @click="showStation = false">取消</button>
        <button class="btn btn-primary" @click="handleSaveStations">儲存站點</button>
      </div>
    </div>
  </div>

  <!-- ── Confirm disable ──────────────────────────────── -->
  <ConfirmDialog
    v-if="confirmTarget"
    :message="`確定要停用使用者「${confirmTarget.displayName}」？停用後該帳號無法登入。`"
    title="停用使用者"
    confirm-text="停用"
    @confirm="handleDisable"
    @cancel="confirmTarget = null"
  />
</template>
