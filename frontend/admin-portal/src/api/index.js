const BASE = '/api'

let _refreshing = null  // 防止同時觸發多次 refresh

function clearAuth() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
}

async function doRefresh() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return null
    const json = await res.json()
    if (json.responseCode === '00000' && json.data?.accessToken) {
      localStorage.setItem('accessToken', json.data.accessToken)
      return json.data.accessToken
    }
  } catch { /* ignore */ }
  return null
}

async function request(path, { method = 'GET', body, noAuth = false } = {}) {
  const makeHeaders = (token) => {
    const h = { 'Content-Type': 'application/json' }
    if (!noAuth && token) h['Authorization'] = `Bearer ${token}`
    return h
  }

  let token = noAuth ? null : localStorage.getItem('accessToken')

  let res
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers: makeHeaders(token),
      body: body != null ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new Error('網路連線失敗，請確認服務是否正常運作')
  }

  // Access token expired → try refresh once
  if (res.status === 401 && !noAuth) {
    if (!_refreshing) _refreshing = doRefresh().finally(() => { _refreshing = null })
    const newToken = await _refreshing
    if (newToken) {
      try {
        res = await fetch(`${BASE}${path}`, {
          method,
          headers: makeHeaders(newToken),
          body: body != null ? JSON.stringify(body) : undefined,
        })
      } catch {
        throw new Error('網路連線失敗，請確認服務是否正常運作')
      }
    } else {
      clearAuth()
      window.location.href = '/login'
      throw new Error('登入已過期，請重新登入')
    }
  }

  if (res.status >= 500) {
    throw new Error('伺服器發生錯誤，請稍後再試')
  }

  let json
  try {
    json = await res.json()
  } catch {
    throw new Error('回應格式錯誤，請稍後再試')
  }

  if (json.responseCode !== '00000') {
    const err = new Error(json.msg || '操作失敗')
    err.code = json.responseCode
    throw err
  }
  return json.data
}

export default {
  // ── Auth ────────────────────────────────────────────
  login(account, password) {
    return request('/auth/login', { method: 'POST', body: { account, password }, noAuth: true })
  },
  logout(refreshToken) {
    return request('/auth/logout', { method: 'POST', body: { refreshToken } })
  },
  refresh(refreshToken) {
    return request('/auth/refresh', { method: 'POST', body: { refreshToken }, noAuth: true })
  },

  // ── Users ────────────────────────────────────────────
  listUsers() {
    return request('/users')
  },
  getUser(id) {
    return request(`/users/${id}`)
  },
  createUser(data) {
    return request('/users', { method: 'POST', body: data })
  },
  updateUser(id, data) {
    return request(`/users/${id}`, { method: 'PUT', body: data })
  },
  disableUser(id) {
    return request(`/users/${id}/disable`, { method: 'POST' })
  },
  getStations(id) {
    return request(`/users/${id}/stations`)
  },
  updateStations(id, stationIds) {
    return request(`/users/${id}/stations`, { method: 'PUT', body: { stationIds } })
  },

  // ── Features ─────────────────────────────────────────
  listFeatures() {
    return request('/features')
  },
  createFeature(data) {
    return request('/features', { method: 'POST', body: data })
  },
  updateFeature(id, data) {
    return request(`/features/${id}`, { method: 'PUT', body: data })
  },
  setFeatureActive(id, active) {
    return request(`/features/${id}/active`, { method: 'PUT', body: { active } })
  },
  deleteFeature(id) {
    return request(`/features/${id}`, { method: 'DELETE' })
  },

  // ── Role Permissions ─────────────────────────────────
  getRolePermissions() {
    return request('/role-permissions')
  },
  updateRolePermissions(role, featureIds) {
    return request(`/role-permissions/${role}`, { method: 'PUT', body: { featureIds } })
  },
}
