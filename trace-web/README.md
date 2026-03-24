# trace-web (移动端溯源前端)

## 1. 项目简介
本项目是农产品溯源系统的移动端/H5 查询前端，主要面向终端消费者。

## 2. 核心功能
- **扫码查询**：扫描农产品上的溯源码，实时查看产品信息。
- **追溯链路**：展示产品从原产地、生产加工到物流质检的全生命周期时间轴。
- **质检报告**：在线查阅权威机构出具的质检报告。
- **用户反馈**：消费者可针对产品质量或溯源信息的真实性进行在线反馈。

## 3. 技术栈
- **框架**：Vue 3 (Composition API)
- **构建工具**：Vite
- **UI 组件库**：Vant 4 (移动端组件) + Element Plus (部分适配)
- **路由管理**：Vue Router
- **网络请求**：Axios

## 4. 目录说明
```text
trace-web/
├── src/
│   ├── api/          # 移动端接口层 (对接 backend 的 publicapi)
│   ├── assets/       # 静态资源 (图片, 样式)
│   ├── composables/  # 移动端业务逻辑 hooks
│   ├── pages/        # 核心查询页面组件
│   ├── router/       # 移动端路由配置
│   ├── views/        # 页面视图容器
│   ├── App.vue       # 移动端根组件
│   └── main.js       # 入口文件
├── .env.development  # 移动端环境配置
├── index.html        # 入口 HTML
└── package.json      # 移动端依赖与脚本
```

## 5. 本地启动
```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev

# 3. 项目打包
npm run build
```

## 6. 开发说明
- **接口安全**：移动端主要对接 `backend` 的 `publicapi` 接口，这部分接口通常无需登录认证。
- **移动端适配**：主要使用 Vant 4 库，并进行了移动端响应式布局优化。
- **逻辑复用**：复杂的查询逻辑封装在 `src/composables/` 中。

## 7. 与其他子项目的关系
- **对接后端**：通过 `src/api/trace.js` 对接 `backend` 项目提供的 `publicapi` 系列接口。
- **数据源**：展示的数据来源于 `admin-web` 在管理端维护并由 `backend` 持久化的产品溯源数据。
