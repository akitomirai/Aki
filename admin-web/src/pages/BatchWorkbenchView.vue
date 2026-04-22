<script setup>
import { useRoute } from 'vue-router'
import BatchWorkbenchDrawerPanel from '../components/BatchWorkbenchDrawerPanel.vue'

const route = useRoute()
</script>

<template>
  <BatchWorkbenchDrawerPanel :batch-id="route.params.id" />
</template>
