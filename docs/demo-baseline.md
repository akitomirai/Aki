# Demo 基线与回归

## 当前 demo 基线

- 数据来源：`backend/src/main/resources/schema-demo.sql` + `backend/src/main/resources/data-demo.sql`
- demo profile：backend 使用 H2 内存库，重启 `demo` profile 后会重新灌入种子数据
- 稳定验收批次：`batch 2 / ORANGE-202603-D1`

## 默认账号

- `platform / 123456`
- `enterprise_admin / 123456`
- `operator / 123456`
- `operator_support / 123456`
- `regulator / 123456`

## batch 2 当前应有状态

- 批次状态：`已发布`
- `publishedAt`：`2026-03-21 11:30`
- 质检状态：`合格`
- 二维码：`orange-202603-d1`
- 默认分配人：`现场操作员B / operator_support`
- 默认公开页：`/t/orange-202603-d1`

## 一键重置 demo 基线

脚本：

- [reset-demo-baseline.ps1](D:/Users/Lenovo/Desktop/traceability-system/scripts/reset-demo-baseline.ps1)

执行方式：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\reset-demo-baseline.ps1
```

说明：

- 这个脚本只重置 demo 数据，不改真实业务逻辑
- 当前实现方式是：重启 backend 的 `demo` profile，让 H2 内存种子重新载入
- 重置完成后，batch 2 会恢复到当前稳定种子状态

## 一键本地运行

启动三端：

- [start-local.ps1](D:/Users/Lenovo/Desktop/traceability-system/scripts/start-local.ps1)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

停止三端：

- [stop-local.ps1](D:/Users/Lenovo/Desktop/traceability-system/scripts/stop-local.ps1)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

健康检查：

- [check-local.ps1](D:/Users/Lenovo/Desktop/traceability-system/scripts/check-local.ps1)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-local.ps1
```

认证感知预检查：

- [demo-precheck.ps1](D:/Users/Lenovo/Desktop/traceability-system/scripts/demo-precheck.ps1)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-precheck.ps1
```

统一入口：

- [dev-cycle.ps1](D:/Users/Lenovo/Desktop/traceability-system/scripts/dev-cycle.ps1)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-cycle.ps1 -Mode start
powershell -ExecutionPolicy Bypass -File .\scripts\dev-cycle.ps1 -Mode check
powershell -ExecutionPolicy Bypass -File .\scripts\dev-cycle.ps1 -Mode reset
powershell -ExecutionPolicy Bypass -File .\scripts\dev-cycle.ps1 -Mode regression
powershell -ExecutionPolicy Bypass -File .\scripts\dev-cycle.ps1 -Mode stop
```

说明：

- `start-local.ps1` 会固定启动：
  - backend `8080`
  - admin-web `5174`
  - trace-web `5173`
- 启动后会记录 PID 到 `output/runtime/local-run-state.json`
- `stop-local.ps1` 优先按这个状态文件停止进程，避免误杀无关系统进程
- 如果端口已被占用，启动脚本会直接报出 PID、进程名和命令行摘要
- `check-local.ps1` 只检查匿名可访问的健康端点、后台登录页和公开追溯页
- `demo-precheck.ps1` 会额外做一次真实平台管理员登录，再检查需要登录态的后台基线接口
- 当前 `GET /api/batches` 不再按匿名接口使用，后台批次基线检查统一放在 `demo-precheck.ps1`

## 一键主回归入口

主入口：

- [run-baseline-regression.ps1](D:/Users/Lenovo/Desktop/traceability-system/scripts/run-baseline-regression.ps1)
- 实际浏览器脚本：[full-regression-baseline.mjs](D:/Users/Lenovo/Desktop/traceability-system/output/playwright/full-regression-baseline.mjs)

执行方式：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-baseline-regression.ps1
```

产物目录：

- JSON：`output/playwright/full-regression-baseline.json`
- 截图：`output/playwright/`

日志权限附加冒烟：

- `tests/e2e/specs/log-permission-smoke.spec.mjs`
- `scripts/run-baseline-regression.ps1` 现在会在主回归末尾追加执行这条 smoke

主回归覆盖：

- 管理员批次列表与工作台
- 操作员待办、草稿、提交
- 分配链：分配、改派、清空、草稿阻断
- 风险链：冻结、处理中、已整改、恢复发布
- 公开页联动与状态巡检
- 日志权限链：
  - `platform` 可进入 `/logs`
  - `enterprise_admin` 进入 `/logs` 时显示“本企业日志模式”并保持本企业范围
  - `operator` 直输 `/logs` 被拦截
  - 后端拒绝请求会写入 `LOG_ACCESS_DENIED`，并能被平台管理员在日志页筛出

入口关系：

- `scripts/check-local.ps1`
  - 轻量本地健康检查，只看匿名安全入口
- `scripts/demo-precheck.ps1`
  - demo 基线预检查，会带真实登录态检查后台批次基线
- `scripts/backend-test.ps1 -Task test`
  - 后端完整测试入口，包含日志权限链的 `OperationLogControllerIntegrationTest`
- `scripts/run-baseline-regression.ps1`
  - 主浏览器回归入口，会先跑原有主链路，再追加日志权限 smoke

## 主要验收入口

- `/batches`
- `/batches/2`
- `/field-entry?batchId=2`
- `/t/orange-202603-d1`

## 轻量巡检项

主回归执行后会额外检查：

- 是否泄漏英文 seed
- 是否泄漏英文状态枚举
- 是否出现 `publishedAt / 已发布` 冲突
- 是否出现任务状态不一致
- 是否出现风险状态不一致

## 什么时候跑哪类测试

定向验证日志权限链时：

```powershell
cd tests\e2e
npm run test -- specs/log-permission-smoke.spec.mjs
```

需要确认整套 demo 基线没有退化时：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-baseline-regression.ps1
```

说明：

- 定向 smoke 只验证日志页权限、企业管理员范围限制、拒绝提示和拒绝留痕可见
- 主回归会先跑原有基线流程，再追加这条日志权限 smoke
- 如果只是确认本地服务是否起来，先跑 `check-local.ps1`
- 如果要确认 demo 数据和需要登录态的后台接口没偏离，再跑 `demo-precheck.ps1`

## 推荐顺序

```powershell
# first start local services
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1

# optional: verify local runtime
powershell -ExecutionPolicy Bypass -File .\scripts\check-local.ps1

# reset demo baseline
powershell -ExecutionPolicy Bypass -File .\scripts\reset-demo-baseline.ps1

# run the main regression
powershell -ExecutionPolicy Bypass -File .\scripts\run-baseline-regression.ps1

# stop local services when finished
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

## 常见问题

- 端口占用
  - 看 `start-local.ps1` 的报错输出，先确认 PID 和命令行
  - 如果就是这套本地三端，先跑 `scripts/stop-local.ps1`
- 服务未启动
  - 先跑 `scripts/check-local.ps1`
  - 再看 `output/runtime/` 下的日志
- 日志位置
  - backend：[backend.log](D:/Users/Lenovo/Desktop/traceability-system/output/runtime/backend.log)
  - admin-web：[admin-web.log](D:/Users/Lenovo/Desktop/traceability-system/output/runtime/admin-web.log)
  - trace-web：[trace-web.log](D:/Users/Lenovo/Desktop/traceability-system/output/runtime/trace-web.log)
  - 本地运行状态文件：`output/runtime/local-run-state.json`
