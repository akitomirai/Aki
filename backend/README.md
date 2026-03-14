# backend (后端核心服务)

## 1. 项目简介
本项目是农产品溯源系统的核心后端服务，负责处理全链路业务逻辑、数据持久化、安全认证及审计记录。

## 2. 核心架构
- **权限分层 Controller**：按操作角色划分为 `admin`, `platform`, `regulator`, `publicapi` 等包，职责清晰，便于权限控制。
- **业务模块 Module**：按业务模块组织代码（`batch`, `product`, `trace`, `log` 等），包含 Entity, DTO, VO, Mapper, Service。
- **通用能力 Common**：统一异常处理、日志切面、JWT 安全过滤、通用结果封装。

## 3. 技术栈
- **核心框架**：Spring Boot 3.3.x (Spring Framework 6)
- **数据库访问**：MyBatis-Plus 3.5.7
- **安全认证**：Spring Security 6 + JJWT 0.11.5
- **构建工具**：Maven 3.x
- **数据库**：MySQL 8.x
- **Java 版本**：JDK 17

## 4. 目录说明
```text
backend/
├── src/main/java/edu/jxust/agritrace/
│   ├── common/         # 公共核心包 (API 封装, 异常处理, 安全, 日志注解, 工具类)
│   ├── config/         # Spring 配置类 (Security, MyBatis-Plus, OpenAPI)
│   ├── controller/     # 按角色分层的控制层 (Admin/Platform/Regulator/PublicAPI)
│   ├── module/         # 核心业务模块 (按业务细分, 包含 Entity, DTO, VO, Service)
│   └── TraceabilityBackendApplication.java  # 启动类
├── src/main/resources/
│   ├── mapper/         # MyBatis XML 映射文件
│   ├── application.yml # 系统配置文件
│   └── logback-spring.xml # 日志配置
├── docs/               # 核心设计与交付文档 (重要)
├── ../sql/01_schema.sql    # 数据库表结构脚本
├── ../sql/02_seed_dev.sql  # 开发/答辩演示数据脚本
├── ../sql/03_reset_dev.sql # 一键重置开发库脚本
└── pom.xml             # Maven 依赖管理
```

## 5. 本地启动
### 数据库准备
1. 创建数据库 `traceability_cs`（字符集推荐 `utf8mb4`）。
2. 优先执行根目录 `sql/03_reset_dev.sql`，或按顺序执行 `sql/01_schema.sql` 与 `sql/02_seed_dev.sql`。
3. 在 `src/main/resources/application.yml` 中核对数据库与 Redis 连接配置。

### 启动服务
```bash
# 1. 编译并安装依赖
mvn clean install -DskipTests

# 2. 运行 Spring Boot 应用
mvn spring-boot:run
```

## 6. 开发说明
- **重要文档**：新加入开发时，请务必先阅读 [日志模块改造交付说明文档](docs/log-module-delivery.md) 以了解当前项目的架构优化方向和日志审计规范。
- **权限控制**：Controller 层的包路径直接对应权限边界，新增接口时需注意所属角色。
- **编码规范**：业务逻辑严禁在 Controller 编写，必须下沉至 Service 层。

## 7. 与其他子项目的关系
- **服务提供**：为 `admin-web`（管理端）和 `trace-web`（移动端）提供统一的 RESTful API 支持。
- **跨域配置**：通过 `SecurityConfig` 进行 CORS 跨域配置。
