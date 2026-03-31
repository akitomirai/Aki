export const taskFilterOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'DRAFT', label: '草稿待续' },
  { value: 'DONE_TODAY', label: '今日已完成' }
]

export function resolveTaskStatusText(task = {}) {
  if (task?.draftPending || task?.hasDraft || String(task?.taskStatus || '').toUpperCase() === 'DRAFT') {
    return task?.draftStatusLabel || '草稿待续'
  }
  if (task?.todayCompleted || String(task?.taskStatus || '').toUpperCase() === 'COMPLETED') {
    return task?.taskStatusLabel || '今日已完成'
  }
  return task?.taskStatusLabel || '待处理'
}

export function resolveTodayStatusText(todayCompleted) {
  return todayCompleted ? '今日已完成' : '待处理'
}

export function resolveQrStatusText(qr = {}) {
  if (qr?.statusLabel) {
    return qr.statusLabel
  }
  if (qr?.generated) {
    return '已生成'
  }
  return qr?.status && qr.status !== 'NOT_GENERATED' ? '已生成' : '待生成'
}

export function resolveRiskStatusText(risk = {}, handling = {}) {
  return risk?.statusLabel || handling?.currentStageLabel || '当前无风险'
}
