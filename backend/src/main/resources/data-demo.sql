INSERT INTO org_company (id, name, license_no, address, contact, phone, status, created_at) VALUES
(1, '赣南果业种植有限公司', 'LIC-JX-001', '江西省赣州市信丰县脐橙种植基地', '刘建国', '13800000001', 'ENABLED', '2026-03-01 09:00:00'),
(2, '婺源茶业合作社', 'LIC-JX-002', '江西省上饶市婺源县生态茶园基地', '张敏', '13800000002', 'ENABLED', '2026-03-01 09:20:00'),
(3, '南昌优质稻米中心', 'LIC-JX-003', '江西省南昌市优质稻米储运中心', '陈浩', '13800000003', 'ENABLED', '2026-03-01 09:40:00');

INSERT INTO sys_user (id, username, password, real_name, role_code, company_id, status, created_at, updated_at) VALUES
(1, 'platform', '{noop}123456', 'Platform Admin', 'PLATFORM_ADMIN', NULL, 1, '2026-03-01 08:00:00', '2026-03-01 08:00:00'),
(2, 'enterprise_admin', '{noop}123456', 'Enterprise Admin', 'ENTERPRISE_ADMIN', 1, 1, '2026-03-01 08:10:00', '2026-03-01 08:10:00'),
(3, 'operator', '{noop}123456', 'Field Operator', 'OPERATOR', 1, 1, '2026-03-01 08:20:00', '2026-03-01 08:20:00'),
(4, 'regulator', '{noop}123456', 'Regulator', 'REGULATOR', NULL, 1, '2026-03-01 08:30:00', '2026-03-01 08:30:00'),
(5, 'operator_support', '{noop}123456', 'Field Operator B', 'OPERATOR', 1, 1, '2026-03-01 08:35:00', '2026-03-01 08:35:00');

INSERT INTO base_product (id, company_id, product_code, name, category, origin_place, spec, unit, image_url, status, created_at) VALUES
(1, 1, 'ORANGE-001', '赣南脐橙', '水果', '江西省赣州市信丰县', '10kg/箱', '箱', '/images/products/orange-batch.svg', 'ENABLED', '2026-03-01 10:00:00'),
(2, 2, 'TEA-001', '明前绿茶', '茶叶', '江西省上饶市婺源县', '250g/罐', '罐', '/images/products/green-tea-batch.svg', 'ENABLED', '2026-03-01 10:10:00'),
(3, 3, 'RICE-001', '优质大米', '粮油', '江西省南昌市', '5kg/袋', '袋', '/images/products/rice-batch.svg', 'ENABLED', '2026-03-01 10:20:00');

INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, assignee_user_id, assigned_at, task_status, task_completed_at, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, frozen_at, recalled_at, created_at, updated_at
) VALUES
(1, 'ORANGE-202603-A1', 1, 1, 5, '2026-03-03 08:00:00', 'COMPLETED', '2026-03-08 09:15:00', 'Xinfeng Orchard Base', '2026-03-03', 'PUBLISHED',
 'Public trace page is available for this batch.', 'Used to verify released-batch linkage with the workbench.', NULL,
 '2026-03-08 10:00:00', NULL, NULL, '2026-03-03 08:00:00', '2026-03-08 10:00:00'),
(2, 'ORANGE-202603-D1', 1, 1, 3, '2026-03-20 09:00:00', 'PENDING', NULL, 'Xinfeng Orchard Base', '2026-03-20', 'DRAFT',
 'The batch has been created and still needs field records, QA and QR data.', 'Used for continuous field-entry verification before publish.', NULL,
 NULL, NULL, NULL, '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
(3, 'TEA-202603-F1', 2, 2, NULL, NULL, 'PENDING', NULL, 'Wuyuan Tea Base', '2026-03-09', 'FROZEN',
 'The batch is paused and waiting for follow-up handling.', 'Used to review frozen-batch rectification flow.', 'Latest QA failed and the batch is waiting for recheck.',
 '2026-03-12 09:00:00', '2026-03-18 14:20:00', NULL, '2026-03-09 08:30:00', '2026-03-18 14:20:00'),
(4, 'RICE-202603-R1', 3, 3, NULL, NULL, 'PENDING', NULL, 'Nanchang Rice Center', '2026-03-05', 'RECALLED',
 'The batch is currently recalled. Follow the risk notice.', 'Used to review recall handling and public warning.', 'A logistics risk was confirmed and recall is in progress.',
 '2026-03-10 11:00:00', NULL, '2026-03-21 09:40:00', '2026-03-05 07:30:00', '2026-03-21 09:40:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(1, 1, 'DRAFT', '批次建档完成，准备补充追溯与质检资料。', '企业管理员', '2026-03-03 08:00:00', '2026-03-03 08:00:00'),
(2, 1, 'PUBLISHED', '质检结果和二维码均已准备完成，批次对外发布。', '企业管理员', '2026-03-08 10:00:00', '2026-03-08 10:00:00'),
(3, 2, 'DRAFT', '批次已创建，等待继续补充关键环节信息。', '企业管理员', '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
(4, 3, 'DRAFT', '茶叶批次已建档，等待生产过程记录。', '企业管理员', '2026-03-09 08:30:00', '2026-03-09 08:30:00'),
(5, 3, 'PUBLISHED', '首轮质检通过，二维码已生成并完成发布。', '企业管理员', '2026-03-12 09:00:00', '2026-03-12 09:00:00'),
(6, 3, 'FROZEN', '最新质检未通过，批次已暂停流通等待复核。', '质检负责人', '2026-03-18 14:20:00', '2026-03-18 14:20:00'),
(7, 4, 'DRAFT', '稻米批次已建档，准备发运。', '企业管理员', '2026-03-05 07:30:00', '2026-03-05 07:30:00'),
(8, 4, 'PUBLISHED', '批次已完成出库准备并对外发布查询。', '企业管理员', '2026-03-10 11:00:00', '2026-03-10 11:00:00'),
(9, 4, 'RECALLED', '物流环节确认存在风险，批次已召回。', '风控负责人', '2026-03-21 09:40:00', '2026-03-21 09:40:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(1, 1, 1, 'ARCHIVE', '企业建档完成', '2026-03-03 08:10:00', '刘建国', '信丰果园办公室', TRUE,
 '{"summary":"企业、产品和批次基础信息已确认，进入正式追溯管理。"}', '["/images/products/orange-batch.svg"]', '2026-03-03 08:10:00'),
(2, 1, 1, 'PRODUCE', '采收分级完成', '2026-03-04 09:20:00', '现场记录员', 'A区果园', TRUE,
 '{"summary":"完成采收、分级和装箱，现场操作人员已留痕。"}', '["/images/products/orange-batch.svg"]', '2026-03-04 09:20:00'),
(3, 1, 1, 'TRANSPORT', '冷链发运开始', '2026-03-06 15:30:00', '物流负责人', '赣州发运点', TRUE,
 '{"summary":"批次进入冷链运输环节，交接信息已记录。"}', '["/images/products/orange-batch.svg"]', '2026-03-06 15:30:00'),
(4, 1, 1, 'MARKET', '门店上架完成', '2026-03-08 09:15:00', '门店管理员', '南昌销售门店', TRUE,
 '{"summary":"批次已上架，可供消费者扫码查询。"}', '["/images/products/orange-batch.svg"]', '2026-03-08 09:15:00'),

(5, 2, 1, 'ARCHIVE', '批次建档完成', '2026-03-20 09:10:00', '企业管理员', '信丰果园办公室', TRUE,
 '{"summary":"草稿批次已建立，等待补充更多关键记录。"}', '["/images/products/orange-batch.svg"]', '2026-03-20 09:10:00'),
(6, 2, 1, 'PRODUCE', '首条现场记录已补充', '2026-03-20 10:30:00', '现场记录员', 'C区果园', TRUE,
 '{"summary":"已补充首条生产记录，后续还需上传质检和生成二维码。"}', '["/images/products/orange-batch.svg"]', '2026-03-20 10:30:00'),

(7, 3, 2, 'ARCHIVE', '茶叶批次建档完成', '2026-03-09 08:45:00', '张敏', '婺源茶业办公室', TRUE,
 '{"summary":"茶叶批次已登记企业和产品信息。"}', '["/images/products/green-tea-batch.svg"]', '2026-03-09 08:45:00'),
(8, 3, 2, 'QUALITY', '最新质检结果已记录', '2026-03-18 13:40:00', '质检负责人', '茶叶质检室', TRUE,
 '{"summary":"最新质检发现异常，批次已转入处理状态。"}', '["/images/products/green-tea-batch.svg"]', '2026-03-18 13:40:00'),

(9, 4, 3, 'ARCHIVE', '稻米批次建档完成', '2026-03-05 07:45:00', '陈浩', '南昌稻米中心', TRUE,
 '{"summary":"批次基础信息已登记，准备出库发运。"}', '["/images/products/rice-batch.svg"]', '2026-03-05 07:45:00'),
(10, 4, 3, 'DELIVERY', '出库发运已记录', '2026-03-10 09:30:00', '发运负责人', '南昌发运码头', TRUE,
 '{"summary":"稻米批次已离库并进入渠道发运。"}', '["/images/products/rice-batch.svg"]', '2026-03-10 09:30:00'),
(11, 4, 3, 'REGULATION', '召回通知已留痕', '2026-03-21 10:10:00', '风控负责人', '联合复核室', TRUE,
 '{"summary":"已记录召回通知，公开页同步展示当前风险状态。"}', '["/images/products/rice-batch.svg"]', '2026-03-21 10:10:00');

INSERT INTO quality_report (id, batch_id, report_no, agency, result, report_json, created_at) VALUES
(1, 1, 'QA-20260308-001', '江西省农产品质检中心', 'PASS',
 '{"highlights":["农残指标合格","微生物指标合格","外观品质正常"]}', '2026-03-08 08:40:00'),
(2, 3, 'QA-20260318-002', '婺源茶叶质检实验室', 'FAIL',
 '{"highlights":["含水率超出内部阈值","批次需保持暂停流通直至复核完成"]}', '2026-03-18 13:20:00'),
(3, 4, 'QA-20260310-003', '南昌粮油检测实验室', 'PASS',
 '{"highlights":["仓储阶段质检合格","当前风险来自物流环节而非仓储质量"]}', '2026-03-10 08:50:00');

INSERT INTO qr_code (id, batch_id, qr_token, status, created_at, remark, status_reason, last_query_at, pv) VALUES
(1, 1, 'demo-normal-2026', 'ACTIVE', '2026-03-08 09:00:00', '已启用公开查询二维码', NULL, '2026-03-24 10:15:00', 6),
(2, 3, 'demo-frozen-2026', 'SUSPENDED', '2026-03-12 08:50:00', '二维码已暂停使用', '批次处理中，暂不对外开放。', '2026-03-24 11:00:00', 2),
(3, 4, 'demo-recall-2026', 'RECALLED', '2026-03-10 10:50:00', '二维码保留召回提醒', '该批次已召回，请按风险提示处理。', '2026-03-24 11:40:00', 4);

INSERT INTO qr_query_log (id, qr_id, batch_id, query_time, ip, ua, referer) VALUES
(1, 1, 1, '2026-03-24 10:15:00', '127.0.0.1', 'Browser/Desktop', 'http://127.0.0.1:5173'),
(2, 1, 1, '2026-03-24 10:25:00', '127.0.0.2', 'Browser/Mobile', 'http://127.0.0.1:5173'),
(3, 2, 3, '2026-03-24 11:00:00', '127.0.0.3', 'Browser/Desktop', 'http://127.0.0.1:5173'),
(4, 3, 4, '2026-03-24 11:40:00', '127.0.0.4', 'Browser/Mobile', 'http://127.0.0.1:5173');

INSERT INTO batch_risk_action (id, batch_id, action_type, reason, comment, operator_name, created_at) VALUES
(1, 3, 'COMMENT', '最新质检未通过，批次已暂停发运。', '质检异常确认后已立即暂停该批次流通。', '质检负责人', '2026-03-18 14:30:00'),
(2, 3, 'RECTIFICATION', '已重新筛选茶叶并调整存储条件。', '整改取证材料已补充，等待复核。', '茶叶主管', '2026-03-18 17:00:00'),
(3, 3, 'PROCESSING', '整改结果仍在复核中。', '企业正在等待第二轮质检结果。', '质检负责人', '2026-03-19 09:30:00'),
(4, 4, 'COMMENT', '物流复核过程中确认存在污染风险。', '已第一时间发布召回提醒。', '风控负责人', '2026-03-21 09:45:00'),
(5, 4, 'RECTIFICATION', '受影响库存已隔离，下游合作方已同步通知。', '回收登记和库存核对已完成。', '运营负责人', '2026-03-21 14:00:00'),
(6, 4, 'PROCESSING', '召回处理仍在持续推进。', '企业正在汇总渠道反馈和回收结果。', '风控负责人', '2026-03-22 10:00:00'),
(7, 4, 'RECTIFIED', '企业已完成整改并提交处理报告。', '召回信息继续保留在公开页，便于后续追查。', '风控负责人', '2026-03-23 16:20:00');
