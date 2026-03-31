<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changeBatchStatus, createRiskAction, getBatchDetail, getBatchList } from '../api/batch'
import { getFriendlyErrorMessage, riskActionOptions } from '../utils/batchExperience'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const message = ref('')
const messageType = ref('info')
const activeTab = ref('FROZEN')
const filters = ref(createFilterState())
const riskSubmitting = ref(false)
const resumeSubmitting = ref(false)
const riskDialog = ref(createRiskDialogState())
const resumeDialog = ref(createResumeDialogState())

const riskTabs = [
  { value: 'FROZEN', label: '已冻结' },
  { value: 'PROCESSING', label: '风险处理中' },
  { value: 'RECTIFIED', label: '已完成整改' },
  { value: 'RECALLED', label: '已召回' }
]

const statusOptions = [
  { value: '', label: '全部批次状态' },
  { value: 'FROZEN', label: '已冻结' },
  { value: 'RECALLED', label: '已召回' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'DRAFT', label: '草稿' }
]

const visibleRows = computed(() => {
  return rows.value.filter((item) => matchesKeyword(item) && matchesCompany(item) && matchesTab(item))
})

const tabCounts = computed(() => {
  return riskTabs.reduce((acc, item) => {
    acc[item.value] = rows.value.filter((row) => matchesTab(row, item.value)).length
    return acc
  }, {})
})

onMounted(async () => {
  await fetchRows()
})

function createFilterState() {
  return {
    keyword: '',
    companyName: '',
    status: ''
  }
}

function createRiskDialogState() {
  return {
    visible: false,
    batch: null,
    actionType: 'COMMENT',
    reason: '',
    comment: '',
    operatorName: '平台管理员'
  }
}

function createResumeDialogState() {
  return {
    visible: false,
    batch: null,
    reason: '整改已完成，恢复批次公开流通。',
    operatorName: '平台管理员'
  }
}

function showMessage(text, type = 'info') {
  message.value = text
  messageType.value = type
}

function cleanObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, item]) => item !== '' && item !== null && item !== undefined)
  )
}

function statusClass(status) {
  return {
    DRAFT: 'draft',
    PUBLISHED: 'published',
    FROZEN: 'frozen',
    RECALLED: 'recalled'
  }[String(status || 'DRAFT').toUpperCase()] ?? 'draft'
}

function actionEnabled(item, code) {
  return Boolean((item.actions || []).find((action) => action.code === code)?.enabled)
}

function canHandleRisk(item) {
  return ['FROZEN', 'RECALLED'].includes(String(item.status || '').toUpperCase())
}

function canResume(item) {
  return Boolean(item.canResume || actionEnabled(item, 'RESUME'))
}

function resumeText(item) {
  if (String(item.status || '').toUpperCase() === 'RECALLED') {
    return '不适用'
  }
  return canResume(item) ? '可恢复发布' : '暂不可恢复'
}

function latestRiskActionText(item) {
  return item.latestRiskActionLabel || '暂无处理动作'
}

function rectificationText(item) {
  return item.riskResolutionLabel || '无需整改'
}

function latestUpdatedText(item) {
  return item.lastUpdatedAt || '暂无更新'
}

function riskToneClass(item) {
  return {
    PROCESSING: 'processing',
    RECTIFIED: 'rectified',
    RECALLED: 'recalled',
    FROZEN: 'frozen'
  }[String(item.riskStatus || '').toUpperCase()] ?? 'pending'
}

function riskActionLabel(actionType) {
  return riskActionOptions.find((item) => item.value === actionType)?.label || actionType
}

function riskActionHint(actionType) {
  return {
    COMMENT: '补充当前风险判断、范围和处理说明。',
    RECTIFICATION: '记录已完成的整改动作、现场复核和责任落实。',
    PROCESSING: '明确已经进入处理中，方便全局队列快速识别。',
    RECTIFIED: '明确整改完成，便于恢复发布前复核。'
  }[actionType] ?? '记录当前风险处理情况。'
}

function matchesKeyword(item) {
  const keyword = filters.value.keyword.trim().toLowerCase()
  if (!keyword) {
    return true
  }
  return [item.batchCode, item.productName]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword))
}

function matchesCompany(item) {
  const keyword = filters.value.companyName.trim().toLowerCase()
  if (!keyword) {
    return true
  }
  return String(item.companyName || '').toLowerCase().includes(keyword)
}

function matchesTab(item, tabValue = activeTab.value) {
  return String(item.riskStatus || '').toUpperCase() === tabValue
}

async function fetchRows() {
  loading.value = true
  try {
    const response = await getBatchList(cleanObject({
      status: filters.value.status || undefined
    }))
    rows.value = response.data ?? []
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '风险批次加载失败，请稍后再试。'), 'error')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = createFilterState()
  fetchRows()
}

function openWorkbench(item) {
  router.push(`/batches/${item.id}`)
}

function openRiskDialog(item, actionType) {
  riskDialog.value = {
    visible: true,
    batch: item,
    actionType,
    reason: actionType === 'PROCESSING'
      ? '当前批次已进入风险处理中，请继续保持冻结并补充复核。'
      : (actionType === 'RECTIFIED' ? '整改已完成，等待恢复发布复核。' : ''),
    comment: actionType === 'COMMENT'
      ? '已补处理说明，将继续核查现场范围与去向。'
      : (actionType === 'RECTIFICATION' ? '已补整改记录，隔离、复核和责任落实已留痕。' : ''),
    operatorName: '平台管理员'
  }
}

function closeRiskDialog() {
  riskDialog.value = createRiskDialogState()
}

function openResumeDialog(item) {
  resumeDialog.value = {
    visible: true,
    batch: item,
    reason: '整改已完成，恢复批次公开流通。',
    operatorName: '平台管理员'
  }
}

function closeResumeDialog() {
  resumeDialog.value = createResumeDialogState()
}

async function submitRiskAction() {
  if (!riskDialog.value.batch?.id) {
    return
  }
  riskSubmitting.value = true
  try {
    await createRiskAction(riskDialog.value.batch.id, {
      actionType: riskDialog.value.actionType,
      reason: riskDialog.value.reason,
      comment: riskDialog.value.comment,
      operatorName: riskDialog.value.operatorName
    })
    await fetchRows()
    showMessage(`风险动作“${riskActionLabel(riskDialog.value.actionType)}”已记录，列表已刷新。`, 'success')
    closeRiskDialog()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '风险处理保存失败，请稍后再试。'), 'error')
  } finally {
    riskSubmitting.value = false
  }
}

async function submitResume() {
  if (!resumeDialog.value.batch?.id) {
    return
  }
  resumeSubmitting.value = true
  try {
    await changeBatchStatus(resumeDialog.value.batch.id, {
      targetStatus: 'PUBLISHED',
      reason: resumeDialog.value.reason,
      operatorName: resumeDialog.value.operatorName
    })
    await fetchRows()
    showMessage('批次已恢复发布，风险列表和工作台状态已同步。', 'success')
    closeResumeDialog()
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '恢复发布失败，请稍后再试。'), 'error')
  } finally {
    resumeSubmitting.value = false
  }
}

async function openWorkbenchAfterRefresh(item) {
  try {
    await getBatchDetail(item.id)
  } catch (error) {
    showMessage(getFriendlyErrorMessage(error, '批次工作台加载失败，请稍后再试。'), 'error')
    return
  }
  openWorkbench(item)
}
</script>

<template>
  <div class="page-shell" data-testid="risk-page">
    <section class="manage-page-header">
      <div>
        <h1 class="manage-page-title">风险处理</h1>
        <p class="manage-page-subtitle">集中处理已冻结、处理中、已整改和已召回批次，直接在列表里补动作，再回工作台核对状态。</p>
      </div>
      <div class="manage-page-actions">
        <button class="ghost" data-testid="risk-refresh-button" :disabled="loading" @click="fetchRows">刷新</button>
      </div>
    </section>

    <section class="panel todo-tabs-panel">
      <div class="todo-tabs">
        <button
          v-for="tab in riskTabs"
          :key="tab.value"
          type="button"
          class="todo-tab"
          :class="{ active: activeTab === tab.value }"
          :data-testid="`risk-tab-${tab.value}`"
          @click="activeTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <strong>{{ tabCounts[tab.value] ?? 0 }}</strong>
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="filter-grid">
        <label>
          <span>批次名称 / 编号</span>
          <input v-model.trim="filters.keyword" data-testid="risk-filter-keyword" type="text" placeholder="输入批次编号或产品名称">
        </label>
        <label>
          <span>企业</span>
          <input v-model.trim="filters.companyName" type="text" placeholder="输入企业名称">
        </label>
        <label>
          <span>批次状态</span>
          <select v-model="filters.status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label>
          <span>当前筛选</span>
          <input :value="riskTabs.find((item) => item.value === activeTab)?.label || '已冻结'" type="text" disabled>
        </label>
      </div>

      <div class="toolbar">
        <span class="list-summary">共 {{ rows.length }} 个批次，当前显示 {{ visibleRows.length }} 个。</span>
        <div class="toolbar-actions">
          <button class="primary" data-testid="risk-search-button" :disabled="loading" @click="fetchRows">查询</button>
          <button class="ghost" data-testid="risk-reset-button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <section v-if="message" class="message-bar" :class="messageType">{{ message }}</section>

    <section v-if="loading" class="panel empty-state">
      <div>
        <h3>正在加载风险批次...</h3>
        <p class="empty-copy">请稍等，正在汇总冻结、处理中和整改中的批次。</p>
      </div>
    </section>

    <section v-else-if="!visibleRows.length" class="panel empty-state">
      <div>
        <h3>当前筛选下没有风险批次</h3>
        <p class="empty-copy">可以切换 tabs 或调整检索条件后再看。</p>
      </div>
    </section>

    <section v-else class="panel">
      <div class="todo-table-head risk-head">
        <span>批次</span>
        <span>企业</span>
        <span>批次状态</span>
        <span>风险状态</span>
        <span>最近风险动作</span>
        <span>整改结果</span>
        <span>恢复发布</span>
        <span>最近更新</span>
        <span>操作</span>
      </div>

      <div class="todo-row-list">
        <article
          v-for="item in visibleRows"
          :key="item.id"
          class="todo-row risk-row"
          :data-testid="`risk-row-${item.id}`"
        >
          <div class="row-main">
            <strong>{{ item.productName }}</strong>
            <span>{{ item.batchCode }}</span>
            <small>{{ item.currentNode || '待确认环节' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ item.companyName }}</strong>
            <small>{{ item.originPlace || '暂无产地' }}</small>
          </div>

          <div class="status-stack">
            <span class="status-badge" :class="statusClass(item.status)">{{ item.statusLabel }}</span>
            <small>{{ item.marketDate || '暂无公开时间' }}</small>
          </div>

          <div class="status-stack">
            <span class="info-pill" :class="riskToneClass(item)">{{ item.riskStatusLabel }}</span>
            <small>{{ canHandleRisk(item) ? '当前处于风险处理链路' : '当前没有进行中的风险处理' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ latestRiskActionText(item) }}</strong>
            <small>最近风险动作</small>
          </div>

          <div class="row-meta">
            <strong>{{ rectificationText(item) }}</strong>
            <small>整改结果</small>
          </div>

          <div class="row-meta">
            <strong>{{ resumeText(item) }}</strong>
            <small>{{ canResume(item) ? '当前已满足恢复发布条件' : '还需继续补齐处理链' }}</small>
          </div>

          <div class="row-meta">
            <strong>{{ latestUpdatedText(item) }}</strong>
            <small>最近更新时间</small>
          </div>

          <div class="row-actions action-stack">
            <button class="text-button primary-text" :data-testid="`risk-open-workbench-${item.id}`" @click="openWorkbenchAfterRefresh(item)">进入工作台</button>
            <button class="text-button" :data-testid="`risk-comment-${item.id}`" :disabled="!canHandleRisk(item)" @click="openRiskDialog(item, 'COMMENT')">补处理说明</button>
            <button class="text-button" :data-testid="`risk-rectification-${item.id}`" :disabled="!canHandleRisk(item)" @click="openRiskDialog(item, 'RECTIFICATION')">补整改记录</button>
            <button class="text-button" :data-testid="`risk-processing-${item.id}`" :disabled="!canHandleRisk(item)" @click="openRiskDialog(item, 'PROCESSING')">标记处理中</button>
            <button class="text-button" :data-testid="`risk-rectified-${item.id}`" :disabled="!canHandleRisk(item)" @click="openRiskDialog(item, 'RECTIFIED')">标记已整改</button>
            <button class="text-button" :data-testid="`risk-resume-${item.id}`" :disabled="!canResume(item)" @click="openResumeDialog(item)">恢复发布</button>
          </div>
        </article>
      </div>
    </section>

    <div v-if="riskDialog.visible" class="dialog-mask" @click.self="closeRiskDialog">
      <section class="dialog-card" data-testid="risk-action-dialog">
        <div class="dialog-head">
          <div>
            <h3>{{ riskActionLabel(riskDialog.actionType) }}</h3>
            <p>{{ riskDialog.batch?.batchCode }} · {{ riskDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" @click="closeRiskDialog">关闭</button>
        </div>

        <div class="form-grid">
          <label>
            <span>处理动作</span>
            <input :value="riskActionLabel(riskDialog.actionType)" type="text" disabled>
          </label>
          <label>
            <span>处理人</span>
            <input v-model.trim="riskDialog.operatorName" type="text" placeholder="例如 平台管理员">
          </label>
          <label class="full-width">
            <span>动作提示</span>
            <textarea :value="riskActionHint(riskDialog.actionType)" rows="2" disabled></textarea>
          </label>
          <label class="full-width">
            <span>处理说明</span>
            <textarea
              v-model.trim="riskDialog.reason"
              rows="4"
              :placeholder="riskDialog.actionType === 'PROCESSING' || riskDialog.actionType === 'RECTIFIED'
                ? '这里是必填项，请写清当前阶段判断或整改完成结论。'
                : '可选补充当前风险原因、范围和判断。'"
            />
          </label>
          <label class="full-width">
            <span>补充记录</span>
            <textarea
              v-model.trim="riskDialog.comment"
              rows="4"
              :placeholder="riskDialog.actionType === 'COMMENT' || riskDialog.actionType === 'RECTIFICATION'
                ? '这里是必填项，请写清处理说明或整改留痕。'
                : '可选补充当前处理动作、现场情况和下一步安排。'"
            />
          </label>
        </div>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="riskSubmitting" @click="closeRiskDialog">取消</button>
          <button class="warning" data-testid="risk-action-submit" :disabled="riskSubmitting" @click="submitRiskAction">
            {{ riskSubmitting ? '正在保存...' : '确认保存' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="resumeDialog.visible" class="dialog-mask" @click.self="closeResumeDialog">
      <section class="dialog-card" data-testid="risk-resume-dialog">
        <div class="dialog-head">
          <div>
            <h3>恢复发布</h3>
            <p>{{ resumeDialog.batch?.batchCode }} · {{ resumeDialog.batch?.productName }}</p>
          </div>
          <button class="ghost" @click="closeResumeDialog">关闭</button>
        </div>

        <div class="overview-grid">
          <div>
            <span>当前风险状态</span>
            <strong>{{ resumeDialog.batch?.riskStatusLabel || '待确认' }}</strong>
          </div>
          <div>
            <span>整改结果</span>
            <strong>{{ resumeDialog.batch?.riskResolutionLabel || '待确认' }}</strong>
          </div>
          <div>
            <span>最近风险动作</span>
            <strong>{{ resumeDialog.batch?.latestRiskActionLabel || '暂无处理动作' }}</strong>
          </div>
          <div>
            <span>恢复条件</span>
            <strong>{{ canResume(resumeDialog.batch || {}) ? '已满足' : '未满足' }}</strong>
          </div>
        </div>

        <div class="form-grid" style="margin-top: 16px;">
          <label>
            <span>处理人</span>
            <input v-model.trim="resumeDialog.operatorName" type="text" placeholder="例如 平台管理员">
          </label>
          <label class="full-width">
            <span>恢复说明</span>
            <textarea
              v-model.trim="resumeDialog.reason"
              rows="4"
              placeholder="写清为什么可以恢复发布，以及已完成哪些整改与复核。"
            />
          </label>
        </div>

        <div class="dialog-actions" style="margin-top: 18px;">
          <button class="ghost" :disabled="resumeSubmitting" @click="closeResumeDialog">取消</button>
          <button class="success" data-testid="risk-resume-submit" :disabled="resumeSubmitting" @click="submitResume">
            {{ resumeSubmitting ? '正在恢复...' : '确认恢复发布' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style src="../assets/styles/admin-task-pages.css" scoped></style>

<style scoped>
.risk-head,
.risk-row {
  grid-template-columns:
    minmax(0, 1.35fr)
    minmax(0, 1.15fr)
    minmax(0, 0.9fr)
    minmax(0, 1fr)
    minmax(0, 1.25fr)
    minmax(0, 0.95fr)
    minmax(0, 0.95fr)
    minmax(0, 0.9fr)
    minmax(0, 1.8fr);
}

.action-stack {
  align-items: flex-start;
}

.processing {
  background: rgba(242, 139, 34, 0.14);
  color: #b96b16;
}

.rectified {
  background: rgba(46, 166, 106, 0.12);
  color: #1e7d50;
}

.recalled {
  background: rgba(221, 74, 74, 0.12);
  color: #b63f3f;
}

.frozen,
.pending {
  background: rgba(129, 154, 184, 0.14);
  color: #57718e;
}

.primary-text {
  font-weight: 700;
}

@media (max-width: 760px) {
  .risk-row {
    grid-template-columns: 1fr;
  }
}
</style>
