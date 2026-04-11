export const batchStatusTransitions = {
  DRAFT: ['PUBLISHED'],
  PUBLISHED: ['FROZEN', 'RECALLED'],
  FROZEN: ['PUBLISHED', 'RECALLED'],
  RECALLED: []
}

export const defaultRecommendedActionByStatus = {
  DRAFT: {
    code: 'ADD_TRACE',
    label: '补录追溯',
    hint: '先补一条关键现场记录，再继续质检与二维码准备。'
  },
  PUBLISHED: {
    code: 'VIEW_PUBLIC',
    label: '查看公开页',
    hint: '重点核对公开页展示和工作台状态是否一致。'
  },
  FROZEN: {
    code: 'RISK_COMMENT',
    label: '补处理说明',
    hint: '先补风险说明与整改动作，再推进恢复发布。'
  },
  RECALLED: {
    code: 'RISK_RECTIFICATION',
    label: '补整改记录',
    hint: '召回态建议持续补整改记录，便于监管回查。'
  }
}

export function normalizeRecommendedCode(code) {
  return String(code || '').trim().toUpperCase()
}

export function normalizeBatchStatus(status) {
  return String(status || '').trim().toUpperCase()
}

export function allowedBatchStatusTargets(currentStatus) {
  const normalized = normalizeBatchStatus(currentStatus)
  return batchStatusTransitions[normalized] ?? []
}

export function canBatchStatusTransition(currentStatus, targetStatus) {
  const normalizedCurrent = normalizeBatchStatus(currentStatus)
  const normalizedTarget = normalizeBatchStatus(targetStatus)
  if (!normalizedCurrent || !normalizedTarget || normalizedCurrent === normalizedTarget || normalizedTarget === 'DRAFT') {
    return false
  }
  return allowedBatchStatusTargets(normalizedCurrent).includes(normalizedTarget)
}

export function resolveDefaultRecommendedAction(status) {
  return defaultRecommendedActionByStatus[normalizeBatchStatus(status)] ?? {
    code: 'VIEW_WORKBENCH',
    label: '查看工作台',
    hint: '优先回工作台核对当前状态和下一步。'
  }
}

export function resolvePublishBlockState(item = {}) {
  const status = normalizeBatchStatus(item.status)
  const qualityCode = String(item.qualityStatusCode || 'PENDING').trim().toUpperCase()
  const qrStatus = String(item.qrStatus || 'NOT_GENERATED').trim().toUpperCase()

  if (status === 'PUBLISHED') {
    return {
      allowed: false,
      code: 'ALREADY_PUBLISHED',
      reason: '当前批次已发布，无需重复操作。'
    }
  }

  if (!canBatchStatusTransition(status, 'PUBLISHED')) {
    return {
      allowed: false,
      code: 'INVALID_STATUS',
      reason: status === 'RECALLED'
        ? '已召回批次不可再变更状态'
        : '当前状态不支持发布或恢复发布'
    }
  }

  if (qualityCode === 'PENDING' || !qualityCode) {
    return {
      allowed: false,
      code: 'MISSING_QUALITY',
      reason: '发布前请先上传质检摘要'
    }
  }

  if (qualityCode === 'FAIL') {
    return {
      allowed: false,
      code: 'QUALITY_FAILED',
      reason: '检测结果不合格，不能发布'
    }
  }

  if (qrStatus === 'NOT_GENERATED') {
    return {
      allowed: false,
      code: 'MISSING_QR',
      reason: '发布前请先生成二维码'
    }
  }

  if (status === 'FROZEN' && !item.canResume) {
    return {
      allowed: false,
      code: 'RISK_NOT_CLEARED',
      reason: '请先补充处理意见并标记整改完成，再恢复发布'
    }
  }

  return {
    allowed: true,
    code: status === 'FROZEN' ? 'READY_TO_RESUME' : 'READY_TO_PUBLISH',
    reason: status === 'FROZEN'
      ? '整改与复核已完成，可以恢复发布。'
      : '质检与二维码已齐，可以发布批次。'
  }
}

export function mapBackendRecommendedRiskActionCode(code) {
  const normalized = normalizeRecommendedCode(code)
  return {
    RESUME: 'resume',
    RISK_COMMENT: 'comment',
    RISK_RECTIFICATION: 'rectification',
    RISK_PROCESSING: 'processing',
    RISK_RECTIFIED: 'rectified',
    VIEW_WORKBENCH: 'workbench',
    VIEW_PUBLIC: 'workbench',
    ADD_TRACE: 'workbench',
    UPLOAD_QUALITY: 'workbench',
    GENERATE_QR: 'workbench',
    PUBLISH: 'workbench'
  }[normalized] || ''
}

export function mapBackendRecommendedQualityActionCode(code) {
  const normalized = normalizeRecommendedCode(code)
  return {
    UPLOAD_QUALITY: 'upload',
    VIEW_QUALITY: 'report',
    VIEW_WORKBENCH: 'workbench',
    ADD_TRACE: 'workbench',
    GENERATE_QR: 'workbench',
    PUBLISH: 'workbench',
    VIEW_PUBLIC: 'workbench',
    RESUME: 'workbench',
    RISK_COMMENT: 'workbench',
    RISK_RECTIFICATION: 'workbench'
  }[normalized] || ''
}
