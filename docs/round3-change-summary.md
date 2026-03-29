# Round 3 Change Summary

## 本轮目标

本轮不重做 UI、不扩新模块，重点把上一轮“演示友好的批次主流程”升级为“真实持久化 + 真实二维码 + 扫码留痕 + 最小自动化测试”。

## backend 持久化改造

### 主要修改文件

- `backend/src/main/java/edu/jxust/agritrace/TraceabilityBackendApplication.java`
- `backend/src/main/java/edu/jxust/agritrace/config/TraceProperties.java`
- `backend/src/main/java/edu/jxust/agritrace/controller/PublicTraceController.java`
- `backend/src/main/java/edu/jxust/agritrace/controller/PublicQrController.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/BatchService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/BatchServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/*.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/*.java`
- `backend/src/main/resources/application.yml`
- `sql/04_round3_batch_persistence.sql`
- `sql/05_round3_seed_dev.sql`
- `docker/init/02_round3_batch_persistence.sql`
- `docker/init/03_round3_seed_dev.sql`

### 完成内容

- 用 MyBatis-Plus + MySQL 实现真实批次持久化，替换 `InMemoryBatchServiceImpl`。
- 批次创建、编辑、列表查询、工作台详情、状态流转全部改为真实落库。
- 追溯记录新增、按批次查询、最近记录摘要改为真实落库。
- 质检记录新增、最近质检摘要、合格/不合格判断改为真实落库。
- 批次状态流转统一收敛到 service 层，并写入 `batch_status_log` 留痕表。
- 为老数据兼容保留状态与阶段映射，避免上一轮页面直接断链。

## 二维码链路改造

### 主要修改文件

- `backend/pom.xml`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/QrImageStorageService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/TraceLinkBuilder.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/QrCodeEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/QrSummaryVO.java`
- `backend/src/main/java/edu/jxust/agritrace/controller/PublicQrController.java`
- `admin-web/src/pages/BatchWorkbenchView.vue`
- `admin-web/src/pages/BatchListView.vue`

### 完成内容

- 引入 ZXing 生成真实 PNG 二维码图片。
- 统一公开页链接生成逻辑，后端、工作台二维码预览和公开查询使用同一套规则。
- 增加 `/api/public/qr-images/{token}` 图片访问接口。
- 同一批次重复生成二维码时默认返回已有二维码，不重复制造脏数据。
- 工作台现在可以直接读取二维码图片地址、公开链接、PV 和 UV 摘要。

## 扫码留痕改造

### 主要修改文件

- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/dto/PublicTraceAccessContext.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/service/PublicTraceService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/service/impl/PublicTraceServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/controller/PublicTraceController.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/QrQueryLogMapper.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/QrQueryLogPO.java`

### 完成内容

- 每次访问公开追溯接口时，记录一次扫码留痕。
- 留痕信息包含 `batch / qr / query_time / ip / ua / referer`。
- 将 PV 写回二维码记录，并通过查询日志计算工作台 UV。
- 工作台可读取二维码扫描摘要，公开页无需额外前端埋点即可自动留痕。

## 测试补充

### 新增测试文件

- `backend/src/test/java/edu/jxust/agritrace/module/batch/BatchServicePersistenceIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/publictrace/PublicTraceControllerIntegrationTest.java`
- `backend/src/test/resources/application.yml`
- `backend/src/test/resources/schema.sql`
- `backend/src/test/resources/data.sql`

### 覆盖点

- 批次状态流转：`DRAFT -> PUBLISHED -> FROZEN -> PUBLISHED -> RECALLED`
- 二维码幂等：同一批次重复生成时返回同一追溯码
- 公开追溯结构：`summary / timeline / quality / company / risk`
- 扫码访问后 PV 递增与基础 UV 汇总可读

## 本轮运行命令

### 构建与测试

- `mvn -q -DskipTests compile`
- `mvn -q test`
- `npm run build`（`admin-web`）
- `npm run build`（`trace-web`）

### 真实 MySQL 联调

- 使用本机 MySQL 8.0 二进制临时启动实例，端口 `3307`
- 导入 `sql/01_schema.sql`
- 导入 `sql/04_round3_batch_persistence.sql`
- 导入 `sql/05_round3_seed_dev.sql`
- 以真实 MySQL 配置启动 `spring-boot:run`
- 验证真实链路：
  - 批次列表
  - 批次工作台详情
  - 创建批次
  - 编辑批次
  - 新增追溯记录
  - 上传质检
  - 生成二维码
  - 打开公开追溯详情
  - 获取二维码图片

## 验证结果

### 已通过

- backend 编译通过
- backend 测试通过
- backend 连接真实 MySQL 启动通过
- `GET /api/batches` 通过
- `GET /api/batches/{id}` 通过
- `POST /api/batches` 通过
- `PATCH /api/batches/{id}` 通过
- `POST /api/batches/{id}/records/quick` 通过
- `POST /api/batches/{id}/quality-reports` 通过
- `POST /api/batches/{id}/qr` 通过
- `GET /api/public/traces/{token}` 通过
- `GET /api/public/qr-images/{token}` 通过
- 已完成一次真实链路：创建批次 -> 补追溯 -> 上传质检 -> 生成二维码 -> 发布 -> 打开公开页
- admin-web 构建通过
- trace-web 构建通过

### 已处理的问题

- MySQL migration 中 `ADD COLUMN IF NOT EXISTS` 在本机环境下不兼容，已改为动态 SQL 方案。
- 追溯记录与质检时间解析原先只支持默认 ISO 格式，已兼容 `yyyy-MM-dd HH:mm`，避免现有前端表单直接报错。

## 仍待后续处理

- 当前二维码图片仍采用本地文件存储，后续可按部署方案替换为对象存储或静态资源服务。
- SQL 目前是初始化脚本方式，尚未引入 Flyway / Liquibase 之类的正式 migration 管理。
- 扫码留痕目前只做轻量 PV/UV 摘要，还没有日统计、区域统计和趋势分析。
- 公开页与工作台虽然已经接入真实数据，但还没有补端到端前端自动化测试。

## 下一轮建议

1. 补“企业建档 / 产品建档 / 批次创建”之间的真实关联选择，而不是继续自动兜底创建。
2. 为工作台补最小文件上传方案，把质检附件和现场图片从文本 URL 升级为真实文件。
3. 为公开追溯页补基础可观测性，如按批次查看最近扫码时间和近 7 天扫码趋势。
4. 引入正式 migration 工具，避免后续 SQL 版本管理继续分散。
