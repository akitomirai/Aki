# 日志模块改造交付说明文档

## 1. 改造背景
为了解决项目初期 `common` 包反向依赖 `module.log` 业务模块导致的循环依赖问题，同时增强系统操作审计的安全性和稳定性，对日志模块进行了系统性重构与功能增强。

## 2. 改动文件清单
| 文件名 | 最终作用 | 改动目的 |
| :--- | :--- | :--- |
| `LogAspect.java` | 核心日志切面类 | 迁移至 `module.log.aspect`，消除包依赖循环；增强异常捕获、参数脱敏与长度截断逻辑。 |
| `AuthController.java` | 认证授权控制器 | 在 `register` 方法补全 `@OperationLogAnnotation`，实现用户注册行为审计。 |
| `FeedbackPublicController.java` | 公众反馈控制器 | 在 `create` 方法补全 `@OperationLogAnnotation`，实现消费者反馈行为审计。 |

## 3. 已完成验证项
- [x] **架构解耦验证**：`common` 包不再依赖 `module.log`，项目分层更加清晰。
- [x] **启动与编译验证**：`mvn clean compile` 通过，Spring AOP 代理挂载正常，不影响主业务启动。
- [x] **成功/异常链路验证**：成功请求（`SUCCESS`）与业务异常（`FAIL`）均能正常触发日志落库。
- [x] **安全性验证**：密码类敏感字段入库前已自动脱敏为 `******`。
- [x] **稳定性验证**：超长请求参数（>2000 字符）自动截断，有效防护数据库存储溢出。

## 4. 当前日志能力边界
### 已支持能力
- **全场景覆盖**：支持成功请求、异常请求、匿名接口（未登录场景）的自动记录。
- **敏感数据保护**：内置正则脱敏引擎，支持 `password`, `token`, `authorization` 等敏感词模糊化。
- **存储溢出保护**：自动截断超长 `request_params`，并在末尾追加 `...(truncated)` 标识。
- **健壮性保障**：日志记录失败不会阻塞或影响主业务返回。

### 暂未支持能力
- **异常详情落库**：受限于当前数据库表结构，暂未单独记录 Exception StackTrace。
- **响应体记录**：当前仅记录 Request Body，未记录 Response Body，主要出于性能考虑。
- **异步处理**：目前日志落库与业务逻辑同步执行（虽然有 try-catch 保护）。

## 5. 测试数据清理建议
验证过程中产生了少量测试脏数据，建议执行以下清理 SQL：
```sql
-- 1. 清理反馈验证数据
DELETE FROM consumer_feedback WHERE content LIKE '%Test Annotation Log%';
-- 2. 清理注册验证数据
DELETE FROM sys_user WHERE username IN ('testuser3', 'testuser4');
-- 3. 清理验证用的临时批次与产品
DELETE FROM trace_batch WHERE batch_code = 'BATCH20260311';
DELETE FROM base_product WHERE id = 1;
```

## 6. 后续优化建议
- **P1 - 异步落库**：使用 `@Async` 将日志保存移出主线程，彻底消除日志对接口响应耗时的影响。
- **P2 - 异常详情补全**：在 `operation_log` 表增加 `error_msg` 字段，配合切面记录异常详情。
- **P3 - 配置化脱敏**：将脱敏字段列表移入 `application.yml`，方便后续动态扩展维护。
