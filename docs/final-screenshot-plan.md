# 定稿截图计划

## 1. 当前截图资产判断

当前已有截图已经覆盖了以下关键页面或场景：

- 批次列表
- 已发布工作台
- 风险工作台
- 监管只读页面
- 现场录入草稿恢复与提交成功
- 正常公开页
- 风险公开页
- 无效码公开页

这些截图足够支撑内部回归记录，但还不够直接用于最终材料，主要有两个问题：

- 命名风格混杂，存在 `round8/round11/round12` 历史前缀，不适合直接进入定稿材料。
- 缺少一套按“最终演示顺序”组织的定稿截图集。

结论：

- 当前截图对测试足够。
- 对最终材料而言，建议再补一组“统一命名、统一基线、统一顺序”的定稿截图。

## 2. 建议补拍范围

建议补拍以下页面：

1. 平台管理员首页总览
2. 批次列表总览
3. 主演示批次工作台
4. 现场录入待办页
5. 现场录入成功页
6. 质检待办页
7. 风险处理页
8. 二维码与发布管理页
9. 正常公开追溯页
10. 风险公开追溯页
11. 无效码公开追溯页
12. 监管只读风险页或监管只读批次页

## 3. 建议拍摄顺序

建议按答辩讲解顺序拍摄：

1. `dashboard`
2. `batch-list`
3. `workbench-main-batch`
4. `field-entry-todo`
5. `field-entry-success`
6. `quality-todo`
7. `risk-todo`
8. `qr-publish-manage`
9. `public-trace-normal`
10. `public-trace-risk`
11. `public-trace-invalid`
12. `regulator-readonly`

## 4. 建议命名规范

建议统一命名为：

```text
final-01-dashboard-platform.png
final-02-batch-list-platform.png
final-03-workbench-orange-202603-d1.png
final-04-field-entry-todo.png
final-05-field-entry-success.png
final-06-quality-todo.png
final-07-risk-todo.png
final-08-qr-publish-manage.png
final-09-public-trace-normal.png
final-10-public-trace-risk.png
final-11-public-trace-invalid.png
final-12-regulator-readonly.png
```

## 5. 拍摄时的固定基线

建议拍摄前固定以下条件：

- 先执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-system-smoke.ps1 -SkipInstall
```

- 使用主演示账号：`platform / 123456`
- 主演示批次：`ORANGE-202603-D1`
- 正常公开码：`orange-202603-d1`
- 风险公开码：`demo-recall-2026`
- 无效码：`invalid-demo-token-2026`

## 6. 拍摄建议

- 优先使用桌面端截图后台页面，保持信息完整。
- 公开页建议保留一张桌面版；如果论文需要强调消费者视角，可额外补一张移动端截图。
- 不建议继续复用历史 `round8 / round11 / round12` 前缀截图进入最终材料，避免材料命名不统一。
