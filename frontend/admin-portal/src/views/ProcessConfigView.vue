<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../components/Sidebar.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import api from '../api'

const router = useRouter()
const auth   = useAuthStore()
const ui     = useUiStore()

const canWrite = auth.user?.role !== 'VIEWER'

// ── State ────────────────────────────────────────────────────
const stations = ref([])
const loading  = ref(false)

// Create modal
const showCreate   = ref(false)
const createForm   = ref(emptyCreate())

// Edit modal
const showEdit     = ref(false)
const editForm     = ref({})

// Confirm deactivate
const confirmTarget = ref(null)

function emptyCreate() {
  return { stationCode: '', name: '', description: '' }
}

// ── API ──────────────────────────────────────────────────────
async function fetchStations() {
  loading.value = true
  try {
    stations.value = await api.listStations()
    stations.value.sort((a, b) => a.sortOrder - b.sortOrder)
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.stationCode || !createForm.value.name) {
    ui.showToast('請填寫站點代碼與名稱')
    return
  }
  ui.loading = true
  try {
    await api.createStation({
      stationCode:  createForm.value.stationCode,
      name:         createForm.value.name,
      description:  createForm.value.description || null,
    })
    ui.showToast('站點已建立')
    showCreate.value = false
    createForm.value = emptyCreate()
    await fetchStations()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function openEdit(s) {
  editForm.value = { id: s.id, name: s.name, description: s.description || '' }
  showEdit.value = true
}

async function handleEdit() {
  if (!editForm.value.name) {
    ui.showToast('請填寫名稱')
    return
  }
  ui.loading = true
  try {
    await api.updateStation(editForm.value.id, {
      name:        editForm.value.name,
      description: editForm.value.description || null,
    })
    ui.showToast('站點已更新')
    showEdit.value = false
    await fetchStations()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function confirmDeactivate(s) {
  confirmTarget.value = s
}

async function handleDeactivate() {
  if (!confirmTarget.value) return
  ui.loading = true
  try {
    const result = await api.deactivateStation(confirmTarget.value.id)
    ui.showToast(`已停用，共停用 ${result.deactivatedMachines} 台機台、${result.deactivatedComponents} 個元件`)
    confirmTarget.value = null
    await fetchStations()
  } catch (e) {
    ui.showToast(e.message)
    confirmTarget.value = null
  } finally {
    ui.loading = false
  }
}

async function handleActivate(s) {
  ui.loading = true
  try {
    await api.activateStation(s.id)
    ui.showToast('站點已啟用')
    await fetchStations()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

// ── Drag-to-reorder ─────────────────────────────────────────
const draggingIndex = ref(null)

function onDragStart(idx) {
  draggingIndex.value = idx
}

function onDragOver(e, idx) {
  e.preventDefault()
  if (draggingIndex.value === null || draggingIndex.value === idx) return
  const moved = stations.value.splice(draggingIndex.value, 1)[0]
  stations.value.splice(idx, 0, moved)
  draggingIndex.value = idx
}

async function onDragEnd() {
  draggingIndex.value = null
  const items = stations.value.map((s, i) => ({ id: s.id, sortOrder: i + 1 }))
  try {
    await api.reorderStations(items)
  } catch (e) {
    ui.showToast(e.message)
    await fetchStations()
  }
}

function goDetail(s) {
  router.push(`/process-config/${s.id}`)
}

onMounted(fetchStations)
</script>

<template>
  <div class="layout">
    <Sidebar />
    <main class="main">
      <div class="page-header">
        <h1 class="page-title">製程配置</h1>
        <button v-if="canWrite" class="btn-primary" @click="showCreate = true">+ 新增站點</button>
      </div>

      <div v-if="loading" class="loading-text">載入中...</div>

      <div v-else class="station-list">
        <div
          v-for="(s, idx) in stations"
          :key="s.id"
          class="station-card"
          :class="{ inactive: !s.active }"
          draggable="true"
          @dragstart="onDragStart(idx)"
          @dragover="onDragOver($event, idx)"
          @dragend="onDragEnd"
        >
          <div class="station-drag-handle" title="拖拉排序">⠿</div>
          <div class="station-body" @click="goDetail(s)" style="cursor:pointer;flex:1">
            <div class="station-header-row">
              <span class="station-code">{{ s.stationCode }}</span>
              <span class="station-name">{{ s.name }}</span>
              <span class="badge" :class="s.active ? 'badge-active' : 'badge-inactive'">
                {{ s.active ? '啟用' : '停用' }}
              </span>
            </div>
            <div v-if="s.description" class="station-desc">{{ s.description }}</div>
            <div class="station-meta">
              機台：{{ s.activeMachineCount }}　元件：{{ s.activeComponentCount }}　排序：{{ s.sortOrder }}
            </div>
          </div>
          <div class="station-actions" @click.stop>
            <button v-if="canWrite" class="btn-sm" @click="openEdit(s)">編輯</button>
            <button v-if="canWrite && s.active"  class="btn-sm btn-danger"  @click="confirmDeactivate(s)">停用</button>
            <button v-if="canWrite && !s.active" class="btn-sm btn-success" @click="handleActivate(s)">啟用</button>
          </div>
        </div>

        <div v-if="!stations.length" class="empty-state">尚無站點，請新增</div>
      </div>
    </main>
  </div>

  <!-- Create Modal -->
  <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
    <div class="modal">
      <div class="modal-header">
        <h2>新增站點</h2>
        <button class="modal-close" @click="showCreate = false">✕</button>
      </div>
      <div class="modal-body">
        <label class="form-label">站點代碼 *</label>
        <input v-model="createForm.stationCode" class="form-input" placeholder="e.g. SMT-01" />
        <label class="form-label">名稱 *</label>
        <input v-model="createForm.name" class="form-input" placeholder="站點顯示名稱" />
        <label class="form-label">說明</label>
        <input v-model="createForm.description" class="form-input" placeholder="選填" />
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="showCreate = false">取消</button>
        <button class="btn-primary" @click="handleCreate">建立</button>
      </div>
    </div>
  </div>

  <!-- Edit Modal -->
  <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
    <div class="modal">
      <div class="modal-header">
        <h2>編輯站點</h2>
        <button class="modal-close" @click="showEdit = false">✕</button>
      </div>
      <div class="modal-body">
        <label class="form-label">名稱 *</label>
        <input v-model="editForm.name" class="form-input" />
        <label class="form-label">說明</label>
        <input v-model="editForm.description" class="form-input" />
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="showEdit = false">取消</button>
        <button class="btn-primary" @click="handleEdit">儲存</button>
      </div>
    </div>
  </div>

  <!-- Confirm Deactivate -->
  <ConfirmDialog
    v-if="confirmTarget"
    :message="`確定停用站點「${confirmTarget.name}」？此操作將連帶停用所有機台與元件。`"
    @confirm="handleDeactivate"
    @cancel="confirmTarget = null"
  />
</template>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.main   { flex: 1; padding: 2rem; background: var(--bg); }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-title  { font-size: 1.5rem; font-weight: 700; color: var(--text-primary); }

.station-list { display: flex; flex-direction: column; gap: 0.75rem; }

.station-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 1rem 1.25rem;
  transition: box-shadow 0.15s;
}
.station-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.12); }
.station-card.inactive { opacity: 0.6; }

.station-drag-handle { cursor: grab; color: var(--text-secondary); font-size: 1.2rem; }

.station-header-row { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.25rem; }
.station-code       { font-family: monospace; font-size: 0.85rem; color: var(--text-secondary); }
.station-name       { font-weight: 600; color: var(--text-primary); }
.station-desc       { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 0.25rem; }
.station-meta       { font-size: 0.8rem; color: var(--text-muted, #888); }

.station-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }

.badge         { display: inline-block; padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.75rem; }
.badge-active  { background: #d4edda; color: #155724; }
.badge-inactive{ background: #f8d7da; color: #721c24; }

.loading-text { padding: 2rem; text-align: center; color: var(--text-secondary); }
.empty-state  { text-align: center; padding: 3rem; color: var(--text-secondary); }

.btn-primary   { padding: 0.5rem 1rem; border-radius: 6px; border: none; cursor: pointer;
                  background: var(--accent); color: #fff; font-size: 0.9rem; }
.btn-secondary { padding: 0.5rem 1rem; border-radius: 6px; border: 1px solid var(--border); cursor: pointer;
                  background: transparent; color: var(--text-primary); font-size: 0.9rem; }
.btn-sm        { padding: 0.3rem 0.7rem; border-radius: 5px; border: 1px solid var(--border);
                  cursor: pointer; background: var(--card-bg); color: var(--text-primary); font-size: 0.8rem; }
.btn-danger    { border-color: #dc3545; color: #dc3545; }
.btn-success   { border-color: #28a745; color: #28a745; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); display: flex;
                  align-items: center; justify-content: center; z-index: 100; }
.modal         { background: var(--card-bg); border-radius: 10px; width: 440px; max-width: 95vw;
                  box-shadow: 0 8px 32px rgba(0,0,0,.2); }
.modal-header  { display: flex; justify-content: space-between; align-items: center;
                  padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--border); }
.modal-header h2 { font-size: 1.1rem; font-weight: 700; color: var(--text-primary); }
.modal-close   { background: none; border: none; cursor: pointer; font-size: 1.1rem; color: var(--text-secondary); }
.modal-body    { padding: 1.25rem 1.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
.modal-footer  { display: flex; justify-content: flex-end; gap: 0.75rem;
                  padding: 1rem 1.5rem; border-top: 1px solid var(--border); }

.form-label    { font-size: 0.85rem; font-weight: 600; color: var(--text-secondary); }
.form-input    { padding: 0.5rem 0.75rem; border: 1px solid var(--border); border-radius: 6px;
                  background: var(--bg); color: var(--text-primary); font-size: 0.9rem; width: 100%;
                  box-sizing: border-box; }
.form-input:focus { outline: none; border-color: var(--accent); }
</style>
