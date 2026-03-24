<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createProduct,
  deleteProduct,
  getCompanyList,
  getProductList,
  updateProduct,
  updateProductStatus
} from '../api/master-data'

const router = useRouter()

const loading = ref(false)
const products = ref([])
const companies = ref([])
const message = ref('')
const messageType = ref('info')

const filters = ref({
  companyId: '',
  keyword: '',
  status: ''
})

const dialog = ref({
  visible: false,
  type: 'create',
  productId: null
})

const form = ref(createProductForm())

const dialogTitle = computed(() => (dialog.value.type === 'edit' ? 'Edit product' : 'Create product'))

onMounted(async () => {
  await loadCompanies()
  await fetchProducts()
})

function createProductForm() {
  return {
    companyId: null,
    productName: '',
    productCode: '',
    category: '',
    originPlace: '',
    coverImage: '',
    specification: '',
    unit: '',
    status: 'ENABLED'
  }
}

async function loadCompanies() {
  const response = await getCompanyList()
  companies.value = response.data ?? []
}

async function fetchProducts() {
  loading.value = true
  try {
    const response = await getProductList(cleanObject(filters.value))
    products.value = response.data ?? []
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

function cleanObject(source) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== null && value !== undefined && value !== '')
  )
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function resetFilters() {
  filters.value = {
    companyId: '',
    keyword: '',
    status: ''
  }
  fetchProducts()
}

function openCreateDialog() {
  form.value = createProductForm()
  dialog.value = {
    visible: true,
    type: 'create',
    productId: null
  }
}

function openEditDialog(item) {
  form.value = {
    companyId: item.companyId,
    productName: item.productName,
    productCode: item.productCode ?? '',
    category: item.category,
    originPlace: item.originPlace,
    coverImage: item.coverImage ?? '',
    specification: item.specification ?? '',
    unit: item.unit ?? '',
    status: item.status
  }
  dialog.value = {
    visible: true,
    type: 'edit',
    productId: item.id
  }
}

function closeDialog() {
  dialog.value = {
    visible: false,
    type: 'create',
    productId: null
  }
}

async function submitDialog() {
  try {
    const response = dialog.value.type === 'edit'
      ? await updateProduct(dialog.value.productId, form.value)
      : await createProduct(form.value)
    showMessage(response.message, 'success')
    closeDialog()
    await fetchProducts()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function toggleStatus(item) {
  try {
    const nextStatus = item.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    const response = await updateProductStatus(item.id, nextStatus)
    showMessage(response.message, 'success')
    await fetchProducts()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function archiveProductItem(item) {
  try {
    const response = await updateProductStatus(item.id, 'ARCHIVED')
    showMessage(response.message, 'success')
    await fetchProducts()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function deleteProductItem(item) {
  if (!window.confirm(`Delete product "${item.productName}"? This only works when no batch still references it.`)) {
    return
  }
  try {
    const response = await deleteProduct(item.id)
    showMessage(response.message, 'success')
    await fetchProducts()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

function statusClass(status) {
  return {
    ENABLED: 'enabled',
    DISABLED: 'disabled',
    ARCHIVED: 'archived'
  }[status] ?? 'disabled'
}

function statusActionLabel(status) {
  if (status === 'ENABLED') {
    return 'Disable'
  }
  if (status === 'ARCHIVED') {
    return 'Restore'
  }
  return 'Enable'
}

function getErrorMessage(error) {
  return error?.response?.data?.message || error?.message || 'Operation failed. Please try again.'
}
</script>

<template>
  <div class="page-shell">
    <header class="hero-card">
      <div>
        <p class="eyebrow">Master data</p>
        <h1>Product maintenance</h1>
        <p class="lead">
          Maintain real product options, archive historical products, and block unsafe deletion when batches still reference them.
        </p>
      </div>
      <div class="hero-actions">
        <button class="primary" @click="openCreateDialog">Create product</button>
        <button class="ghost" @click="router.push('/companies')">Go to companies</button>
        <button class="ghost" @click="router.push('/batches')">Back to batches</button>
      </div>
    </header>

    <section class="panel">
      <div class="filter-grid">
        <label>
          <span>Company</span>
          <select v-model="filters.companyId">
            <option value="">All companies</option>
            <option v-for="item in companies" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </label>
        <label>
          <span>Keyword</span>
          <input v-model.trim="filters.keyword" type="text" placeholder="name / code / category">
        </label>
        <label>
          <span>Status</span>
          <select v-model="filters.status">
            <option value="">All status</option>
            <option value="ENABLED">Enabled</option>
            <option value="DISABLED">Disabled</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </label>
      </div>

      <div class="toolbar">
        <button class="primary" :disabled="loading" @click="fetchProducts">Search</button>
        <button class="ghost" :disabled="loading" @click="resetFilters">Reset</button>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">
      {{ message }}
    </section>

    <section v-if="loading" class="panel empty-state">
      Loading products...
    </section>

    <section v-else-if="!products.length" class="panel empty-state">
      No product matched the current filter.
    </section>

    <section v-else class="list-grid">
      <article v-for="item in products" :key="item.id" class="item-card">
        <div class="item-main">
          <img class="cover-image" :src="item.coverImage" :alt="item.productName">
          <div class="item-copy">
            <div class="item-head">
              <div>
                <p class="item-code">{{ item.productCode || 'No code' }}</p>
                <h3>{{ item.productName }}</h3>
              </div>
              <span class="status-badge" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
            </div>

            <div class="meta-grid">
              <div>
                <span>Company</span>
                <strong>{{ item.companyName }}</strong>
              </div>
              <div>
                <span>Category</span>
                <strong>{{ item.category }}</strong>
              </div>
              <div>
                <span>Origin</span>
                <strong>{{ item.originPlace }}</strong>
              </div>
              <div>
                <span>Spec</span>
                <strong>{{ item.specification || 'N/A' }}</strong>
              </div>
              <div>
                <span>Unit</span>
                <strong>{{ item.unit || 'N/A' }}</strong>
              </div>
              <div>
                <span>Batch refs</span>
                <strong>{{ item.batchCount }}</strong>
              </div>
            </div>
          </div>
        </div>

        <div class="toolbar">
          <button class="ghost" @click="openEditDialog(item)">Edit</button>
          <button :class="item.status === 'ENABLED' ? 'warning' : 'success'" @click="toggleStatus(item)">
            {{ statusActionLabel(item.status) }}
          </button>
          <button class="neutral" :disabled="item.status === 'ARCHIVED'" @click="archiveProductItem(item)">
            Archive
          </button>
          <button class="danger" @click="deleteProductItem(item)">
            Delete
          </button>
        </div>
      </article>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">Product</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost" @click="closeDialog">Close</button>
        </div>

        <div class="form-grid">
          <label>
            <span>Company</span>
            <select v-model="form.companyId">
              <option :value="null">Select company</option>
              <option v-for="item in companies" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span>Product name</span>
            <input v-model.trim="form.productName" type="text">
          </label>
          <label>
            <span>Product code</span>
            <input v-model.trim="form.productCode" type="text" placeholder="Optional">
          </label>
          <label>
            <span>Category</span>
            <input v-model.trim="form.category" type="text">
          </label>
          <label>
            <span>Origin place</span>
            <input v-model.trim="form.originPlace" type="text">
          </label>
          <label>
            <span>Status</span>
            <select v-model="form.status">
              <option value="ENABLED">Enabled</option>
              <option value="DISABLED">Disabled</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </label>
          <label>
            <span>Cover image</span>
            <input v-model.trim="form.coverImage" type="url" placeholder="Optional image URL">
          </label>
          <label>
            <span>Specification</span>
            <input v-model.trim="form.specification" type="text">
          </label>
          <label>
            <span>Unit</span>
            <input v-model.trim="form.unit" type="text">
          </label>
        </div>

        <div class="toolbar end">
          <button class="ghost" @click="closeDialog">Cancel</button>
          <button class="primary" @click="submitDialog">Submit</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 20px 44px;
}

.hero-card,
.panel,
.item-card,
.dialog-card,
.message-bar {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 42px rgba(31, 55, 47, 0.08);
}

.hero-card {
  display: grid;
  grid-template-columns: 1.5fr auto;
  gap: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(242, 204, 105, 0.26), transparent 28%),
    linear-gradient(140deg, #17362d, #285143 70%, #edf5eb 160%);
  color: #f7f2e7;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f2cc69;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 12px;
  font-size: 38px;
}

.lead {
  margin: 0;
  line-height: 1.8;
  max-width: 700px;
}

.hero-actions,
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-actions {
  justify-content: flex-end;
  align-items: flex-end;
}

.panel,
.message-bar {
  margin-top: 18px;
  padding: 24px;
}

.filter-grid,
.form-grid,
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.list-grid {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.item-card {
  padding: 22px;
}

.item-main {
  display: grid;
  grid-template-columns: 132px 1fr;
  gap: 18px;
}

.cover-image {
  width: 132px;
  height: 132px;
  object-fit: cover;
  border-radius: 22px;
  background: linear-gradient(160deg, #eef5ed, #dce9df);
}

.item-head,
.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.item-code {
  margin-bottom: 6px;
  color: #60786d;
  font-size: 14px;
}

.item-head h3 {
  margin-bottom: 0;
  color: #17362d;
  font-size: 28px;
}

.meta-grid {
  margin-top: 16px;
}

.meta-grid div {
  padding: 14px;
  border-radius: 18px;
  background: rgba(245, 248, 244, 0.96);
}

.meta-grid span,
label span {
  display: block;
  margin-bottom: 8px;
  color: #60786d;
  font-size: 12px;
}

.meta-grid strong {
  color: #17362d;
  line-height: 1.5;
}

.message-bar.success {
  background: rgba(226, 244, 233, 0.94);
  color: #245846;
}

.message-bar.error {
  background: rgba(253, 236, 235, 0.94);
  color: #8f2f29;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  color: #546b61;
}

.status-badge,
button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  font: inherit;
}

.status-badge.enabled {
  background: rgba(40, 110, 74, 0.14);
  color: #245846;
}

.status-badge.disabled {
  background: rgba(190, 70, 58, 0.16);
  color: #8f2f29;
}

.status-badge.archived {
  background: rgba(96, 120, 109, 0.16);
  color: #53675e;
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid rgba(45, 85, 71, 0.16);
  border-radius: 16px;
  background: rgba(247, 249, 246, 0.95);
  color: #17362d;
  font: inherit;
}

button {
  cursor: pointer;
}

.primary {
  background: #245846;
  color: #fff7ea;
}

.ghost {
  background: rgba(36, 88, 70, 0.08);
  color: #245846;
}

.success {
  background: rgba(40, 110, 74, 0.12);
  color: #245846;
}

.warning {
  background: rgba(214, 137, 58, 0.16);
  color: #8b4c13;
}

.neutral {
  background: rgba(96, 120, 109, 0.14);
  color: #53675e;
}

.danger {
  background: rgba(190, 70, 58, 0.14);
  color: #8f2f29;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 24, 20, 0.4);
}

.dialog-card {
  width: min(920px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 24px;
}

.end {
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 900px) {
  .hero-card,
  .filter-grid,
  .form-grid,
  .meta-grid,
  .item-main {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .cover-image {
    width: 100%;
    height: 220px;
  }
}
</style>
