<script setup>
defineProps({ message: String })
</script>

<template>
  <div class="toast">{{ message }}</div>
</template>
