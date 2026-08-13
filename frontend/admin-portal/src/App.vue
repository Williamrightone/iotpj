<script setup>
import { useUiStore } from './stores/ui'
import LoadingOverlay from './components/LoadingOverlay.vue'
import ToastMessage from './components/ToastMessage.vue'

const ui = useUiStore()
</script>

<template>
  <router-view />
  <LoadingOverlay v-if="ui.loading" />
  <ToastMessage v-if="ui.toastMsg" :message="ui.toastMsg" />
</template>
