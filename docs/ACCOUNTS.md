# 最终演示账号清单

以下账号固定用于最终答辩演示，基于 `backend/src/main/resources/data-demo.sql` 的 demo baseline。

## 固定账号

| 账号 | 密码 | 角色 | 所属企业 | 默认进入页面 | 默认用途 |
| --- | --- | --- | --- | --- | --- |
| `platform` | `123456` | 平台管理员 | 无 | `/dashboard` | 主线讲解、全局总览、批次工作台、质检、风险、二维码与发布 |
| `enterprise_admin` | `123456` | 企业管理员 | 赣南果业种植有限公司 | `/batches` | 企业视角讲解批次管理、质检待办、二维码发布 |
| `operator` | `123456` | 现场操作员 | 赣南果业种植有限公司 | `/field-entry` | 现场作业、任务待办、现场填报链路 |
| `regulator` | `123456` | 监管人员 | 无 | `/risk` | 只读查看、权限边界、监管视角讲解 |

## 辅助账号

| 账号 | 密码 | 角色 | 所属企业 | 用途 |
| --- | --- | --- | --- | --- |
| `operator_support` | `123456` | 现场操作员 | 赣南果业种植有限公司 | 明星演示批次 `ORANGE-202603-D1` 的固定执行人 |

## 使用建议

1. 主线答辩优先使用 `platform / 123456`。
2. 企业视角单独切换 `enterprise_admin / 123456`。
3. 现场作业入口单独切换 `operator / 123456`。
4. 权限边界与只读监管视角单独切换 `regulator / 123456`。

## 说明

- 以上账号由 demo profile 固定灌入。
- 建议答辩前执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\restore-final-demo.ps1
```
