# traceability-system

农产品质量安全追溯系统，包含管理后台、后端服务和公开追溯页三端。当前仓库已整理为本地开发可启动、答辩演示可复现、开发数据可重置的状态。

## 项目组成

- `backend`
  Spring Boot 3 + MyBatis-Plus + Spring Security + Redis。负责认证、产品、批次、二维码、公开追溯、日志和 PV/UV 统计。
- `admin-web`
  Vue 3 + Vite + Element Plus。用于平台管理员和企业账号登录后的业务管理与演示。
- `trace-web`
  Vue 3 + Vite。用于公开追溯页展示，消费者或老师通过追溯链接查看档案。
- `docs`
  账号、演示流程、交付说明等文档。
- `sql`
  数据库表结构、开发种子数据、开发重置脚本。
- `scripts`
  本地演示前自检脚本。

## 默认端口

- `backend`: `http://127.0.0.1:8080`
- `admin-web`: `http://127.0.0.1:5174`
- `trace-web`: `http://127.0.0.1:5173`
- Swagger: `http://127.0.0.1:8080/swagger-ui.html`

## 环境依赖

- JDK 17
- Maven 3.9+
- Node.js 20+
- MySQL 8.0
- Redis 7.x

## Redis 用途

Redis 只用于公开追溯访问统计：

- `PV`: 追溯页总访问次数
- `UV`: 去重访客人数

说明：

- 这部分统计不写入 SQL 种子数据。
- 重置 MySQL 不会自动清空 Redis 统计。
- 如需从 0 开始演示 PV/UV，请单独清理 Redis 中 `agri:trace:*` 相关 key。

## 数据库脚本

- [sql/01_schema.sql](/Users/Lenovo/Desktop/traceability-system/sql/01_schema.sql)
  表结构脚本，只包含建表和约束。
- [sql/02_seed_dev.sql](/Users/Lenovo/Desktop/traceability-system/sql/02_seed_dev.sql)
  精简开发演示数据，包含默认账号、演示企业、产品、批次、追溯码、节点、质检、监管、日志和反馈。
- [sql/03_reset_dev.sql](/Users/Lenovo/Desktop/traceability-system/sql/03_reset_dev.sql)
  一键重建 `traceability_cs` 并导入结构与演示数据。

推荐执行方式：

```bash
mysql -uroot -p < sql/03_reset_dev.sql
```

如果只导入现有库：

```bash
mysql -uroot -p traceability_cs < sql/01_schema.sql
mysql -uroot -p traceability_cs < sql/02_seed_dev.sql
```

## 启动顺序

1. 启动 MySQL 和 Redis
2. 导入开发数据脚本
3. 启动 `backend`
4. 启动 `admin-web`
5. 启动 `trace-web`

### backend

```bash
cd backend
mvn spring-boot:run
```

### admin-web

```bash
cd admin-web
npm install
npm run dev
```

### trace-web

```bash
cd trace-web
npm install
npm run dev
```

## 默认账号

详见 [docs/ACCOUNTS.md](/Users/Lenovo/Desktop/traceability-system/docs/ACCOUNTS.md)。

常用账号：

- 平台管理员：`platform / 123456`
- 企业用户：`testuser1 / 123456`

## 演示主链路

详见 [docs/DEMO_FLOW.md](/Users/Lenovo/Desktop/traceability-system/docs/DEMO_FLOW.md)。

推荐顺序：

1. 平台账号登录后台
2. 查看 Dashboard 统计与公开追溯快捷入口
3. 查看产品列表
4. 查看批次列表并进入批次详情
5. 在批次详情页查看追溯码、复制链接、预览二维码
6. 打开公开追溯页：`http://127.0.0.1:5173/t/test-token-2026`
7. 返回 Dashboard 观察 PV/UV 变化

## 当前主演示数据

- 主演示企业：`赣南脐橙示范基地`
- 主演示产品：`赣南脐橙`
- 主演示批次：`BATCH20260311001`
- 主演示 token：`test-token-2026`

## 补充文档

- [docs/README.md](/Users/Lenovo/Desktop/traceability-system/docs/README.md)
- [docs/ACCOUNTS.md](/Users/Lenovo/Desktop/traceability-system/docs/ACCOUNTS.md)
- [docs/DEMO_FLOW.md](/Users/Lenovo/Desktop/traceability-system/docs/DEMO_FLOW.md)
