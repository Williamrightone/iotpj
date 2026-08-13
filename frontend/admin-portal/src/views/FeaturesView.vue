<script setup>
import { ref, computed, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import api from '../api'

const auth = useAuthStore()
const ui   = useUiStore()

const features = ref([])
const loading  = ref(false)

const showCreate = ref(false)
const showEdit   = ref(false)
const deleteTarget = ref(null)

const createForm = ref(emptyCreate())
const editForm   = ref({})

function emptyCreate() {
  return { parentId: null, featureCode: '', featureName: '', route: '', sortOrder: 0 }
}

// ── Computed ───────────────────────────────────────────────
// 平坦 → 階層結構
const tree = computed(() => {
  const roots = features.value.filter(f => !f.parentId)
  const children = {}
  features.value.forEach(f => {
    if (f.parentId) {
      if (!children[f.parentId]) children[f.parentId] = []
      children[f.parentId].push(f)
    }
  })
  const result = []
  for (const root of roots) {
    result.push({ ...root, isParent: true })
    if (children[root.featureId]) {
      for (const child of children[root.featureId]) {
        result.push({ ...child, isChild: true })
      }
    }
  }
  // 沒有 parent 的子節點（parentId 找不到對應）也要顯示
  features.value.forEach(f => {
    if (f.parentId && !features.value.find(p => p.featureId === f.parentId)) {
      result.push({ ...f, isOrphan: true })
    }
  })
  return result
})

const parentOptions = computed(() =>
  features.value.filter(f => !f.parentId).map(f => ({ value: f.featureId, label: f.featureName }))
)

// ── API ────────────────────────────────────────────────────
async function fetchFeatures() {
  loading.value = true
  try {
    features.value = await api.listFeatures()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.featureCode || !createForm.value.featureName) {
    ui.showToast('請填寫功能代碼與名稱')
    return
  }
  ui.loading = true
  try {
    await api.createFeature({
      parentId:    createForm.value.parentId || null,
      featureCode: createForm.value.featureCode,
      featureName: createForm.value.featureName,
      route:       createForm.value.route || null,
      sortOrder:   Number(createForm.value.sortOrder) || 0,
    })
    ui.showToast('功能已建立')
    showCreate.value = false
    createForm.value = emptyCreate()
    await fetchFeatures()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function openEdit(f) {
  editForm.value = {
    featureId:   f.featureId,
    featureName: f.featureName,
    route:       f.route || '',
    sortOrder:   f.sortOrder,
  }
  showEdit.value = true
}

async function handleEdit() {
  if (!editForm.value.featureName) {
    ui.showToast('請填寫功能名稱')
    return
  }
  ui.loading = true
  try {
    await api.updateFeature(editForm.value.featureId, {
      featureName: editForm.value.featureName,
      route:       editForm.value.route || null,
      sortOrder:   Number(editForm.value.sortOrder) || 0,
    })
    ui.showToast('功能已更新')
    showEdit.value = false
    await fetchFeatures()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

async function toggleActive(f) {
  try {
    await api.setFeatureActive(f.featureId, !f.isActive)
    f.isActive = !f.isActive
    ui.showToast(`功能「${f.featureName}」已${f.isActive ? '啟用' : '停用'}`)
  } catch (e) {
    ui.showToast(e.message)
  }
}

async function handleDelete() {
  if (!deleteTarget.value) return
  ui.loading = true
  try {
    await api.deleteFeature(deleteTarget.value.featureId)
    ui.showToast(`功能「${deleteTarget.value.featureName}」已刪除`)
    deleteTarget.value = null
    await fetchFeatures()
  } catch (e) {
    ui.showToast(e.message)
    deleteTarget.value = null
  } finally {
    ui.loading = false
  }
}

onMounted(fetchFeatures)
</script>

<template>
  <Sidebar />
  <div class="main">
    <div class="topbar">
      <div>
        <div class="topbar-title">功能管理</div>
        <div class="topbar-sub">管理系統功能項目與選單結構</div>
      </div>
      <div v-if="auth.isAdmin" class="topbar-actions">
        <button class="btn btn-primary" @click="showCreate = true">+ 新增功能</button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2>功能清單（{{ features.length }}）</h2>
        <button class="btn btn-ghost btn-sm" @click="fetchFeatures">重新整理</button>
      </div>

      <div v-if="loading" class="empty-state">
        <div class="loading-spinner" style="margin: 0 auto 8px"></div>
        <p>載入中...</p>
      </div>

      <div v-else-if="!tree.length" class="empty-state">
        <div class="empty-icon">◆</div>
        <p>尚無功能項目</p>
      </div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>功能代碼</th>
              <th>功能名稱</th>
              <th>路由</th>
              <th>排序</th>
              <th>狀態</th>
              <th v-if="auth.isAdmin">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="f in tree"
              :key="f.featureId"
              :class="{ 'feature-parent-row': f.isParent, 'feature-child-row': f.isChild }"
            >
              <td>
                <span v-if="f.isChild" class="text-sec" style="margin-right: 4px">└</span>
                <code style="font-size: 12px; background: var(--bg); padding: 1px 6px; border-radius: 4px;">
                  {{ f.featureCode }}
                </code>
              </td>
              <td>{{ f.featureName }}</td>
              <td class="text-sec text-sm">{{ f.route || '—' }}</td>
              <td class="text-sec">{{ f.sortOrder }}</td>
              <td>
                <label v-if="auth.isAdmin" class="switch" @click.prevent="toggleActive(f)">
                  <input type="checkbox" :checked="f.isActive" readonly />
                  <span class="switch-slider"></span>
                </label>
                <span v-else class="badge" :class="f.isActive ? 'badge-success' : 'badge-gray'">
                  {{ f.isActive ? '啟用' : '停用' }}
                </span>
              </td>
              <td v-if="auth.isAdmin">
                <div class="td-actions">
                  <button class="btn btn-ghost btn-sm" @click="openEdit(f)">編輯</button>
                  <button class="btn btn-danger btn-sm" @click="deleteTarget = f">刪除</button>
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
        <h3>新增功能</h3>
        <button class="modal-close" @click="showCreate = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="form-group" style="margin-bottom: 14px">
          <label>上層功能（留空為根功能）</label>
          <select v-model="createForm.parentId" class="form-select">
            <option :value="null">— 根功能 —</option>
            <option v-for="p in parentOptions" :key="p.value" :value="p.value">{{ p.label }}</option>
          </select>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>功能代碼 <span class="required">*</span></label>
            <input v-model="createForm.featureCode" class="form-input" placeholder="USER_MGMT" />
          </div>
          <div class="form-group">
            <label>功能名稱 <span class="required">*</span></label>
            <input v-model="createForm.featureName" class="form-input" placeholder="使用者管理" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>路由路徑</label>
            <input v-model="createForm.route" class="form-input" placeholder="/users" />
          </div>
          <div class="form-group">
            <label>排序</label>
            <input v-model.number="createForm.sortOrder" class="form-input" type="number" min="0" />
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
        <h3>編輯功能</h3>
        <button class="modal-close" @click="showEdit = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="form-group" style="margin-bottom: 14px">
          <label>功能名稱 <span class="required">*</span></label>
          <input v-model="editForm.featureName" class="form-input" />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>路由路徑</label>
            <input v-model="editForm.route" class="form-input" />
          </div>
          <div class="form-group">
            <label>排序</label>
            <input v-model.number="editForm.sortOrder" class="form-input" type="number" min="0" />
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-ghost" @click="showEdit = false">取消</button>
        <button class="btn btn-primary" @click="handleEdit">儲存</button>
      </div>
    </div>
  </div>

  <!-- ── Delete Confirm ───────────────────────────────── -->
  <ConfirmDialog
    v-if="deleteTarget"
    title="刪除功能"
    :message="`確定要刪除功能「${deleteTarget?.featureName}」？此操作將同步清除所有角色的此功能權限，且無法復原。`"
    confirm-text="刪除"
    @confirm="handleDelete"
    @cancel="deleteTarget = null"
  />
</template>
