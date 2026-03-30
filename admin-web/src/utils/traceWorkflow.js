const stageProfiles = {
  ARCHIVE: {
    label: '建档',
    defaultTitle: '完成批次建档',
    defaultOperator: '企业管理员',
    locations: ['企业办公室', '品控办公室', '资料中心'],
    summaries: ['已完成批次建档，基础信息已核对。', '建档信息已补齐，可继续录入现场节点。']
  },
  PRODUCE: {
    label: '生产',
    defaultTitle: '记录生产关键节点',
    defaultOperator: '现场操作员',
    locations: ['种植基地', '生产区', '加工区'],
    summaries: ['已完成本批次现场作业记录，原料与责任人已核对。', '生产节点已补录，可继续记录下一环节。']
  },
  QUALITY: {
    label: '质检',
    defaultTitle: '记录质检节点',
    defaultOperator: '质检员',
    locations: ['企业质检室', '第三方检测机构', '出厂抽检点'],
    summaries: ['已完成抽检与送检登记，可继续上传质检摘要。', '质检节点已补录，结论和样品信息已留痕。']
  },
  TRANSPORT: {
    label: '运输',
    defaultTitle: '记录运输交接',
    defaultOperator: '物流负责人',
    locations: ['冷链发运区', '运输途中', '到货交接点'],
    summaries: ['已完成运输交接，批次流转信息已补录。', '运输节点已记录，可继续补仓储或出库节点。']
  },
  WAREHOUSE: {
    label: '仓储',
    defaultTitle: '记录仓储状态',
    defaultOperator: '仓库管理员',
    locations: ['成品冷库', '周转库位', '待发货区'],
    summaries: ['批次已入库并完成库位登记。', '仓储节点已补齐，当前状态清晰可追溯。']
  },
  DELIVERY: {
    label: '发运',
    defaultTitle: '记录出库发运',
    defaultOperator: '发货专员',
    locations: ['成品出库口', '分拨中心', '渠道交接点'],
    summaries: ['已完成出库发运，去向和责任人已记录。', '发运信息已补录，可继续生成二维码或发布。']
  },
  MARKET: {
    label: '上市',
    defaultTitle: '记录上市销售',
    defaultOperator: '渠道管理员',
    locations: ['门店陈列区', '商超上架点', '电商发货仓'],
    summaries: ['批次已进入销售环节。', '上市节点已记录，可同步核对公开页信息。']
  },
  REGULATION: {
    label: '监管',
    defaultTitle: '记录监管处理',
    defaultOperator: '监管联络员',
    locations: ['监管检查现场', '企业整改会议室', '复核点'],
    summaries: ['已记录监管动作和处理意见。', '监管节点已补录，可继续更新后续处置进度。']
  }
}

export const stageOptions = Object.entries(stageProfiles).map(([value, profile]) => ({
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

export function getStageProfile(stage = 'PRODUCE') {
  return stageProfiles[stage] ?? stageProfiles.PRODUCE
}

export function currentDateTime() {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 16)
}

export function createTraceForm(overrides = {}) {
  const stage = overrides.stage ?? 'PRODUCE'
  const profile = getStageProfile(stage)
  return {
    stage,
    title: overrides.title ?? profile.defaultTitle,
    eventTime: overrides.eventTime ?? currentDateTime(),
    operatorName: overrides.operatorName ?? profile.defaultOperator,
    location: overrides.location ?? profile.locations[0],
    summary: overrides.summary ?? profile.summaries[0],
    imageUrl: overrides.imageUrl ?? '',
    attachmentIds: overrides.attachmentIds ?? [],
    uploadedFiles: overrides.uploadedFiles ?? [],
    visibleToConsumer: overrides.visibleToConsumer ?? true
  }
}

export function cloneTraceForm(record, overrides = {}) {
  if (!record) {
    return createTraceForm(overrides)
  }
  return createTraceForm({
    stage: record.stageCode,
    title: record.title,
    operatorName: record.operatorName,
    location: record.location,
    summary: record.summary,
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
    highlightsText: overrides.highlightsText ?? '关键指标合格\n样品抽检正常',
    attachmentIds: overrides.attachmentIds ?? [],
    uploadedFiles: overrides.uploadedFiles ?? []
  }
}

export function splitHighlightsInput(text = '') {
  return text
    .split(/[\n,;，；]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export function formatStageLabel(code = '') {
  return getStageProfile(code).label
}
