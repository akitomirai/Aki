SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `batch_status_log`;
DELETE FROM `qr_query_log`;
DELETE FROM `qr_code`;
DELETE FROM `quality_report`;
DELETE FROM `trace_event`;
DELETE FROM `trace_batch`;
DELETE FROM `org_company`;
DELETE FROM `base_product`;

ALTER TABLE `base_product` AUTO_INCREMENT = 1;
ALTER TABLE `org_company` AUTO_INCREMENT = 1;
ALTER TABLE `trace_batch` AUTO_INCREMENT = 1;
ALTER TABLE `trace_event` AUTO_INCREMENT = 1;
ALTER TABLE `quality_report` AUTO_INCREMENT = 1;
ALTER TABLE `qr_code` AUTO_INCREMENT = 1;
ALTER TABLE `qr_query_log` AUTO_INCREMENT = 1;
ALTER TABLE `batch_status_log` AUTO_INCREMENT = 1;

INSERT INTO `org_company` (`id`, `name`, `license_no`, `address`, `contact`, `phone`, `created_at`) VALUES
(1, '赣南脐橙示范基地', 'LIC-DEMO-001', '江西省赣州市信丰县高标准果园', '刘建国', '13800000001', '2026-03-11 09:00:00'),
(2, '江西生态茶园合作社', 'LIC-DEMO-002', '江西省上饶市婺源县生态茶园', '陈红', '13800000002', '2026-03-11 09:05:00'),
(3, '宜春富硒大米基地', 'LIC-DEMO-003', '江西省宜春市富硒稻米基地', '周敏', '13800000003', '2026-03-11 09:10:00');

INSERT INTO `base_product` (`id`, `name`, `category`, `spec`, `unit`, `image_url`, `created_at`) VALUES
(1, '赣南脐橙', '水果', '10kg/箱', '箱', '/images/products/orange-batch.svg', '2026-03-11 10:00:00'),
(2, '江西绿茶', '茶叶', '250g/盒', '盒', '/images/products/green-tea-batch.svg', '2026-03-11 10:05:00'),
(3, '富硒大米', '粮食', '5kg/袋', '袋', '/images/products/rice-batch.svg', '2026-03-11 10:10:00');

INSERT INTO `trace_batch` (
  `id`, `batch_code`, `product_id`, `company_id`, `origin_place`, `start_date`, `status`,
  `public_remark`, `internal_remark`, `status_reason`, `published_at`, `frozen_at`, `recalled_at`,
  `created_at`, `updated_at`
) VALUES
(1, 'BATCH20260311001', 1, 1, '江西省赣州市信丰县高标准果园', '2026-03-01', 'PUBLISHED',
 '该批次已完成追溯信息归集，可用于扫码演示。', '主演示批次，优先验证完整链路。', NULL,
 '2026-03-11 10:30:00', NULL, NULL, '2026-03-11 10:20:00', '2026-03-11 10:30:00'),
(2, 'BATCH20260312001', 2, 2, '江西省上饶市婺源县生态茶园', '2026-03-05', 'DRAFT',
 '该批次尚未发布，当前仅供后台完善信息。', '待补录更多过程节点后再发布。', NULL,
 NULL, NULL, NULL, '2026-03-12 09:00:00', '2026-03-12 09:00:00'),
(3, 'BATCH20260312002', 3, 3, '江西省宜春市富硒稻米基地', '2026-03-02', 'FROZEN',
 '该批次当前处于冻结状态，请以企业通知为准。', '待补齐仓储留痕后再决定是否恢复。', '监管抽查发现仓储记录缺失',
 '2026-03-12 12:00:00', '2026-03-13 09:30:00', NULL, '2026-03-12 11:30:00', '2026-03-13 09:30:00'),
(4, 'BATCH20260312003', 3, 3, '江西省宜春市富硒稻米基地', '2026-03-03', 'RECALLED',
 '该批次已召回，请勿继续购买。', '召回演示批次。', '抽检异常，已启动召回流程',
 '2026-03-12 15:00:00', '2026-03-12 18:00:00', '2026-03-13 09:00:00', '2026-03-12 14:30:00', '2026-03-13 09:00:00');

INSERT INTO `batch_status_log` (`id`, `batch_id`, `status`, `reason`, `operator_name`, `operated_at`, `created_at`) VALUES
(1, 1, 'DRAFT', '创建批次草稿', '企业管理员', '2026-03-01 08:00:00', '2026-03-01 08:00:00'),
(2, 1, 'PUBLISHED', '质检和二维码已准备完成，允许正式发布。', '企业管理员', '2026-03-11 10:30:00', '2026-03-11 10:30:00'),
(3, 2, 'DRAFT', '创建批次草稿', '企业管理员', '2026-03-12 09:00:00', '2026-03-12 09:00:00'),
(4, 3, 'DRAFT', '创建批次草稿', '企业管理员', '2026-03-12 11:30:00', '2026-03-12 11:30:00'),
(5, 3, 'PUBLISHED', '已生成二维码并对外发布。', '企业管理员', '2026-03-12 12:00:00', '2026-03-12 12:00:00'),
(6, 3, 'FROZEN', '监管抽查发现仓储记录缺失', '平台巡检', '2026-03-13 09:30:00', '2026-03-13 09:30:00'),
(7, 4, 'DRAFT', '创建批次草稿', '企业管理员', '2026-03-12 14:30:00', '2026-03-12 14:30:00'),
(8, 4, 'PUBLISHED', '已完成上线前准备。', '企业管理员', '2026-03-12 15:00:00', '2026-03-12 15:00:00'),
(9, 4, 'FROZEN', '抽查中发现异常，先冻结处理。', '平台巡检', '2026-03-12 18:00:00', '2026-03-12 18:00:00'),
(10, 4, 'RECALLED', '抽检异常，已启动召回流程', '监管人员', '2026-03-13 09:00:00', '2026-03-13 09:00:00');

INSERT INTO `trace_event` (
  `id`, `batch_id`, `company_id`, `stage`, `title`, `event_time`, `operator_name`,
  `location`, `is_public`, `content_json`, `attachments_json`, `created_at`
) VALUES
(1, 1, 1, 'ARCHIVE', '完成企业建档', '2026-03-01 08:00:00', '刘建国', '信丰果园', 1,
 '{"summary":"已录入企业主体、产地和批次基础信息。"}', '["/images/products/orange-batch.svg"]', '2026-03-01 08:00:00'),
(2, 1, 1, 'PRODUCE', '春季田间管理', '2026-03-05 09:30:00', '王小果', '信丰果园', 1,
 '{"summary":"记录修枝、灌溉和巡园情况，便于后续扫码查看。"}', '["/images/products/orange-batch.svg"]', '2026-03-05 09:30:00'),
(3, 1, 1, 'WAREHOUSE', '入库待发', '2026-03-10 10:20:00', '周敏', '南昌仓配中心', 1,
 '{"summary":"完成分级包装并进入仓储待发状态。"}', '["/images/products/orange-batch.svg"]', '2026-03-10 10:20:00'),
(4, 1, 1, 'MARKET', '门店上架', '2026-03-11 09:00:00', '门店导购', '南昌示范门店', 1,
 '{"summary":"已具备对消费者展示的完整追溯信息。"}', '["/images/products/orange-batch.svg"]', '2026-03-11 09:00:00'),
(5, 2, 2, 'PRODUCE', '春茶采摘完成', '2026-03-12 09:00:00', '陈红', '婺源茶园', 0,
 '{"summary":"已完成采摘，等待包装和检测录入。"}', '["/images/products/green-tea-batch.svg"]', '2026-03-12 09:00:00'),
(6, 3, 3, 'ARCHIVE', '基地建档完成', '2026-03-02 07:40:00', '周敏', '宜春基地', 1,
 '{"summary":"已完成批次基础信息建档。"}', '["/images/products/rice-batch.svg"]', '2026-03-02 07:40:00'),
(7, 3, 3, 'WAREHOUSE', '入库复核待补录', '2026-03-13 16:00:00', '李仓', '宜春成品仓', 1,
 '{"summary":"已入库，但仓储温湿度留痕仍需补充。"}', '["/images/products/rice-batch.svg"]', '2026-03-13 16:00:00'),
(8, 4, 3, 'QUALITY', '抽检异常复核', '2026-03-12 17:00:00', '监管人员', '宜春抽检点', 1,
 '{"summary":"抽检提示异常，进入召回处理。"}', '["/images/products/rice-batch.svg"]', '2026-03-12 17:00:00');

INSERT INTO `quality_report` (`id`, `batch_id`, `report_no`, `agency`, `result`, `report_json`, `created_at`) VALUES
(1, 1, 'QA-20260311-001', '江西省农产品质量检测中心', 'PASS', '{"highlights":["农残未检出","微生物合格","外观检验正常"]}', '2026-03-11 09:40:00'),
(2, 3, 'QA-20260314-009', '宜春农产品检测站', 'PASS', '{"highlights":["重金属合格","外观合格"]}', '2026-03-14 10:20:00'),
(3, 4, 'QA-20260313-021', '宜春农产品检测站', 'FAIL', '{"highlights":["抽检异常","已触发召回流程"]}', '2026-03-13 08:30:00');

INSERT INTO `qr_code` (`id`, `batch_id`, `qr_token`, `status`, `created_at`, `remark`, `status_reason`, `last_query_at`, `pv`) VALUES
(1, 1, 'test-token-2026', 'ACTIVE', '2026-03-11 11:10:00', '主演示二维码', NULL, '2026-03-24 10:15:00', 2),
(2, 3, 'frozen-token-2026', 'SUSPENDED', '2026-03-12 12:10:00', '冻结批次二维码', '监管抽查发现仓储记录缺失', '2026-03-20 09:00:00', 1),
(3, 4, 'recalled-token-2026', 'RECALLED', '2026-03-12 15:10:00', '召回批次二维码', '抽检异常，已启动召回流程', '2026-03-20 11:00:00', 1);

INSERT INTO `qr_query_log` (`id`, `qr_id`, `batch_id`, `query_time`, `ip`, `ua`, `referer`) VALUES
(1, 1, 1, '2026-03-23 09:00:00', '127.0.0.1', 'DemoBrowser/Desktop', 'http://127.0.0.1:5173'),
(2, 1, 1, '2026-03-24 10:15:00', '127.0.0.2', 'DemoBrowser/Mobile', 'http://127.0.0.1:5173'),
(3, 2, 3, '2026-03-20 09:00:00', '127.0.0.1', 'DemoBrowser/Desktop', 'http://127.0.0.1:5173'),
(4, 3, 4, '2026-03-20 11:00:00', '127.0.0.3', 'DemoBrowser/Mobile', 'http://127.0.0.1:5173');

SET FOREIGN_KEY_CHECKS = 1;
