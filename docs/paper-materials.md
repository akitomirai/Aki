# 论文支撑材料整理

## 1. 本次整理范围

- 项目目录：`traceability-system / Aki`
- 整理时间：`2026-04-22`
- 约束：未修改业务代码、样式、配置或初始化数据
- 本次新增内容：
  - 本文档 `docs/paper-materials.md`
  - 截图目录 `screenshots/paper-materials/`

## 2. 三端结构说明

### 2.1 后端 `backend`

- 技术栈：
  - Java 17
  - Spring Boot 3.5.x
  - Spring Web / Validation / Security / Actuator / AOP
  - MyBatis-Plus
  - H2 runtime 依赖
  - Redis 依赖
  - JWT
  - Springdoc OpenAPI
- 作用：
  - 提供登录认证、批次管理、质检、风险、二维码、公开追溯、日志、用户、企业、产品等接口
- 启动健康检查：
  - `GET http://127.0.0.1:8080/actuator/health`

### 2.2 管理端 `admin-web`

- 技术栈：
  - Vue 3
  - Vite
  - Element Plus
  - Pinia
  - Vue Router
  - Axios
  - ECharts
- 作用：
  - 提供平台管理员、企业管理员、操作员、监管人员的后台页面
- 默认访问地址：
  - `http://127.0.0.1:5174`

### 2.3 公开追溯端 `trace-web`

- 技术栈：
  - Vue 3
  - Vite
  - Vant
  - Element Plus
  - Vue Router
  - Axios
- 作用：
  - 面向消费者/答辩评审的公开追溯查询页
- 默认访问地址：
  - `http://127.0.0.1:5173`

## 3. 启动检查结果

### 3.1 三端当前状态

本次实际核验结果：

- backend：可正常访问，`/actuator/health` 返回 `UP`
- admin-web：可正常访问，首页返回 `200`
- trace-web：可正常访问，首页返回 `200`

### 3.2 启动脚本说明

项目已有本地启动脚本：

- `scripts/start-local.ps1`
- `scripts/stop-local.ps1`
- `scripts/check-local.ps1`

其中 `start-local.ps1` 会：

- 检查 `8080 / 5174 / 5173` 端口
- 启动 backend、admin-web、trace-web
- 写入运行日志到 `output/runtime/`

## 4. 核心业务流程

当前项目的核心闭环与仓库文档保持一致，主链路为：

`产品档案 -> 批次管理 -> 任务分配 / 现场填报 -> 质检 -> 二维码与发布 -> 公开追溯 -> 风险处理 -> 监管只读查看`

适合论文描述的流程解释：

1. 管理端维护企业与产品基础信息。
2. 以批次为中心创建具体业务对象。
3. 操作员或业务人员补录现场追溯记录，可包含草稿与图片。
4. 企业/平台侧录入质检结果，形成公开可信依据。
5. 系统为批次生成二维码并绑定公开 token。
6. 公众通过公开追溯页查看产品、批次、时间线、质检和风险状态。
7. 如果发生冻结/召回等风险，后台风险状态与公众侧提示联动。
8. 监管角色以只读方式查看批次、风险和质检，不直接改写业务数据。

## 5. 演示账号和演示数据

### 5.1 当前实际可用固定账号

经 `scripts/demo-precheck.ps1` 与登录接口核验，当前固定演示账号可用：

| 账号 | 密码 | 角色 | 默认页 |
| --- | --- | --- | --- |
| `platform` | `123456` | 平台管理员 | `/dashboard` |
| `enterprise_admin` | `123456` | 企业管理员 | `/batches` |
| `operator` | `123456` | 现场操作员 | `/field-entry` |
| `regulator` | `123456` | 监管人员 | `/risk` |

### 5.2 当前固定演示批次 / token

经 `demo-precheck` 和接口核验，当前固定基线为：

| 对象 | 当前状态 | token / 说明 |
| --- | --- | --- |
| `ORANGE-202603-D1` | 已发布 | `orange-202603-d1` |
| `ORANGE-202604-Q1` | 草稿 / 待质检 | 无公开 token |
| `TEA-202603-F1` | 已冻结 | `demo-frozen-2026` |
| `RICE-202603-R1` | 已召回 | 风险公开样例，风险页预检使用 `demo-recall-2026` |
| `ORANGE-202603-A1` | 已发布 | `demo-normal-2026` |

### 5.3 DEMO_FLOW.md 与当前基线的差异

按 `docs/DEMO_FLOW.md` 实测发现，该文档中的部分演示数据已经与当前基线不一致：

- `platform / 123456`：仍可正常登录
- `testuser1 / 123456`：当前环境登录失败，接口返回“用户名或密码错误”
- `/t/test-token-2026`：当前不存在，对应页面落到“查询异常”
- `/t/invalid-token`：同样落到“查询异常”
- 文档中提到的 `BATCH20260311001`：当前批次列表中不存在

结论：

- `docs/DEMO_FLOW.md` 更像旧演示链路文档
- 当前真实可用演示基线应以 `scripts/demo-precheck.ps1` 和 `docs/final-demo-baseline.md` 为准

## 6. 主要页面截图清单

### 6.1 本次新增截图

本次新增截图目录：`screenshots/paper-materials/`

| 截图 | 说明 |
| --- | --- |
| `screenshots/paper-materials/platform-login.png` | 平台端登录页 |
| `screenshots/paper-materials/platform-dashboard.png` | 平台管理员 Dashboard |
| `screenshots/paper-materials/testuser1-login-failed.png` | `testuser1` 登录失败现状 |
| `screenshots/paper-materials/public-test-token-2026.png` | `/t/test-token-2026` 当前表现 |
| `screenshots/paper-materials/public-invalid-token.png` | `/t/invalid-token` 当前表现 |

### 6.2 仓库内已有可复用截图

仓库中已存在多组适合论文或答辩材料复用的截图：

| 目录 | 用途 |
| --- | --- |
| `docs/ui-check/` | 主要后台页面的固定截图 |
| `output/playwright/` | Playwright 回归过程产出的工作台、风险、质检、二维码、公开页截图 |
| `screenshots/test-cases-batch-review/` | 多案例业务测试截图 |

建议论文正文优先选取以下页面：

- 平台 Dashboard
- 批次列表 / 批次工作台
- 质检页
- 二维码与发布页
- 风险页
- 公开追溯页正常态
- 公开追溯页风险态
- 公开追溯页无效码态

## 7. 主要接口清单

以下接口来自当前 `backend/src/main/java/.../controller` 实际控制器定义。

### 7.1 认证与账号

- `POST /api/auth/login`
- `GET /api/auth/profile`
- `PATCH /api/auth/profile`
- `POST /api/auth/change-password`

### 7.2 Dashboard

- `GET /api/dashboard/overview`

### 7.3 批次主链路

- `GET /api/batches`
- `GET /api/batches/{batchId}`
- `POST /api/batches`
- `PATCH /api/batches/{batchId}`
- `POST /api/batches/{batchId}/status`
- `POST /api/batches/{batchId}/records`
- `POST /api/batches/{batchId}/records/quick`
- `POST /api/batches/{batchId}/assignment`
- `POST /api/batches/{batchId}/quality-reports`
- `POST /api/batches/{batchId}/risk-actions`
- `POST /api/batches/{batchId}/qr`
- `GET /api/batches/{batchId}/trace-chain/verify`

### 7.4 批次辅助接口

- `GET /api/batches/lookup/companies`
- `GET /api/batches/lookup/products`
- `GET /api/batches/lookup/operators`
- `POST /api/batches/files/upload`
- `POST /api/batches/files/cleanup`

### 7.5 现场草稿

- `GET /api/batches/field-drafts`
- `GET /api/batches/{batchId}/field-draft`
- `POST /api/batches/{batchId}/field-draft`
- `DELETE /api/batches/{batchId}/field-draft`

### 7.6 企业 / 产品 / 用户 / 日志

- `GET /api/companies`
- `GET /api/companies/{companyId}`
- `POST /api/companies`
- `PATCH /api/companies/{companyId}`
- `POST /api/companies/{companyId}/status`
- `DELETE /api/companies/{companyId}`

- `GET /api/products`
- `GET /api/products/{productId}`
- `POST /api/products`
- `PATCH /api/products/{productId}`
- `POST /api/products/{productId}/status`
- `DELETE /api/products/{productId}`

- `GET /api/users`
- `POST /api/users`
- `PATCH /api/users/{userId}`
- `POST /api/users/{userId}/status`
- `POST /api/users/{userId}/reset-password`

- `GET /api/logs`

### 7.7 公开追溯

- `GET /api/public/traces/{token}`
- `GET /api/public/qr-images/{token}`
- `GET /api/public/files/{fileId}`

## 8. 当前数据库 / 内存演示数据状态说明

### 8.1 当前状态结论

本次通过 `scripts/demo-precheck.ps1` 实测，当前 demo 基线处于可演示状态。

关键事实：

- 当前批次数量：`15`
- 已发布批次：`4`
- 草稿批次：`9`
- 风险批次：`2`

### 8.2 当前演示数据特征

- 已存在一组固定的“明星批次 / 风险批次 / 待处理批次”组合
- 后端预检能成功识别：
  - `ORANGE-202603-D1` 已发布
  - `ORANGE-202604-Q1` 待质检
  - `TEA-202603-F1` 冻结
  - `RICE-202603-R1` 召回
- `ORANGE-202603-D1` 的工作台详情已包含：
  - 最近追溯记录
  - 质检报告编号
  - 二维码
  - 公开追溯页

### 8.3 数据存储与运行态说明

从依赖和脚本可以判断：

- demo 运行主要依赖后端 demo baseline 数据
- 后端具备 H2 runtime 依赖，适合本地演示场景
- Dashboard 中的 PV / UV 统计与文档描述一致，来自 Redis 相关能力，而不是静态 SQL 计数

### 8.4 当前需要注意的文档漂移

当前基线中不存在：

- `testuser1`
- `BATCH20260311001`
- `test-token-2026`

因此论文中如果引用“演示账号 / 演示 token / 演示批次”，建议以当前预检通过的数据为准，不建议继续直接引用 `docs/DEMO_FLOW.md` 的旧样例。

## 9. 已运行脚本与结果

### 9.1 已运行预检脚本

已运行：

- `scripts/demo-precheck.ps1`

结果：

- 全部通过
- 说明当前固定 demo 基线和核心账号/公开 token 均处于健康状态

### 9.2 已运行系统 e2e 脚本

已运行：

- `scripts/run-system-smoke.ps1 -KeepServices`

结果摘要：

- 共运行 `13` 个测试
- `4` 个通过
- `7` 个失败
- `2` 个未执行

通过项主要集中在：

- 公开追溯页正常态
- 公开追溯页风险态
- 公开追溯页无效码态
- 召回批次不能被重新发布的接口约束

失败项主要集中在：

- 批次工作台入口或页面 `data-testid` 与测试预期不一致
- 监管只读页缺少测试脚本预期的只读 banner 标识
- 若干工作台类测试仍假设可以直接访问 `batch-workbench-page`

这说明：

- 系统本身并未完全不可用
- 但现有 e2e 测试与当前前端页面结构之间存在一部分选择器/入口漂移
- 更适合作为“自动化回归需要同步维护”的论文讨论点，而不是作为业务功能完全失效的结论

### 9.3 失败用例示例

系统 smoke 中的典型失败包括：

- `admin batch list and workbench support the main smoke path`
- `field entry draft can resume and sync to workbench with image evidence`
- `quality to publish path stays stable for demo batches`
- `regulator lands on /risk and only sees read-only regulator menu entries`
- `risk workbench shows current status, recent actions and checklist`

相关测试产物位于：

- `artifacts/test-results/`
- `artifacts/logs/playwright-results.json`

## 10. 可用于论文的测试用例清单

以下用例适合直接写入论文“系统测试”或“演示验证”章节。

### 10.1 启动与健康检查

1. 后端健康检查
   - 步骤：访问 `/actuator/health`
   - 期望：返回 `UP`

2. 三端可达性检查
   - 步骤：访问 `8080 / 5174 / 5173`
   - 期望：三端均可访问

### 10.2 认证与权限

3. 平台管理员登录
   - 步骤：`platform / 123456`
   - 期望：进入 `/dashboard`

4. 企业管理员登录
   - 步骤：`enterprise_admin / 123456`
   - 期望：进入 `/batches`

5. 操作员登录
   - 步骤：`operator / 123456`
   - 期望：进入 `/field-entry`

6. 监管账号只读访问
   - 步骤：`regulator / 123456`
   - 期望：进入 `/risk`，仅查看，不执行写操作

### 10.3 批次与追溯

7. 明星批次工作台完整性检查
   - 对象：`ORANGE-202603-D1`
   - 期望：存在质检、二维码、公开链接、最近追溯记录

8. 待处理批次存在性检查
   - 对象：`ORANGE-202604-Q1`
   - 期望：草稿 / 待质检状态

9. 公开追溯正常态
   - token：`orange-202603-d1`
   - 期望：能展示批次和公开信息

10. 公开追溯风险态
    - token：`demo-recall-2026`
    - 期望：展示风险提示

11. 公开追溯无效码
    - token：任意不存在 token，例如 `invalid-token`
    - 期望：展示友好的查询异常页

### 10.4 风险与状态流转

12. 冻结样例存在性检查
    - 对象：`TEA-202603-F1`
    - 期望：状态为 `FROZEN`

13. 召回样例存在性检查
    - 对象：`RICE-202603-R1`
    - 期望：状态为 `RECALLED`

14. 已召回批次不能直接重新发布
    - 方式：接口/自动化验证
    - 期望：发布被拒绝

### 10.5 文档一致性检查

15. DEMO_FLOW 文档一致性检查
    - 检查项：
      - `platform / 123456`
      - `testuser1 / 123456`
      - `/t/test-token-2026`
      - `BATCH20260311001`
    - 期望：
      - 当前文档样例与当前 demo 基线若不一致，应在论文中注明“旧演示链路已漂移”

## 11. 论文写作建议

在论文或答辩材料中，建议区分两组内容：

- “旧 DEMO_FLOW 文档链路”
  - 可作为历史演示说明
  - 但不应直接当作当前运行基线
- “当前固定 demo 基线”
  - 应以 `demo-precheck.ps1`、`docs/final-demo-baseline.md`、当前接口实测结果为准

更稳妥的论文表述方式是：

1. 先说明系统设计的标准业务链路。
2. 再说明当前演示环境中已冻结的真实账号、批次和公开 token。
3. 对已经漂移的旧文档样例如实标注，不把它们当成当前有效基线。

## 12. 本次结论

本次整理结论如下：

- 三端当前可以正常启动与访问。
- 当前固定 demo 基线是健康的，适合做论文演示支撑。
- `docs/DEMO_FLOW.md` 中的 `testuser1 / test-token-2026 / BATCH20260311001` 已与当前演示数据脱节。
- 仓库自带 `demo-precheck.ps1` 全通过，说明真实可用基线存在。
- 仓库自带系统 smoke 仅部分通过，失败主要表现为测试脚本与页面结构的选择器/入口漂移，不等同于系统整体不可用。
