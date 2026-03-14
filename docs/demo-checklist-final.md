# 演示前检查清单 (Checklist)

本 Checklist 用于在正式答辩演示前进行全方位环境确认。

## 1. 启动顺序与环境检查 (必查)
1. **启动后端**：进入 `backend` 目录执行 `mvn spring-boot:run`。
2. **启动管理端**：进入 `admin-web` 目录执行 `npm run dev`。
3. **启动追溯端**：进入 `trace-web` 目录执行 `npm run dev`。
4. **运行自检脚本**：在根目录运行 `powershell -ExecutionPolicy Bypass -File scripts/demo-precheck.ps1`。
5. **确认结果**：确保脚本输出全部为 **[PASS]**。

## 2. 服务运行状态细节
- [ ] **Backend (Spring Boot)**
  - [ ] 端口 8080 是否被占用
  - [ ] 是否启动成功 (终端无 ERROR)
- [ ] **Admin-Web (Vite)**
  - [ ] 端口 5174 是否启动
- [ ] **Trace-Web (Vite)**
  - [ ] 端口 5173 是否启动
- [ ] **MySQL**
  - [ ] 数据库 `traceability_cs` 连通性
  - [ ] 用户 `root` 权限检查

## 3. 数据与功能验证 (自检通过后)
- [ ] **登录检查**
  - [ ] 账号 `platform` / `123456` 是否可登录
  - [ ] 账号 `testuser1` / `123456` 是否可登录
- [ ] **批次详情**
  - [ ] `BATCH20260311001` 是否存在并可查询
- [ ] **追溯码**
  - [ ] `http://localhost:5173/trace/test-token-2026` 是否能展示完整信息

## 3. 常见问题 (故障排除)
- **问题：页面加载缓慢**
  - **排查**：检查后端日志，确认数据库连接池是否正常。
- **问题：新录入节点在追溯页不显示**
  - **排查**：确认“消费者可见 (isPublic)”开关是否开启。
- **问题：登录提示“用户名或密码错误”**
  - **排查**：通过 MySQL 执行 `SELECT username, password FROM sys_user` 检查账号。

## 4. 备选演示方案 (Plan B)
- **方案 A (正常演示)**: 使用真实前端页面进行全链路演示。
- **方案 B (离线演示)**: 使用录屏或 PPT 截图 (如前端/后端环境突然崩溃)。
- **方案 C (接口演示)**: 直接使用 Postman 调用后端 API 展示返回 JSON 结构 (证明后端能力)。
