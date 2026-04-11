# 最终演示基线

## 1. 后台主入口页

- 登录页：`http://127.0.0.1:5174/login`
- 登录后的主入口页：`/dashboard`
- 推荐从首页总览开始讲，再进入批次列表和批次工作台

## 2. 主演示账号

- 主演示账号：`platform / 123456`
- 监管只读补充账号：`regulator / 123456`

说明：

- `platform` 账号适合完整展示后台主链路、风险链路和二维码/公开页联动。
- `regulator` 账号只建议在讲“权限边界”和“监管只读视角”时单独登录，不建议与主链路混用。

## 3. 主演示批次

- 主演示批次：`ORANGE-202603-D1`
- 批次 ID：`2`
- 推荐入口：`http://127.0.0.1:5174/batches/2`

当前稳定状态：

- 批次状态：`已发布`
- 质检状态：`合格`
- 二维码状态：`已生成`
- 公开追溯已可访问

这个批次适合作为“系统主链路已经跑通”的最终稳定样例。

## 4. 正常公开码

- 正常公开码：`orange-202603-d1`
- 推荐公开页地址：`http://127.0.0.1:5173/t/orange-202603-d1`

说明：

- 这个地址与主演示批次 `ORANGE-202603-D1` 一一对应，适合展示消费者视角下的正常追溯链路。

## 5. 风险公开码

- 风险公开码：`demo-recall-2026`
- 推荐公开页地址：`http://127.0.0.1:5173/t/demo-recall-2026`

说明：

- 这个地址适合展示风险横幅、召回提示和公开页风险状态联动。

## 6. 无效码示例

- 无效码示例：`invalid-demo-token-2026`
- 推荐公开页地址：`http://127.0.0.1:5173/t/invalid-demo-token-2026`

说明：

- 当前无效码页面已收口为友好错误态，适合答辩时解释系统异常兜底能力。

## 7. 推荐演示顺序

推荐顺序如下：

1. 登录 `platform / 123456`
2. 进入首页总览 `/dashboard`
3. 打开批次列表 `/batches`
4. 进入主演示批次 `/batches/2`
5. 在工作台说明：追溯、质检、二维码、发布状态已经收口
6. 打开正常公开页 `/t/orange-202603-d1`
7. 返回后台展示风险批次或风险页 `/risk`
8. 打开风险公开页 `/t/demo-recall-2026`
9. 切换 `regulator / 123456`，展示只读视角
10. 最后用 `/t/invalid-demo-token-2026` 说明异常兜底

## 8. 演示前建议动作

在正式演示前，建议固定执行一次：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-system-smoke.ps1 -SkipInstall
```

如果只想启动本地服务，不立刻跑回归：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```
