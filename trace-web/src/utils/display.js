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
  SOLD_OUT: '已售完',
  FROZEN: '已冻结',
  RECALLED: '已召回',
  INVALID: '已作废'
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

export const nodeTypeTextMap = {
  PRODUCTION: '生产环节',
  TRANSPORT: '物流环节',
  PROCESSING: '加工环节',
  WAREHOUSING: '仓储环节',
  SELLING: '销售环节',
  QUALITY_CHECK: '质检环节',
  REGULATION: '监管环节',
  SYSTEM: '系统记录'
}

export function normalizeDisplayText(value, fallback = '-') {
  if (value == null) return fallback
  const text = String(value).trim()
  if (!text) return fallback

  const normalized = text.toLowerCase()
  if (['null', 'undefined', 'nan'].includes(normalized)) return fallback
  if (['????', '锟', '�'].some((item) => text.includes(item))) return fallback

  return text
}

export function getStatusText(status) {
  if (!status) return '-'
  if (status in batchStatusTextMap) return batchStatusTextMap[status]
  if (status in regulationStatusTextMap) return regulationStatusTextMap[status]
  if (status === 'EXPIRED') return '已过期'
  if (status === 'DISABLED') return '已停用'
  return '未知状态'
}

export function getStatusTagType(status) {
  const map = {
    ACTIVE: 'success',
    NORMAL: 'success',
    NONE: 'info',
    SOLD_OUT: 'info',
    DRAFT: 'info',
    PENDING_RECTIFY: 'warning',
    EXPIRED: 'warning',
    FROZEN: 'danger',
    RECALLED: 'danger',
    INVALID: 'danger',
    RISK: 'danger',
    DISABLED: 'danger'
  }
  return map[status] || 'info'
}

export function getBizRoleText(role) {
  if (!role) return '-'
  return bizRoleTextMap[role] || '未知角色'
}

export function getNodeTypeText(type) {
  if (!type) return '-'
  return nodeTypeTextMap[type] || '未知节点'
}

export function getStageText(stage) {
  const map = {
    PRODUCE: '生产记录',
    TRANSPORT: '运输记录',
    PROCESS: '加工记录',
    WAREHOUSE: '仓储记录',
    SALE: '销售记录',
    INSPECT: '质检记录',
    SYSTEM: '系统记录'
  }
  if (!stage) return '-'
  return map[stage] || '未知阶段'
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

export function getRegulationResultTagType(result) {
  const text = String(result || '')
  if (!text) return 'info'
  if (text.includes('合格') || text.includes('正常') || text.toUpperCase().includes('PASS')) return 'success'
  if (text.includes('整改') || text.includes('预警')) return 'warning'
  return 'danger'
}

export function formatShortDate(time) {
  const text = normalizeDisplayText(time, '-')
  if (text === '-') return text
  return text.replace('T', ' ').slice(0, 16)
}
