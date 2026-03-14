export const roleTextMap = {
  PLATFORM_ADMIN: '平台管理员',
  ENTERPRISE_ADMIN: '企业管理员',
  ENTERPRISE_USER: '企业用户',
  REGULATOR: '监管人员',
  ADMIN: '系统管理员'
}

export const batchStatusTextMap = {
  DRAFT: '草稿',
  ACTIVE: '正常',
  FROZEN: '已冻结',
  RECALLED: '已召回'
}

export const regulationStatusTextMap = {
  NONE: '未纳入监管',
  NORMAL: '监管正常',
  PENDING_RECTIFY: '待整改',
  RISK: '风险预警',
  RECALLED: '已召回',
  FROZEN: '已冻结'
}

export const bizRoleTextMap = {
  PRODUCER: '生产商',
  FARMER: '种植/养殖户',
  TRANSPORTER: '承运商',
  PROCESSOR: '加工厂',
  WAREHOUSE: '仓储中心',
  SELLER: '销售商',
  INSPECTOR: '检测机构',
  REGULATOR: '监管机构',
  SYSTEM: '系统'
}

export const stageTextMap = {
  PRODUCE: '生产',
  TRANSPORT: '运输',
  PROCESS: '加工',
  WAREHOUSE: '仓储',
  SALE: '销售',
  INSPECT: '质检',
  SYSTEM: '系统'
}

export const nodeTypeTextMap = {
  PRODUCTION: '生产',
  TRANSPORT: '运输',
  PROCESSING: '加工',
  WAREHOUSING: '仓储',
  SELLING: '销售',
  QUALITY_CHECK: '质检',
  REGULATION: '监管',
  SYSTEM: '系统'
}

export function normalizeDisplayText(value, fallback = '-') {
  if (value == null) return fallback
  const text = String(value).trim()
  if (!text) return fallback

  const normalized = text.toLowerCase()
  const invalidValues = ['null', 'undefined', 'nan']
  if (invalidValues.includes(normalized)) return fallback

  const invalidFragments = ['????', '锟', '�']
  if (invalidFragments.some((item) => text.includes(item))) return fallback

  return text
}

export function normalizeDisplayNumber(value, fallback = '0') {
  if (value == null || value === '') return fallback
  const num = Number(value)
  if (Number.isNaN(num)) return fallback
  return String(num)
}

export function getRoleText(roleCode) {
  if (!roleCode) return '-'
  return roleTextMap[roleCode] || '未知角色'
}

export function getBatchStatusText(status) {
  if (!status) return '-'
  return batchStatusTextMap[status] || '未知状态'
}

export function getBatchStatusTagType(status) {
  const map = {
    DRAFT: 'info',
    ACTIVE: 'success',
    FROZEN: 'danger',
    RECALLED: 'danger'
  }
  return map[status] || 'info'
}

export function getRegulationStatusText(status) {
  if (!status) return '-'
  return regulationStatusTextMap[status] || '未知状态'
}

export function getRegulationStatusTagType(status) {
  const map = {
    NONE: 'info',
    NORMAL: 'success',
    PENDING_RECTIFY: 'warning',
    RISK: 'danger',
    RECALLED: 'danger',
    FROZEN: 'danger'
  }
  return map[status] || 'info'
}

export function getBizRoleText(role) {
  if (!role) return '-'
  return bizRoleTextMap[role] || '未知角色'
}

export function getStageText(stage) {
  if (!stage) return '-'
  return stageTextMap[stage] || '未知阶段'
}

export function getNodeTypeText(type) {
  if (!type) return '-'
  return nodeTypeTextMap[type] || '未知节点'
}

export function getQualityResultText(result) {
  const normalized = String(result || '').toUpperCase()
  if (['合格', 'PASS', 'PASSED', 'QUALIFIED', 'NORMAL'].includes(normalized)) return '合格'
  if (['不合格', 'FAIL', 'FAILED', 'UNQUALIFIED', 'RISK'].includes(normalized)) return '不合格'
  return result ? String(result) : '-'
}

export function getQualityResultTagType(result) {
  const normalized = String(result || '').toUpperCase()
  if (['合格', 'PASS', 'PASSED', 'QUALIFIED', 'NORMAL'].includes(normalized)) return 'success'
  if (['不合格', 'FAIL', 'FAILED', 'UNQUALIFIED', 'RISK'].includes(normalized)) return 'danger'
  return 'info'
}

export function getLogResultText(resultStatus) {
  const normalized = String(resultStatus || '').toUpperCase()
  if (normalized === 'SUCCESS') return '成功'
  if (normalized === 'FAIL' || normalized === 'ERROR') return '失败'
  return resultStatus ? '未知结果' : '-'
}

export function getLogResultTagType(resultStatus) {
  const normalized = String(resultStatus || '').toUpperCase()
  if (normalized === 'SUCCESS') return 'success'
  if (normalized === 'FAIL' || normalized === 'ERROR') return 'danger'
  return 'info'
}

export function getLogModuleText(module) {
  const map = {
    AUTH: '认证登录',
    USER: '用户管理',
    PRODUCT: '产品管理',
    BATCH: '批次管理',
    TRACE_EVENT: '业务节点',
    QUALITY: '质检管理',
    QR: '追溯码管理',
    FEEDBACK: '用户反馈',
    COMPANY: '企业管理',
    REGULATION: '监管记录',
    LOG: '操作日志'
  }
  if (!module) return '-'
  return map[module] || '未知模块'
}

export function getLogActionText(action) {
  const map = {
    LOGIN: '登录',
    REGISTER: '注册',
    CREATE_BATCH: '新建批次',
    UPDATE_BATCH: '修改批次',
    SAVE_BATCH_PARTICIPANTS: '维护参与主体',
    CREATE_TRACE_EVENT: '新增业务节点',
    GENERATE_QR: '生成追溯码',
    DISABLE_QR: '停用追溯码',
    CREATE_REPORT: '新增质检报告',
    UPDATE_REPORT: '修改质检报告',
    DELETE_REPORT: '删除质检报告',
    UPDATE_REGULATION_STATUS: '更新监管状态',
    CREATE_QUALITY_REPORT: '新增质检报告',
    UPDATE_QUALITY_REPORT: '修改质检报告',
    DELETE_QUALITY_REPORT: '删除质检报告'
  }
  if (!action) return '-'
  return map[action] || '未知操作'
}

export function getTraceOrigin() {
  const envOrigin = String(import.meta.env.VITE_TRACE_WEB_ORIGIN || '').trim()
  if (envOrigin) {
    return envOrigin.replace(/\/$/, '')
  }
  if (typeof window !== 'undefined' && window.location?.hostname) {
    const { protocol, hostname } = window.location
    return `${protocol}//${hostname}:5173`
  }
  return 'http://127.0.0.1:5173'
}

export function buildTraceLink(qrToken) {
  if (!qrToken) return ''
  return `${getTraceOrigin()}/t/${qrToken}`
}

export function formatDateTime(value) {
  const text = normalizeDisplayText(value, '-')
  if (text === '-') return text
  return text.replace('T', ' ')
}
