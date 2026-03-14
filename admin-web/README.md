# admin-web (后台管理前端)

## 1. 项目简介
本项目是农产品溯源系统的后台管理前端，主要面向生产企业用户和政府监管机构。

## 2. 核心功能
- **企业端**：生产批次管理、溯源码生成、质检报告上传、溯源事件记录、消费者反馈处理。
- **监管端**：企业入驻审核、产品准入管理、追溯过程监管、监管日志审计。
- **基础模块**：用户认证、权限管理、个人信息维护。

## 3. 技术栈
- **框架**：Vue 3 (Composition API)
- **构建工具**：Vite
- **UI 组件库**：Element Plus
- **状态管理**：Pinia
- **图表库**：ECharts
- **网络请求**：Axios

## 4. 目录说明
```text
admin-web/
├── src/
│   ├── api/          # 后端接口对接层
│   ├── assets/       # 静态资源
│   ├── composables/  # 业务逻辑提取 (Hooks)
│   ├── layouts/      # 页面布局组件
│   ├── pages/        # 业务页面组件
│   ├── router/       # 路由配置
│   ├── stores/       # Pinia 状态管理
│   ├── utils/        # 通用工具类
│   ├── App.vue       # 根组件
│   └── main.js       # 入口文件
├── .env.development  # 开发环境配置（包含后端接口地址）
├── index.html        # 入口 HTML
└── package.json      # 项目依赖与脚本
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
- **接口对接**：所有接口统一定义在 `src/api/` 下，根据业务模块划分。
- **逻辑复用**：复杂的页面逻辑优先提取到 `src/composables/` 中。
- **样式规范**：通用样式位于 `src/assets/styles/`。

## 7. 与其他子项目的关系
- **对接后端**：通过 `src/api/http.js` 对接 `backend` 项目提供的 RESTful 接口。
- **环境配置**：在 `.env.development` 中配置 `VITE_API_BASE_URL` 指向后端服务地址。
