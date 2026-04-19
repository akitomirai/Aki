<script setup>
defineProps({
  toolbarClass: {
    type: [String, Array, Object],
    default: ''
  },
  contentClass: {
    type: [String, Array, Object],
    default: ''
  },
  wrapContent: {
    type: Boolean,
    default: true
  }
})
</script>

<template>
  <div class="admin-list-page-frame">
    <div class="manage-summary-row" :class="toolbarClass">
      <slot name="summary" />
      <div v-if="$slots.actions" class="manage-summary-actions">
        <slot name="actions" />
      </div>
    </div>

    <slot name="banner" />

    <section v-if="wrapContent" class="panel admin-list-content-card" :class="contentClass">
      <slot />
    </section>

    <div v-else class="admin-list-content-stack" :class="contentClass">
      <slot />
    </div>
  </div>
</template>

<style>
.admin-list-page-frame {
  display: grid;
  gap: var(--admin-page-section-gap);
}

.admin-list-page-frame > .manage-summary-row {
  margin: 0;
}

.admin-list-page-frame > .admin-list-content-stack {
  display: grid;
  gap: var(--admin-page-section-gap);
}

.admin-list-page-frame > .admin-list-content-card {
  position: relative;
  display: grid;
  gap: 0;
  overflow: hidden;
  padding: 20px 22px;
  border: 1px solid rgba(56, 134, 217, 0.18);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(248, 252, 255, 0.97) 100%);
  box-shadow:
    0 22px 46px rgba(45, 113, 194, 0.14),
    0 1px 0 rgba(255, 255, 255, 0.78) inset;
}

.admin-list-page-frame > .admin-list-content-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.52);
}

.admin-list-page-frame > .admin-list-content-card > .manage-filter-card {
  position: relative;
  z-index: 1;
  padding: 0;
  border: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.admin-list-page-frame > .admin-list-content-card > .message-bar {
  position: relative;
  z-index: 1;
  margin: 18px 0 0;
}

.admin-list-page-frame > .admin-list-content-card > .ledger-panel {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.admin-list-page-frame > .admin-list-content-card > .ledger-panel .panel-heading {
  padding-left: 28px;
}

@media (max-width: 760px) {
  .admin-list-page-frame > .admin-list-content-card {
    padding: 16px;
    border-radius: 20px;
  }

  .admin-list-page-frame > .admin-list-content-card > .ledger-panel .panel-heading {
    padding-left: 0;
  }
}
</style>
