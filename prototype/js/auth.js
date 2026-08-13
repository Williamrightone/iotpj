// ===== Mock Users =====
const USERS = [
  { username: 'admin',      password: 'admin123', role: 'Admin',      name: '王管理員' },
  { username: 'maintainer', password: 'mt123',    role: 'Maintainer', name: '李維護員' },
  { username: 'viewer',     password: 'view123',  role: 'Viewer',     name: '張觀察者' },
];

// ===== Session Helpers =====
function setSession(user) {
  sessionStorage.setItem('wt_user', JSON.stringify({
    username: user.username,
    name: user.name,
    role: user.role,
  }));
}

function getSession() {
  const raw = sessionStorage.getItem('wt_user');
  return raw ? JSON.parse(raw) : null;
}

function clearSession() {
  sessionStorage.removeItem('wt_user');
}

// ===== Guards =====
function requireAuth() {
  if (!getSession()) {
    window.location.href = 'login.html';
  }
}

function requireRole(...roles) {
  const user = getSession();
  if (!user || !roles.includes(user.role)) {
    alert('您沒有權限執行此操作。');
    return false;
  }
  return true;
}

// ===== Login Page Logic =====
const loginForm = document.getElementById('loginForm');
if (loginForm) {
  if (getSession()) window.location.href = 'users.html';

  loginForm.addEventListener('submit', function (e) {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const errorMsg = document.getElementById('errorMsg');

    const user = USERS.find(u => u.username === username && u.password === password);
    if (user) {
      setSession(user);
      window.location.href = 'users.html';
    } else {
      errorMsg.textContent = '帳號或密碼錯誤，請重新輸入。';
      document.getElementById('password').value = '';
    }
  });
}
