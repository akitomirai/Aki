<script setup>
defineProps({
  toolbarClass: {
    type: [String, Array, Object],
    default: ''
  },
  templateClass: {
    type: [String, Array, Object],
    default: ''
  },
  filterCardClass: {
    type: [String, Array, Object],
    default: ''
  },
  filterPrimaryClass: {
    type: [String, Array, Object],
    default: ''
  },
  filterSecondaryClass: {
    type: [String, Array, Object],
    default: ''
  },
  ledgerCardClass: {
    type: [String, Array, Object],
    default: ''
  }
})
</script>

<template>
  <div class="admin-list-template" :class="templateClass">
    <div class="manage-summary-row" :class="toolbarClass">
      <slot name="summary" />
      <div v-if="$slots.actions" class="manage-summary-actions">
        <slot name="actions" />
      </div>
    </div>

    <slot name="banner" />

    <section
      v-if="$slots.filterPrimary || $slots.filterSecondary"
      class="panel manage-filter-card admin-list-template__filter-card"
      :class="filterCardClass"
    >
      <div
        v-if="$slots.filterPrimary"
        class="admin-list-template__filter-primary"
        :class="filterPrimaryClass"
      >
        <slot name="filterPrimary" />
      </div>

      <div
        v-if="$slots.filterSecondary"
        class="admin-list-template__filter-secondary"
        :class="filterSecondaryClass"
      >
        <slot name="filterSecondary" />
      </div>
    </section>

    <slot name="message" />

    <section class="panel ledger-panel admin-list-template__ledger-card" :class="ledgerCardClass">
      <slot name="ledger" />
    </section>
  </div>
</template>

<style>
.admin-list-template {
  display: grid;
  gap: var(--admin-page-section-gap);
}

.admin-list-template__filter-card,
.admin-list-template__ledger-card {
  position: relative;
  overflow: hidden;
}

.admin-list-template__filter-card {
  padding: 18px 22px 0;
}

.admin-list-template__filter-primary {
  padding: 0 var(--admin-list-card-inline-padding, 18px);
}

.admin-list-template__filter-secondary {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 14px;
  padding: 0 var(--admin-list-card-inline-padding, 18px) 18px;
}

.admin-list-template__filter-secondary > * {
  min-width: 0;
}

.admin-list-template__ledger-card {
  padding: 20px 22px 18px;
  background: #fff;
}

.admin-list-template__ledger-card .table-scroll-shell,
.admin-list-template__ledger-card :is(.ledger-row-list, .product-row-list, .todo-row-list, .batch-row-list) {
  background: #fff;
}

.admin-list-template__ledger-card .table-scroll-shell :is(
  .ledger-table-head,
  .product-table-head,
  .company-table-head,
  .batch-table-head,
  .qr-head,
  .quality-head,
  .risk-head,
  .log-head,
  .user-head
) {
  background: #fff;
}

.admin-list-template__ledger-card .admin-list-pagination {
  background: #fff;
}

@media (max-width: 768px) {
  .admin-list-template__filter-card,
  .admin-list-template__ledger-card {
    padding-left: 16px;
    padding-right: 16px;
  }

  .admin-list-template__filter-primary,
  .admin-list-template__filter-secondary {
    padding-left: 0;
    padding-right: 0;
  }
}
</style>
