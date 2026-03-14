<template>
  <div class="batch-detail-page" v-loading="batchLoading">
    <el-card v-if="batchError && !batch" shadow="never" class="overview-card">
      <el-result icon="error" title="加载失败" :sub-title="batchError">
        <template #extra>
          <el-button type="primary" @click="loadBatch">重新加载</el-button>
        </template>
      </el-result>
    </el-card>
    <template v-else>
    <!-- 1. 顶部概览与状态区 -->
    <el-card class="overview-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="batch-code">{{ batch?.batchCode }}</span>
            <el-tag :type="getStatusType(batch?.status)" effect="dark" class="status-tag">
              {{ getStatusText(batch?.status) }}
            </el-tag>
            <el-tag :type="getRegulationStatusType(batch?.regulationStatus)" effect="plain" class="status-tag">
              {{ getRegulationStatusText(batch?.regulationStatus) }}
            </el-tag>
          </div>
          <div class="header-actions">
            <el-button v-if="canEdit" type="primary" @click="openEditBatchDialog">编辑批次信息</el-button>
            <el-button 
              v-if="canPublish" 
              type="success" 
              :loading="publishing" 
              @click="handlePublish"
            >
              发布批次
            </el-button>
          </div>
        </div>
      </template>
      
      <el-descriptions :column="3" border>
        <el-descriptions-item label="产品名称">{{ batch?.productName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属企业">{{ batch?.creatorCompanyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ batch?.originPlace || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ batch?.startDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(batch?.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="对外说明">{{ batch?.publicRemark || '暂无' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="overview-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="section-title">公开追溯入口</span>
          <el-button size="small" @click="loadQrList">刷新追溯码</el-button>
        </div>
      </template>
      <div v-if="currentQrToken" class="trace-entry-row">
        <el-tag type="success" effect="plain">消费者扫码后进入此页面</el-tag>
        <div class="trace-token-text">追溯码：{{ currentQrToken }}</div>
        <div class="trace-link-text">{{ currentTraceLink }}</div>
        <div class="trace-entry-actions">
          <el-button type="primary" @click="openTraceLink">查看公开追溯页</el-button>
          <el-button @click="copyTraceLink">复制追溯链接</el-button>
          <el-button @click="copyTraceToken">复制追溯码</el-button>
          <el-button @click="showTraceQrPreview">预览二维码</el-button>
        </div>
      </div>
      <el-empty v-else description="当前批次暂无公开追溯码，请先在二维码管理中生成后再查看" :image-size="52" />
    </el-card>

    <el-row :gutter="20" class="main-content">
      <!-- 左侧：参与主体 + 质检信息 -->
      <el-col :span="9">
        <!-- 2. 参与主体区 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="section-title">参与主体</span>
              <el-button v-if="canManageParticipants" type="primary" link @click="openParticipantDialog">
                维护参与企业
              </el-button>
            </div>
          </template>

          <el-table :data="participantRows" stripe style="width: 100%" size="small" empty-text="暂无参与主体信息">
            <el-table-column prop="companyName" label="企业名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="bizRole" label="角色" width="100">
              <template #default="{ row }">
                {{ getBizRoleText(row.bizRole) }}
              </template>
            </el-table-column>
            <el-table-column label="标识" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isCreator" size="small" type="success" effect="plain">创建者</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 3. 质检报告区 -->
        <el-card class="section-card" shadow="never" style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <span class="section-title">质检报告</span>
            </div>
          </template>
          <el-table :data="qualityReports" style="width: 100%" size="small" empty-text="暂无质检信息">
            <el-table-column prop="result" label="结论" width="80">
              <template #default="{ row }">
                <el-tag :type="getQualityResultType(row.result)" size="small">
                  {{ getQualityResultText(row.result) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="agency" label="检测机构" min-width="100" show-overflow-tooltip />
            <el-table-column prop="reportNo" label="报告编号" width="100" show-overflow-tooltip />
          </el-table>
        </el-card>

        <!-- 4. 监管记录区 -->
        <el-card class="section-card" shadow="never" style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <span class="section-title">监管记录</span>
            </div>
          </template>
          <el-table :data="regulationRecords" style="width: 100%" size="small" empty-text="暂无监管记录">
            <el-table-column prop="inspectResult" label="结论" width="80">
              <template #default="{ row }">
                <el-tag :type="getRegulationResultType(row.inspectResult)" size="small">
                  {{ getRegulationResultText(row.inspectResult) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="actionTaken" label="措施" min-width="100" show-overflow-tooltip />
            <el-table-column label="时间" width="100">
              <template #default="{ row }">
                {{ formatShortTime(row.inspectTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：业务节点时间轴 -->
      <el-col :span="15">
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="section-title">业务追溯节点</span>
              <el-button v-if="canAddNode" type="primary" @click="openNodeDialog">
                <el-icon class="mr-1"><Plus /></el-icon>新增节点
              </el-button>
            </div>
          </template>

          <el-timeline v-if="nodeRows.length > 0" class="node-timeline">
            <el-timeline-item
              v-for="(node, index) in nodeRows"
              :key="index"
              :timestamp="formatTime(node.eventTime)"
              placement="top"
              :type="node.isPublic ? 'success' : 'info'"
              :hollow="!node.isPublic"
            >
              <el-card class="node-item-card" shadow="hover">
                <div class="node-header">
                  <span class="node-title">{{ node.title }}</span>
                  <div class="node-tags">
                    <el-tag size="small" effect="plain">{{ getNodeTypeText(node.nodeType) }}</el-tag>
                    <el-tag size="small" type="info" class="ml-1">{{ getBizRoleText(node.bizRole) }}</el-tag>
                    <el-tooltip content="消费者可见性" placement="top">
                      <el-icon v-if="node.isPublic" class="ml-2 text-success"><View /></el-icon>
                      <el-icon v-else class="ml-2 text-gray"><Hide /></el-icon>
                    </el-tooltip>
                  </div>
                </div>
                <div class="node-company text-gray text-sm mt-1">
                  <el-icon><OfficeBuilding /></el-icon> {{ node.companyName }}
                </div>
                <div class="node-content mt-2">
                  <!-- 解析并展示结构化内容 -->
                  <div v-for="(val, key) in parseNodeContent(node.content)" :key="key" class="content-field">
                    <span class="field-label">{{ key }}：</span>
                    <span class="field-value">{{ val }}</span>
                  </div>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无业务节点，请点击上方按钮添加" />
        </el-card>
      </el-col>
    </el-row>

    </template>
    <!-- Dialog: 编辑批次 -->
    <el-dialog v-model="editBatchDialogVisible" title="编辑批次信息" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="产品ID">
          <el-input-number v-model="editForm.productId" :min="1" class="full-width" disabled />
          <span class="form-tip">（产品关联暂不可修改）</span>
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

    <!-- Dialog: 维护参与企业 (保持原有逻辑) -->
    <el-dialog
      v-model="participantDialogVisible"
      title="维护参与企业"
      width="800px"
      @closed="resetParticipantDialog"
    >
      <el-table :data="participantEditRows" border>
        <el-table-column label="企业ID" width="160">
          <template #default="{ row }">
            <el-select v-if="canSelectCompany" v-model="row.companyId" class="full-width" filterable placeholder="请选择企业">
              <el-option
                v-for="item in companyOptions"
                :key="item.id"
                :label="`${item.name || '未命名企业'} (ID:${item.id})`"
                :value="item.id"
              />
            </el-select>
            <div v-else class="company-id-wrap">
              <el-input-number v-model="row.companyId" :min="1" class="full-width" controls-position="right" placeholder="请输入企业ID，例如 1" />
              <div class="company-name-tip">
                {{ getCompanyNameById(row.companyId) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="业务角色" width="180">
          <template #default="{ row }">
            <el-select v-model="row.bizRole" class="full-width">
              <el-option
                v-for="role in bizRoleOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="顺序" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.stageOrder" :min="0" class="full-width" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="创建方" width="100" align="center">
          <template #default="{ row }">
            <div class="creator-cell">
              <el-switch v-model="row.isCreator" />
              <span>{{ row.isCreator ? '是' : '否' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.remark" />
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
          <el-icon><Plus /></el-icon> 新增一行
        </el-button>
      </div>
      <template #footer>
        <el-button @click="participantDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingParticipants" @click="saveParticipants">保存</el-button>
      </template>
    </el-dialog>

    <!-- Dialog: 新增业务节点 (保持模板化录入逻辑) -->
    <el-dialog
      v-model="nodeDialogVisible"
      title="新增业务节点"
      width="700px"
      @closed="resetNodeDialog"
    >
      <el-form :model="nodeForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="节点类型">
              <el-select v-model="nodeForm.stage" class="full-width" @change="handleStageChange">
                <el-option
                  v-for="(label, value) in stageLabelMap"
                  :key="value"
                  :label="label"
                  :value="value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务角色">
              <el-select v-model="nodeForm.bizRole" class="full-width" placeholder="请选择">
                <el-option
                  v-for="role in bizRoleOptions"
                  :key="role.value"
                  :label="role.label"
                  :value="role.value"
                />
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

        <!-- 动态模板字段 -->
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
          <el-date-picker
            v-model="nodeForm.eventTime"
            type="datetime"
            class="full-width"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, View, Hide, OfficeBuilding } from '@element-plus/icons-vue'
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
  getRegulationStatusText as mapRegulationStatusText
} from '../../utils/display'

const route = useRoute()
const authStore = useAuthStore()
const roleCode = computed(() => authStore.user?.roleCode || '')

const batchId = computed(() => route.params.id)
const { batch, loading: batchLoading, error: batchError, loadBatch } = useBatchDetail(batchId)
const qualityReports = ref([])
const regulationRecords = ref([])
const qrList = ref([])

// 权限控制
const canEdit = computed(() => ['ENTERPRISE_ADMIN', 'ENTERPRISE_USER', 'ADMIN'].includes(roleCode.value))
const canManageParticipants = computed(() => ['PLATFORM_ADMIN', 'ENTERPRISE_ADMIN', 'ENTERPRISE_USER', 'ADMIN'].includes(roleCode.value))
const canAddNode = computed(() => canEdit.value)
const canPublish = computed(() => canEdit.value && batch.value && batch.value.status === 'DRAFT')

// 状态
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

// 表单
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

// 字典映射
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

// 辅助函数
const formatTime = (time) => formatDateTime(time)

const formatShortTime = (time) => {
  if (!time) return '-'
  return formatDateTime(time).slice(0, 10)
}

const getStatusText = (status) => getBatchStatusText(status)
const getStatusType = (status) => getBatchStatusTagType(status)
const getRegulationStatusText = (status) => mapRegulationStatusText(status)
const getRegulationStatusType = (status) => getRegulationStatusTagType(status)
const getQualityResultText = (result) => mapQualityResultText(result)
const getQualityResultType = (result) => getQualityResultTagType(result)

const getRegulationResultType = (result) => {
  if (!result) return 'info'
  if (result.includes('合格') || result.includes('正常') || result.includes('PASS')) return 'success'
  if (result.includes('整改') || result.includes('预警')) return 'warning'
  return 'danger'
}

const getRegulationResultText = (result) => {
  const mapped = mapRegulationStatusText(result)
  return mapped === '-' ? (result || '-') : mapped
}

const getBizRoleText = (role) => mapBizRoleText(role)
const getNodeTypeText = (type) => mapNodeTypeText(type)

// 解析节点内容JSON
const parseNodeContent = (contentStr) => {
  if (!contentStr) return {}
  try {
    const raw = JSON.parse(contentStr)
    // 兼容 {"fields": {...}} 结构
    const fields = raw.fields || raw
    
    // 转换 Key 为中文 Label
    const result = {}
    Object.entries(fields).forEach(([key, val]) => {
      const label = eventFieldLabelMap[key] || key
      result[label] = val
    })
    return result
  } catch (e) {
    return { '原始内容': contentStr }
  }
}

// 业务逻辑
const loadQualityAndRegulation = async () => {
  if (!batchId.value) return
  try {
    const [qRes, rRes] = await Promise.all([
      listQualityReportApi(batchId.value),
      listRegulationRecordApi(batchId.value)
    ])
    if (qRes.code === 0) qualityReports.value = qRes.data || []
    if (rRes.code === 0) regulationRecords.value = rRes.data || []
  } catch (e) {
    console.warn('加载关联数据失败', e)
  }
}

const loadQrList = async () => {
  if (!batchId.value) return
  try {
    const res = await listQrApi(batchId.value)
    if (res.code === 0) {
      qrList.value = (res.data || []).filter(item => item?.qrToken)
    }
  } catch (e) {
    qrList.value = []
  }
}

const openTraceLink = () => {
  if (!currentTraceLink.value) {
    ElMessage.warning('当前批次暂无追溯链接')
    return
  }
  window.open(currentTraceLink.value, '_blank')
}

const copyTraceLink = async () => {
  if (!currentTraceLink.value) {
    ElMessage.warning('当前批次暂无追溯链接')
    return
  }
  try {
    await navigator.clipboard.writeText(currentTraceLink.value)
    ElMessage.success('追溯链接已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const copyTraceToken = async () => {
  if (!currentQrToken.value) {
    ElMessage.warning('当前批次暂无追溯码')
    return
  }
  try {
    await navigator.clipboard.writeText(currentQrToken.value)
    ElMessage.success('追溯码已复制')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const showTraceQrPreview = async () => {
  if (!currentTraceLink.value) {
    ElMessage.warning('当前批次暂无追溯链接')
    return
  }
  traceQrDialogVisible.value = true
  traceQrImageUrl.value = ''
  try {
    traceQrImageUrl.value = await QRCode.toDataURL(currentTraceLink.value, {
      width: 220,
      margin: 2
    })
  } catch (e) {
    ElMessage.error('二维码预览生成失败')
  }
}

const loadCompanyOptions = async () => {
  if (!canSelectCompany.value) return
  try {
    const res = await listCompanyApi()
    if (res.code === 0) {
      companyOptions.value = res.data || []
    }
  } catch (e) {
    companyOptions.value = []
  }
}

const getCompanyNameById = (companyId) => {
  if (!companyId) return '请先输入企业ID'
  const fromOptions = companyOptions.value.find(item => Number(item.id) === Number(companyId))
  if (fromOptions?.name) return `企业名称：${fromOptions.name}`
  const fromParticipants = participantRows.value.find(item => Number(item.companyId) === Number(companyId))
  if (fromParticipants?.companyName) return `企业名称：${fromParticipants.companyName}`
  return '未匹配到企业名称，可检查企业ID'
}

// 监听批次加载，填充表单
watch(batch, (newVal) => {
  if (newVal) {
    editForm.id = newVal.id
    editForm.productId = newVal.productId
    editForm.originPlace = newVal.originPlace
    editForm.startDate = newVal.startDate
    editForm.remark = newVal.remark
    editForm.publicRemark = newVal.publicRemark
    editForm.internalRemark = newVal.internalRemark
    
    // 加载关联数据
    loadQualityAndRegulation()
    loadQrList()
  }
})

// 操作处理
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

// 参与主体管理
const openParticipantDialog = () => {
  participantEditRows.value = (participantRows.value || []).map(p => ({ ...p }))
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
    const res = await saveBatchParticipantsApi({
      batchId: batch.value.id,
      participants: participantEditRows.value
    }, roleCode.value)
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

// 节点管理
const openNodeDialog = () => {
  nodeForm.stage = 'TRANSPORT'
  nodeForm.bizRole = 'TRANSPORTER'
  nodeForm.title = '运输记录'
  nodeForm.location = ''
  nodeForm.content = ''
  nodeForm.eventTime = new Date().toISOString().slice(0, 19)
  nodeForm.isPublic = true
  
  // 智能建议角色
  if (participantRows.value?.length) {
    const me = participantRows.value.find(p => p.isCreator) // 简化逻辑：默认选创建者角色，实际应取当前登录用户所在企业
    if (me) nodeForm.bizRole = me.bizRole
  }
  
  // 清空模板数据
  Object.keys(nodeTemplateData).forEach(k => nodeTemplateData[k] = '')
  
  nodeDialogVisible.value = true
}

const handleStageChange = (val) => {
  if (val) {
    nodeForm.title = stageTitleMap[val] || ''
    nodeForm.bizRole = stageRoleMap[val] || ''
  }
}

const resetNodeDialog = () => {
  // 保持默认值
}

const saveNode = async () => {
  if (!nodeForm.title) return ElMessage.error('请输入标题')
  
  savingNode.value = true
  try {
    // 组装内容
    const fields = {}
    if (nodeForm.content) fields.remark = nodeForm.content
    Object.entries(nodeTemplateData).forEach(([k, v]) => {
      if (v) fields[k] = v
    })
    
    const payload = {
      batchId: batch.value.id,
      stage: nodeForm.stage,
      bizRole: nodeForm.bizRole,
      title: nodeForm.title,
      eventTime: nodeForm.eventTime,
      location: nodeForm.location,
      isPublic: nodeForm.isPublic,
      contentJson: JSON.stringify({ fields }), // 严格符合 {"fields": {...}} 结构
      attachmentsJson: '[]'
    }
    
    const res = await createEventApi(payload, roleCode.value)
    if (res.code === 0) {
      ElMessage.success('节点创建成功')
      nodeDialogVisible.value = false
      await loadBatch() // 刷新列表
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

.overview-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-code {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.status-tag {
  font-weight: 500;
}

.trace-entry-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trace-entry-actions {
  display: flex;
  gap: 8px;
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

.company-id-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.company-name-tip {
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

.section-card {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}

.full-width {
  width: 100%;
}

.template-fields-area {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

/* Timeline Styles */
.node-timeline {
  padding-left: 10px;
  margin-top: 10px;
}

.node-item-card {
  border: 1px solid #ebeef5;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.node-title {
  font-weight: bold;
  font-size: 15px;
}

.node-tags {
  display: flex;
  align-items: center;
}

.text-success { color: #67c23a; }
.text-gray { color: #909399; }
.text-sm { font-size: 12px; }
.ml-1 { margin-left: 4px; }
.ml-2 { margin-left: 8px; }
.mr-1 { margin-right: 4px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }

.content-field {
  display: flex;
  font-size: 13px;
  line-height: 1.8;
}

.field-label {
  color: #909399;
  width: 70px;
  flex-shrink: 0;
}

.field-value {
  color: #606266;
  flex: 1;
}
</style>
