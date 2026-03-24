# Docker 说明

`docker/` 目录保留了演示环境的一键启动方案，但当前项目第一轮改造的重点仍然是主链路收敛，而不是容器编排本身。

## 包含内容
- `mysql`
- `backend`
- `admin-web`
- `trace-web`

## 配置方式
`docker-compose.yml` 已经移除了硬编码密码表达，改为优先读取环境变量：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`

如果未显式提供，会使用演示默认值。

## 说明
当前后端为了保证仓库可运行，使用内存演示数据即可启动；SQL 与 MySQL 容器仍然保留，便于下一轮接回真实持久化。
