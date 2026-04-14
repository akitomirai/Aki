<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <section class="manage-page-header">
    <div>
      <h1 class="manage-page-title">{{ title }}</h1>
      <p v-if="subtitle" class="manage-page-subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="manage-page-actions">
      <slot name="actions" />
    </div>
  </section>
</template>
