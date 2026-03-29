# Round 8 Smoke Test

## 一条命令怎么跑

在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-round8-smoke.ps1
```

这条命令会自动完成：

1. 启动 demo 环境
2. 等待 backend / admin-web / trace-web 就绪
3. 运行 Playwright 冒烟测试
4. 收集报告、截图和日志
5. 默认停止服务并清理残留

如果你想跑完后保留服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-round8-smoke.ps1 -KeepServices
```

## 单独启动 / 停止

### 单独启动

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-round7-demo.ps1
```

### 单独停止

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-round8-demo.ps1
```

### 单独预检

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-precheck.ps1
```

## 测试覆盖哪些场景

### 1. 企业管理员链路

- 打开 admin-web 批次列表
- 过滤并定位测试批次
- 进入批次工作台
- 验证下一步推荐和动作分组
- 实际执行一次关键动作：生成二维码

### 2. 消费者扫码链路

- 打开正常公开追溯页
- 验证产品名、状态、企业名、批次号、产地、质检结果
- 验证精简时间线
- 打开召回风险页并验证风险提示存在

### 3. 风险处理链路

- 打开风险批次工作台
- 验证风险区、处理检查项和状态处理分组可见

## 报告和日志输出到哪里

- HTML 报告：`artifacts/playwright-report`
- Playwright 原始结果：`artifacts/test-results`
- 成功截图：`artifacts/screenshots`
- 服务日志：`artifacts/logs/services`
- 进程状态文件：`artifacts/logs/round8-processes.json`
- Playwright JSON 结果：`artifacts/logs/playwright-results.json`

## 失败时先看什么

推荐按这个顺序排查：

1. 先看 `artifacts/logs/services/backend-demo.log`
2. 再看 `artifacts/logs/services/admin-web.log`
3. 再看 `artifacts/logs/services/trace-web.log`
4. 然后看 `artifacts/logs/playwright-results.json`
5. 如果是页面失败，再打开 `artifacts/playwright-report/index.html`

如果测试失败，额外关注：

- `artifacts/test-results` 下的 `trace.zip`
- `artifacts/test-results` 下的 `video.webm`
- `artifacts/test-results` 或 `artifacts/screenshots` 下的截图

## 依赖说明

- Java：脚本默认使用 `C:\Program Files\Java\jdk-21`
- Node / npm：用于启动前端和运行 Playwright
- Playwright 依赖安装目录：`tests/e2e`

如果是首次运行，`run-round8-smoke.ps1` 会自动安装：

- `tests/e2e` 下的 npm 依赖
- Playwright Chromium 浏览器

## 当前稳定演示入口

- 后台：`http://127.0.0.1:5174/batches`
- 公开页：`http://127.0.0.1:5173/t/demo-normal-2026`

## 当前稳定演示样例

- `DEMO-ORANGE-202603-A1`：正常批次
- `DEMO-ORANGE-202603-D1`：草稿批次
- `DEMO-TEA-202603-F1`：冻结批次
- `DEMO-RICE-202603-R1`：召回批次
