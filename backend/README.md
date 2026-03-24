# backend

## 项目角色
`backend` 是当前仓库的接口与演示数据服务层，负责把“批次中心”的主链路串起来。

## 本轮方向
这一轮后端不再朝“大而全后台”扩张，而是优先保证以下能力：

- 批次列表
- 批次工作台详情
- 过程记录补录入口
- 质检补录入口
- 二维码生成入口
- 批次状态流转
- 面向消费者的公开追溯查询

## 当前实现说明
- 使用统一返回结构 `ApiResponse`
- Controller 只负责接口收发
- 批次工作台与公开追溯页使用独立 VO
- 状态收敛为 `DRAFT / PUBLISHED / FROZEN / RECALLED`
- 为了保证仓库可运行，本轮后端使用内存演示数据

## 为什么先用内存演示数据
当前仓库存在历史源码缺失问题，但 SQL 结构仍然保留。为了不让项目停留在“只剩文档和表结构”的状态，本轮先补一套可编译、可启动、可演示的后端骨架，后续再按 `sql/` 中的结构接回 MySQL 与 MyBatis-Plus。

## 本地运行
```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="C:\Program Files\Java\jdk-21\bin;$env:Path"
cd backend
mvn spring-boot:run
```

## 关键接口
- `GET /api/dashboard/overview`
- `GET /api/batches`
- `GET /api/batches/{id}`
- `POST /api/batches`
- `POST /api/batches/{id}/status`
- `POST /api/batches/{id}/records`
- `POST /api/batches/{id}/quality-reports`
- `POST /api/batches/{id}/qr`
- `GET /api/public/traces/{token}`

## 后续优先事项
1. 把内存演示数据替换成数据库持久化
2. 接入企业建档和批次创建的真实表单提交
3. 打通质检上传、二维码发布和状态留痕
