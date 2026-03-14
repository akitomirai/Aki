SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `hash_notary`;
DELETE FROM `consumer_feedback`;
DELETE FROM `operation_log`;
DELETE FROM `pesticide_record`;
DELETE FROM `qr_query_log`;
DELETE FROM `qr_query_stat_day`;
DELETE FROM `qr_code`;
DELETE FROM `quality_report`;
DELETE FROM `regulation_record`;
DELETE FROM `trace_event`;
DELETE FROM `trace_batch_participant`;
DELETE FROM `trace_batch`;
DELETE FROM `invite_code`;
DELETE FROM `org_company_biz_role`;
DELETE FROM `sys_user`;
DELETE FROM `regulator_org`;
DELETE FROM `org_company`;
DELETE FROM `base_product`;

ALTER TABLE `base_product` AUTO_INCREMENT = 1;
ALTER TABLE `consumer_feedback` AUTO_INCREMENT = 1;
ALTER TABLE `operation_log` AUTO_INCREMENT = 1;
ALTER TABLE `org_company` AUTO_INCREMENT = 1;
ALTER TABLE `org_company_biz_role` AUTO_INCREMENT = 1;
ALTER TABLE `qr_code` AUTO_INCREMENT = 1;
ALTER TABLE `quality_report` AUTO_INCREMENT = 1;
ALTER TABLE `regulation_record` AUTO_INCREMENT = 1;
ALTER TABLE `sys_user` AUTO_INCREMENT = 1;
ALTER TABLE `trace_batch` AUTO_INCREMENT = 1;
ALTER TABLE `trace_batch_participant` AUTO_INCREMENT = 1;
ALTER TABLE `trace_event` AUTO_INCREMENT = 1;

INSERT INTO `org_company` (`id`, `name`, `license_no`, `address`, `contact`, `phone`, `created_at`) VALUES
(1, '赣南脐橙示范基地', 'LIC-DEMO-001', '江西省赣州市信丰县高标准果园', '刘建国', '13800000001', '2026-03-11 09:00:00'),
(2, '赣州冷链物流中心', 'LIC-DEMO-002', '江西省赣州市章贡区冷链物流园', '陈海峰', '13800000002', '2026-03-11 09:05:00'),
(3, '江西鲜果加工中心', 'LIC-DEMO-003', '江西省赣州市经开区农产品加工园', '周敏', '13800000003', '2026-03-11 09:10:00'),
(4, '南昌仓配服务中心', 'LIC-DEMO-004', '江西省南昌市红谷滩仓储配送园', '吴倩', '13800000004', '2026-03-11 09:15:00');

INSERT INTO `org_company_biz_role` (`id`, `company_id`, `biz_role`, `created_at`, `updated_at`) VALUES
(1, 1, 'PRODUCER', '2026-03-11 09:20:00', '2026-03-11 09:20:00'),
(2, 1, 'SELLER', '2026-03-11 09:20:00', '2026-03-11 09:20:00'),
(3, 2, 'TRANSPORTER', '2026-03-11 09:21:00', '2026-03-11 09:21:00'),
(4, 3, 'PROCESSOR', '2026-03-11 09:22:00', '2026-03-11 09:22:00'),
(5, 4, 'WAREHOUSE', '2026-03-11 09:23:00', '2026-03-11 09:23:00');

INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `role_code`, `company_id`, `regulator_org_id`, `status`, `deleted`, `created_at`, `updated_at`) VALUES
(1, 'platform', '$2a$12$0spe2MzTFCV2/jlCyuMeDeF..MNj2PZ44OECe9YiwHNCUAf/5FQbW', '平台管理员', NULL, 'PLATFORM_ADMIN', NULL, NULL, 1, 0, '2026-03-11 09:30:00', '2026-03-11 09:30:00'),
(2, 'testuser1', '$2a$10$BMHzgS.7V.aoDs8Jsjm83uxSThHzvGe/Gn8vZY0gcxRQTWDPdBK2a', '示范企业用户', '13800138001', 'ENTERPRISE_USER', 1, NULL, 1, 0, '2026-03-11 09:35:00', '2026-03-11 09:35:00');

INSERT INTO `base_product` (`id`, `name`, `category`, `spec`, `unit`, `created_at`) VALUES
(1, '赣南脐橙', '水果', '10kg/箱', '箱', '2026-03-11 10:00:00'),
(2, '南丰蜜橘', '水果', '5kg/箱', '箱', '2026-03-11 10:05:00'),
(3, '江西绿茶', '茶叶', '250g/盒', '盒', '2026-03-11 10:10:00'),
(4, '有机富硒大米', '粮食', '5kg/袋', '袋', '2026-03-11 10:15:00');

INSERT INTO `trace_batch` (
  `id`, `batch_code`, `product_id`, `company_id`, `origin_place`, `start_date`, `status`,
  `regulation_status`, `remark`, `public_remark`, `internal_remark`, `status_reason`,
  `published_at`, `frozen_at`, `recalled_at`, `created_at`, `updated_at`, `created_by`, `updated_by`
) VALUES
(1, 'BATCH20260311001', 1, 1, '江西省赣州市信丰县高标准果园', '2026-03-01', 'ACTIVE', 'NORMAL',
 '主演示批次：用于展示公开追溯链路', '该批次已完成追溯信息归集，可用于答辩演示。', '内部演示数据，请勿用于生产环境。', NULL,
 '2026-03-11 10:30:00', NULL, NULL, '2026-03-11 10:20:00', '2026-03-11 10:30:00', 2, 1),
(2, 'BATCH20260312001', 2, 1, '江西省抚州市南丰县蜜橘示范园', '2026-03-05', 'DRAFT', 'NONE',
 '草稿批次，用于展示未发布状态', '该批次尚未发布，公开追溯页不可见。', '待补充节点信息后发布。', NULL,
 NULL, NULL, NULL, '2026-03-12 09:00:00', '2026-03-12 09:00:00', 2, 2),
(3, 'BATCH20260312002', 3, 1, '江西省上饶市婺源县生态茶园', '2026-03-08', 'FROZEN', 'PENDING_RECTIFY',
 '冻结批次，用于展示监管整改状态', '该批次处于监管整改中，暂不对外推荐。', '待整改完成后恢复。', '监管抽查发现信息需补充',
 '2026-03-12 11:00:00', '2026-03-13 14:00:00', NULL, '2026-03-12 10:40:00', '2026-03-13 14:00:00', 2, 1),
(4, 'BATCH20260312003', 4, 1, '江西省宜春市富硒稻米基地', '2026-03-02', 'RECALLED', 'RECALLED',
 '召回批次，用于展示问题批次状态', '该批次已召回，请勿继续销售。', '召回演示数据。', '抽检异常，启动召回流程',
 '2026-03-12 12:00:00', '2026-03-12 16:00:00', '2026-03-13 09:30:00', '2026-03-12 11:30:00', '2026-03-13 09:30:00', 2, 1),
(5, 'BATCH20260313001', 1, 1, '江西省赣州市安远县脐橙协作基地', '2026-03-10', 'ACTIVE', 'NONE',
 '补充展示批次，用于列表与筛选演示', '该批次处于正常流转状态。', '演示数据。', NULL,
 '2026-03-13 15:20:00', NULL, NULL, '2026-03-13 15:00:00', '2026-03-13 15:20:00', 2, 2);

INSERT INTO `trace_batch_participant` (`id`, `batch_id`, `company_id`, `biz_role`, `stage_order`, `is_creator`, `remark`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'PRODUCER', 0, 1, '主演示企业，负责生产建档。', '2026-03-11 10:35:00', '2026-03-11 10:35:00'),
(2, 1, 2, 'TRANSPORTER', 1, 0, '负责从果园到冷链中心的运输。', '2026-03-11 10:36:00', '2026-03-11 10:36:00'),
(3, 1, 3, 'PROCESSOR', 2, 0, '负责分级包装。', '2026-03-11 10:37:00', '2026-03-11 10:37:00'),
(4, 1, 4, 'WAREHOUSE', 3, 0, '负责仓储待发。', '2026-03-11 10:38:00', '2026-03-11 10:38:00'),
(5, 1, 1, 'SELLER', 4, 0, '负责终端销售展示。', '2026-03-11 10:39:00', '2026-03-11 10:39:00');

INSERT INTO `trace_event` (
  `id`, `batch_id`, `company_id`, `biz_role`, `stage`, `title`, `event_time`,
  `operator_id`, `operator_name`, `location`, `source_type`, `is_public`,
  `content_json`, `attachments_json`, `created_at`
) VALUES
(1, 1, 1, 'PRODUCER', 'PRODUCE', '果园建档', '2026-03-01 08:00:00',
 2, 'testuser1', '赣州信丰果园', 'ADMIN', 1,
 '{"fields":{"workType":"育苗与挂果管理","remark":"按标准化流程记录果园生产信息"}}', '[]', '2026-03-11 10:40:00'),
(2, 1, 2, 'TRANSPORTER', 'TRANSPORT', '冷链运输', '2026-03-08 09:30:00',
 2, 'testuser1', '赣州冷链物流中心', 'ADMIN', 1,
 '{"fields":{"fromAddress":"赣州信丰果园","toAddress":"赣州冷链物流中心","remark":"全程冷链运输"}}', '[]', '2026-03-11 10:45:00'),
(3, 1, 3, 'PROCESSOR', 'PROCESS', '分级包装', '2026-03-09 14:00:00',
 2, 'testuser1', '江西鲜果加工中心', 'ADMIN', 1,
 '{"fields":{"processName":"分级包装","processResult":"一级果入库","remark":"完成清洗、分级和包装"}}', '[]', '2026-03-11 10:50:00'),
(4, 1, 4, 'WAREHOUSE', 'WAREHOUSE', '入库待发', '2026-03-10 10:20:00',
 2, 'testuser1', '南昌仓配服务中心', 'ADMIN', 1,
 '{"fields":{"warehouseName":"南昌仓配一号库","remark":"完成入库待发"}}', '[]', '2026-03-11 10:55:00'),
(5, 1, 1, 'SELLER', 'SALE', '门店上架', '2026-03-11 09:00:00',
 2, 'testuser1', '南昌演示门店', 'ADMIN', 1,
 '{"fields":{"saleChannel":"门店展示","remark":"用于消费者扫码演示"}}', '[]', '2026-03-11 11:00:00');

INSERT INTO `qr_code` (
  `id`, `batch_id`, `qr_token`, `status`, `created_at`, `expired_at`, `remark`,
  `status_reason`, `generated_by`, `last_query_at`, `pv`
) VALUES
(1, 1, 'test-token-2026', 'ACTIVE', '2026-03-11 11:10:00', NULL,
 '主演示追溯码', NULL, 2, NULL, 0);

INSERT INTO `quality_report` (
  `id`, `batch_id`, `report_no`, `agency`, `result`, `report_file_url`, `report_json`, `created_at`
) VALUES
(1, 1, 'QA-20260311-001', '江西省农产品质量检测中心', 'PASS', NULL,
 '{"pesticide":"未检出","microbial":"合格","appearance":"合格"}', '2026-03-11 11:20:00');

INSERT INTO `regulation_record` (
  `id`, `batch_id`, `inspector_id`, `inspector_name`, `inspect_time`, `inspect_result`,
  `action_taken`, `remark`, `attachment_url`, `created_at`, `updated_at`
) VALUES
(1, 1, 1, '赣州市市场监管局', '2026-03-11 15:00:00', 'NORMAL',
 '例行抽检通过', '监管抽查未发现异常。', NULL, '2026-03-11 15:10:00', '2026-03-11 15:10:00'),
(2, 4, 1, '赣州市市场监管局', '2026-03-13 09:00:00', 'RECALLED',
 '已启动召回', '用于展示召回状态批次。', NULL, '2026-03-13 09:10:00', '2026-03-13 09:10:00');

INSERT INTO `consumer_feedback` (
  `id`, `batch_id`, `qr_id`, `feedback_type`, `content`, `contact_name`, `contact_phone`,
  `source_channel`, `status`, `is_public`, `handled_by`, `handled_at`, `handle_result`, `created_at`, `updated_at`
) VALUES
(1, 1, 1, 'SUGGESTION', '追溯页面信息清晰，适合课堂演示。', '演示观众', '13800000011',
 'SCAN_PAGE', 'PENDING', 0, NULL, NULL, NULL, '2026-03-11 16:00:00', '2026-03-11 16:00:00'),
(2, 1, 1, 'SUGGESTION', '希望后续补充更多图片材料。', '课程老师', '13800000012',
 'SCAN_PAGE', 'CLOSED', 0, 1, '2026-03-11 17:00:00', '已记录为后续优化建议。', '2026-03-11 16:30:00', '2026-03-11 17:00:00');

INSERT INTO `invite_code` (
  `id`, `code`, `invite_type`, `org_type`, `org_id`, `role_code`, `expire_at`,
  `status`, `used_by`, `used_at`, `created_by`, `remark`, `created_at`, `updated_at`
) VALUES
(1, 'DEMO-ENTERPRISE-2026', 'ENTERPRISE_USER', 'COMPANY', 1, 'ENTERPRISE_USER',
 '2026-12-31 23:59:59', 'USED', 2, '2026-03-11 09:35:00', 1, '演示企业账号初始化邀请码',
 '2026-03-11 09:32:00', '2026-03-11 09:35:00');

INSERT INTO `operation_log` (
  `id`, `operator_id`, `operator_name`, `role_code`, `module`, `action`, `target_type`, `target_id`,
  `request_path`, `request_method`, `request_params`, `result_status`, `old_value`, `new_value`,
  `ip`, `user_agent`, `created_at`
) VALUES
(1, NULL, NULL, NULL, 'AUTH', 'LOGIN', 'SYS_USER', NULL,
 '/api/auth/login', 'POST', '[{\"username\":\"platform\"}]', 'SUCCESS', NULL, NULL,
 '127.0.0.1', 'Mozilla/5.0 Demo Browser', '2026-03-11 09:30:10'),
(2, 1, 'platform', 'PLATFORM_ADMIN', 'PRODUCT', 'CREATE_PRODUCT', 'BASE_PRODUCT', 1,
 '/api/platform/product', 'POST', '[{\"name\":\"赣南脐橙\"}]', 'SUCCESS', NULL, NULL,
 '127.0.0.1', 'Mozilla/5.0 Demo Browser', '2026-03-11 10:00:10'),
(3, 2, 'testuser1', 'ENTERPRISE_USER', 'BATCH', 'UPDATE_BATCH', 'TRACE_BATCH', 1,
 '/api/enterprise/batch/update', 'PUT', '[{\"id\":1}]', 'SUCCESS', NULL, NULL,
 '127.0.0.1', 'Mozilla/5.0 Demo Browser', '2026-03-11 10:30:10'),
(4, 2, 'testuser1', 'ENTERPRISE_USER', 'GENERATE_QR', 'GENERATE_QR', 'QR_CODE', 1,
 '/api/admin/qr/generate', 'POST', '[{\"batchId\":1}]', 'SUCCESS', NULL, NULL,
 '127.0.0.1', 'Mozilla/5.0 Demo Browser', '2026-03-11 11:10:10'),
(5, NULL, NULL, NULL, 'FEEDBACK', 'SUBMIT_FEEDBACK', 'CONSUMER_FEEDBACK', 1,
 '/api/public/feedback/create', 'POST', '[{\"batchId\":1}]', 'SUCCESS', NULL, NULL,
 '127.0.0.1', 'Mozilla/5.0 Demo Browser', '2026-03-11 16:00:10'),
(6, 1, 'platform', 'PLATFORM_ADMIN', 'LOG', 'VIEW_LOGS', 'OPERATION_LOG', NULL,
 '/api/platform/log/page', 'GET', '[{\"current\":1,\"size\":10}]', 'SUCCESS', NULL, NULL,
 '127.0.0.1', 'Mozilla/5.0 Demo Browser', '2026-03-11 17:10:00');

SET FOREIGN_KEY_CHECKS = 1;
