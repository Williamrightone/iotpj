// ===== App Shell Init =====
function initApp() {
  requireAuth();

  const user = getSession();

  const avatarEl = document.getElementById('userAvatar');
  const nameEl   = document.getElementById('userName');
  const roleEl   = document.getElementById('userRole');
  if (avatarEl) avatarEl.textContent = user.name[0];
  if (nameEl)   nameEl.textContent   = user.name;
  if (roleEl)   roleEl.textContent   = user.role;

  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      clearSession();
      window.location.href = 'login.html';
    });
  }

  const currentPage = location.pathname.split('/').pop();
  document.querySelectorAll('.nav-item[data-page]').forEach(item => {
    if (item.dataset.page === currentPage) item.classList.add('active');
  });

  if (user.role !== 'Admin') {
    document.querySelectorAll('[data-admin-only]').forEach(el => el.remove());
  }

  if (user.role === 'Viewer') {
    document.querySelectorAll('[data-write-action]').forEach(el => {
      el.disabled = true;
      el.title = '觀察者無法執行此操作';
      el.style.opacity = '.4';
      el.style.pointerEvents = 'none';
    });
  }
}

// ===== Feature Tree =====
const MOCK_FEATURES = [
  { id:'F00', name:'製程配置',     code:'process-config',    parentId:null,  route:null,                      icon:'🏭', sort:0 },
  { id:'F00A',name:'製程配置管理', code:'process-config-mgmt',parentId:'F00',route:'process-config.html',     icon:'⊞', sort:1 },
  { id:'F01', name:'系統設定',     code:'sys-settings',      parentId:null,  route:null,                      icon:'⚙',  sort:1 },
  { id:'F02', name:'使用者管理',   code:'user-mgmt',          parentId:'F01', route:'users.html',              icon:'⊙',  sort:1 },
  { id:'F03', name:'功能管理',     code:'feature-mgmt',       parentId:'F01', route:'features.html',           icon:'⊞',  sort:2 },
  { id:'F04', name:'角色權限設定', code:'role-permissions',   parentId:'F01', route:'role-permissions.html',   icon:'⚑',  sort:3 },
];

// Maintainer / Viewer 可存取的子功能 code 清單（Admin 自動擁有全部）
const MOCK_ROLE_PERMISSIONS = {
  Maintainer: ['process-config-mgmt'],
  Viewer:     ['process-config-mgmt'],
};

function getAccessibleLeaves(role) {
  if (role === 'Admin') {
    return MOCK_FEATURES.filter(f => f.parentId !== null);
  }
  const allowed = new Set(MOCK_ROLE_PERMISSIONS[role] || []);
  return MOCK_FEATURES.filter(f => f.parentId !== null && allowed.has(f.code));
}

// ===== Sidebar HTML (dynamic) =====
function renderSidebar() {
  const user = getSession();
  const role = user ? user.role : 'Viewer';

  const leaves = getAccessibleLeaves(role);
  const visibleParentIds = new Set(leaves.map(f => f.parentId));

  const parents = MOCK_FEATURES
    .filter(f => f.parentId === null && visibleParentIds.has(f.id))
    .sort((a, b) => a.sort - b.sort);

  let navHtml = '';
  parents.forEach(parent => {
    navHtml += `<div class="nav-section-label">${parent.name}</div>`;
    leaves
      .filter(f => f.parentId === parent.id)
      .sort((a, b) => a.sort - b.sort)
      .forEach(child => {
        navHtml += `<a class="nav-item" data-page="${child.route}" href="${child.route}">
          <span class="nav-icon">${child.icon}</span> ${child.name}
        </a>`;
      });
  });

  return `
<aside class="sidebar">
  <div class="sidebar-logo">
    <div class="brand">willThx</div>
    <div class="tagline">IoT 製造監控平台</div>
  </div>
  <nav class="sidebar-nav">
    ${navHtml}
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

// ===== Mock Data =====
const MOCK_USERS = [
  { id:'U01', username:'admin',      name:'王管理員', role:'Admin',      stations:[], lastLogin:'2026-08-11 09:12', active:true },
  { id:'U02', username:'maintainer', name:'李維護員', role:'Maintainer', stations:[], lastLogin:'2026-08-11 08:30', active:true },
  { id:'U03', username:'viewer',     name:'張觀察者', role:'Viewer',     stations:[], lastLogin:'2026-08-11 10:01', active:true },
];

// 製程站點
const MOCK_STATIONS = [
  { id:1, code:'S01', name:'錫膏印刷',       sortOrder:1, isActive:true },
  { id:2, code:'S02', name:'回流焊',          sortOrder:2, isActive:true },
  { id:3, code:'S03', name:'AOI 光學檢測',    sortOrder:3, isActive:true },
  { id:4, code:'S04', name:'電測／最終組裝',  sortOrder:4, isActive:true },
  { id:5, code:'S05', name:'廠務環境',         sortOrder:5, isActive:true },
];
