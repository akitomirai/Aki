<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  activeKey: {
    type: [String, Number],
    default: ''
  },
  testIdPrefix: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select'])

const normalizedItems = computed(() => {
  return props.items.map((item, index) => ({
    key: item.key ?? item.value ?? index,
    label: item.label ?? '',
    value: item.value ?? item.count ?? 0,
    detail: item.detail ?? ''
  }))
})

function handleSelect(item) {
  emit('select', item.key)
}

function resolveTestId(item) {
  if (!props.testIdPrefix) {
    return undefined
  }
  return `${props.testIdPrefix}-${item.key}`
}
</script>

<template>
  <div class="manage-summary admin-overview-cards">
    <button
      v-for="item in normalizedItems"
      :key="item.key"
      type="button"
      class="manage-summary-chip manage-summary-chip--interactive"
      :class="{ 'is-active': String(activeKey) === String(item.key) }"
      :data-testid="resolveTestId(item)"
      @click="handleSelect(item)"
    >
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
    </button>
  </div>
</template>
