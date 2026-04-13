# 数据库脚本整理说明

- 当前数据库脚本查阅入口统一放在 `docs/database/sql/`。
- 原顶层 `sql/` 目录已按原样归档到 `archive/sql-original/sql/`，便于保留历史上下文。
- `docs/database/sql/03_reset_dev.sql` 已调整为引用同目录脚本，方便在文档目录中直接查看脚本关系。
- 历史 Docker 初始化脚本保留在 `archive/docker/docker/init/`，需要对照旧容器初始化方案时可从那里查看。
- 后端演示与测试当前仍主要依赖 `backend/src/main/resources/` 和 `backend/src/test/resources/` 下的 H2 脚本；这里保留的是项目原始 MySQL/初始化脚本整理入口。
