# 本轮变更总结

## 修改了哪些文件
### 文档
- `README.md`
- `docs/project-reorientation.md`
- `docs/frontend-direction.md`
- `docs/change-summary.md`
- `docs/README.md`
- `backend/README.md`

### 后端
- `backend/pom.xml`
- `backend/src/main/java/...`
- `backend/src/main/resources/application.yml`

### 管理端
- `admin-web/src/router/index.js`
- `admin-web/src/api/http.js`
- `admin-web/src/api/dashboard.js`
- `admin-web/src/api/batch.js`
- `admin-web/src/pages/DashboardView.vue`
- `admin-web/src/pages/BatchListView.vue`
- `admin-web/src/pages/BatchWorkbenchView.vue`
- `admin-web/src/assets/styles/global.css`
- `admin-web/src/style.css`
- `admin-web/.env.development`

### 公开端
- `trace-web/src/router/index.js`
- `trace-web/src/api/http.js`
- `trace-web/src/api/trace.js`
- `trace-web/src/views/TraceDetailView.vue`
- `trace-web/src/style.css`
- `trace-web/.env.development`

### 配置
- `docker/docker-compose.yml`

## 每类修改的目的
### 文档重写
把项目表达从“功能列表”改成“场景驱动 + 批次中心 + 扫码追溯”。

### 后端重建
在原源码缺失的情况下，先补出一套可以编译、可以启动、可以演示主链路的统一接口骨架。

### 管理端收敛
把后台页面压缩成最有价值的三类页面，降低理解成本与跳转成本。

### 公开端重组
让公开查询结果更像消费者看得懂的追溯页，而不是后台字段回显。

### 配置清理
去掉明显不合适的默认值与硬编码密码表达，降低展示与交付风险。

## 哪些已完成
- 项目方向文档补齐
- README 全量重写
- 后端统一返回结构落地
- 批次主线接口落地
- 批次工作台数据结构落地
- 公开追溯返回结构落地
- 批次核心状态收敛
- 管理端最小可用页面落地
- 公开端最小可用页面落地
- 后端编译通过
- 管理端构建通过
- 公开端构建通过
- 后端启动与关键接口探测通过

## 哪些只完成了一部分
- MySQL / MyBatis-Plus 真正持久化：保留了 SQL 与依赖，但本轮后端先使用内存演示数据保证可运行
- 后台动作提交：工作台已预留动作入口，尚未补完整表单弹窗和提交链路
- 企业建档页：README 和方向文档已经纳入重点，页面本轮尚未单独展开
- 监管专用页面：状态流转已具备，但独立监管工作区未展开

## 哪些问题仍待后续处理
- 仓库中历史暂存文件引用了缺失的 Git 对象，说明这份仓库本身存在源码不完整问题
- 现有 SQL、Docker、文档和本轮内存服务之间，仍属于“先保演示、后接数据库”的过渡状态
- 前端构建产物体积偏大，后续可以继续做按页拆包

## 推荐下一步开发顺序
1. 先把企业建档页和批次创建表单真正做成可提交
2. 再把批次工作台中的“补录过程 / 上传质检 / 生成二维码 / 发布”串成真实操作
3. 然后把内存演示服务替换成 MySQL + MyBatis-Plus 持久化
4. 最后再补监管页、权限和统计能力
