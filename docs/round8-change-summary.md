# Round 8 Change Summary

## 本轮目标

本轮只做“真实启动测试自动化”，不扩业务模块，不重做页面视觉，不重写 README。

核心目标是把以下事情固化成可重复执行的一套流程：

- 启动 demo 环境
- 等待服务就绪
- 跑真实 Playwright 冒烟测试
- 输出报告、截图和日志
- 一键停止并清理残留进程

## 本轮主要改动

### 1. 启动与等待机制

- 增强 [start-round7-demo.ps1](C:/Users/Lenovo/Desktop/traceability-system/scripts/start-round7-demo.ps1)，让它真正承担 round8 的启动职责。
- 启动前会检查 `8080 / 5173 / 5174` 端口占用，并先清理旧的 demo 残留进程。
- 启动后会等待：
  - backend health endpoint
  - admin-web 页面可访问
  - trace-web 页面可访问
- 如果任一服务未就绪，会直接输出对应日志尾部并停止已启动服务。

### 2. 停止与清理机制

- 新增 [stop-round8-demo.ps1](C:/Users/Lenovo/Desktop/traceability-system/scripts/stop-round8-demo.ps1)。
- 启动时会写入 `artifacts/logs/round8-processes.json`，停止脚本优先清理已追踪 PID。
- 如果仍有端口残留，会继续清理 demo 端口上的监听进程，尽量避免 java/node 残留。

### 3. Playwright 真实冒烟测试

- 新增 `tests/e2e` 目录和 Playwright 配置。
- 冒烟覆盖三类真实页面场景：
  - 后台管理员链路
  - 消费者公开追溯页链路
  - 风险工作台链路
- 后台链路中实际完成了一次关键动作：生成二维码。
- 公开页链路中验证了正常态与召回风险态。

### 4. 结果产物输出

- HTML report：`artifacts/playwright-report`
- Playwright 原始结果：`artifacts/test-results`
- 成功截图：`artifacts/screenshots`
- 服务日志和测试 JSON：`artifacts/logs`
- 失败时自动保留 trace / video 到 `artifacts/test-results`，并复制到专门目录。

### 5. 最小测试支撑

- 在后台列表页、批次工作台、公开追溯页补了最小 `data-testid`。
- 这些标记只用于稳定测试入口，没有重写页面逻辑，也没有新增复杂测试专用系统。

## 为什么这样改

- 第七轮已经把手工演示链路跑顺了，第八轮要解决的是“怎么稳定重复执行”。
- 真正有价值的不是单次能跑，而是下次、下下次也能一条命令跑通。
- 真实页面冒烟测试能更早发现：
  - 服务没起来
  - 端口冲突
  - 页面关键区域不可见
  - demo 数据失效
  - 关键动作入口失稳

## 本轮覆盖场景

- demo 环境一键启动
- demo 环境一键停止
- 后台批次列表可访问
- 批次工作台可访问
- 公开追溯页可访问
- 风险工作台可访问
- 后台工作台二维码动作可执行

## 验证结果摘要

- `backend`: `mvn compile` 通过
- `backend`: `mvn test` 通过
- `admin-web`: `npm run build` 通过
- `trace-web`: `npm run build` 通过
- `scripts/run-round8-smoke.ps1`: 真实执行通过
- Playwright 冒烟：4 个测试全部通过

## 相关入口

- 总控脚本：[run-round8-smoke.ps1](C:/Users/Lenovo/Desktop/traceability-system/scripts/run-round8-smoke.ps1)
- 启动脚本：[start-round7-demo.ps1](C:/Users/Lenovo/Desktop/traceability-system/scripts/start-round7-demo.ps1)
- 停止脚本：[stop-round8-demo.ps1](C:/Users/Lenovo/Desktop/traceability-system/scripts/stop-round8-demo.ps1)
- 预检脚本：[demo-precheck.ps1](C:/Users/Lenovo/Desktop/traceability-system/scripts/demo-precheck.ps1)
- 测试目录：`tests/e2e`
