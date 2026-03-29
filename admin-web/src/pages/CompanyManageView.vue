<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createCompany,
  deleteCompany,
  getCompanyList,
  updateCompany,
  updateCompanyStatus
} from '../api/master-data'

const router = useRouter()

const loading = ref(false)
const companies = ref([])
const message = ref('')
const messageType = ref('info')

const filters = ref({
  keyword: '',
  status: ''
})

const dialog = ref({
  visible: false,
  type: 'create',
  companyId: null
})

const form = ref(createCompanyForm())

const dialogTitle = computed(() => (dialog.value.type === 'edit' ? 'Edit company' : 'Create company'))

onMounted(() => {
  fetchCompanies()
})

function createCompanyForm() {
  return {
    name: '',
    licenseNo: '',
    contactPerson: '',
    contactPhone: '',
    address: '',
    status: 'ENABLED'
  }
}

async function fetchCompanies() {
  loading.value = true
  try {
    const response = await getCompanyList(cleanObject(filters.value))
    companies.value = response.data ?? []
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
    keyword: '',
    status: ''
  }
  fetchCompanies()
}

function openCreateDialog() {
  form.value = createCompanyForm()
  dialog.value = {
    visible: true,
    type: 'create',
    companyId: null
  }
}

function openEditDialog(item) {
  form.value = {
    name: item.name,
    licenseNo: item.licenseNo ?? '',
    contactPerson: item.contactPerson,
    contactPhone: item.contactPhone,
    address: item.address,
    status: item.status
  }
  dialog.value = {
    visible: true,
    type: 'edit',
    companyId: item.id
  }
}

function closeDialog() {
  dialog.value = {
    visible: false,
    type: 'create',
    companyId: null
  }
}

async function submitDialog() {
  try {
    const response = dialog.value.type === 'edit'
      ? await updateCompany(dialog.value.companyId, form.value)
      : await createCompany(form.value)
    showMessage(response.message, 'success')
    closeDialog()
    await fetchCompanies()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function toggleStatus(item) {
  try {
    const nextStatus = item.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    const response = await updateCompanyStatus(item.id, nextStatus)
    showMessage(response.message, 'success')
    await fetchCompanies()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function archiveCompanyItem(item) {
  try {
    const response = await updateCompanyStatus(item.id, 'ARCHIVED')
    showMessage(response.message, 'success')
    await fetchCompanies()
  } catch (error) {
    showMessage(getErrorMessage(error), 'error')
  }
}

async function deleteCompanyItem(item) {
  if (!window.confirm(`Delete company "${item.name}"? This only works when no product or batch still references it.`)) {
    return
  }
  try {
    const response = await deleteCompany(item.id)
    showMessage(response.message, 'success')
    await fetchCompanies()
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
        <h1>Company maintenance</h1>
        <p class="lead">
          Maintain real companies, archive historical ones, and prevent unsafe deletion when products or batches still depend on them.
        </p>
      </div>
      <div class="hero-actions">
        <button class="primary" @click="openCreateDialog">Create company</button>
        <button class="ghost" @click="router.push('/products')">Go to products</button>
        <button class="ghost" @click="router.push('/batches')">Back to batches</button>
      </div>
    </header>

    <section class="panel">
      <div class="filter-grid">
        <label>
          <span>Keyword</span>
          <input v-model.trim="filters.keyword" type="text" placeholder="name / contact / phone">
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
        <button class="primary" :disabled="loading" @click="fetchCompanies">Search</button>
        <button class="ghost" :disabled="loading" @click="resetFilters">Reset</button>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">
      {{ message }}
    </section>

    <section v-if="loading" class="panel empty-state">
      Loading companies...
    </section>

    <section v-else-if="!companies.length" class="panel empty-state">
      No company matched the current filter.
    </section>

    <section v-else class="list-grid">
      <article v-for="item in companies" :key="item.id" class="item-card">
        <div class="item-head">
          <div>
            <p class="item-code">{{ item.licenseNo || 'No license no' }}</p>
            <h3>{{ item.name }}</h3>
          </div>
          <span class="status-badge" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
        </div>

        <div class="meta-grid">
          <div>
            <span>Contact</span>
            <strong>{{ item.contactPerson }}</strong>
          </div>
          <div>
            <span>Phone</span>
            <strong>{{ item.contactPhone }}</strong>
          </div>
          <div>
            <span>Products</span>
            <strong>{{ item.productCount }}</strong>
          </div>
          <div>
            <span>Batches</span>
            <strong>{{ item.batchCount }}</strong>
          </div>
          <div class="full-width">
            <span>Address</span>
            <strong>{{ item.address }}</strong>
          </div>
        </div>

        <div class="toolbar">
          <button class="ghost" @click="openEditDialog(item)">Edit</button>
          <button :class="item.status === 'ENABLED' ? 'warning' : 'success'" @click="toggleStatus(item)">
            {{ statusActionLabel(item.status) }}
          </button>
          <button class="neutral" :disabled="item.status === 'ARCHIVED'" @click="archiveCompanyItem(item)">
            Archive
          </button>
          <button class="danger" @click="deleteCompanyItem(item)">
            Delete
          </button>
        </div>
      </article>
    </section>

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">Company</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button class="ghost" @click="closeDialog">Close</button>
        </div>

        <div class="form-grid">
          <label>
            <span>Name</span>
            <input v-model.trim="form.name" type="text">
          </label>
          <label>
            <span>License no</span>
            <input v-model.trim="form.licenseNo" type="text" placeholder="Optional">
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
            <span>Contact person</span>
            <input v-model.trim="form.contactPerson" type="text">
          </label>
          <label>
            <span>Contact phone</span>
            <input v-model.trim="form.contactPhone" type="text">
          </label>
          <label class="full-width">
            <span>Address</span>
            <textarea v-model.trim="form.address" rows="3"></textarea>
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

.full-width {
  grid-column: 1 / -1;
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

textarea {
  resize: vertical;
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
  width: min(820px, 100%);
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
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
