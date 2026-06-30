<script setup>
import AppToast from './components/AppToast.vue'
import AppConfirm from './components/AppConfirm.vue'
import { provide, ref } from 'vue'

const confirmRef = ref(null)
provide('$confirm', (msg) => confirmRef.value?.show(msg))
</script>

<template>
  <router-view />
  <AppToast />
  <AppConfirm ref="confirmRef" />
</template>
