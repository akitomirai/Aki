const traceStageProfiles = {
  ARCHIVE: {
    label: '建档',
    defaultTitle: '完成批次建档',
    defaultOperator: '企业管理员',
    locationTemplates: ['企业档案室', '品控办公室', '总部资料中心'],
    summaryTemplates: ['完成批次建档并核对企业、产品与批次基础信息。', '补齐建档资料，准备进入现场补录与质检上传。']
  },
  PRODUCE: {
    label: '生产',
    defaultTitle: '记录生产关键节点',
    defaultOperator: '现场补录员',
    locationTemplates: ['一号种植基地', '采收作业区', '加工包装区'],
    summaryTemplates: ['完成本批次现场作业记录，原料与操作人已核对。', '本节点已完成补录，可继续补下一个生产或流转节点。']
  },
  QUALITY: {
    label: '质检',
    defaultTitle: '补充质检节点',
    defaultOperator: '质检员',
    locationTemplates: ['企业质检室', '第三方检测机构', '出厂质检点'],
    summaryTemplates: ['完成质检取样与结果登记，可继续上传质检摘要。', '已记录检测节点，结果与样品信息已留痕。']
  },
  TRANSPORT: {
    label: '运输',
    defaultTitle: '记录运输流转',
    defaultOperator: '物流负责人',
    locationTemplates: ['冷链发运区', '干线运输途中', '到货交接点'],
    summaryTemplates: ['本批次已完成运输交接，运输信息已补录。', '运输节点已记录，下一步可补仓储或出库信息。']
  },
  WAREHOUSE: {
    label: '仓储',
    defaultTitle: '记录仓储状态',
    defaultOperator: '仓库管理员',
    locationTemplates: ['成品冷库', '周转仓', '待发货仓位'],
    summaryTemplates: ['本批次已入库并完成仓位登记。', '仓储节点已补齐，当前批次状态清晰可追溯。']
  },
  DELIVERY: {
    label: '发运',
    defaultTitle: '记录出库发运',
    defaultOperator: '发货专员',
    locationTemplates: ['成品出库口', '分拨中心', '渠道交接点'],
    summaryTemplates: ['本批次已完成出库发运，去向与责任人已记录。', '发运信息已补录，可继续生成二维码或发布。']
  },
  MARKET: {
    label: '上市',
    defaultTitle: '记录上市销售',
    defaultOperator: '渠道管理员',
    locationTemplates: ['门店陈列区', '商超上架点', '电商发货仓'],
    summaryTemplates: ['本批次已进入销售环节，对外展示信息已准备。', '上市节点已记录，可同步检查二维码与公开页。']
  },
  REGULATION: {
    label: '监管',
    defaultTitle: '记录监管处理',
    defaultOperator: '监管联络员',
    locationTemplates: ['监管检查现场', '企业整改会议室', '追溯复核点'],
    summaryTemplates: ['已记录监管动作与处理意见，便于答辩演示说明。', '当前监管节点已补录，后续可继续更新处理进度。']
  }
}

export const stageOptions = Object.entries(traceStageProfiles).map(([value, profile]) => ({
  value,
  label: profile.label
}))

export const qualityOptions = [
  { value: 'PASS', label: '合格' },
  { value: 'FAIL', label: '不合格' },
  { value: 'REVIEW', label: '待复核' }
]

export const riskActionOptions = [
  { value: 'COMMENT', label: '补处理说明' },
  { value: 'RECTIFICATION', label: '补整改记录' },
  { value: 'PROCESSING', label: '标记处理中' },
  { value: 'RECTIFIED', label: '标记已整改' }
]

export function getTraceStageProfile(stage) {
  return traceStageProfiles[stage] ?? traceStageProfiles.PRODUCE
}

export function createBatchCode() {
  const now = new Date()
  return `BATCH${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`
}

export function todayString() {
  return new Date().toISOString().slice(0, 10)
}

export function currentDateTime() {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
}

export function createTraceForm(overrides = {}) {
  const stage = overrides.stage ?? 'PRODUCE'
  const profile = getTraceStageProfile(stage)
  return {
    stage,
    title: overrides.title ?? profile.defaultTitle,
    eventTime: overrides.eventTime ?? currentDateTime(),
    operatorName: overrides.operatorName ?? profile.defaultOperator,
    location: overrides.location ?? profile.locationTemplates[0],
    summary: overrides.summary ?? profile.summaryTemplates[0],
    imageUrl: overrides.imageUrl ?? '',
    attachmentIds: overrides.attachmentIds ?? [],
    uploadedFiles: overrides.uploadedFiles ?? [],
    visibleToConsumer: overrides.visibleToConsumer ?? true
  }
}

export function cloneTraceRecord(record, overrides = {}) {
  if (!record) {
    return createTraceForm(overrides)
  }
  return createTraceForm({
    stage: record.stageCode,
    title: record.title,
    eventTime: currentDateTime(),
    operatorName: record.operatorName,
    location: record.location,
    summary: record.summary,
    imageUrl: '',
    attachmentIds: [],
    uploadedFiles: [],
    visibleToConsumer: record.visibleToConsumer,
    ...overrides
  })
}

export function createQualityForm(overrides = {}) {
  return {
    reportNo: overrides.reportNo ?? '',
    agency: overrides.agency ?? '',
    result: overrides.result ?? 'PASS',
    reportTime: overrides.reportTime ?? currentDateTime(),
    highlightsText: overrides.highlightsText ?? '关键指标合格\n信息可用于公开页展示',
    attachmentIds: overrides.attachmentIds ?? [],
    uploadedFiles: overrides.uploadedFiles ?? []
  }
}

export function splitHighlights(text) {
  return text
    .split(/[\n,;，；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export function getFriendlyErrorMessage(error, fallback = '操作未完成，请稍后重试。') {
  const raw = error?.response?.data?.message || error?.message || ''
  if (!raw) {
    return fallback
  }
  if (/product does not belong/i.test(raw)) {
    return '所选产品不属于当前企业，请重新选择。'
  }
  if (/batch code/i.test(raw) && /exist/i.test(raw)) {
    return '批次号已存在，请更换后再创建。'
  }
  if (/quality/i.test(raw) && /upload/i.test(raw)) {
    return '还没有上传质检摘要，请先补齐质检信息。'
  }
  if (/qr/i.test(raw) && /generate/i.test(raw)) {
    return '还没有生成二维码，请先生成后再继续。'
  }
  return raw || fallback
}

export function getFriendlyUploadError(error, label = '文件') {
  const raw = error?.response?.data?.message || error?.message || ''
  if (/size|too large/i.test(raw)) {
    return `${label}太大了，建议压缩后再上传。`
  }
  if (/type|format|content/i.test(raw)) {
    return `${label}格式暂不支持，请换成常见图片或 PDF 后重试。`
  }
  return `${label}上传失败了，请检查网络或文件后再试一次。`
}
