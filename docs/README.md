# 项目文档导航 (Project Documentation Index)

欢迎使用农产品质量安全溯源系统文档库。本目录旨在汇集项目全生命周期的设计、开发、运维及交付文档。

## 1. 核心文档清单

| 文档名称 | 文件路径 | 用途说明 |
| :--- | :--- | :--- |
| **项目总览 (README)** | [README.md](../README.md) | 整个仓库的入口，包含项目简介、结构、技术栈及快速启动指南。 |
| **反馈与权限改造交付** | [feedback-and-permission-delivery.md](./feedback-and-permission-delivery.md) | 消费者反馈模块功能打通、全仓权限注解规范化、角色鉴权冒烟测试报告。 |
| **模块交付说明** | [log-module-delivery.md](../backend/docs/log-module-delivery.md) | 后端日志模块重构深度文档，涵盖 AOP 审计、脱敏规范及重构细节。 |
| **项目架构图 (PNG)** | [项目架构.png](../项目架构.png) | 系统的分层架构示意图，展示前后端交互及模块划分。 |
| **核心系统架构 (SVG)** | [核心系统架构.svg](../核心系统架构.svg) | 矢量格式的系统架构图，方便缩放查看核心组件关系。 |

## 2. 文档说明
- **[README.md](../README.md)**: 开发者首选阅读文档，快速建立对项目的整体认知。
- **[feedback-and-permission-delivery.md](./feedback-and-permission-delivery.md)**: 了解本轮消费者反馈业务链路及系统级角色权限控制体系的最新实现。
- **[log-module-delivery.md](../backend/docs/log-module-delivery.md)**: 适合后端开发人员深入了解项目的代码规范、安全处理策略及公共模块设计思路。
- **架构资源**: 位于根目录的 `.png` 和 `.svg` 文件提供了直观的视觉参考，有助于理解模块间的调用链路。

## 3. 推荐阅读顺序
1. **[项目总览 (README)](../README.md)**: 了解项目全貌。
2. **架构图资源**: 建立空间架构感。
3. **[反馈与权限改造交付](./feedback-and-permission-delivery.md)**: 学习最新的业务功能实现与权限控制标准。
4. **[模块交付说明](../backend/docs/log-module-delivery.md)**: 学习项目具体的工程实现标准。

## 4. 目录现状说明
- **顶级 `docs/` 目录**: 目前作为全局性文档的索引与分流入口，优先存放核心交付文档。
- **子项目 `docs/` 目录**: 业务相关的详细设计文档、接口定义、数据库变更记录将优先存放在各子项目（如 `backend/docs/`）下，随后通过本导航页进行关联。
- **完善计划**: 后续将逐步补充“环境部署手册”、“API 开发规范”及“前端组件说明”。
