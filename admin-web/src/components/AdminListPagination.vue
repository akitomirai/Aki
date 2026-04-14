<script setup>
defineProps({
  summary: {
    type: String,
    default: ''
  },
  prevDisabled: {
    type: Boolean,
    default: false
  },
  nextDisabled: {
    type: Boolean,
    default: false
  },
  prevTestid: {
    type: String,
    default: ''
  },
  nextTestid: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['prev', 'next'])
</script>

<template>
  <div class="toolbar admin-list-pagination">
    <span class="list-summary">{{ summary }}</span>
    <div class="toolbar-actions">
      <button class="ghost" :data-testid="prevTestid || undefined" :disabled="prevDisabled" @click="emit('prev')">
        上一页
      </button>
      <button class="ghost" :data-testid="nextTestid || undefined" :disabled="nextDisabled" @click="emit('next')">
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.admin-list-pagination {
  margin-top: 18px;
  padding: 18px 28px 0;
  border-top: 1px solid rgba(56, 134, 217, 0.1);
}

.admin-list-pagination .list-summary {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
}

@media (max-width: 760px) {
  .admin-list-pagination {
    padding: 18px 0 0;
  }
}
</style>
