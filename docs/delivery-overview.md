# 项目交付总览

## 1. 项目目标与定位

本项目定位为一个面向农产品批次全链路管理的轻量级追溯系统，核心目标不是堆叠大量边缘功能，而是打通一条可以真实演示、可以持续验证、可以直接体现业务价值的闭环：

`产品档案 -> 批次管理 -> 任务分配/现场填报 -> 质检 -> 二维码/公开追溯 -> 风险处理 -> 监管只读查看`

项目当前更强调以下能力：

- 以批次为业务主线组织数据和页面
- 用公开追溯页承接消费者扫码查询
- 用角色与权限区分平台、企业、操作员、监管方
- 用本地脚本和 Playwright 回归把 clean baseline 固化下来

## 2. 技术栈概览

### 后端

- Java 17
- Spring Boot 3.5.x
- Spring Web / Validation / Security / Actuator / AOP
- MyBatis-Plus
- H2（demo profile）
- MySQL Connector
- Redis 相关依赖
- JWT
- Springdoc OpenAPI
- Maven

### 管理端前端

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

### 公开追溯前端

- Vue 3
- Vite
- Vant
- Element Plus
- Vue Router
- Axios

### 自动化与验证

- PowerShell 本地启动/检查脚本
- Playwright 端到端回归

## 3. 前后端目录结构概览

### 仓库主目录

- `backend`
  - Spring Boot 后端服务
- `admin-web`
  - 管理端前端
- `trace-web`
  - 公开追溯前端
- `tests/e2e`
  - Playwright 端到端脚本
- `scripts`
  - 本地启动、停止、检查、回归脚本
- `docs`
  - 项目说明、演示说明、基线文档
- `output`
  - 本地产物、回归 JSON、截图、运行日志

### 后端结构

- `backend/src/main/java/edu/jxust/agritrace/common`
  - 通用能力
- `backend/src/main/java/edu/jxust/agritrace/config`
  - 配置类
- `backend/src/main/java/edu/jxust/agritrace/controller`
  - 控制器与接口入口
- `backend/src/main/java/edu/jxust/agritrace/module`
  - 按业务模块组织的核心逻辑
- `backend/src/main/resources`
  - 配置与 demo 数据

### 管理端结构

- `admin-web/src/api`
  - 后台接口调用
- `admin-web/src/pages`
  - 页面级视图
- `admin-web/src/router`
  - 路由与角色访问控制
- `admin-web/src/stores`
  - 状态管理
- `admin-web/src/components`
  - 可复用组件

当前后台页面覆盖了：

- Dashboard
- 产品管理
- 企业管理
- 用户管理
- 批次列表/批次工作台
- 现场填报
- 质检
- 风险
- 二维码与发布
- 操作日志

### 公开追溯端结构

- `trace-web/src/api`
  - 公开追溯接口调用
- `trace-web/src/router`
  - 公开页路由
- `trace-web/src/views` / `trace-web/src/pages`
  - 追溯详情展示

## 4. 角色与权限概览

系统当前明确支持以下后台角色：

- 平台管理员 `PLATFORM_ADMIN`
  - 默认落点：`/dashboard`
  - 适合查看全局总览、产品、批次、用户、日志、质检、风险。
- 企业管理员 `ENTERPRISE_ADMIN`
  - 默认落点：`/batches`
  - 适合查看本企业批次、工作台、质检、发布和本企业范围内的管理功能。
- 企业操作员 `OPERATOR`
  - 默认落点：`/field-entry`
  - 适合处理现场任务、草稿和过程记录。
- 监管方 `REGULATOR`
  - 默认落点：`/risk`
  - 当前回归重点覆盖的是只读能力和禁止写操作。
- 消费者
  - 不登录后台，直接访问公开追溯页。

当前权限特点：

- 监管方能看风险、批次、质检等只读页面，但不能执行写操作。
- 操作日志页不是所有角色都可用，权限链路已有 smoke 验证。
- 操作员更偏向现场填报，不承担后台全局管理职责。

## 5. 核心业务闭环概览

项目当前最适合强调的业务闭环如下：

1. 创建产品档案
2. 创建批次
3. 在批次工作台查看状态和任务
4. 分配任务给现场操作员
5. 操作员进入现场填报页处理任务、保存草稿或提交
6. 管理端查看最新过程记录与图片
7. 录入或查看质检结果
8. 生成二维码并对外发布
9. 消费者访问公开追溯页
10. 监管方只读查看风险与批次信息
11. 如遇风险，执行冻结、处理中、已整改、恢复发布等状态流转

这个闭环的价值在于：

- 既有内部管理链路，也有对外展示链路
- 既有正常流程，也覆盖异常处理流程
- 既有角色分工，也有权限边界

## 6. 当前测试与基线状态

当前仓库已经形成可重复执行的本地验证链路：

- `scripts/check-local.ps1`
  - 检查 backend、admin-web、trace-web 的匿名安全入口是否正常
- `scripts/demo-precheck.ps1`
  - 带真实登录态做后台基线预检查
- `scripts/run-baseline-regression.ps1`
  - 主浏览器回归入口
  - 会先验证 runtime，再执行主回归，再追加两个 smoke：
    - `specs/log-permission-smoke.spec.mjs`
    - `specs/regulator-readonly-smoke.spec.mjs`

当前 clean baseline 结论以冻结文档为准：

- 主回归状态：`PASSED`
- Expected intercepts：
  - 改派前草稿阻断 `400 POST /api/batches/2/assignment`
  - 这是预期业务拦截，不算失败
- True failures：
  - `none`

## 7. 已冻结基线文档路径

- 冻结基线文档：`docs/baselines/2026-04-02-clean-baseline.md`
- 本地摘要产物：`output/playwright/latest-baseline-summary.json`
- 主回归 JSON：`output/playwright/full-regression-baseline.json`

其中冻结基线文档已经入库，`output` 下产物继续保留为本地产物。

## 8. 启动与验证脚本说明

推荐使用以下脚本组合，不建议答辩时临时手动拼运行命令：

- `scripts/stop-local.ps1`
  - 停止本地三端，清理端口占用
- `scripts/start-local.ps1`
  - 启动 backend、admin-web、trace-web
- `scripts/check-local.ps1`
  - 快速健康检查
- `scripts/demo-precheck.ps1`
  - 带登录态的后台预检查
- `scripts/run-baseline-regression.ps1`
  - 完整浏览器回归
- `scripts/reset-demo-baseline.ps1`
  - 重置 demo baseline

建议日常顺序：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\check-local.ps1
```

如果需要完整证明系统链路通过，再执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-baseline-regression.ps1
```

## 9. 适合答辩时强调的亮点

- 多角色协同
  - 平台管理员、企业管理员、操作员、监管方、消费者各自有清晰入口和权限边界。
- 以批次为中心的工作台
  - 批次工作台不是简单详情页，而是状态、任务、质检、风险、公开链接的聚合入口。
- 公开追溯页真正面向消费者
  - 不是后台拼接字段，而是直接服务于扫码查看。
- 风险链路完整
  - 包含冻结、处理中、已整改、恢复发布，并与公开页状态联动。
- 回归基线已冻结
  - 当前不是“理论上能跑”，而是有真实脚本、真实 JSON、真实截图和冻结文档支撑。
- 高频页面已做展示层清理
  - 当前主回归会检查高频页面不暴露英文 seed 值和原始枚举值。

## 10. 当前明确未做或暂不扩展的部分

结合现有仓库设计与文档，当前不把下面这些方向作为主要交付重点：

- 复杂多级审批流
- 大屏可视化与重统计报表
- 复杂消息中心
- 过细颗粒度的权限拆分
- 面向生产环境的大规模部署编排
- 超出当前答辩范围的外部系统集成

这样做的原因是保持项目聚焦在“追溯闭环是否真实成立、是否可演示、是否可验证”上，而不是为了看起来很大而摊薄完成度。

