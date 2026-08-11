// ===== App Shell Init =====
// Called by every page (except login) after DOM is ready

function initApp() {
  requireAuth();

  const user = getSession();

  // Render user info in sidebar footer
  const avatarEl  = document.getElementById('userAvatar');
  const nameEl    = document.getElementById('userName');
  const roleEl    = document.getElementById('userRole');
  if (avatarEl) avatarEl.textContent = user.name[0];
  if (nameEl)   nameEl.textContent   = user.name;
  if (roleEl)   roleEl.textContent   = user.role;

  // Logout button
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      clearSession();
      window.location.href = 'login.html';
    });
  }

  // Highlight active nav item
  const currentPage = location.pathname.split('/').pop();
  document.querySelectorAll('.nav-item[data-page]').forEach(item => {
    if (item.dataset.page === currentPage) item.classList.add('active');
  });

  // Hide Admin-only elements for non-Admin users
  if (user.role !== 'Admin') {
    document.querySelectorAll('[data-admin-only]').forEach(el => el.remove());
  }

  // Disable write actions for Viewer
  if (user.role === 'Viewer') {
    document.querySelectorAll('[data-write-action]').forEach(el => {
      el.disabled = true;
      el.title = '觀察者無法執行此操作';
      el.style.opacity = '.4';
      el.style.pointerEvents = 'none';
    });
  }
}

// ===== Sidebar HTML =====
function renderSidebar() {
  return `
<aside class="sidebar">
  <div class="sidebar-logo">
    <div class="brand">willThx</div>
    <div class="tagline">IoT 製造監控平台</div>
  </div>

  <nav class="sidebar-nav">
    <a class="nav-item" data-page="dashboard.html" href="dashboard.html">
      <span class="nav-icon">⬡</span> Dashboard
    </a>

    <div class="nav-section-label">製程配置</div>
    <a class="nav-item" data-page="stations.html" href="stations.html">
      <span class="nav-icon">⊞</span> 站點管理
    </a>
    <a class="nav-item" data-page="machines.html" href="machines.html">
      <span class="nav-icon">⚙</span> 機台管理
    </a>
    <a class="nav-item" data-page="iot-components.html" href="iot-components.html">
      <span class="nav-icon">◈</span> IoT 元件管理
    </a>

    <div class="nav-section-label">告警中心</div>
    <a class="nav-item" data-page="alerts-live.html" href="alerts-live.html">
      <span class="nav-icon">◉</span> 即時告警
    </a>
    <a class="nav-item" data-page="alerts-history.html" href="alerts-history.html">
      <span class="nav-icon">☰</span> 歷史告警
    </a>
    <a class="nav-item" data-page="alert-rules.html" href="alert-rules.html" data-admin-only>
      <span class="nav-icon">⚑</span> 告警規則設定
    </a>

    <div class="nav-section-label">資料查詢</div>
    <a class="nav-item" data-page="traceability.html" href="traceability.html">
      <span class="nav-icon">⦿</span> 板件追溯
    </a>
    <a class="nav-item" data-page="lot-query.html" href="lot-query.html">
      <span class="nav-icon">▤</span> 批號查詢
    </a>

    <div class="nav-section-label" data-admin-only>系統設定</div>
    <a class="nav-item" data-page="users.html" href="users.html" data-admin-only>
      <span class="nav-icon">⊙</span> 使用者管理
    </a>
    <a class="nav-item" data-page="notifications.html" href="notifications.html" data-admin-only>
      <span class="nav-icon">⊛</span> 通知設定
    </a>
  </nav>

  <div class="sidebar-footer">
    <div class="user-info">
      <div class="user-avatar" id="userAvatar"></div>
      <div>
        <div class="user-name" id="userName"></div>
        <div class="user-role" id="userRole"></div>
      </div>
      <button class="btn-logout" id="logoutBtn">登出</button>
    </div>
  </div>
</aside>`;
}

// ===== Mini Sparkline (canvas) =====
function drawSparkline(canvas, data, color = '#2563eb') {
  const ctx = canvas.getContext('2d');
  const W = canvas.width, H = canvas.height;
  const min = Math.min(...data), max = Math.max(...data);
  const range = max - min || 1;

  ctx.clearRect(0, 0, W, H);

  // Fill gradient
  const grad = ctx.createLinearGradient(0, 0, 0, H);
  grad.addColorStop(0, color + '33');
  grad.addColorStop(1, color + '00');

  ctx.beginPath();
  data.forEach((v, i) => {
    const x = (i / (data.length - 1)) * W;
    const y = H - ((v - min) / range) * (H - 8) - 4;
    i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
  });
  ctx.lineTo(W, H); ctx.lineTo(0, H); ctx.closePath();
  ctx.fillStyle = grad; ctx.fill();

  // Line
  ctx.beginPath();
  data.forEach((v, i) => {
    const x = (i / (data.length - 1)) * W;
    const y = H - ((v - min) / range) * (H - 8) - 4;
    i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
  });
  ctx.strokeStyle = color; ctx.lineWidth = 2;
  ctx.lineJoin = 'round'; ctx.stroke();
}

// ===== Simple Line Chart (canvas) =====
function drawLineChart(canvas, datasets, labels) {
  const ctx = canvas.getContext('2d');
  const W = canvas.width, H = canvas.height;
  const PAD = { top: 16, right: 16, bottom: 32, left: 48 };
  const cW = W - PAD.left - PAD.right;
  const cH = H - PAD.top - PAD.bottom;

  ctx.clearRect(0, 0, W, H);

  // Grid
  ctx.strokeStyle = '#e2e8f0'; ctx.lineWidth = 1;
  for (let i = 0; i <= 4; i++) {
    const y = PAD.top + (cH / 4) * i;
    ctx.beginPath(); ctx.moveTo(PAD.left, y); ctx.lineTo(W - PAD.right, y); ctx.stroke();
  }

  const allVals = datasets.flatMap(d => d.data);
  const min = Math.min(...allVals) * 0.95;
  const max = Math.max(...allVals) * 1.05;
  const range = max - min || 1;

  // Y labels
  ctx.fillStyle = '#94a3b8'; ctx.font = '11px system-ui'; ctx.textAlign = 'right';
  for (let i = 0; i <= 4; i++) {
    const val = max - (range / 4) * i;
    ctx.fillText(val.toFixed(1), PAD.left - 6, PAD.top + (cH / 4) * i + 4);
  }

  // X labels
  ctx.textAlign = 'center';
  if (labels) {
    const step = Math.ceil(labels.length / 6);
    labels.forEach((l, i) => {
      if (i % step === 0) {
        const x = PAD.left + (i / (labels.length - 1)) * cW;
        ctx.fillText(l, x, H - 8);
      }
    });
  }

  // Lines
  datasets.forEach(({ data, color }) => {
    ctx.beginPath();
    data.forEach((v, i) => {
      const x = PAD.left + (i / (data.length - 1)) * cW;
      const y = PAD.top + cH - ((v - min) / range) * cH;
      i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
    });
    ctx.strokeStyle = color; ctx.lineWidth = 2;
    ctx.lineJoin = 'round'; ctx.stroke();
  });
}

// ===== Modal Helpers =====
function openModal(title, bodyHTML, onSubmit, size = '') {
  const overlay = document.getElementById('modalOverlay');
  const modal   = overlay.querySelector('.modal');
  modal.className = 'modal' + (size ? ' modal-' + size : '');
  document.getElementById('modalTitle').textContent = title;
  document.getElementById('modalBody').innerHTML = bodyHTML;
  const submitBtn = document.getElementById('modalSubmit');
  if (onSubmit) { submitBtn.style.display = ''; submitBtn.onclick = onSubmit; }
  else { submitBtn.style.display = 'none'; }
  overlay.classList.add('open');
}

function closeModal() {
  document.getElementById('modalOverlay').classList.remove('open');
}

// Shared modal HTML — paste once per page
const MODAL_HTML = `
<div class="modal-overlay" id="modalOverlay" onclick="closeModal()">
  <div class="modal" onclick="event.stopPropagation()">
    <div class="modal-header">
      <div class="modal-title" id="modalTitle"></div>
      <div class="modal-close" onclick="closeModal()">✕</div>
    </div>
    <div class="modal-body" id="modalBody"></div>
    <div class="modal-footer">
      <button class="btn btn-ghost" onclick="closeModal()">取消</button>
      <button class="btn btn-primary" id="modalSubmit">儲存</button>
    </div>
  </div>
</div>`;

// ===== Shared Mock Data =====
const MOCK_STATIONS = [
  { id:'S01', name:'錫膏印刷', order:1, machineCount:2, iotCount:6, status:'running' },
  { id:'S02', name:'AOI 檢測', order:2, machineCount:2, iotCount:4, status:'running' },
  { id:'S03', name:'回流焊',   order:3, machineCount:2, iotCount:8, status:'error'   },
  { id:'S04', name:'最終組裝', order:4, machineCount:2, iotCount:4, status:'running' },
  { id:'S05', name:'廠務監控', order:5, machineCount:0, iotCount:3, status:'running' },
];

const MOCK_MACHINES = {
  S01: [
    { id:'M01', name:'印刷機-01', model:'DEK Horizon', status:'running', iotCount:3 },
    { id:'M02', name:'印刷機-02', model:'DEK Horizon', status:'running', iotCount:3 },
  ],
  S02: [
    { id:'M03', name:'AOI機-01', model:'Koh Young KY8030', status:'running', iotCount:2 },
    { id:'M04', name:'AOI機-02', model:'Koh Young KY8030', status:'running', iotCount:2 },
  ],
  S03: [
    { id:'M05', name:'回焊爐-01', model:'Heller 1800EXL', status:'error',   iotCount:4 },
    { id:'M06', name:'回焊爐-02', model:'Heller 1800EXL', status:'running', iotCount:4 },
  ],
  S04: [
    { id:'M07', name:'組裝台-01', model:'Manual', status:'running', iotCount:2 },
    { id:'M08', name:'組裝台-02', model:'Manual', status:'running', iotCount:2 },
  ],
  S05: [],
};

const MOCK_COMPONENTS = [
  { id:'C01', machineId:'M01', stationId:'S01', name:'刮刀壓力感測器', code:'SP-PRESSURE', type:'Telemetry', unit:'kgf',   freq:1,  min:1.5, max:3.0 },
  { id:'C02', machineId:'M01', stationId:'S01', name:'印刷厚度感測器', code:'SP-THICK',    type:'Telemetry', unit:'mm',    freq:1,  min:0.10, max:0.20 },
  { id:'C03', machineId:'M01', stationId:'S01', name:'刮刀速度感測器', code:'SP-SPEED',    type:'Telemetry', unit:'mm/s',  freq:1,  min:30, max:80 },
  { id:'C04', machineId:'M02', stationId:'S01', name:'刮刀壓力感測器', code:'SP-PRESSURE', type:'Telemetry', unit:'kgf',   freq:1,  min:1.5, max:3.0 },
  { id:'C05', machineId:'M02', stationId:'S01', name:'印刷厚度感測器', code:'SP-THICK',    type:'Telemetry', unit:'mm',    freq:1,  min:0.10, max:0.20 },
  { id:'C06', machineId:'M02', stationId:'S01', name:'刮刀速度感測器', code:'SP-SPEED',    type:'Telemetry', unit:'mm/s',  freq:1,  min:30, max:80 },
  { id:'C07', machineId:'M03', stationId:'S02', name:'AOI 判定結果',   code:'AOI-RESULT',  type:'Event',     unit:'-',    freq:0,  min:null, max:null },
  { id:'C08', machineId:'M03', stationId:'S02', name:'缺陷碼',         code:'AOI-DEFECT',  type:'Event',     unit:'-',    freq:0,  min:null, max:null },
  { id:'C09', machineId:'M04', stationId:'S02', name:'AOI 判定結果',   code:'AOI-RESULT',  type:'Event',     unit:'-',    freq:0,  min:null, max:null },
  { id:'C10', machineId:'M04', stationId:'S02', name:'缺陷碼',         code:'AOI-DEFECT',  type:'Event',     unit:'-',    freq:0,  min:null, max:null },
  { id:'C11', machineId:'M05', stationId:'S03', name:'預熱區溫度',     code:'RF-TEMP-PH',  type:'Telemetry', unit:'°C',   freq:2,  min:120, max:180 },
  { id:'C12', machineId:'M05', stationId:'S03', name:'活化區溫度',     code:'RF-TEMP-AK',  type:'Telemetry', unit:'°C',   freq:2,  min:150, max:200 },
  { id:'C13', machineId:'M05', stationId:'S03', name:'回流區溫度',     code:'RF-TEMP-RF',  type:'Telemetry', unit:'°C',   freq:2,  min:220, max:260 },
  { id:'C14', machineId:'M05', stationId:'S03', name:'冷卻區溫度',     code:'RF-TEMP-CL',  type:'Telemetry', unit:'°C',   freq:2,  min:50, max:100 },
  { id:'C15', machineId:'M07', stationId:'S04', name:'鎖點扭力',       code:'FA-TORQUE',   type:'Telemetry', unit:'N·m',  freq:1,  min:0.8, max:1.6 },
  { id:'C16', machineId:'M07', stationId:'S04', name:'功能測試結果',   code:'FA-TEST',     type:'Event',     unit:'-',    freq:0,  min:null, max:null },
  { id:'C17', machineId:null,  stationId:'S05', name:'站區溫度',       code:'ENV-TEMP',    type:'Telemetry', unit:'°C',   freq:10, min:18, max:28 },
  { id:'C18', machineId:null,  stationId:'S05', name:'站區濕度',       code:'ENV-HUM',     type:'Telemetry', unit:'%',    freq:10, min:30, max:65 },
  { id:'C19', machineId:null,  stationId:'S05', name:'電表功耗',       code:'ENV-POWER',   type:'Telemetry', unit:'kW',   freq:30, min:0, max:50 },
];

const MOCK_USERS = [
  { id:'U01', username:'admin',     name:'王管理員', role:'Admin',    lastLogin:'2026-08-07 09:12', active:true },
  { id:'U02', username:'operator',  name:'李操作員', role:'Operator', lastLogin:'2026-08-07 08:30', active:true },
  { id:'U03', username:'operator2', name:'陳操作員', role:'Operator', lastLogin:'2026-08-06 17:45', active:true },
  { id:'U04', username:'viewer',    name:'張觀察者', role:'Viewer',   lastLogin:'2026-08-07 10:01', active:true },
];

const MOCK_RULES = [
  { id:'R01', component:'回流區溫度 (回焊爐-01)', condition:'> 260', unit:'°C', sev:'high', notify:'dashboard,telegram', active:true },
  { id:'R02', component:'印刷厚度感測器 (印刷機-01)', condition:'< 0.10', unit:'mm', sev:'high', notify:'dashboard,telegram', active:true },
  { id:'R03', component:'站區濕度 (廠務監控)', condition:'> 65', unit:'%', sev:'low', notify:'dashboard', active:true },
  { id:'R04', component:'刮刀壓力感測器 (印刷機-02)', condition:'> 3.0', unit:'kgf', sev:'low', notify:'dashboard', active:false },
];

// ===== Mock Data Generator =====
function randomWalk(base, noise, length) {
  const arr = [base];
  for (let i = 1; i < length; i++) {
    arr.push(Math.max(0, arr[i-1] + (Math.random() - 0.5) * noise));
  }
  return arr;
}

function timeLabels(n, intervalSec = 10) {
  const now = new Date();
  return Array.from({ length: n }, (_, i) => {
    const t = new Date(now - (n - 1 - i) * intervalSec * 1000);
    return t.getHours().toString().padStart(2,'0') + ':' + t.getMinutes().toString().padStart(2,'0');
  });
}
