import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const loading  = ref(false)
  const toastMsg = ref('')
  let toastTimer = null

  function showToast(msg, duration = 2800) {
    toastMsg.value = msg
    clearTimeout(toastTimer)
    toastTimer = setTimeout(() => { toastMsg.value = '' }, duration)
  }

  return { loading, toastMsg, showToast }
})
