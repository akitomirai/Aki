<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'neutral'
  }
})

const normalizedTone = computed(() => String(props.tone || 'neutral').toLowerCase())
</script>

<template>
  <span class="status-tag" :class="normalizedTone">{{ text }}</span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
}

.draft,
.pending,
.neutral {
  background: rgba(129, 154, 184, 0.14);
  color: #57718e;
}

.primary,
.published {
  background: rgba(48, 149, 246, 0.12);
  color: #1d5c98;
}

.success,
.rectified {
  background: rgba(46, 166, 106, 0.12);
  color: #1e7d50;
}

.warning,
.frozen,
.processing {
  background: rgba(242, 139, 34, 0.14);
  color: #b96b16;
}

.danger,
.recalled,
.fail {
  background: rgba(221, 74, 74, 0.12);
  color: #b63f3f;
}
</style>
