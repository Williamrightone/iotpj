<script setup>
defineProps({
  title:     { type: String, default: '確認操作' },
  message:   { type: String, required: true },
  confirmText: { type: String, default: '確認' },
  confirmClass: { type: String, default: 'btn-danger' },
})
const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <div class="confirm-overlay">
    <div class="confirm-dialog">
      <h4>{{ title }}</h4>
      <p>{{ message }}</p>
      <div class="confirm-actions">
        <button class="btn btn-ghost" @click="emit('cancel')">取消</button>
        <button class="btn" :class="confirmClass" @click="emit('confirm')">{{ confirmText }}</button>
      </div>
    </div>
  </div>
</template>
