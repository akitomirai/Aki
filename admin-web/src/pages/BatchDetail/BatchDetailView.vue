<template>
  <div class="batch-detail-page">
    <el-card v-if="batchLoading" shadow="never" class="overview-card page-state-card">
      <el-skeleton :rows="10" animated />
    </el-card>
    <el-card v-else-if="batchError && !batch" shadow="never" class="overview-card page-state-card">
      <el-result icon="error" title="批次详情加载失败" :sub-title="batchError">
        <template #extra>
          <el-button type="primary" @click="loadBatch">重新加载</el-button>
        </template>
      </el-result>
    </el-card>
    <el-card v-else-if="!batch" shadow="never" class="overview-card page-state-card">
      <el-empty description="暂无批次详情数据" />
    </el-card>
    <template v-else>
      <el-alert
        v-if="batchError"
        class="page-alert"
        type="warning"
        show-icon
        :closable="false"
        title="部分关联信息加载失败"
        :description="batchError"
      />

      <el-card class="overview-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <span class="batch-code">{{ normalizeText(batch.batchCode, '未生成批次编码') }}</span>
              <el-tag :type="getStatusType(batch.status)" effect="dark" class="status-tag">
                {{ getStatusText(batch.status) }}
              </el-tag>
              <el-tag :type="getRegulationStatusType(batch.regulationStatus)" effect="plain" class="status-tag">
                {{ getRegulationStatusText(batch.regulationStatus) }}
              </el-tag>
            </div>
            <div class="header-actions">
              <el-button v-if="canEdit" type="primary" @click="openEditBatchDialog">编辑批次信息</el-button>
              <el-button v-if="canPublish" type="success" :loading="publishing" @click="handlePublish">发布批次</el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="产品名称">{{ normalizeText(batch.productName, '未关联产品') }}</el-descriptions-item>
          <el-descriptions-item label="所属企业">{{ normalizeText(batch.creatorCompanyName, '暂无企业信息') }}</el-descriptions-item>
          <el-descriptions-item label="产地">{{ normalizeText(batch.originPlace, '未填写产地') }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ normalizeText(batch.startDate, '未填写') }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(batch.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="对外说明">{{ normalizeText(batch.publicRemark, '暂无对外说明') }}</el-descriptions-item>
        </el-descriptions>

        <div class="summary-grid">
          <div class="summary-item">
            <div class="summary-label">批次编号</div>
            <el-tooltip :content="summaryBatchCode" placement="top">
              <div class="summary-value summary-mono text-ellipsis">{{ summaryBatchCode }}</div>
            </el-tooltip>
          </div>
          <div class="summary-item">
            <div class="summary-label">产品名称</div>
            <el-tooltip :content="summaryProductName" placement="top">
              <div class="summary-value text-ellipsis">{{ summaryProductName }}</div>
            </el-tooltip>
          </div>
          <div class="summary-item">
            <div class="summary-label">所属企业</div>
            <el-tooltip :content="summaryCompanyName" placement="top">
              <div class="summary-value text-ellipsis">{{ summaryCompanyName }}</div>
            </el-tooltip>
          </div>
          <div class="summary-item">
            <div class="summary-label">开始日期</div>
            <div class="summary-value">{{ summaryStartDate }}</div>
          </div>
          <div class="summary-item">
            <div class="summary-label">创建时间</div>
            <div class="summary-value">{{ summaryCreatedAt }}</div>
          </div>
          <div class="summary-item summary-item-status">
            <div class="summary-label">状态总览</div>
            <div class="summary-statuses">
              <el-tag :type="getStatusType(batch.status)" effect="dark">批次：{{ getStatusText(batch.status) }}</el-tag>
              <el-tag :type="getRegulationStatusType(batch.regulationStatus)" effect="plain">监管：{{ getRegulationStatusText(batch.regulationStatus) }}</el-tag>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="overview-card trace-entry-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <span class="section-title">公开追溯入口</span>
              <p class="section-subtitle">老师或消费者查看公开档案时，直接使用下面的追溯码与链接。</p>
            </div>
            <el-button size="small" @click="loadQrList">刷新入口</el-button>
          </div>
        </template>
        <div v-if="currentQrToken" class="trace-entry-panel">
          <div class="trace-entry-note">
            追溯码用于快速核验当前批次，公开链接可直接分享给老师或消费者查看追溯档案。
          </div>
          <div class="trace-entry-meta">
            <div class="trace-entry-block">
              <span class="trace-entry-label">追溯码 Token</span>
              <div class="trace-entry-value trace-entry-token">{{ currentQrToken }}</div>
            </div>
            <div class="trace-entry-block">
              <span class="trace-entry-label">公开链接</span>
              <div class="trace-entry-value trace-entry-link">{{ currentTraceLink }}</div>
            </div>
          </div>
          <div class="trace-entry-actions">
            <el-button @click="copyTraceToken">复制追溯码</el-button>
            <el-button @click="copyTraceLink">复制链接</el-button>
            <el-button type="primary" @click="openTraceLink">打开页面</el-button>
            <el-button type="success" plain @click="showTraceQrPreview">预览二维码</el-button>
          </div>
        </div>
        <el-empty v-else description="当前批次暂无公开追溯码，请先在二维码管理中生成后再查看。" :image-size="52" />
      </el-card>

      <el-row :gutter="20" class="main-content">
        <el-col :span="9">
          <el-card class="section-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="section-title">参与主体</span>
                <el-button v-if="canManageParticipants" type="primary" link @click="openParticipantDialog">维护参与企业</el-button>
              </div>
            </template>
            <el-table :data="participantRows" stripe style="width: 100%" size="small" empty-text="暂无参与主体信息">
              <el-table-column prop="companyName" label="企业名称" min-width="120" show-overflow-tooltip />
              <el-table-column prop="bizRole" label="业务角色" width="110">
                <template #default="{ row }">
                  {{ getBizRoleText(row.bizRole) }}
                </template>
              </el-table-column>
              <el-table-column label="标记" width="90" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isCreator" size="small" type="success" effect="plain">创建方</el-tag>
                  <span v-else class="cell-muted">-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card class="section-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="section-title">质检报告</span>
              </div>
            </template>
            <el-table :data="qualityReports" style="width: 100%" size="small" empty-text="暂无质检信息">
              <el-table-column prop="result" label="结论" width="90">
                <template #default="{ row }">
                  <el-tag :type="getQualityResultType(row.result)" size="small">
                    {{ getQualityResultText(row.result) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="agency" label="检测机构" min-width="110" show-overflow-tooltip />
              <el-table-column prop="reportNo" label="报告编号" width="120" show-overflow-tooltip />
            </el-table>
          </el-card>

          <el-card class="section-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="section-title">监管记录</span>
              </div>
            </template>
            <el-table :data="regulationRecords" style="width: 100%" size="small" empty-text="暂无监管记录">
              <el-table-column prop="inspectResult" label="结论" width="90">
                <template #default="{ row }">
                  <el-tag :type="getRegulationResultType(row.inspectResult)" size="small">
                    {{ getRegulationResultText(row.inspectResult) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="actionTaken" label="处理措施" min-width="110" show-overflow-tooltip />
              <el-table-column label="时间" width="110">
                <template #default="{ row }">
                  {{ formatShortTime(row.inspectTime) }}
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="15">
          <el-card class="section-card" shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="section-title">业务节点时间轴</span>
                  <p class="section-subtitle">按业务过程展示关键信息，默认摘要展示，展开后可查看完整内容。</p>
                </div>
                <el-button v-if="canAddNode" type="primary" @click="openNodeDialog">
                  <el-icon class="mr-1"><Plus /></el-icon>新增节点
                </el-button>
              </div>
            </template>

            <el-timeline v-if="nodeRows.length > 0" class="node-timeline">
              <el-timeline-item
                v-for="node in nodeRows"
                :key="node.id || `${node.title}-${node.eventTime}`"
                :timestamp="formatTime(node.eventTime)"
                placement="top"
                :type="node.isPublic ? 'success' : 'info'"
                :hollow="!node.isPublic"
              >
                <el-card class="node-item-card" shadow="hover">
                  <div class="node-header">
                    <div>
                      <div class="node-title">{{ normalizeText(node.title, '未命名节点') }}</div>
                      <div class="node-company">
                        <el-icon><OfficeBuilding /></el-icon>
                        <span>{{ normalizeText(node.companyName, '暂无企业信息') }}</span>
                      </div>
                    </div>
                    <div class="node-tags">
                      <el-tag size="small" effect="plain">节点类型：{{ getNodeTypeText(node.nodeType) }}</el-tag>
                      <el-tag size="small" type="info">业务角色：{{ getBizRoleText(node.bizRole) }}</el-tag>
                      <el-tag size="small" :type="node.isPublic ? 'success' : 'warning'">
                        {{ node.isPublic ? '对外公开' : '仅内部可见' }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="node-content">
                    <template v-for="field in getNodeContentEntries(node)" :key="field.key">
                      <div class="content-field">
                        <span class="field-label">{{ field.label }}</span>
                        <el-tooltip v-if="field.truncated" :content="field.fullValue" placement="top">
                          <span class="field-value">{{ field.displayValue }}</span>
                        </el-tooltip>
                        <span v-else class="field-value">{{ field.displayValue }}</span>
                      </div>
                    </template>
                  </div>
                  <div v-if="hasHiddenNodeFields(node)" class="node-expand">
                    <el-button type="primary" link @click="toggleNodeExpanded(node)">
                      {{ isNodeExpanded(node) ? '收起完整内容' : `展开查看剩余 ${getNodeHiddenCount(node)} 项` }}
                    </el-button>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无业务节点，可点击右上角按钮新增。" />
          </el-card>
        </el-col>
      </el-row>

      <el-dialog v-model="editBatchDialogVisible" title="编辑批次信息" width="600px">
        <el-form :model="editForm" label-width="100px">
          <el-form-item label="产品 ID">
            <el-input-number v-model="editForm.productId" :min="1" class="full-width" disabled />
            <span class="form-tip">产品关联暂不可修改</span>
          </el-form-item>
          <el-form-item label="产地">
            <el-input v-model="editForm.originPlace" />
          </el-form-item>
          <el-form-item label="开始日期">
            <el-date-picker v-model="editForm.startDate" type="date" value-format="YYYY-MM-DD" class="full-width" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="editForm.remark" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="对外说明">
            <el-input v-model="editForm.publicRemark" type="textarea" :rows="2" placeholder="消费者可见的说明信息" />
          </el-form-item>
          <el-form-item label="内部备注">
            <el-input v-model="editForm.internalRemark" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editBatchDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSaveBatch">保存修改</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="participantDialogVisible" title="维护参与主体" width="1040px" @closed="resetParticipantDialog">
        <div class="participant-dialog-tip">平台管理员优先通过企业下拉选择，避免手输企业 ID 出错。</div>
        <el-table :data="participantEditRows" border class="participant-edit-table">
          <el-table-column label="企业" min-width="230">
            <template #default="{ row }">
              <el-select v-if="canSelectCompany" v-model="row.companyId" class="full-width" filterable placeholder="请选择企业">
                <el-option
                  v-for="item in companyOptions"
                  :key="item.id"
                  :label="`${normalizeText(item.name, '未命名企业')} (ID:${item.id})`"
                  :value="item.id"
                />
              </el-select>
              <div v-else class="company-id-wrap">
                <el-input-number v-model="row.companyId" :min="1" class="full-width" controls-position="right" placeholder="请输入企业 ID" />
                <div class="company-name-tip">{{ getCompanyNameById(row.companyId) }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="企业 ID" width="120" align="center">
            <template #default="{ row }">
              <span>{{ row.companyId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="业务角色" width="170">
            <template #default="{ row }">
              <el-select v-model="row.bizRole" class="full-width">
                <el-option v-for="role in bizRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="顺序" width="110" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.stageOrder" :min="0" class="participant-order-input" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="创建方" width="120" align="center">
            <template #default="{ row }">
              <div class="creator-cell">
                <el-switch v-model="row.isCreator" />
                <span>{{ row.isCreator ? '是' : '否' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.remark" type="textarea" :rows="2" resize="none" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeParticipantRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="mt-2">
          <el-button type="primary" plain size="small" @click="addParticipantRow">
            <el-icon><Plus /></el-icon>新增一行
          </el-button>
        </div>
        <template #footer>
          <el-button @click="participantDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingParticipants" @click="saveParticipants">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="nodeDialogVisible" title="新增业务节点" width="700px" @closed="resetNodeDialog">
        <el-form :model="nodeForm" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="节点类型">
                <el-select v-model="nodeForm.stage" class="full-width" @change="handleStageChange">
                  <el-option v-for="(label, value) in stageLabelMap" :key="value" :label="label" :value="value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="业务角色">
                <el-select v-model="nodeForm.bizRole" class="full-width" placeholder="请选择">
                  <el-option v-for="role in bizRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="标题">
            <el-input v-model="nodeForm.title" placeholder="如：采摘记录、入库登记" />
          </el-form-item>
          <el-form-item label="地点">
            <el-input v-model="nodeForm.location" placeholder="发生地点" />
          </el-form-item>

          <div class="template-fields-area">
            <template v-if="nodeForm.stage === 'PRODUCE'">
              <el-form-item label="作业名称">
                <el-input v-model="nodeTemplateData.operationName" placeholder="如：播种、施肥" />
              </el-form-item>
            </template>
            <template v-else-if="nodeForm.stage === 'TRANSPORT'">
              <el-form-item label="始发地">
                <el-input v-model="nodeTemplateData.fromAddress" />
              </el-form-item>
              <el-form-item label="目的地">
                <el-input v-model="nodeTemplateData.toAddress" />
              </el-form-item>
            </template>
            <template v-else-if="nodeForm.stage === 'PROCESS'">
              <el-form-item label="加工环节">
                <el-input v-model="nodeTemplateData.processName" />
              </el-form-item>
              <el-form-item label="加工结果">
                <el-input v-model="nodeTemplateData.processResult" />
              </el-form-item>
            </template>
            <template v-else-if="nodeForm.stage === 'WAREHOUSE'">
              <el-form-item label="仓库名称">
                <el-input v-model="nodeTemplateData.warehouseName" />
              </el-form-item>
            </template>
            <template v-else-if="nodeForm.stage === 'SALE'">
              <el-form-item label="销售渠道">
                <el-input v-model="nodeTemplateData.saleChannel" />
              </el-form-item>
            </template>
            <template v-else-if="nodeForm.stage === 'INSPECT'">
              <el-form-item label="检测机构">
                <el-input v-model="nodeTemplateData.inspectAgency" />
              </el-form-item>
              <el-form-item label="检测结论">
                <el-input v-model="nodeTemplateData.inspectConclusion" />
              </el-form-item>
            </template>
          </div>

          <el-form-item label="详细说明">
            <el-input v-model="nodeForm.content" type="textarea" :rows="3" placeholder="补充说明（将存入备注字段）" />
          </el-form-item>
          <el-form-item label="发生时间">
            <el-date-picker v-model="nodeForm.eventTime" type="datetime" class="full-width" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item label="消费者可见">
            <el-switch v-model="nodeForm.isPublic" active-text="公开" inactive-text="内部" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="nodeDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingNode" @click="saveNode">确认创建</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="traceQrDialogVisible" title="追溯二维码预览" width="360px">
        <div class="trace-qr-dialog-body">
          <img v-if="traceQrImageUrl" :src="traceQrImageUrl" alt="追溯二维码" class="trace-qr-image" />
          <el-empty v-else description="二维码加载中" :image-size="40" />
          <div class="trace-token-text">追溯码：{{ currentQrToken || '-' }}</div>
          <div class="trace-link-text">{{ currentTraceLink || '-' }}</div>
        </div>
        <template #footer>
          <el-button @click="traceQrDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="copyTraceLink">复制追溯链接</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, OfficeBuilding } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { useAuthStore } from '../../stores/auth'
import { useBatchDetail } from '../../composables/useBatchDetail'
import { saveBatchParticipantsApi, updateBatchApi } from '../../api/batch'
import { createEventApi } from '../../api/event'
import { listQualityReportApi } from '../../api/quality'
import { listRegulationRecordApi } from '../../api/regulation'
import { listQrApi } from '../../api/qr'
import { listCompanyApi } from '../../api/company'
import {
  buildTraceLink,
  formatDateTime,
  getBatchStatusTagType,
  getBatchStatusText,
  getBizRoleText as mapBizRoleText,
  getNodeTypeText as mapNodeTypeText,
  getQualityResultTagType,
  getQualityResultText as mapQualityResultText,
  getRegulationStatusTagType,
  getRegulationStatusText as mapRegulationStatusText,
  normalizeDisplayText
} from '../../utils/display'

const route = useRoute()
const authStore = useAuthStore()
const roleCode = computed(() => authStore.user?.roleCode || '')

const batchId = computed(() => route.params.id)
const { batch, loading: batchLoading, error: batchError, loadBatch } = useBatchDetail(batchId)
const qualityReports = ref([])
const regulationRecords = ref([])
const qrList = ref([])
const nodeExpandedMap = ref({})

const canEdit = computed(() => ['ENTERPRISE_ADMIN', 'ENTERPRISE_USER', 'ADMIN'].includes(roleCode.value))
const canManageParticipants = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER', 'ADMIN'].includes(roleCode.value))
const canAddNode = computed(() => canEdit.value)
const canPublish = computed(() => canEdit.value && batch.value && batch.value.status === 'DRAFT')

const saving = ref(false)
const publishing = ref(false)
const editBatchDialogVisible = ref(false)
const participantDialogVisible = ref(false)
const savingParticipants = ref(false)
const nodeDialogVisible = ref(false)
const savingNode = ref(false)
const traceQrDialogVisible = ref(false)
const traceQrImageUrl = ref('')

const participantRows = computed(() => batch.value?.participants || [])
const nodeRows = computed(() => batch.value?.nodes || [])
const participantEditRows = ref([])
const companyOptions = ref([])
const canSelectCompany = computed(() => roleCode.value === 'PLATFORM_ADMIN')
const currentQr = computed(() => qrList.value[0] || null)
const currentQrToken = computed(() => currentQr.value?.qrToken || '')
const currentTraceLink = computed(() => buildTraceLink(currentQrToken.value))
const summaryBatchCode = computed(() => normalizeText(batch.value?.batchCode, '未生成批次编码'))
const summaryProductName = computed(() => normalizeText(batch.value?.productName, '未关联产品'))
const summaryCompanyName = computed(() => normalizeText(batch.value?.creatorCompanyName, '暂无企业信息'))
const summaryStartDate = computed(() => normalizeText(batch.value?.startDate, '未填写'))
const summaryCreatedAt = computed(() => formatTime(batch.value?.createdAt))

const editForm = reactive({
  id: null,
  productId: null,
  originPlace: '',
  startDate: '',
  remark: '',
  publicRemark: '',
  internalRemark: ''
})

const nodeForm = reactive({
  stage: 'TRANSPORT',
  bizRole: '',
  title: '',
  location: '',
  content: '',
  eventTime: '',
  isPublic: true
})

const nodeTemplateData = reactive({
  operationName: '',
  fromAddress: '',
  toAddress: '',
  processName: '',
  processResult: '',
  warehouseName: '',
  saleChannel: '',
  inspectAgency: '',
  inspectConclusion: ''
})

const stageLabelMap = {
  PRODUCE: '生产',
  TRANSPORT: '运输',
  PROCESS: '加工',
  WAREHOUSE: '仓储',
  SALE: '销售',
  INSPECT: '质检'
}

const stageTitleMap = {
  PRODUCE: '生产记录',
  TRANSPORT: '运输记录',
  PROCESS: '加工记录',
  WAREHOUSE: '仓储记录',
  SALE: '销售记录',
  INSPECT: '质检记录'
}

const stageRoleMap = {
  PRODUCE: 'PRODUCER',
  TRANSPORT: 'TRANSPORTER',
  PROCESS: 'PROCESSOR',
  WAREHOUSE: 'WAREHOUSE',
  SALE: 'SELLER',
  INSPECT: 'INSPECTOR'
}

const bizRoleOptions = [
  { label: '生产商 (PRODUCER)', value: 'PRODUCER' },
  { label: '种植/养殖户 (FARMER)', value: 'FARMER' },
  { label: '承运商 (TRANSPORTER)', value: 'TRANSPORTER' },
  { label: '加工厂 (PROCESSOR)', value: 'PROCESSOR' },
  { label: '销售商 (SELLER)', value: 'SELLER' },
  { label: '仓储中心 (WAREHOUSE)', value: 'WAREHOUSE' },
  { label: '检测机构 (INSPECTOR)', value: 'INSPECTOR' }
]

const eventFieldLabelMap = {
  operationName: '作业名称',
  fromAddress: '始发地',
  toAddress: '目的地',
  processName: '加工环节',
  processResult: '加工结果',
  warehouseName: '仓库名称',
  saleChannel: '销售渠道',
  inspectAgency: '检测机构',
  inspectConclusion: '检测结论',
  workType: '作业类型',
  remark: '备注'
}

const normalizeText = (value, fallback = '-') => normalizeDisplayText(value, fallback)
const formatTime = (time) => formatDateTime(time)
const formatShortTime = (time) => {
  const text = formatDateTime(time)
  return text === '-' ? text : text.slice(0, 10)
}

const getStatusText = (status) => getBatchStatusText(status)
const getStatusType = (status) => getBatchStatusTagType(status)
const getRegulationStatusText = (status) => mapRegulationStatusText(status)
const getRegulationStatusType = (status) => getRegulationStatusTagType(status)
const getQualityResultText = (result) => mapQualityResultText(result)
const getQualityResultType = (result) => getQualityResultTagType(result)
const getBizRoleText = (role) => mapBizRoleText(role)
const getNodeTypeText = (type) => mapNodeTypeText(type)

const getRegulationResultType = (result) => {
  const text = String(result || '')
  if (!text) return 'info'
  if (text.includes('合格') || text.includes('正常') || text.toUpperCase().includes('PASS')) return 'success'
  if (text.includes('整改') || text.includes('预警')) return 'warning'
  return 'danger'
}

const getRegulationResultText = (result) => {
  const mapped = mapRegulationStatusText(result)
  return mapped === '未知状态' ? normalizeText(result) : mapped
}

const parseNodeContent = (contentStr) => {
  if (!contentStr) return {}
  try {
    const raw = JSON.parse(contentStr)
    const fields = raw.fields || raw
    const result = {}
    Object.entries(fields).forEach(([key, val]) => {
      result[eventFieldLabelMap[key] || key] = normalizeText(val)
    })
    return result
  } catch (e) {
    return { 原始内容: normalizeText(contentStr) }
  }
}

const truncateText = (value, length = 30) => {
  const text = normalizeText(value)
  return text.length > length ? `${text.slice(0, length)}...` : text
}

const getNodeKey = (node) => String(node.id || `${node.title}-${node.eventTime}`)
const isNodeExpanded = (node) => Boolean(nodeExpandedMap.value[getNodeKey(node)])
const toggleNodeExpanded = (node) => {
  const key = getNodeKey(node)
  nodeExpandedMap.value = { ...nodeExpandedMap.value, [key]: !nodeExpandedMap.value[key] }
}

const getNodeFields = (node) => Object.entries(parseNodeContent(node.content)).map(([label, value], index) => {
  const fullValue = normalizeText(value)
  const displayValue = truncateText(fullValue)
  return {
    key: `${getNodeKey(node)}-${index}`,
    label,
    fullValue,
    displayValue,
    truncated: displayValue !== fullValue
  }
})
const getNodeContentEntries = (node) => isNodeExpanded(node) ? getNodeFields(node) : getNodeFields(node).slice(0, 3)
const hasHiddenNodeFields = (node) => getNodeFields(node).length > 3
const getNodeHiddenCount = (node) => Math.max(getNodeFields(node).length - 3, 0)

const loadQualityAndRegulation = async () => {
  if (!batchId.value) return
  try {
    const [qRes, rRes] = await Promise.all([listQualityReportApi(batchId.value), listRegulationRecordApi(batchId.value)])
    if (qRes.code === 0) qualityReports.value = qRes.data || []
    if (rRes.code === 0) regulationRecords.value = rRes.data || []
  } catch (e) {
    console.warn('加载关联数据失败', e)
  }
}

const loadQrList = async () => {
  if (!batchId.value) return
  try {
    const res = await listQrApi(batchId.value, roleCode.value)
    if (res.code === 0) qrList.value = (res.data || []).filter((item) => item?.qrToken)
  } catch (e) {
    qrList.value = []
  }
}

const openTraceLink = () => {
  if (!currentTraceLink.value) return ElMessage.warning('当前批次暂无追溯链接')
  window.open(currentTraceLink.value, '_blank')
}

const copyTraceLink = async () => {
  if (!currentTraceLink.value) return ElMessage.warning('当前批次暂无追溯链接')
  try {
    await navigator.clipboard.writeText(currentTraceLink.value)
    ElMessage.success('追溯链接已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const copyTraceToken = async () => {
  if (!currentQrToken.value) return ElMessage.warning('当前批次暂无追溯码')
  try {
    await navigator.clipboard.writeText(currentQrToken.value)
    ElMessage.success('追溯码已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const showTraceQrPreview = async () => {
  if (!currentTraceLink.value) return ElMessage.warning('当前批次暂无追溯链接')
  traceQrDialogVisible.value = true
  traceQrImageUrl.value = ''
  try {
    traceQrImageUrl.value = await QRCode.toDataURL(currentTraceLink.value, { width: 220, margin: 2 })
  } catch (e) {
    ElMessage.error('二维码预览生成失败')
  }
}

const loadCompanyOptions = async () => {
  try {
    const res = await listCompanyApi()
    if (res.code === 0) companyOptions.value = res.data || []
  } catch (e) {
    companyOptions.value = []
  }
}

const getCompanyNameById = (companyId) => {
  if (!companyId) return '请先输入企业 ID'
  const fromOptions = companyOptions.value.find((item) => Number(item.id) === Number(companyId))
  if (fromOptions?.name) return `企业名称：${fromOptions.name}`
  const fromParticipants = participantRows.value.find((item) => Number(item.companyId) === Number(companyId))
  if (fromParticipants?.companyName) return `企业名称：${fromParticipants.companyName}`
  return '未匹配到企业名称，可检查企业 ID'
}

watch(batch, (newVal) => {
  if (!newVal) return
  editForm.id = newVal.id
  editForm.productId = newVal.productId
  editForm.originPlace = newVal.originPlace
  editForm.startDate = newVal.startDate
  editForm.remark = newVal.remark
  editForm.publicRemark = newVal.publicRemark
  editForm.internalRemark = newVal.internalRemark
  nodeExpandedMap.value = {}
  loadQualityAndRegulation()
  loadQrList()
})

const openEditBatchDialog = () => {
  editBatchDialogVisible.value = true
}

const handleSaveBatch = async () => {
  saving.value = true
  try {
    const res = await updateBatchApi(editForm, roleCode.value)
    if (res.code === 0) {
      ElMessage.success('保存成功')
      editBatchDialogVisible.value = false
      await loadBatch()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const handlePublish = async () => {
  publishing.value = true
  try {
    const res = await updateBatchApi({ id: batch.value.id, status: 'ACTIVE' }, roleCode.value)
    if (res.code === 0) {
      ElMessage.success('发布成功')
      await loadBatch()
    }
  } finally {
    publishing.value = false
  }
}

const openParticipantDialog = () => {
  participantEditRows.value = (participantRows.value || []).map((item) => ({ ...item }))
  loadCompanyOptions()
  participantDialogVisible.value = true
}

const resetParticipantDialog = () => {
  participantEditRows.value = []
}

const addParticipantRow = () => {
  participantEditRows.value.push({ companyId: null, bizRole: 'TRANSPORTER', stageOrder: 0, isCreator: false, remark: '' })
}

const removeParticipantRow = (index) => {
  participantEditRows.value.splice(index, 1)
}

const saveParticipants = async () => {
  savingParticipants.value = true
  try {
    const res = await saveBatchParticipantsApi({ batchId: batch.value.id, participants: participantEditRows.value }, roleCode.value)
    if (res.code === 0) {
      ElMessage.success('保存成功')
      participantDialogVisible.value = false
      await loadBatch()
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    savingParticipants.value = false
  }
}

const openNodeDialog = () => {
  nodeForm.stage = 'TRANSPORT'
  nodeForm.bizRole = 'TRANSPORTER'
  nodeForm.title = '运输记录'
  nodeForm.location = ''
  nodeForm.content = ''
  nodeForm.eventTime = new Date().toISOString().slice(0, 19)
  nodeForm.isPublic = true
  if (participantRows.value?.length) {
    const me = participantRows.value.find((item) => item.isCreator)
    if (me) nodeForm.bizRole = me.bizRole
  }
  Object.keys(nodeTemplateData).forEach((key) => {
    nodeTemplateData[key] = ''
  })
  nodeDialogVisible.value = true
}

const handleStageChange = (val) => {
  if (!val) return
  nodeForm.title = stageTitleMap[val] || ''
  nodeForm.bizRole = stageRoleMap[val] || ''
}

const resetNodeDialog = () => {}

const saveNode = async () => {
  if (!nodeForm.title) return ElMessage.error('请输入标题')
  savingNode.value = true
  try {
    const fields = {}
    if (nodeForm.content) fields.remark = nodeForm.content
    Object.entries(nodeTemplateData).forEach(([key, value]) => {
      if (value) fields[key] = value
    })
    const payload = {
      batchId: batch.value.id,
      stage: nodeForm.stage,
      bizRole: nodeForm.bizRole,
      title: nodeForm.title,
      eventTime: nodeForm.eventTime,
      location: nodeForm.location,
      isPublic: nodeForm.isPublic,
      contentJson: JSON.stringify({ fields }),
      attachmentsJson: '[]'
    }
    const res = await createEventApi(payload, roleCode.value)
    if (res.code === 0) {
      ElMessage.success('节点创建成功')
      nodeDialogVisible.value = false
      await loadBatch()
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    savingNode.value = false
  }
}

onMounted(() => {
  loadBatch()
})
</script>

<style scoped>
.batch-detail-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.overview-card,
.section-card {
  margin-bottom: 20px;
  border-radius: 16px;
}

.page-state-card {
  min-height: 260px;
}

.page-alert {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header-left,
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.batch-code {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.status-tag {
  font-weight: 500;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}

.section-subtitle {
  margin: 6px 0 0 14px;
  color: #6b7280;
  font-size: 12px;
}

.summary-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fbfdff;
}

.summary-label {
  margin-bottom: 6px;
  font-size: 12px;
  color: #6b7280;
}

.summary-value {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
}

.summary-mono {
  font-family: 'Consolas', 'Courier New', monospace;
}

.summary-item-status {
  background: linear-gradient(135deg, #f8fbff 0%, #eff6ff 100%);
}

.summary-statuses {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-entry-card {
  border: 1px solid #dbeafe;
  background: linear-gradient(135deg, #f8fbff 0%, #eff6ff 100%);
}

.trace-entry-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.trace-entry-note {
  padding: 10px 12px;
  border: 1px dashed #bfdbfe;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1.6;
}

.trace-entry-meta {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
}

.trace-entry-block {
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid #dbeafe;
}

.trace-entry-label {
  display: block;
  margin-bottom: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.trace-entry-value {
  color: #111827;
  word-break: break-all;
}

.trace-entry-token {
  font-size: 18px;
  font-weight: 700;
}

.trace-entry-link {
  font-size: 13px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trace-entry-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.trace-token-text {
  font-size: 13px;
  color: #374151;
}

.trace-link-text {
  color: #4b5563;
  font-size: 13px;
  word-break: break-all;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
}

.participant-dialog-tip {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  color: #475569;
  font-size: 13px;
}

.participant-edit-table :deep(.el-input__wrapper),
.participant-edit-table :deep(.el-select__wrapper),
.participant-edit-table :deep(.el-textarea__inner),
.participant-order-input {
  width: 100%;
}

.company-id-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.company-name-tip,
.cell-muted {
  font-size: 12px;
  color: #6b7280;
}

.creator-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.trace-qr-dialog-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.trace-qr-image {
  width: 220px;
  height: 220px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.full-width {
  width: 100%;
}

.template-fields-area {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.node-timeline {
  padding-left: 10px;
  margin-top: 10px;
}

.node-item-card {
  border: 1px solid #ebeef5;
  border-radius: 14px;
}

.node-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.node-title {
  font-weight: 700;
  font-size: 18px;
  color: #111827;
}

.node-company {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
}

.node-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.node-content {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.content-field {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 10px;
  font-size: 13px;
  line-height: 1.7;
}

.field-label {
  color: #909399;
  font-weight: 600;
}

.field-value {
  color: #303133;
}

.node-expand {
  margin-top: 10px;
}

.mr-1 {
  margin-right: 4px;
}

.mt-2 {
  margin-top: 8px;
}

@media (max-width: 960px) {
  .card-header,
  .node-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .trace-entry-meta {
    grid-template-columns: 1fr;
  }

  .content-field {
    grid-template-columns: 1fr;
    gap: 2px;
  }
}
</style>
