<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../components/Sidebar.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import api from '../api'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()
const ui     = useUiStore()

const stationId = Number(route.params.id)
const canWrite  = auth.user?.role !== 'VIEWER'

// ── State ────────────────────────────────────────────────────
const detail  = ref(null)
const loading = ref(false)

// Accordion
const expandedMachines = ref(new Set())

// Modals
const showAddMachine      = ref(false)
const showEditMachine     = ref(false)
const showCopyMachine     = ref(false)
const showAddComponent    = ref(false)  // stationId → null means station-level, else machineId
const componentTargetId   = ref(null)   // null = station-level, number = machineId
const componentTargetLabel = ref('')

const machineForm  = ref(emptyMachine())
const editMachineForm = ref({})
const copyForm     = ref({ newMachineCode: '', newName: '' })
const copySourceId = ref(null)
const componentForm = ref(emptyComponent())

// Confirm
const confirmDeact = ref(null)  // { type: 'machine'|'component', id, name }

function emptyMachine() {
  return { machineCode: '', name: '', model: '' }
}
function emptyComponent() {
  return { componentCode: '', name: '', dataType: 'TELEMETRY', unit: '', reportIntervalSec: 60, normalUpper: '', normalLower: '' }
}

const DATA_TYPES = ['TELEMETRY', 'EVENT']

// ── API ──────────────────────────────────────────────────────
async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await api.getStationDetail(stationId)
  } catch (e) {
    ui.showToast(e.message)
    if (e.code === 'IC00001') router.push('/process-config')
  } finally {
    loading.value = false
  }
}

// Machine actions
async function handleAddMachine() {
  if (!machineForm.value.machineCode || !machineForm.value.name) {
    ui.showToast('請填寫機台代碼與名稱')
    return
  }
  ui.loading = true
  try {
    await api.createMachine(stationId, {
      machineCode: machineForm.value.machineCode,
      name:        machineForm.value.name,
      model:       machineForm.value.model || null,
    })
    ui.showToast('機台已新增')
    showAddMachine.value = false
    machineForm.value = emptyMachine()
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function openEditMachine(m) {
  editMachineForm.value = { id: m.id, name: m.name, model: m.model || '' }
  showEditMachine.value = true
}

async function handleEditMachine() {
  if (!editMachineForm.value.name) {
    ui.showToast('請填寫名稱')
    return
  }
  ui.loading = true
  try {
    await api.updateMachine(editMachineForm.value.id, {
      name:  editMachineForm.value.name,
      model: editMachineForm.value.model || null,
    })
    ui.showToast('機台已更新')
    showEditMachine.value = false
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function openCopy(m) {
  copySourceId.value = m.id
  copyForm.value = { newMachineCode: '', newName: '' }
  showCopyMachine.value = true
}

async function handleCopy() {
  if (!copyForm.value.newMachineCode || !copyForm.value.newName) {
    ui.showToast('請填寫新機台代碼與名稱')
    return
  }
  ui.loading = true
  try {
    await api.copyMachine(copySourceId.value, {
      newMachineCode: copyForm.value.newMachineCode,
      newName:        copyForm.value.newName,
    })
    ui.showToast('機台已複製（含元件）')
    showCopyMachine.value = false
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

// Component actions
function openAddComponent(machineId, label) {
  componentTargetId.value = machineId
  componentTargetLabel.value = label
  componentForm.value = emptyComponent()
  showAddComponent.value = true
}

async function handleAddComponent() {
  const f = componentForm.value
  if (!f.componentCode || !f.name || !f.dataType) {
    ui.showToast('請填寫元件代碼、名稱與資料類型')
    return
  }
  if (f.dataType === 'TELEMETRY' && !f.unit) {
    ui.showToast('TELEMETRY 類型需填寫單位')
    return
  }
  ui.loading = true
  try {
    const body = {
      componentCode:    f.componentCode,
      name:             f.name,
      dataType:         f.dataType,
      unit:             f.unit || null,
      reportIntervalSec: f.reportIntervalSec ? Number(f.reportIntervalSec) : null,
      normalUpper:      f.normalUpper !== '' ? Number(f.normalUpper) : null,
      normalLower:      f.normalLower !== '' ? Number(f.normalLower) : null,
    }
    if (componentTargetId.value === null) {
      await api.createStationComponent(stationId, body)
    } else {
      await api.createMachineComponent(componentTargetId.value, body)
    }
    ui.showToast('元件已新增')
    showAddComponent.value = false
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

async function handleDeactivateComponent(c) {
  ui.loading = true
  try {
    await api.deactivateComponent(c.id)
    ui.showToast('元件已停用')
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

async function handleActivateComponent(c) {
  ui.loading = true
  try {
    await api.activateComponent(c.id)
    ui.showToast('元件已啟用')
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

async function handleDeactivateMachine(m) {
  ui.loading = true
  try {
    const r = await api.deactivateMachine(m.id)
    ui.showToast(`機台已停用，共停用 ${r.deactivatedComponents} 個元件`)
    confirmDeact.value = null
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
    confirmDeact.value = null
  } finally {
    ui.loading = false
  }
}

async function handleActivateMachine(m) {
  ui.loading = true
  try {
    await api.activateMachine(m.id)
    ui.showToast('機台已啟用')
    await fetchDetail()
  } catch (e) {
    ui.showToast(e.message)
  } finally {
    ui.loading = false
  }
}

function toggleMachine(id) {
  if (expandedMachines.value.has(id)) {
    expandedMachines.value.delete(id)
  } else {
    expandedMachines.value.add(id)
  }
}

function dataTypeLabel(dt) {
  return { TELEMETRY: '遙測', EVENT: '事件' }[dt] || dt
}

onMounted(fetchDetail)
</script>

<template>
  <div class="layout">
    <Sidebar />
    <main class="main">
      <div class="breadcrumb">
        <span class="breadcrumb-link" @click="router.push('/process-config')">製程配置</span>
        <span class="breadcrumb-sep">›</span>
        <span>{{ detail?.station?.stationCode || '...' }}</span>
      </div>

      <div v-if="loading" class="loading-text">載入中...</div>

      <template v-else-if="detail">
        <!-- Station Info -->
        <div class="section-card">
          <div class="section-header">
            <div>
              <span class="station-code-badge">{{ detail.station.stationCode }}</span>
              <span class="station-name-text">{{ detail.station.name }}</span>
              <span class="badge" :class="detail.station.active ? 'badge-active' : 'badge-inactive'">
                {{ detail.station.active ? '啟用' : '停用' }}
              </span>
            </div>
          </div>
          <div v-if="detail.station.description" class="station-desc">{{ detail.station.description }}</div>
        </div>

        <!-- Station-Level Components -->
        <div class="section-card">
          <div class="section-title-row">
            <h2 class="section-title">站點元件</h2>
            <button v-if="canWrite" class="btn-sm btn-add"
              @click="openAddComponent(null, `站點「${detail.station.name}」`)">
              + 新增元件
            </button>
          </div>
          <div v-if="!detail.stationComponents?.length" class="empty-inline">尚無站點元件</div>
          <table v-else class="comp-table">
            <thead>
              <tr><th>代碼</th><th>名稱</th><th>類型</th><th>單位</th><th>頻率(s)</th><th>狀態</th><th v-if="canWrite">操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="c in detail.stationComponents" :key="c.id" :class="{ inactive: !c.active }">
                <td class="mono">{{ c.componentCode }}</td>
                <td>{{ c.name }}</td>
                <td><span class="type-badge">{{ dataTypeLabel(c.dataType) }}</span></td>
                <td>{{ c.unit || '—' }}</td>
                <td>{{ c.reportIntervalSec ?? '—' }}</td>
                <td><span class="badge" :class="c.active ? 'badge-active' : 'badge-inactive'">{{ c.active ? '啟用' : '停用' }}</span></td>
                <td v-if="canWrite">
                  <button v-if="c.active"  class="btn-sm btn-danger"  @click="handleDeactivateComponent(c)">停用</button>
                  <button v-if="!c.active" class="btn-sm btn-success" @click="handleActivateComponent(c)">啟用</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Machines -->
        <div class="section-card">
          <div class="section-title-row">
            <h2 class="section-title">機台</h2>
            <button v-if="canWrite" class="btn-sm btn-add" @click="showAddMachine = true">+ 新增機台</button>
          </div>
          <div v-if="!detail.machines?.length" class="empty-inline">尚無機台</div>

          <div v-for="m in detail.machines" :key="m.id" class="machine-block" :class="{ inactive: !m.active }">
            <div class="machine-header" @click="toggleMachine(m.id)">
              <span class="expand-icon">{{ expandedMachines.has(m.id) ? '▾' : '▸' }}</span>
              <span class="machine-code">{{ m.machineCode }}</span>
              <span class="machine-name">{{ m.name }}</span>
              <span v-if="m.model" class="machine-model">({{ m.model }})</span>
              <span class="badge" :class="m.active ? 'badge-active' : 'badge-inactive'">
                {{ m.active ? '啟用' : '停用' }}
              </span>
              <div class="machine-actions" @click.stop>
                <button v-if="canWrite" class="btn-sm" @click="openEditMachine(m)">編輯</button>
                <button v-if="canWrite" class="btn-sm" @click="openCopy(m)">複製</button>
                <button v-if="canWrite && m.active"  class="btn-sm btn-danger"  @click="confirmDeact = { type: 'machine', target: m }">停用</button>
                <button v-if="canWrite && !m.active" class="btn-sm btn-success" @click="handleActivateMachine(m)">啟用</button>
              </div>
            </div>

            <div v-if="expandedMachines.has(m.id)" class="machine-body">
              <div class="machine-comp-header">
                <span class="comp-count">{{ m.components?.length || 0 }} 個元件</span>
                <button v-if="canWrite" class="btn-sm btn-add"
                  @click="openAddComponent(m.id, `機台「${m.name}」`)">
                  + 新增元件
                </button>
              </div>
              <div v-if="!m.components?.length" class="empty-inline">尚無元件</div>
              <table v-else class="comp-table">
                <thead>
                  <tr><th>代碼</th><th>名稱</th><th>類型</th><th>單位</th><th>頻率(s)</th><th>上限</th><th>下限</th><th>狀態</th><th v-if="canWrite">操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="c in m.components" :key="c.id" :class="{ inactive: !c.active }">
                    <td class="mono">{{ c.componentCode }}</td>
                    <td>{{ c.name }}</td>
                    <td><span class="type-badge">{{ dataTypeLabel(c.dataType) }}</span></td>
                    <td>{{ c.unit || '—' }}</td>
                    <td>{{ c.reportIntervalSec ?? '—' }}</td>
                    <td>{{ c.normalUpper ?? '—' }}</td>
                    <td>{{ c.normalLower ?? '—' }}</td>
                    <td><span class="badge" :class="c.active ? 'badge-active' : 'badge-inactive'">{{ c.active ? '啟用' : '停用' }}</span></td>
                    <td v-if="canWrite">
                      <button v-if="c.active"  class="btn-sm btn-danger"  @click="handleDeactivateComponent(c)">停用</button>
                      <button v-if="!c.active" class="btn-sm btn-success" @click="handleActivateComponent(c)">啟用</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
    </main>
  </div>

  <!-- Add Machine Modal -->
  <div v-if="showAddMachine" class="modal-overlay" @click.self="showAddMachine = false">
    <div class="modal">
      <div class="modal-header">
        <h2>新增機台</h2>
        <button class="modal-close" @click="showAddMachine = false">✕</button>
      </div>
      <div class="modal-body">
        <label class="form-label">機台代碼 *</label>
        <input v-model="machineForm.machineCode" class="form-input" placeholder="e.g. M-001" />
        <label class="form-label">名稱 *</label>
        <input v-model="machineForm.name" class="form-input" placeholder="機台顯示名稱" />
        <label class="form-label">型號</label>
        <input v-model="machineForm.model" class="form-input" placeholder="選填" />
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="showAddMachine = false">取消</button>
        <button class="btn-primary" @click="handleAddMachine">建立</button>
      </div>
    </div>
  </div>

  <!-- Edit Machine Modal -->
  <div v-if="showEditMachine" class="modal-overlay" @click.self="showEditMachine = false">
    <div class="modal">
      <div class="modal-header">
        <h2>編輯機台</h2>
        <button class="modal-close" @click="showEditMachine = false">✕</button>
      </div>
      <div class="modal-body">
        <label class="form-label">名稱 *</label>
        <input v-model="editMachineForm.name" class="form-input" />
        <label class="form-label">型號</label>
        <input v-model="editMachineForm.model" class="form-input" />
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="showEditMachine = false">取消</button>
        <button class="btn-primary" @click="handleEditMachine">儲存</button>
      </div>
    </div>
  </div>

  <!-- Copy Machine Modal -->
  <div v-if="showCopyMachine" class="modal-overlay" @click.self="showCopyMachine = false">
    <div class="modal">
      <div class="modal-header">
        <h2>複製機台</h2>
        <button class="modal-close" @click="showCopyMachine = false">✕</button>
      </div>
      <div class="modal-body">
        <p class="form-hint">複製時會同步複製所有啟用中的元件</p>
        <label class="form-label">新機台代碼 *</label>
        <input v-model="copyForm.newMachineCode" class="form-input" placeholder="e.g. M-002" />
        <label class="form-label">新機台名稱 *</label>
        <input v-model="copyForm.newName" class="form-input" placeholder="新機台顯示名稱" />
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="showCopyMachine = false">取消</button>
        <button class="btn-primary" @click="handleCopy">複製</button>
      </div>
    </div>
  </div>

  <!-- Add Component Modal -->
  <div v-if="showAddComponent" class="modal-overlay" @click.self="showAddComponent = false">
    <div class="modal modal-wide">
      <div class="modal-header">
        <h2>新增元件 — {{ componentTargetLabel }}</h2>
        <button class="modal-close" @click="showAddComponent = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="form-row">
          <div>
            <label class="form-label">元件代碼 *</label>
            <input v-model="componentForm.componentCode" class="form-input" placeholder="e.g. TEMP-01" />
          </div>
          <div>
            <label class="form-label">名稱 *</label>
            <input v-model="componentForm.name" class="form-input" placeholder="顯示名稱" />
          </div>
        </div>
        <div class="form-row">
          <div>
            <label class="form-label">資料類型 *</label>
            <select v-model="componentForm.dataType" class="form-input">
              <option v-for="dt in DATA_TYPES" :key="dt" :value="dt">{{ dataTypeLabel(dt) }}（{{ dt }}）</option>
            </select>
          </div>
          <div>
            <label class="form-label">單位{{ componentForm.dataType === 'TELEMETRY' ? ' *' : '' }}</label>
            <input v-model="componentForm.unit" class="form-input" placeholder="e.g. °C" />
          </div>
        </div>
        <div class="form-row">
          <div>
            <label class="form-label">上報頻率(秒)</label>
            <input v-model.number="componentForm.reportIntervalSec" type="number" class="form-input" />
          </div>
          <div>
            <label class="form-label">正常上限</label>
            <input v-model="componentForm.normalUpper" type="number" class="form-input" />
          </div>
          <div>
            <label class="form-label">正常下限</label>
            <input v-model="componentForm.normalLower" type="number" class="form-input" />
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="showAddComponent = false">取消</button>
        <button class="btn-primary" @click="handleAddComponent">建立</button>
      </div>
    </div>
  </div>

  <!-- Confirm Deactivate Machine -->
  <ConfirmDialog
    v-if="confirmDeact?.type === 'machine'"
    :message="`確定停用機台「${confirmDeact.target.name}」？此操作將連帶停用該機台所有元件。`"
    @confirm="handleDeactivateMachine(confirmDeact.target)"
    @cancel="confirmDeact = null"
  />
</template>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.main   { flex: 1; padding: 2rem; background: var(--bg); }

.breadcrumb { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 1.5rem;
              font-size: 0.9rem; color: var(--text-secondary); }
.breadcrumb-link { cursor: pointer; color: var(--accent); }
.breadcrumb-link:hover { text-decoration: underline; }
.breadcrumb-sep  { color: var(--text-muted, #888); }

.section-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px;
                padding: 1.25rem 1.5rem; margin-bottom: 1rem; }

.section-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
.station-code-badge { font-family: monospace; font-size: 0.85rem; color: var(--text-secondary); margin-right: 0.25rem; }
.station-name-text  { font-size: 1.2rem; font-weight: 700; color: var(--text-primary); margin-right: 0.5rem; }
.station-desc       { font-size: 0.875rem; color: var(--text-secondary); margin-top: 0.25rem; }

.section-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.section-title     { font-size: 1rem; font-weight: 700; color: var(--text-primary); }

.machine-block  { border: 1px solid var(--border); border-radius: 6px; margin-bottom: 0.75rem; overflow: hidden; }
.machine-block.inactive { opacity: 0.65; }
.machine-header { display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem 1rem;
                  background: var(--bg); cursor: pointer; user-select: none; }
.machine-header:hover { background: color-mix(in srgb, var(--bg), #000 5%); }
.expand-icon  { font-size: 0.9rem; width: 1rem; text-align: center; flex-shrink: 0; }
.machine-code { font-family: monospace; font-size: 0.8rem; color: var(--text-secondary); }
.machine-name { font-weight: 600; color: var(--text-primary); }
.machine-model{ font-size: 0.8rem; color: var(--text-secondary); }
.machine-actions { margin-left: auto; display: flex; gap: 0.4rem; }

.machine-body { padding: 0.75rem 1rem; border-top: 1px solid var(--border); background: var(--card-bg); }
.machine-comp-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.comp-count { font-size: 0.8rem; color: var(--text-secondary); }

.comp-table  { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
.comp-table th { text-align: left; padding: 0.4rem 0.6rem; border-bottom: 2px solid var(--border);
                  color: var(--text-secondary); font-weight: 600; }
.comp-table td { padding: 0.4rem 0.6rem; border-bottom: 1px solid var(--border); color: var(--text-primary); }
.comp-table tr.inactive td { opacity: 0.6; }
.mono { font-family: monospace; }

.type-badge { padding: 0.1rem 0.4rem; background: var(--bg); border-radius: 3px; font-size: 0.75rem;
              border: 1px solid var(--border); }

.badge         { display: inline-block; padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.75rem; }
.badge-active  { background: #d4edda; color: #155724; }
.badge-inactive{ background: #f8d7da; color: #721c24; }

.loading-text { padding: 2rem; text-align: center; color: var(--text-secondary); }
.empty-inline { padding: 1rem; text-align: center; color: var(--text-secondary); font-size: 0.875rem; }

.btn-primary   { padding: 0.5rem 1rem; border-radius: 6px; border: none; cursor: pointer;
                  background: var(--accent); color: #fff; font-size: 0.9rem; }
.btn-secondary { padding: 0.5rem 1rem; border-radius: 6px; border: 1px solid var(--border); cursor: pointer;
                  background: transparent; color: var(--text-primary); font-size: 0.9rem; }
.btn-sm        { padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid var(--border);
                  cursor: pointer; background: var(--card-bg); color: var(--text-primary); font-size: 0.78rem; }
.btn-add       { border-color: var(--accent); color: var(--accent); }
.btn-danger    { border-color: #dc3545; color: #dc3545; }
.btn-success   { border-color: #28a745; color: #28a745; }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); display: flex;
                  align-items: center; justify-content: center; z-index: 100; }
.modal         { background: var(--card-bg); border-radius: 10px; width: 440px; max-width: 95vw;
                  box-shadow: 0 8px 32px rgba(0,0,0,.2); }
.modal-wide    { width: 600px; }
.modal-header  { display: flex; justify-content: space-between; align-items: center;
                  padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--border); }
.modal-header h2 { font-size: 1.05rem; font-weight: 700; color: var(--text-primary); }
.modal-close   { background: none; border: none; cursor: pointer; font-size: 1.1rem; color: var(--text-secondary); }
.modal-body    { padding: 1.25rem 1.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
.modal-footer  { display: flex; justify-content: flex-end; gap: 0.75rem;
                  padding: 1rem 1.5rem; border-top: 1px solid var(--border); }

.form-row  { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 0.75rem; }
.form-label{ font-size: 0.85rem; font-weight: 600; color: var(--text-secondary); }
.form-hint { font-size: 0.82rem; color: var(--text-secondary); margin: 0; }
.form-input{ padding: 0.5rem 0.75rem; border: 1px solid var(--border); border-radius: 6px;
              background: var(--bg); color: var(--text-primary); font-size: 0.9rem; width: 100%;
              box-sizing: border-box; }
.form-input:focus { outline: none; border-color: var(--accent); }
</style>
