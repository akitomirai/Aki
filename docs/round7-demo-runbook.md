# Round 7 Demo Runbook

## 目标

答辩当天只做三件事：

1. 启动 backend demo profile
2. 启动 admin-web
3. 启动 trace-web

如果你不想手工一条条敲命令，优先直接运行一键脚本。

---

## 一键启动

在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-round7-demo.ps1
```

启动后默认地址：

- 后台批次列表：`http://127.0.0.1:5174/batches`
- 公开追溯页：`http://127.0.0.1:5173/t/demo-normal-2026`

启动后如需快速确认环境是否就绪，再执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-precheck.ps1
```

---

## 后端启动

### 推荐方式

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path=\"$env:JAVA_HOME\\bin;$env:Path\"
cd .\backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

### 说明

- `demo` profile 使用内置 H2 内存库
- 启动后会自动初始化 schema 和演示数据
- demo 二维码图片会在启动时自动预热生成

### 数据库准备

不需要额外准备 MySQL。  
`demo` profile 会直接使用 H2，并自动导入：

- `backend/src/main/resources/schema-demo.sql`
- `backend/src/main/resources/data-demo.sql`

---

## 前端启动

### admin-web

```powershell
cd .\admin-web
npx vite --host 127.0.0.1 --port 5174
```

### trace-web

```powershell
cd .\trace-web
npx vite --host 127.0.0.1 --port 5173
```

---

## 演示账号

当前演示版后台无需登录拦截。  
答辩时可直接打开后台页面，不需要输入账号密码。

---

## 推荐演示批次 / Token

### 1. 正常批次

- 批次号：`DEMO-ORANGE-202603-A1`
- Token：`demo-normal-2026`
- 用途：消费者扫码页、正常链路、二维码演示

### 2. 草稿批次

- 批次号：`DEMO-ORANGE-202603-D1`
- 用途：企业管理员主流程、补追溯、传质检、生成二维码前的缺失项提示

### 3. 冻结批次

- 批次号：`DEMO-TEA-202603-F1`
- Token：`demo-frozen-2026`
- 用途：风险处理中、恢复发布前检查项、整改进度演示

### 4. 召回批次

- 批次号：`DEMO-RICE-202603-R1`
- Token：`demo-recall-2026`
- 用途：监管 / 答辩场景、公开页风险提示、处理历史展示

---

## 推荐演示顺序

1. 先开后台批次列表，看“下一步推荐”和缺失项
2. 进入草稿批次 `DEMO-ORANGE-202603-D1` 的工作台
3. 展示快速补录、质检上传、二维码入口和完成检查项
4. 打开正常批次 `DEMO-ORANGE-202603-A1` 的公开页
5. 展示冻结批次或召回批次的风险区和处理历史

---

## 常见问题

### 后端起不来

- 先确认 `JAVA_HOME` 指向 JDK 21
- 再确认使用的是 `demo` profile，不要直接走默认 MySQL 配置

### 前端打不开数据

- 确认 backend 在 `127.0.0.1:8080`
- 确认 admin-web 在 `5174`
- 确认 trace-web 在 `5173`
- 直接运行 `scripts/demo-precheck.ps1`

### 公开页二维码图片不显示

- demo profile 启动时会自动预热二维码图片
- 如果仍异常，重启 backend demo profile 后再试
