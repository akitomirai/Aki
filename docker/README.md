# 农产品质量安全溯源系统 - 容器化部署指南

本目录提供了项目的最小可演示 Docker 部署方案，支持一键启动整个演示环境。

## 1. 包含组件
- **MySQL (8.0)**: 数据库服务，挂载了 `init/01_schema.sql` 自动初始化结构。
- **Backend (Spring Boot)**: 后端接口服务。
- **Admin-Web (Vue3)**: 平台管理后台。
- **Trace-Web (Vue3)**: 消费者溯源展示页。

## 2. 部署前准备
- 确保已安装 [Docker](https://www.docker.com/) 和 [Docker Compose](https://docs.docker.com/compose/install/)。
- 确保本地 3306, 8080, 5173, 5174 端口未被占用。

## 3. 一键启动
在项目根目录或 `docker/` 目录下运行：

```bash
cd docker
docker-compose up -d --build
```

## 4. 访问地址
| 组件 | 访问地址 | 说明 |
| :--- | :--- | :--- |
| **平台管理后台** | [http://localhost:5174](http://localhost:5174) | 管理员、企业、监管端入口 |
| **消费者溯源页** | [http://localhost:5173](http://localhost:5173) | 溯源扫码展示页 |
| **后端接口文档** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Swagger 接口文档 |

## 5. 初始账号 (演示使用)
请参考 `backend/traceability.sql` 或 `docs/feedback-and-permission-delivery.md` 中的账号说明进行登录。

## 6. 注意事项
- 第一次启动时，MySQL 容器初始化数据库结构可能需要几秒钟，Backend 会等待 MySQL 就绪后自动启动。
- 若需更新代码，请重新运行 `docker-compose up -d --build`。
- 本方案采用 Nginx 反向代理模式，前端访问后端 API 的请求会自动通过容器内部网络转发。
