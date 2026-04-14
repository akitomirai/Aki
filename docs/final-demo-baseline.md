# 最终答辩 Demo Baseline

本文档用于固定最终答辩使用的演示企业、产品、批次、公开 token、重置方式与讲解顺序。

## 1. 固定演示对象

### 主演示企业

- 企业名称：`赣南果业种植有限公司`
- 企业定位：主演示企业
- 说明：平台管理员、企业管理员、现场操作员三条主链路都围绕这家企业展开

### 主演示产品

| 产品编码 | 产品名称 | 所属企业 | 用途 |
| --- | --- | --- | --- |
| `ORANGE-001` | `赣南脐橙` | 赣南果业种植有限公司 | 主讲产品，覆盖建档、现场记录、质检、二维码、公开发布 |
| `TEA-001` | `明前绿茶` | 婺源茶业合作社 | 风险处理中样例，用于补讲冻结与整改 |

### 支撑性产品

| 产品编码 | 产品名称 | 所属企业 | 用途 |
| --- | --- | --- | --- |
| `RICE-001` | `优质大米` | 南昌优质稻米中心 | 召回与公开风险提示样例 |

## 2. 固定演示批次

| 批次编码 | 当前状态 | 所属企业 | 所属产品 | 用途说明 | 公开查询地址 / token |
| --- | --- | --- | --- | --- | --- |
| `ORANGE-202603-D1` | 已发布 | 赣南果业种植有限公司 | 赣南脐橙 | 明星演示批次，完整生命周期主线 | `/t/orange-202603-d1` |
| `ORANGE-202604-Q1` | 草稿 / 待质检 | 赣南果业种植有限公司 | 赣南脐橙 | 待处理样例，给企业管理员和现场操作员演示用 | 无 |
| `TEA-202603-F1` | 已冻结 | 婺源茶业合作社 | 明前绿茶 | 风险处理中样例 | `demo-frozen-2026` |
| `RICE-202603-R1` | 已召回 | 南昌优质稻米中心 | 优质大米 | 召回与风险公开提示样例 | `/t/demo-recall-2026` |
| `ORANGE-202603-A1` | 已发布 | 赣南果业种植有限公司 | 赣南脐橙 | 第二条稳定公开查询样例 | `/t/demo-normal-2026` |

## 3. 明星演示批次

### 主批次

- 批次编码：`ORANGE-202603-D1`
- 后台入口：`/batches/2`
- 公开 token：`orange-202603-d1`
- 公开地址：`http://127.0.0.1:5173/t/orange-202603-d1`

### 明星批次固定讲解顺序

1. 使用 `platform / 123456` 登录后台。
2. 进入 `/batches`，说明这是主演示企业的核心批次。
3. 进入 `/batches/2`，讲批次工作台。
4. 在工作台中说明：
   - 已完成建档
   - 已固定任务分配
   - 已有现场追溯记录
   - 已上传质检结果
   - 已生成二维码
   - 已完成公开发布
5. 打开 `/qr`，说明该批次绑定固定 token `orange-202603-d1`。
6. 打开公开页 `/t/orange-202603-d1`，从消费者视角讲解首屏摘要、时间轴、质检结果和风险状态。

### 每一步看什么

| 页面 | 看点 |
| --- | --- |
| `/batches` | 批次状态、负责人、任务状态 |
| `/batches/2` | 明星批次全链路工作台 |
| `/quality` | 明星批次已完成质检，可对照待处理批次 |
| `/qr` | 固定公开 token 与二维码发布状态 |
| `/t/orange-202603-d1` | 消费者视角的最终查询结果 |

## 4. 为“溯源时间轴回放”预留的事件数据

明星批次 `ORANGE-202603-D1` 已补齐以下时间顺序事件：

| 时间 | 操作人 / 角色 | 动作名称 | 结果状态 | 数据位置 |
| --- | --- | --- | --- | --- |
| `2026-03-20 09:10` | 企业管理员 / ENTERPRISE_ADMIN | 批次建档 | SUCCESS | `trace_batch` + `operation_audit_log` |
| `2026-03-20 09:25` | 平台管理员 / PLATFORM_ADMIN | 任务分配 | SUCCESS | `trace_event` + `operation_audit_log` |
| `2026-03-20 14:20` | 现场操作员B / OPERATOR | 现场采后分拣完成 | SUCCESS | `trace_event` + `operation_audit_log` |
| `2026-03-21 08:40` | 现场操作员B / OPERATOR | 冷链交接完成 | SUCCESS | `trace_event` |
| `2026-03-21 10:50` | 企业管理员 / ENTERPRISE_ADMIN | 质检结果确认 | PASS | `quality_report` + `trace_event` + `operation_audit_log` |
| `2026-03-21 11:00` | 平台管理员 / PLATFORM_ADMIN | 二维码生成 | SUCCESS | `qr_code` + `trace_event` + `operation_audit_log` |
| `2026-03-21 11:30` | 企业管理员 / ENTERPRISE_ADMIN | 公开发布 | SUCCESS | `batch_status_log` + `trace_event` + `operation_audit_log` |

待处理批次 `ORANGE-202604-Q1` 也已准备了后续可延展数据：

| 时间 | 操作人 / 角色 | 动作名称 | 结果状态 | 数据位置 |
| --- | --- | --- | --- | --- |
| `2026-04-02 08:25` | 企业管理员 / ENTERPRISE_ADMIN | 建档完成 | SUCCESS | `trace_event` + `operation_audit_log` |
| `2026-04-02 11:10` | 现场操作员 / OPERATOR | 首轮现场记录完成 | SUCCESS | `trace_event` + `operation_audit_log` |

## 5. 重置与恢复

### 推荐一键恢复方式

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\restore-final-demo.ps1
```

这条命令会依次执行：

1. `stop-local.ps1`
2. `reset-demo-baseline.ps1`
3. `start-local.ps1`
4. `check-local.ps1`
5. `demo-precheck.ps1`
6. `verify-final-demo-baseline.ps1`

### 分步执行方式

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\reset-demo-baseline.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo-precheck.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-final-demo-baseline.ps1
```

## 6. 固定公开查询地址

| 用途 | 地址 |
| --- | --- |
| 明星批次公开查询 | `http://127.0.0.1:5173/t/orange-202603-d1` |
| 正常公开样例 | `http://127.0.0.1:5173/t/demo-normal-2026` |
| 风险召回样例 | `http://127.0.0.1:5173/t/demo-recall-2026` |
| 无效 token 兜底样例 | `http://127.0.0.1:5173/t/invalid-demo-token-2026` |

## 7. 答辩主线建议

优先主线：

1. `platform` 登录
2. `/dashboard`
3. `/batches`
4. `/batches/2`
5. `/qr`
6. `/t/orange-202603-d1`
7. `/risk`
8. `regulator` 登录补讲只读权限

这条主线最完整，也最容易讲清楚“后台管理 -> 过程留痕 -> 质检 -> 二维码 -> 公开追溯”的闭环价值。
