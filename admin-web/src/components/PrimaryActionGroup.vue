<script setup>
const props = defineProps({
  primaryLabel: {
    type: String,
    default: '执行'
  },
  primaryClass: {
    type: String,
    default: 'primary'
  },
  primaryDisabled: {
    type: Boolean,
    default: false
  },
  primaryTestid: {
    type: String,
    default: ''
  },
  primaryHint: {
    type: String,
    default: ''
  },
  moreLabel: {
    type: String,
    default: '更多操作'
  },
  secondaryActions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['primary-click', 'secondary-click'])

function handlePrimaryClick() {
  if (props.primaryDisabled) {
    return
  }
  emit('primary-click')
}

function handleSecondaryClick(action, event) {
  if (action?.disabled) {
    return
  }
  emit('secondary-click', action?.key)
  const details = event?.currentTarget?.closest('details')
  if (details) {
    details.open = false
  }
}
</script>

<template>
  <div class="primary-action-group">
    <button
      type="button"
      class="action-primary"
      :class="primaryClass"
      :disabled="primaryDisabled"
      :data-testid="primaryTestid || undefined"
      @click="handlePrimaryClick"
    >
      {{ primaryLabel }}
    </button>

    <p v-if="primaryHint" class="action-hint">{{ primaryHint }}</p>

    <details v-if="secondaryActions.length" class="secondary-menu">
      <summary>{{ moreLabel }}</summary>
      <div class="secondary-list">
        <button
          v-for="item in secondaryActions"
          :key="item.key"
          type="button"
          class="secondary-button"
          :disabled="item.disabled"
          :data-testid="item.testId || undefined"
          @click="handleSecondaryClick(item, $event)"
        >
          {{ item.label }}
        </button>
      </div>
    </details>
  </div>
</template>

<style scoped>
.primary-action-group {
  display: grid;
  gap: 12px;
}

.action-primary,
.secondary-button {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease, border-color 0.16s ease;
}

.action-primary {
  color: #fff;
}

.action-primary.primary {
  background: linear-gradient(135deg, #3597f6 0%, #2f7dd8 100%);
  box-shadow: 0 12px 22px rgba(48, 149, 246, 0.16);
}

.action-primary.success {
  background: linear-gradient(135deg, #74c89c 0%, #63b98b 100%);
  box-shadow: 0 12px 22px rgba(99, 185, 139, 0.16);
}

.action-primary.warning {
  background: linear-gradient(135deg, #ffbf66 0%, #f29a34 100%);
  box-shadow: 0 12px 22px rgba(242, 154, 52, 0.18);
}

.action-primary.danger {
  background: linear-gradient(135deg, #ef8c86 0%, #df6d67 100%);
  box-shadow: 0 12px 22px rgba(223, 109, 103, 0.16);
}

.action-primary.ghost {
  border-color: rgba(56, 134, 217, 0.16);
  background: #fff;
  color: var(--admin-primary-deep);
}

.action-primary:disabled,
.secondary-button:disabled {
  opacity: 0.58;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.action-hint {
  margin: 0;
  color: var(--admin-text-soft);
  line-height: 1.7;
  font-size: 13px;
}

.secondary-menu {
  border: 1px solid rgba(194, 212, 230, 0.72);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
}

.secondary-menu > summary {
  min-height: 40px;
  padding: 9px 14px;
  color: var(--admin-primary-deep);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  user-select: none;
}

.secondary-menu[open] > summary {
  border-bottom: 1px solid rgba(194, 212, 230, 0.72);
}

.secondary-list {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.secondary-button {
  border-color: rgba(56, 134, 217, 0.16);
  background: #fff;
  color: var(--admin-primary-deep);
  font-size: 13px;
}
</style>
