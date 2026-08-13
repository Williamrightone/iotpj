<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'

const router = useRouter()
const auth   = useAuthStore()
const ui     = useUiStore()

const account  = ref('')
const password = ref('')
const error    = ref('')

async function handleLogin() {
  if (!account.value || !password.value) {
    error.value = '請輸入帳號與密碼'
    return
  }
  error.value = ''
  ui.loading = true
  try {
    await auth.login(account.value, password.value)
    router.push('/dashboard')
  } catch (e) {
    error.value = e.message || '登入失敗'
  } finally {
    ui.loading = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">W</div>
      <div class="login-title">willThx IoT Admin</div>
      <div class="login-sub">PCB 製造業 SaaS IoT 平台</div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label>帳號（Email）</label>
          <input
            v-model="account"
            type="text"
            class="form-input"
            placeholder="your@email.com"
            autocomplete="username"
          />
        </div>
        <div class="form-group">
          <label>密碼</label>
          <input
            v-model="password"
            type="password"
            class="form-input"
            placeholder="••••••••"
            autocomplete="current-password"
          />
        </div>
        <div v-if="error" class="login-error">{{ error }}</div>
        <button type="submit" class="login-btn" :disabled="ui.loading">
          {{ ui.loading ? '登入中...' : '登入' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex; align-items: center; justify-content: center;
  min-height: 100vh; background: var(--bg);
}
.login-card {
  background: #fff; border-radius: 16px; padding: 48px 40px;
  width: 420px; box-shadow: 0 4px 32px rgba(0,0,0,0.1);
  text-align: center;
}
.login-logo {
  width: 56px; height: 56px; background: var(--primary); border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px; color: #fff; font-size: 26px; font-weight: 800;
}
.login-title { font-size: 22px; font-weight: 700; }
.login-sub   { font-size: 13px; color: var(--text-sec); margin-top: 4px; margin-bottom: 32px; }
.login-form  { text-align: left; }
.login-form .form-group { margin-bottom: 18px; }
.login-form .form-group label { font-size: 13px; font-weight: 500; }
.login-error {
  color: var(--danger); font-size: 13px; margin-bottom: 12px;
  padding: 8px 12px; background: #FEE2E2; border-radius: 6px;
}
.login-btn {
  width: 100%; padding: 12px; background: var(--primary); color: #fff;
  border: none; border-radius: 8px; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: background 0.2s;
}
.login-btn:hover:not(:disabled) { background: var(--primary-dark); }
.login-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
