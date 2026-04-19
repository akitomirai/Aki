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
  <div class="admin-list-pagination">
    <el-button
      class="admin-list-pagination__button"
      :data-testid="prevTestid || undefined"
      :disabled="prevDisabled"
      @click="emit('prev')"
    >
      上一页
    </el-button>
    <span class="admin-list-pagination__summary">{{ summary }}</span>
    <el-button
      class="admin-list-pagination__button"
      :data-testid="nextTestid || undefined"
      :disabled="nextDisabled"
      @click="emit('next')"
    >
      下一页
    </el-button>
  </div>
</template>

<style scoped>
.admin-list-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: nowrap;
  gap: 12px;
  margin-top: 18px;
  padding: 0 28px;
}

.admin-list-pagination__summary {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  flex: 0 0 auto;
  color: var(--admin-text-mid);
  font-size: 13px;
  line-height: 1.65;
}

.admin-list-pagination__button {
  flex: 0 0 auto;
}

.admin-list-pagination :deep(.el-button) {
  min-width: 72px;
  min-height: 32px;
  padding: 0 18px;
  border-radius: 999px;
}

@media (max-width: 760px) {
  .admin-list-pagination {
    padding: 0;
    flex-wrap: wrap;
    row-gap: 8px;
  }
}
</style>
