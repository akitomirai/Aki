# 第二轮改造变更说明

## 本轮目标

本轮只围绕三条主线推进：

1. `backend`：把接口进一步收敛到批次主流程，补齐批次工作台聚合结构、状态流转留痕、公开追溯摘要与快速录入能力。
2. `admin-web`：把批次列表和批次详情改造成后台主界面，减少跳转，让录入、质检、二维码和状态操作都能直接触达。
3. `trace-web`：把公开追溯页改成扫码后首屏 3 秒可读的移动端页面，优先展示消费者真正关心的信息。

## 本轮修改文件

### backend

- `backend/src/main/java/edu/jxust/agritrace/controller/BatchController.java`
- `backend/src/main/java/edu/jxust/agritrace/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/BatchService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/InMemoryBatchServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/dashboard/service/impl/DashboardServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/service/impl/PublicTraceServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/BatchCreateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/BatchListQueryRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/BatchUpdateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/TraceRecordCreateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/QualityReportCreateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/BatchStatusActionRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/ProductEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/TraceRecordEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/TraceStage.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchActionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchListItemVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchWorkbenchVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchOverviewVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ProductSummaryVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/CompanySummaryVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchStatusSummaryVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/TraceSectionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/QualitySectionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/QrSummaryVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/TraceRecordVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/QualityReportVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/vo/PublicTraceDetailVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/vo/PublicTraceSummaryVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/vo/PublicTimelineItemVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/vo/PublicQualityVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/vo/PublicRiskVO.java`
- 删除：`backend/src/main/java/edu/jxust/agritrace/module/batch/vo/QrInfoVO.java`
- 删除：`backend/src/main/java/edu/jxust/agritrace/module/publictrace/vo/PublicExceptionVO.java`

### admin-web

- `admin-web/src/api/batch.js`
- `admin-web/src/pages/BatchListView.vue`
- `admin-web/src/pages/BatchWorkbenchView.vue`
- `admin-web/public/images/products/orange-batch.svg`
- `admin-web/public/images/products/green-tea-batch.svg`
- `admin-web/public/images/products/rice-batch.svg`

### trace-web

- `trace-web/src/views/TraceDetailView.vue`
- `trace-web/public/images/products/orange-batch.svg`
- `trace-web/public/images/products/green-tea-batch.svg`
- `trace-web/public/images/products/rice-batch.svg`

### 文档

- `docs/round2-change-summary.md`

## backend 改了什么

### 1. 批次列表接口改成面向主入口使用

- 新增 `BatchListQueryRequest`，支持按批次号、产品名、状态、企业、日期区间筛选。
- `BatchListItemVO` 补充产品图片、状态标签、产地、上市日期、二维码状态、快捷动作等字段，方便后台列表直接渲染。

### 2. 批次详情接口升级为工作台聚合结构

- `BatchWorkbenchVO` 改成明确的聚合对象：
  - `batch`
  - `product`
  - `company`
  - `status`
  - `trace`
  - `quality`
  - `qr`
  - `statusHistory`
  - `actions`
- 新增 `BatchOverviewVO`、`ProductSummaryVO`、`CompanySummaryVO`、`BatchStatusSummaryVO`、`TraceSectionVO`、`QualitySectionVO`、`QrSummaryVO` 等结构，减少零散字段拼装。

### 3. 批次状态流转收敛到 service 层

- 统一了核心状态：
  - `DRAFT`
  - `PUBLISHED`
  - `FROZEN`
  - `RECALLED`
- 状态变更统一在 `InMemoryBatchServiceImpl` 中处理，并保留：
  - `reason`
  - `operator`
  - `time`
- 状态变更结果同步更新工作台状态摘要和状态留痕列表。

### 4. 追溯记录改成快速录入友好

- 新增 `TraceStage` 枚举，统一阶段代码和显示名称。
- `TraceRecordCreateRequest` 只保留高频字段：阶段、时间、地点、操作人、摘要、图片和是否公开。
- 工作台接口返回最近记录摘要，方便批次详情页直接展示。

### 5. 质检与二维码能力做了收口

- `QualityReportCreateRequest` 支持更轻量的质检摘要提交。
- `QualityReportVO` 增加结果标签，工作台和公开页无需自行转换。
- 二维码聚合结构改为 `QrSummaryVO`。
- 同一批次重复生成二维码时走幂等处理，不再无限制造新数据。

### 6. 公开追溯接口改成消费者可读结构

- `PublicTraceDetailVO` 收敛为：
  - `summary`
  - `timeline`
  - `quality`
  - `company`
  - `risk`
  - `consumerTips`
- `summary` 重点保留产品名、图片、批次号、企业、产地、状态、质检结果、生产日期、上市日期。
- `timeline` 只取公开可见且关键的记录节点。
- `risk` 用于首屏风险横幅，明确冻结或召回原因。

## admin-web 改了什么

### 1. 批次列表页改成后台主入口

- 支持：
  - 批次号搜索
  - 产品名搜索
  - 状态筛选
  - 企业筛选
  - 日期筛选
- 每条批次记录直接提供：
  - 查看详情
  - 编辑
  - 新增追溯记录
  - 上传质检
  - 生成二维码
  - 发布
  - 冻结
  - 召回

### 2. 批次详情页升级成工作台

- 页面重组为几个核心区域：
  - 顶部摘要卡
  - 快捷操作区
  - 追溯记录区
  - 质检摘要区
  - 二维码区
  - 状态留痕区
- 快捷动作都在当前页完成，不再引导用户跳到深层页面。

### 3. 表单顺手化

- 批次创建只保留主流程需要的高频字段。
- 追溯记录与质检上传都改成弹层处理。
- 状态流转改成单独的轻量操作弹层，方便补充原因和操作人。

## trace-web 改了什么

### 1. 扫码首屏改成 3 秒可读

- 首屏优先展示：
  - 产品名称
  - 产品图片
  - 批次编号
  - 企业名称
  - 产地
  - 当前状态
  - 质检结果
  - 生产日期
  - 上市日期

### 2. 风险态前置

- 如果批次处于冻结或召回状态，首屏顶部直接显示风险横幅。
- 风险原因和建议放在首屏，不再埋在页面下方。

### 3. 时间线更适合消费者阅读

- 只展示关键节点。
- 每个节点只保留阶段、标题、时间、地点、简短说明和可选图片。
- 页面默认为手机扫码场景设计，避免首屏横向滚动。

## 跑了哪些验证

### backend

- `mvn -q -DskipTests compile`
- `mvn -q test`
- 启动后探测：
  - `GET /actuator/health`
  - `GET /api/batches`
  - `GET /api/public/traces/test-token-2026`

### admin-web

- `npm run build`

### trace-web

- `npm run build`

## 哪些通过了

- backend 编译通过。
- backend `mvn test` 通过。
- backend 可以启动，批次列表接口和公开追溯接口返回 `200`。
- admin-web 构建通过。
- trace-web 构建通过。

## 哪些没通过

- 本轮没有出现阻断性的构建失败。
- `vite build` 在 `admin-web` 和 `trace-web` 中都给出了 chunk size warning，这不是构建失败，但说明后续可以考虑做更细的拆包优化。

## 补充说明

- 当前 backend 仍以演示友好的内存数据服务为主，已能支撑主流程演示，但还没有替换成真实数据库持久化链路。
- `backend/src/test` 下当前没有测试文件，因此 `mvn test` 的结果主要表示测试生命周期可执行、工程没有测试阶段报错，而不是已经具备完整自动化测试覆盖。

## 建议下一轮处理顺序

1. 先把批次创建、编辑、追溯补录、质检上传、状态变更接到真实数据库。
2. 再补真实二维码图片或短链生成，并记录扫码次数和最近扫码时间。
3. 然后补企业建档、监管处理和权限边界，但继续保持批次中心而不是拆成过多碎模块。
4. 最后再补自动化测试，优先覆盖批次状态流转、公开追溯输出结构和二维码幂等逻辑。
