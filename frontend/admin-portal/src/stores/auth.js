import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import api from '../api'

function loadUser() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch { return null }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken  = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user         = ref(loadUser())  // { userId, account, displayName, role, tenantId, stationIds, features }

  const isLoggedIn  = computed(() => !!accessToken.value)
  const isAdmin     = computed(() => user.value?.role === 'ADMIN')
  const displayName = computed(() => user.value?.displayName || user.value?.account || '')

  async function login(account, password) {
    const data = await api.login(account, password)
    accessToken.value  = data.accessToken
    refreshToken.value = data.refreshToken
    user.value = {
      userId:      data.userId,
      account:     data.account,
      displayName: data.displayName,
      role:        data.role,
      tenantId:    data.tenantId,
      stationIds:  data.stationIds  || [],
      features:    data.features    || [],
    }
    localStorage.setItem('accessToken',  data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('userInfo',     JSON.stringify(user.value))
  }

  async function logout() {
    const rt = refreshToken.value
    // 清除 local 狀態（無論 API 成敗）
    accessToken.value  = ''
    refreshToken.value = ''
    user.value         = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    if (rt) {
      await api.logout(rt).catch(() => {})
    }
  }

  return { accessToken, refreshToken, user, isLoggedIn, isAdmin, displayName, login, logout }
})
