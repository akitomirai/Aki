# 消费者反馈与权限体系改造收口文档

## 1. 本轮改造范围
本轮工作主要围绕“消费者反馈模块的功能实现”与“后端全仓权限体系的规范化修复”展开，确保了反馈链路的完整性与各端（平台、企业、监管、公共）权限的精确隔离。

## 2. 反馈链路完成情况
- **消费者端 (Public)**：打通了 `/api/public/feedback/create` 接口，支持匿名提交建议、投诉与风险举报。
- **企业端 (Enterprise)**：实现了企业侧反馈列表查询与详情查看，支持企业根据所属批次自动过滤反馈。
- **平台端 (Platform)**：实现了全局反馈管理，包括反馈列表查询、处理状态更新、处理结果记录及公开状态切换。
- **数据库**：在 `docker/init/01_schema.sql` 中同步补全了 `consumer_feedback` 表结构及关联外键。

## 3. 权限体系修复情况
- **角色前缀修复**：在 `JwtAuthenticationFilter.java` 中补全了 `ROLE_` 前缀注入，解决了 Spring Security `hasRole` 校验失效的问题。
- **注解一致性**：将全仓 Controller 的权限校验从 `hasAuthority` 统一切换为 `hasRole`，并修正了 `PLATFORM_ADMIN` 等角色名不一致的问题。
- **异常处理增强**：在 `GlobalExceptionHandler.java` 中增加了对 `AccessDeniedException` 的捕获，统一返回 403 错误码及友好提示。

## 4. 冒烟测试结论
经过真实账号联调，确认以下结论：
- **纵向隔离**：平台管理员、企业用户、监管用户仅能访问各自端（`/api/platform/**`, `/api/enterprise/**`, `/api/regulator/**`）的接口。
- **横向隔离**：企业用户只能查询属于自己企业的反馈数据（通过 `company_id` 过滤）。
- **匿名访问**：`/api/public/**` 接口无需 Token 即可访问，后台接口未携带有效 Token 均被 403 拦截。
- **交互友好**：权限不足时，后端能正确返回 `{"code": 403, "message": "权限不足，拒绝访问"}`。

## 5. 保留改动清单
- **核心安全逻辑**：`JwtAuthenticationFilter`, `GlobalExceptionHandler`, `SecurityConfig` 的权限增强。
- **Controller 权限注解**：全仓 13+ 个 Controller 的 `hasRole` 规范化。
- **反馈业务模块**：`FeedbackPlatformController`, `FeedbackEnterpriseController` 及其对应的 Service/Mapper。
- **数据库初始化**：`docker/init/01_schema.sql` 中的反馈表结构。

## 6. 临时数据与清理建议
本轮测试在本地数据库中产生了一些测试数据，如需清理，请执行以下 SQL（**生产环境请慎重**）：

```sql
-- 清理测试反馈数据
DELETE FROM consumer_feedback WHERE content LIKE '%Audit%' OR content LIKE '%Test%';

-- 清理测试账号 (若不需要保留)
DELETE FROM sys_user WHERE username IN ('regulator');

-- 清理测试监管机构
DELETE FROM regulator_org WHERE code = 'REG001';
```

## 7. 下一步建议
1. **监管端业务填充**：目前监管端权限已打通，但业务接口（如抽检记录管理）尚需进一步丰富。
2. **审计日志审计**：建议对 `OperationLogAnnotation` 在各角色的覆盖情况进行一次专项检查。
3. **前端权限匹配**：确认前端菜单权限逻辑是否已同步更新为匹配 `PLATFORM_ADMIN` 等真实角色名。
