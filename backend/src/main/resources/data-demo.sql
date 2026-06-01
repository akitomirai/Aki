-- Demo data reset for thesis testing.
-- Accounts: platform / enterprise_admin / fruit_admin / operator / operator_fruit / regulator, password: 123456

INSERT INTO org_company (id, name, license_no, address, contact, phone, status, created_at) VALUES
(1, '江西稻香生态农业有限公司', 'LIC-JX-RICE-001', '江西省南昌市进贤县高标准稻田基地', '陈浩', '13800000001', 'ENABLED', '2026-04-01 08:30:00'),
(2, '赣南果业种植有限公司', 'LIC-JX-FRUIT-002', '江西省赣州市信丰县脐橙产业园', '刘建国', '13800000002', 'ENABLED', '2026-04-01 08:45:00'),
(3, '赣州鲜果联合合作社', 'LIC-JX-FRUIT-003', '江西省赣州市于都县现代农业示范园', '张敏', '13800000003', 'ENABLED', '2026-04-01 09:00:00');

INSERT INTO sys_user (id, username, password, real_name, phone, role_code, company_id, status, need_change_password, password_updated_at, created_at, updated_at) VALUES
(1, 'platform', '{noop}123456', '平台管理员', '13900000001', 'PLATFORM_ADMIN', NULL, 1, 0, '2026-04-01 08:00:00', '2026-04-01 08:00:00', '2026-04-01 08:00:00'),
(2, 'enterprise_admin', '{noop}123456', '稻香企业管理员', '13900000002', 'ENTERPRISE_ADMIN', 1, 1, 0, '2026-04-01 08:05:00', '2026-04-01 08:05:00', '2026-04-01 08:05:00'),
(3, 'fruit_admin', '{noop}123456', '果业企业管理员', '13900000003', 'ENTERPRISE_ADMIN', 2, 1, 0, '2026-04-01 08:10:00', '2026-04-01 08:10:00', '2026-04-01 08:10:00'),
(4, 'operator', '{noop}123456', '稻香现场操作员', '13900000004', 'OPERATOR', 1, 1, 0, '2026-04-01 08:15:00', '2026-04-01 08:15:00', '2026-04-01 08:15:00'),
(5, 'operator_fruit', '{noop}123456', '果业现场操作员', '13900000005', 'OPERATOR', 2, 1, 0, '2026-04-01 08:20:00', '2026-04-01 08:20:00', '2026-04-01 08:20:00'),
(6, 'regulator', '{noop}123456', '监管查看人员', '13900000006', 'REGULATOR', NULL, 1, 0, '2026-04-01 08:25:00', '2026-04-01 08:25:00', '2026-04-01 08:25:00');

INSERT INTO base_product (id, company_id, product_code, name, category, origin_place, spec, unit, image_url, status, created_at) VALUES
(1, 1, 'RICE-001', '大米', '粮油', '江西省南昌市进贤县', '5kg/袋', '袋', '/images/products/rice-batch.svg', 'ENABLED', '2026-04-01 09:10:00'),
(2, 1, 'PEANUT-001', '花生', '油料作物', '江西省赣州市兴国县', '2.5kg/袋', '袋', '/images/products/rice-batch.svg', 'ENABLED', '2026-04-01 09:15:00'),
(3, 2, 'ORANGE-001', '赣南脐橙', '水果', '江西省赣州市信丰县', '10kg/箱', '箱', '/images/products/orange-batch.svg', 'ENABLED', '2026-04-01 09:20:00'),
(4, 2, 'GRAPE-001', '阳光葡萄', '水果', '江西省赣州市章贡区', '5kg/箱', '箱', '/images/products/orange-batch.svg', 'ENABLED', '2026-04-01 09:25:00'),
(5, 3, 'STRAWBERRY-001', '草莓', '水果', '江西省赣州市于都县', '3kg/箱', '箱', '/images/products/orange-batch.svg', 'ENABLED', '2026-04-01 09:30:00');

INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, assignee_user_id, assigned_at, task_status, task_completed_at, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, frozen_at, recalled_at, created_at, updated_at
) VALUES
(1, 'RICE-202604-R1', 1, 1, 4, '2026-04-05 08:30:00', 'COMPLETED', '2026-04-08 16:30:00', '江西省南昌市进贤县高标准稻田基地', '2026-04-05', 'PUBLISHED',
 '该批次大米已开放扫码查询，可查看建档、种植、质检、仓储和销售环节。', '用于正常公开查询和二维码预览测试。', '追溯资料、质检结果和二维码均已完成，当前对外发布。',
 '2026-04-09 09:10:00', NULL, NULL, '2026-04-05 08:30:00', '2026-04-09 09:10:00'),
(2, 'PEANUT-202604-P1', 2, 1, 4, '2026-04-06 09:00:00', 'COMPLETED', '2026-04-09 15:20:00', '江西省赣州市兴国县花生种植基地', '2026-04-06', 'PUBLISHED',
 '该批次花生已开放扫码查询，可查看采收、烘干、质检和入库记录。', '用于补充粮油类农产品测试数据。', '关键资料齐全，已生成二维码并对外发布。',
 '2026-04-10 10:20:00', NULL, NULL, '2026-04-06 09:00:00', '2026-04-10 10:20:00'),
(3, 'ORANGE-202604-Q1', 3, 2, 5, '2026-04-07 08:20:00', 'COMPLETED', '2026-04-10 17:00:00', '江西省赣州市信丰县脐橙产业园', '2026-04-07', 'PUBLISHED',
 '该批次赣南脐橙已开放扫码查询，展示采收分级、质检和冷链运输信息。', '用于水果类正常批次测试。', '质检合格并已完成二维码发布。',
 '2026-04-11 09:40:00', NULL, NULL, '2026-04-07 08:20:00', '2026-04-11 09:40:00'),
(4, 'GRAPE-202604-G1', 4, 2, 5, '2026-04-08 08:40:00', 'COMPLETED', '2026-04-11 16:10:00', '江西省赣州市章贡区阳光葡萄基地', '2026-04-08', 'FROZEN',
 '该批次阳光葡萄因冷链温度异常暂缓流通，公开页会同步显示风险提醒。', '用于冻结状态和风险处理流程测试。', '冷链中转温度记录异常，批次已冻结等待复核。',
 '2026-04-12 09:30:00', '2026-04-13 14:20:00', NULL, '2026-04-08 08:40:00', '2026-04-13 14:20:00'),
(5, 'STRAWBERRY-202604-S1', 5, 3, NULL, NULL, 'COMPLETED', '2026-04-12 15:00:00', '江西省赣州市于都县草莓示范园', '2026-04-09', 'RECALLED',
 '该批次草莓已召回，公开页保留风险提示和处理建议。', '用于召回状态和异常追溯测试。', '复核发现包装批次信息不一致，已启动召回。',
 '2026-04-12 16:00:00', NULL, '2026-04-14 10:10:00', '2026-04-09 09:00:00', '2026-04-14 10:10:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(1, 1, 'DRAFT', '大米批次建档完成，等待补全追溯资料。', '稻香企业管理员', '2026-04-05 08:30:00', '2026-04-05 08:30:00'),
(2, 1, 'PUBLISHED', '大米批次资料齐全，已对外发布。', '稻香企业管理员', '2026-04-09 09:10:00', '2026-04-09 09:10:00'),
(3, 2, 'DRAFT', '花生批次建档完成。', '稻香企业管理员', '2026-04-06 09:00:00', '2026-04-06 09:00:00'),
(4, 2, 'PUBLISHED', '花生批次质检合格并已发布。', '稻香企业管理员', '2026-04-10 10:20:00', '2026-04-10 10:20:00'),
(5, 3, 'DRAFT', '赣南脐橙批次建档完成。', '果业企业管理员', '2026-04-07 08:20:00', '2026-04-07 08:20:00'),
(6, 3, 'PUBLISHED', '脐橙批次完成质检、二维码发布和公开查询。', '果业企业管理员', '2026-04-11 09:40:00', '2026-04-11 09:40:00'),
(7, 4, 'DRAFT', '阳光葡萄批次建档完成。', '果业企业管理员', '2026-04-08 08:40:00', '2026-04-08 08:40:00'),
(8, 4, 'PUBLISHED', '阳光葡萄批次先行发布，等待后续冷链记录。', '果业企业管理员', '2026-04-12 09:30:00', '2026-04-12 09:30:00'),
(9, 4, 'FROZEN', '冷链中转温度异常，批次冻结并进入风险处理。', '监管查看人员', '2026-04-13 14:20:00', '2026-04-13 14:20:00'),
(10, 5, 'DRAFT', '草莓批次建档完成。', '企业管理员', '2026-04-09 09:00:00', '2026-04-09 09:00:00'),
(11, 5, 'PUBLISHED', '草莓批次完成质检和二维码发布。', '企业管理员', '2026-04-12 16:00:00', '2026-04-12 16:00:00'),
(12, 5, 'RECALLED', '包装批次信息复核不一致，已召回。', '监管查看人员', '2026-04-14 10:10:00', '2026-04-14 10:10:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(1, 1, 1, 'ARCHIVE', '大米批次建档完成', '2026-04-05 08:40:00', '稻香企业管理员', '进贤县高标准稻田基地', TRUE,
 '{"summary":"完成企业、产品、批次编号和产地信息确认，纳入追溯管理。"}', '["/images/products/rice-batch.svg"]', '2026-04-05 08:40:00'),
(2, 1, 1, 'PRODUCE', '田间管理记录完成', '2026-04-06 10:30:00', '稻香现场操作员', '稻田 A 区', TRUE,
 '{"summary":"记录灌溉、施肥和病虫害巡查情况，田间管理状态正常。"}', '["/images/products/rice-batch.svg"]', '2026-04-06 10:30:00'),
(3, 1, 1, 'QUALITY', '大米质检合格', '2026-04-08 09:00:00', '质检员', '南昌农产品质检中心', TRUE,
 '{"summary":"水分、杂质和重金属检测结果均符合标准。"}', '["/images/products/rice-batch.svg"]', '2026-04-08 09:00:00'),
(4, 1, 1, 'WAREHOUSE', '大米入库完成', '2026-04-08 16:30:00', '仓储管理员', '南昌中心仓', TRUE,
 '{"summary":"完成称重、封签和入库登记，仓储温湿度正常。"}', '["/images/products/rice-batch.svg"]', '2026-04-08 16:30:00'),
(5, 1, 1, 'MARKET', '大米门店上架', '2026-04-09 09:00:00', '销售管理员', '南昌直营门店', TRUE,
 '{"summary":"批次已上架销售，消费者可扫码查看追溯信息。"}', '["/images/products/rice-batch.svg"]', '2026-04-09 09:00:00'),

(6, 2, 1, 'ARCHIVE', '花生批次建档完成', '2026-04-06 09:10:00', '稻香企业管理员', '兴国县花生基地', TRUE,
 '{"summary":"完成花生批次基本信息登记。"}', '["/images/products/rice-batch.svg"]', '2026-04-06 09:10:00'),
(7, 2, 1, 'PRODUCE', '花生采收与晾晒完成', '2026-04-07 11:20:00', '稻香现场操作员', '兴国县花生基地', TRUE,
 '{"summary":"完成采收、去泥和初步晾晒，现场记录已提交。"}', '["/images/products/rice-batch.svg"]', '2026-04-07 11:20:00'),
(8, 2, 1, 'QUALITY', '花生质检合格', '2026-04-09 10:10:00', '质检员', '赣州农产品质检中心', TRUE,
 '{"summary":"黄曲霉毒素、含水率和感官指标检测合格。"}', '["/images/products/rice-batch.svg"]', '2026-04-09 10:10:00'),
(9, 2, 1, 'WAREHOUSE', '花生烘干入库', '2026-04-09 15:20:00', '仓储管理员', '兴国县仓储点', TRUE,
 '{"summary":"完成烘干、称重和入库，仓储记录完整。"}', '["/images/products/rice-batch.svg"]', '2026-04-09 15:20:00'),

(10, 3, 2, 'ARCHIVE', '赣南脐橙批次建档完成', '2026-04-07 08:40:00', '果业企业管理员', '信丰县脐橙产业园', TRUE,
 '{"summary":"完成果园、采收批次和企业主体信息确认。"}', '["/images/products/orange-batch.svg"]', '2026-04-07 08:40:00'),
(11, 3, 2, 'PRODUCE', '脐橙采收分级完成', '2026-04-08 10:50:00', '果业现场操作员', '信丰县 A 区果园', TRUE,
 '{"summary":"完成采收、分级和装箱，外观品质符合公开销售要求。"}', '["/images/products/orange-batch.svg"]', '2026-04-08 10:50:00'),
(12, 3, 2, 'QUALITY', '脐橙质检合格', '2026-04-10 09:30:00', '质检员', '赣南果品质检室', TRUE,
 '{"summary":"农残、糖度和外观抽检结果符合标准。"}', '["/images/products/orange-batch.svg"]', '2026-04-10 09:30:00'),
(13, 3, 2, 'TRANSPORT', '脐橙冷链发运', '2026-04-10 17:00:00', '物流负责人', '赣州冷链中转仓', TRUE,
 '{"summary":"完成冷链装车和发运登记，运输温度正常。"}', '["/images/products/orange-batch.svg"]', '2026-04-10 17:00:00'),

(14, 4, 2, 'ARCHIVE', '阳光葡萄批次建档完成', '2026-04-08 08:50:00', '果业企业管理员', '章贡区葡萄基地', TRUE,
 '{"summary":"完成阳光葡萄批次建档，进入追溯流程。"}', '["/images/products/orange-batch.svg"]', '2026-04-08 08:50:00'),
(15, 4, 2, 'PRODUCE', '葡萄采摘分拣完成', '2026-04-09 09:40:00', '果业现场操作员', '章贡区葡萄基地', TRUE,
 '{"summary":"完成采摘、分拣和包装，果穗外观正常。"}', '["/images/products/orange-batch.svg"]', '2026-04-09 09:40:00'),
(16, 4, 2, 'QUALITY', '葡萄质检通过', '2026-04-11 10:00:00', '质检员', '赣州果品质检室', TRUE,
 '{"summary":"农残和糖度抽检合格，允许进入冷链环节。"}', '["/images/products/orange-batch.svg"]', '2026-04-11 10:00:00'),
(17, 4, 2, 'REGULATION', '冷链异常冻结批次', '2026-04-13 14:20:00', '监管查看人员', '赣州冷链中转仓', TRUE,
 '{"summary":"中转温度超出设定范围，批次冻结并等待复核处理。"}', '["/images/products/orange-batch.svg"]', '2026-04-13 14:20:00'),

(18, 5, 3, 'ARCHIVE', '草莓批次建档完成', '2026-04-09 09:20:00', '企业管理员', '于都县草莓示范园', TRUE,
 '{"summary":"完成草莓批次建档和产地信息登记。"}', '["/images/products/orange-batch.svg"]', '2026-04-09 09:20:00'),
(19, 5, 3, 'PRODUCE', '草莓采摘包装完成', '2026-04-10 08:50:00', '现场操作员', '于都县草莓示范园', TRUE,
 '{"summary":"完成采摘、预冷和包装，包装批号已记录。"}', '["/images/products/orange-batch.svg"]', '2026-04-10 08:50:00'),
(20, 5, 3, 'QUALITY', '草莓复核发现异常', '2026-04-14 09:30:00', '质检员', '于都县农产品复核点', TRUE,
 '{"summary":"复核发现包装批次信息不一致，建议召回并复查。"}', '["/images/products/orange-batch.svg"]', '2026-04-14 09:30:00'),
(21, 5, 3, 'REGULATION', '草莓召回通知发布', '2026-04-14 10:10:00', '监管查看人员', '于都县监管联络点', TRUE,
 '{"summary":"批次召回通知已记录，公开页同步展示风险提醒。"}', '["/images/products/orange-batch.svg"]', '2026-04-14 10:10:00');

INSERT INTO quality_report (id, batch_id, report_no, agency, result, report_json, created_at) VALUES
(1, 1, 'QA-20260408-RICE', '江西省农产品质检中心', 'PASS', '{"highlights":["水分指标合格","杂质指标合格","重金属检测合格"]}', '2026-04-08 09:10:00'),
(2, 2, 'QA-20260409-PEANUT', '赣州市农产品质检中心', 'PASS', '{"highlights":["黄曲霉毒素未超标","含水率合格","感官指标正常"]}', '2026-04-09 10:20:00'),
(3, 3, 'QA-20260410-ORANGE', '赣南果品质检室', 'PASS', '{"highlights":["农残指标合格","糖度抽检合格","外观分级合格"]}', '2026-04-10 09:40:00'),
(4, 4, 'QA-20260411-GRAPE', '赣州果品质检室', 'PASS', '{"highlights":["农残指标合格","糖度抽检合格","冷链复核待处理"]}', '2026-04-11 10:10:00'),
(5, 5, 'QA-20260414-STRAWBERRY', '于都县农产品复核点', 'FAIL', '{"highlights":["包装批号需复核","已触发召回流程","建议停止销售并联系企业"]}', '2026-04-14 09:40:00');

INSERT INTO qr_code (id, batch_id, qr_token, status, created_at, remark, status_reason, expired_at, last_query_at, pv) VALUES
(1, 1, 'rice-202604-r1', 'ACTIVE', '2026-04-09 09:15:00', '大米公开追溯码', NULL, NULL, '2026-04-16 10:20:00', 18),
(2, 2, 'peanut-202604-p1', 'ACTIVE', '2026-04-10 10:25:00', '花生公开追溯码', NULL, NULL, '2026-04-16 10:25:00', 9),
(3, 3, 'orange-202604-q1', 'ACTIVE', '2026-04-11 09:45:00', '赣南脐橙公开追溯码', NULL, NULL, '2026-04-16 10:30:00', 21),
(4, 4, 'grape-202604-g1', 'SUSPENDED', '2026-04-12 09:35:00', '阳光葡萄风险追溯码', '冷链中转温度异常，暂缓流通。', NULL, '2026-04-16 10:35:00', 6),
(5, 5, 'strawberry-202604-s1', 'RECALLED', '2026-04-12 16:05:00', '草莓召回追溯码', '包装批次信息不一致，已召回。', NULL, '2026-04-16 10:40:00', 4);

INSERT INTO qr_query_log (id, qr_id, batch_id, query_time, ip, ua, referer) VALUES
(1, 1, 1, '2026-04-15 09:10:00', '192.168.0.21', 'Mobile Safari', 'scan'),
(2, 1, 1, '2026-04-16 10:20:00', '192.168.0.32', 'Chrome', 'scan'),
(3, 2, 2, '2026-04-15 11:30:00', '192.168.0.33', 'Chrome', 'scan'),
(4, 2, 2, '2026-04-16 10:25:00', '192.168.0.34', 'Mobile Safari', 'scan'),
(5, 3, 3, '2026-04-15 14:20:00', '192.168.0.41', 'WeChat', 'scan'),
(6, 3, 3, '2026-04-16 10:30:00', '192.168.0.42', 'WeChat', 'scan'),
(7, 4, 4, '2026-04-16 10:35:00', '192.168.0.51', 'WeChat', 'scan'),
(8, 5, 5, '2026-04-16 10:40:00', '192.168.0.61', 'WeChat', 'scan');

INSERT INTO batch_risk_action (id, batch_id, action_type, reason, comment, operator_name, created_at) VALUES
(1, 4, 'COMMENT', '冷链温度异常', '已联系冷链承运方调取温度记录，批次暂缓流通。', '监管查看人员', '2026-04-13 14:30:00'),
(2, 4, 'PROCESSING', '等待复核', '企业已提交冷链复核申请，等待质检复查。', '果业企业管理员', '2026-04-13 16:00:00'),
(3, 5, 'COMMENT', '包装批号不一致', '公开页保留召回提示，销售端已通知下架。', '监管查看人员', '2026-04-14 10:20:00'),
(4, 5, 'RECTIFICATION', '召回处置', '企业正在核对包装线记录并回收已发出批次。', '企业管理员', '2026-04-14 11:00:00');

INSERT INTO operation_audit_log (id, operator_user_id, operator_name, role_code, company_id, action_type, target_type, target_id, target_name, result, summary, created_at) VALUES
(1, 1, '平台管理员', 'PLATFORM_ADMIN', NULL, 'COMPANY_CREATE', 'COMPANY', 1, '江西稻香生态农业有限公司', 'SUCCESS', '初始化稻香农业企业主体。', '2026-04-01 08:30:00'),
(2, 1, '平台管理员', 'PLATFORM_ADMIN', NULL, 'COMPANY_CREATE', 'COMPANY', 2, '赣南果业种植有限公司', 'SUCCESS', '初始化果业企业主体。', '2026-04-01 08:45:00'),
(3, 2, '稻香企业管理员', 'ENTERPRISE_ADMIN', 1, 'PRODUCT_CREATE', 'PRODUCT', 1, '大米', 'SUCCESS', '新增大米产品档案。', '2026-04-01 09:10:00'),
(4, 2, '稻香企业管理员', 'ENTERPRISE_ADMIN', 1, 'PRODUCT_CREATE', 'PRODUCT', 2, '花生', 'SUCCESS', '新增花生产品档案。', '2026-04-01 09:15:00'),
(5, 3, '果业企业管理员', 'ENTERPRISE_ADMIN', 2, 'PRODUCT_CREATE', 'PRODUCT', 3, '赣南脐橙', 'SUCCESS', '新增赣南脐橙产品档案。', '2026-04-01 09:20:00'),
(6, 3, '果业企业管理员', 'ENTERPRISE_ADMIN', 2, 'PRODUCT_CREATE', 'PRODUCT', 4, '阳光葡萄', 'SUCCESS', '新增阳光葡萄产品档案。', '2026-04-01 09:25:00'),
(7, 2, '稻香企业管理员', 'ENTERPRISE_ADMIN', 1, 'BATCH_PUBLISH', 'BATCH', 1, 'RICE-202604-R1', 'SUCCESS', '大米批次发布并生成公开追溯码。', '2026-04-09 09:10:00'),
(8, 2, '稻香企业管理员', 'ENTERPRISE_ADMIN', 1, 'BATCH_PUBLISH', 'BATCH', 2, 'PEANUT-202604-P1', 'SUCCESS', '花生批次发布并生成公开追溯码。', '2026-04-10 10:20:00'),
(9, 3, '果业企业管理员', 'ENTERPRISE_ADMIN', 2, 'BATCH_PUBLISH', 'BATCH', 3, 'ORANGE-202604-Q1', 'SUCCESS', '赣南脐橙批次发布并生成公开追溯码。', '2026-04-11 09:40:00'),
(10, 6, '监管查看人员', 'REGULATOR', NULL, 'RISK_FREEZE', 'BATCH', 4, 'GRAPE-202604-G1', 'SUCCESS', '阳光葡萄批次因冷链异常冻结。', '2026-04-13 14:20:00'),
(11, 6, '监管查看人员', 'REGULATOR', NULL, 'RISK_RECALL', 'BATCH', 5, 'STRAWBERRY-202604-S1', 'SUCCESS', '草莓批次因包装批号异常召回。', '2026-04-14 10:10:00'),
(12, 1, '平台管理员', 'PLATFORM_ADMIN', NULL, 'QR_QUERY', 'QR', 3, 'orange-202604-q1', 'SUCCESS', '公开查询二维码访问记录已写入。', '2026-04-16 10:30:00');

-- Extra workflow coverage: draft, quality-passed but no QR, and quality-pending batches.
INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, assignee_user_id, assigned_at, task_status, task_completed_at, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, frozen_at, recalled_at, created_at, updated_at
) VALUES
(6, 'RICE-202604-DRAFT', 1, 1, 4, '2026-04-15 08:30:00', 'DRAFT', NULL, '江西省南昌市进贤县高标准稻田基地', '2026-04-15', 'DRAFT',
 '该批次仍在录入中，暂未开放公开查询。', '用于测试现场草稿待续、未质检、未生成二维码状态。', '现场记录仍为草稿，暂不允许发布。',
 NULL, NULL, NULL, '2026-04-15 08:30:00', '2026-04-15 10:20:00'),
(7, 'PEANUT-202604-READY', 2, 1, 4, '2026-04-13 09:00:00', 'COMPLETED', '2026-04-14 16:00:00', '江西省赣州市兴国县花生种植基地', '2026-04-13', 'DRAFT',
 '该批次已完成追溯和质检，等待生成二维码并发布。', '用于测试质检已通过但二维码未生成的待发布状态。', '质检已通过，二维码待生成。',
 NULL, NULL, NULL, '2026-04-13 09:00:00', '2026-04-14 16:00:00'),
(8, 'ORANGE-202604-QA-PENDING', 3, 2, 5, '2026-04-14 09:20:00', 'COMPLETED', '2026-04-15 15:30:00', '江西省赣州市信丰县脐橙产业园', '2026-04-14', 'DRAFT',
 '该批次已补充现场追溯记录，等待质检上传。', '用于测试待质检、不可发布、不可生成二维码状态。', '缺少质检报告，暂不允许发布。',
 NULL, NULL, NULL, '2026-04-14 09:20:00', '2026-04-15 15:30:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(13, 6, 'DRAFT', '大米草稿批次已建档，现场记录仍在编辑。', '稻香企业管理员', '2026-04-15 08:30:00', '2026-04-15 08:30:00'),
(14, 7, 'DRAFT', '花生待发布批次已建档。', '稻香企业管理员', '2026-04-13 09:00:00', '2026-04-13 09:00:00'),
(15, 8, 'DRAFT', '脐橙待质检批次已建档。', '果业企业管理员', '2026-04-14 09:20:00', '2026-04-14 09:20:00');

INSERT INTO batch_field_draft (
  id, batch_id, operator_user_id, stage, title, event_time, operator_name, location, summary, image_url, attachment_ids_json, uploaded_files_json, visible_to_consumer, created_at, updated_at
) VALUES
(1, 6, 4, 'PRODUCE', '大米田间巡查草稿', '2026-04-15 10:10:00', '稻香现场操作员', '稻田 B 区',
 '已记录水稻长势、灌溉情况和病虫害巡查结果，照片待补充后提交。', '/images/products/rice-batch.svg', '[]', '[]', TRUE, '2026-04-15 10:10:00', '2026-04-15 10:20:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(22, 6, 1, 'ARCHIVE', '大米草稿批次建档', '2026-04-15 08:40:00', '稻香企业管理员', '进贤县高标准稻田基地', FALSE,
 '{"summary":"批次已创建，现场记录仍为草稿，暂不对消费者展示。"}', '["/images/products/rice-batch.svg"]', '2026-04-15 08:40:00'),
(23, 7, 1, 'ARCHIVE', '花生待发布批次建档', '2026-04-13 09:10:00', '稻香企业管理员', '兴国县花生基地', TRUE,
 '{"summary":"批次基础信息已确认，准备补全追溯和质检资料。"}', '["/images/products/rice-batch.svg"]', '2026-04-13 09:10:00'),
(24, 7, 1, 'PRODUCE', '花生采收记录完成', '2026-04-13 14:30:00', '稻香现场操作员', '兴国县花生基地', TRUE,
 '{"summary":"完成采收、脱壳前处理和晾晒记录。"}', '["/images/products/rice-batch.svg"]', '2026-04-13 14:30:00'),
(25, 7, 1, 'QUALITY', '花生质检通过', '2026-04-14 15:40:00', '质检员', '赣州农产品质检中心', TRUE,
 '{"summary":"质检结果合格，下一步可生成二维码并发布。"}', '["/images/products/rice-batch.svg"]', '2026-04-14 15:40:00'),
(26, 8, 2, 'ARCHIVE', '脐橙待质检批次建档', '2026-04-14 09:30:00', '果业企业管理员', '信丰县脐橙产业园', TRUE,
 '{"summary":"批次基础信息已登记，等待现场操作补充。"}', '["/images/products/orange-batch.svg"]', '2026-04-14 09:30:00'),
(27, 8, 2, 'PRODUCE', '脐橙采收记录完成', '2026-04-15 15:30:00', '果业现场操作员', '信丰县 B 区果园', TRUE,
 '{"summary":"完成采收、分级和装箱记录，质检报告待上传。"}', '["/images/products/orange-batch.svg"]', '2026-04-15 15:30:00');

INSERT INTO quality_report (id, batch_id, report_no, agency, result, report_json, created_at) VALUES
(6, 7, 'QA-20260414-PEANUT-READY', '赣州市农产品质检中心', 'PASS', '{"highlights":["黄曲霉毒素未超标","含水率合格","等待二维码生成"]}', '2026-04-14 15:50:00');

INSERT INTO operation_audit_log (id, operator_user_id, operator_name, role_code, company_id, action_type, target_type, target_id, target_name, result, summary, created_at) VALUES
(13, 4, '稻香现场操作员', 'OPERATOR', 1, 'FIELD_DRAFT_SAVE', 'BATCH', 6, 'RICE-202604-DRAFT', 'SUCCESS', '保存大米田间巡查草稿。', '2026-04-15 10:20:00'),
(14, 2, '稻香企业管理员', 'ENTERPRISE_ADMIN', 1, 'QUALITY_UPLOAD', 'BATCH', 7, 'PEANUT-202604-READY', 'SUCCESS', '花生待发布批次已上传合格质检报告。', '2026-04-14 15:50:00'),
(15, 5, '果业现场操作员', 'OPERATOR', 2, 'TRACE_EVENT_SUBMIT', 'BATCH', 8, 'ORANGE-202604-QA-PENDING', 'SUCCESS', '脐橙待质检批次已提交现场追溯记录。', '2026-04-15 15:30:00');

-- Extended demo data for management pages: every summary chip has at least two records.
INSERT INTO org_company (id, name, license_no, address, contact, phone, status, created_at) VALUES
(4, '南昌绿谷农业合作社', 'LIC-JX-GREEN-004', '江西省南昌市新建区现代农业园', '吴芳', '13800000004', 'DISABLED', '2026-04-02 09:00:00'),
(5, '赣州山地果蔬有限公司', 'LIC-JX-MOUNTAIN-005', '江西省赣州市上犹县高山果蔬基地', '李晨', '13800000005', 'DISABLED', '2026-04-02 09:20:00'),
(6, '九江湖口粮油有限公司', 'LIC-JX-GRAIN-006', '江西省九江市湖口县粮油加工园', '罗敏', '13800000006', 'ARCHIVED', '2026-04-02 09:40:00'),
(7, '抚州生态蔬菜基地', 'LIC-JX-VEG-007', '江西省抚州市临川区生态蔬菜基地', '赵强', '13800000007', 'ARCHIVED', '2026-04-02 10:00:00');

INSERT INTO base_product (id, company_id, product_code, name, category, origin_place, spec, unit, image_url, status, created_at) VALUES
(6, 4, 'BLUEBERRY-001', '蓝莓', '水果', '江西省南昌市新建区', '1.5kg/盒', '盒', '/images/products/orange-batch.svg', 'DISABLED', '2026-04-02 10:10:00'),
(7, 5, 'SWEETPOTATO-001', '红薯', '根茎类', '江西省赣州市上犹县', '5kg/箱', '箱', '/images/products/rice-batch.svg', 'DISABLED', '2026-04-02 10:20:00'),
(8, 6, 'KIWI-001', '猕猴桃', '水果', '江西省九江市湖口县', '3kg/箱', '箱', '/images/products/orange-batch.svg', 'ARCHIVED', '2026-04-02 10:30:00'),
(9, 7, 'LOTUS-ROOT-001', '莲藕', '蔬菜', '江西省抚州市临川区', '4kg/箱', '箱', '/images/products/rice-batch.svg', 'ARCHIVED', '2026-04-02 10:40:00');

INSERT INTO sys_user (id, username, password, real_name, phone, role_code, company_id, status, need_change_password, password_updated_at, created_at, updated_at) VALUES
(7, 'platform_backup', '{noop}123456', '平台备份管理员', '13900000007', 'PLATFORM_ADMIN', NULL, 1, 1, '2026-04-01 08:35:00', '2026-04-01 08:35:00', '2026-04-01 08:35:00'),
(8, 'regulator_backup', '{noop}123456', '监管复核人员', '13900000008', 'REGULATOR', NULL, 1, 1, '2026-04-01 08:40:00', '2026-04-01 08:40:00', '2026-04-01 08:40:00'),
(9, 'operator_disabled', '{noop}123456', '停用现场操作员', '13900000009', 'OPERATOR', 1, 0, 0, '2026-04-01 08:45:00', '2026-04-01 08:45:00', '2026-04-01 08:45:00'),
(10, 'enterprise_disabled', '{noop}123456', '停用企业管理员', '13900000010', 'ENTERPRISE_ADMIN', 4, 0, 0, '2026-04-01 08:50:00', '2026-04-01 08:50:00', '2026-04-01 08:50:00');

INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, assignee_user_id, assigned_at, task_status, task_completed_at, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, frozen_at, recalled_at, created_at, updated_at
) VALUES
(9, 'RICE-202604-READY-A', 1, 1, 4, '2026-04-16 08:30:00', 'COMPLETED', '2026-04-16 11:30:00', '江西省南昌市进贤县高标准稻田基地', '2026-04-16', 'DRAFT',
 '该批次大米资料已齐全，等待管理员确认发布。', '用于测试可发布、待发布二维码和质检待发布状态。', '追溯、质检和二维码均已完成，可发布。',
 NULL, NULL, NULL, '2026-04-16 08:30:00', '2026-04-16 11:30:00'),
(10, 'ORANGE-202604-READY-B', 3, 2, 5, '2026-04-16 08:40:00', 'COMPLETED', '2026-04-16 11:40:00', '江西省赣州市信丰县脐橙产业园', '2026-04-16', 'DRAFT',
 '该批次赣南脐橙资料已齐全，等待发布。', '用于测试水果类可发布样本。', '追溯、质检和二维码均已完成，可发布。',
 NULL, NULL, NULL, '2026-04-16 08:40:00', '2026-04-16 11:40:00'),
(11, 'GRAPE-202604-FROZEN-A', 4, 2, 5, '2026-04-12 09:10:00', 'COMPLETED', '2026-04-13 14:00:00', '江西省赣州市章贡区葡萄基地', '2026-04-12', 'FROZEN',
 '该批次阳光葡萄因冷链异常暂停公开流通。', '风险冻结样本一。', '冷链温度记录异常，已冻结等待处理。',
 NULL, '2026-04-14 09:20:00', NULL, '2026-04-12 09:10:00', '2026-04-14 09:20:00'),
(12, 'PEANUT-202604-FROZEN-B', 2, 1, 4, '2026-04-12 09:30:00', 'COMPLETED', '2026-04-13 15:00:00', '江西省赣州市兴国县花生种植基地', '2026-04-12', 'FROZEN',
 '该批次花生因复检不合格暂停公开流通。', '风险冻结样本二，同时补充不合格质检样本。', '复检发现指标异常，已冻结。',
 NULL, '2026-04-14 09:50:00', NULL, '2026-04-12 09:30:00', '2026-04-14 09:50:00'),
(13, 'ORANGE-202604-PROCESSING-B', 3, 2, 5, '2026-04-13 08:30:00', 'COMPLETED', '2026-04-14 14:30:00', '江西省赣州市信丰县脐橙产业园', '2026-04-13', 'FROZEN',
 '该批次脐橙正在处理包装标签复核问题。', '风险处理中样本。', '包装标签复核中，暂不恢复公开。',
 NULL, '2026-04-15 09:00:00', NULL, '2026-04-13 08:30:00', '2026-04-15 09:00:00'),
(14, 'RICE-202604-RECTIFIED-A', 1, 1, 4, '2026-04-11 08:30:00', 'COMPLETED', '2026-04-12 17:00:00', '江西省南昌市进贤县高标准稻田基地', '2026-04-11', 'FROZEN',
 '该批次大米异常已整改，等待恢复公开。', '已整改样本一。', '整改材料已提交，等待恢复发布。',
 NULL, '2026-04-13 10:00:00', NULL, '2026-04-11 08:30:00', '2026-04-15 11:20:00'),
(15, 'GRAPE-202604-RECTIFIED-B', 4, 2, 5, '2026-04-11 08:50:00', 'COMPLETED', '2026-04-12 17:20:00', '江西省赣州市章贡区葡萄基地', '2026-04-11', 'FROZEN',
 '该批次阳光葡萄冷链记录已补齐，等待恢复公开。', '已整改样本二。', '整改材料已提交，等待恢复发布。',
 NULL, '2026-04-13 10:20:00', NULL, '2026-04-11 08:50:00', '2026-04-15 11:40:00'),
(16, 'ORANGE-202604-RECALL-B', 3, 2, 5, '2026-04-10 08:40:00', 'COMPLETED', '2026-04-11 16:30:00', '江西省赣州市信丰县脐橙产业园', '2026-04-10', 'RECALLED',
 '该批次脐橙已召回并保留公开提醒。', '召回样本二。', '抽检发现包装信息异常，已召回。',
 NULL, NULL, '2026-04-15 14:00:00', '2026-04-10 08:40:00', '2026-04-15 14:00:00'),
(17, 'STRAWBERRY-202604-RECALL-C', 5, 3, 5, '2026-04-10 09:10:00', 'COMPLETED', '2026-04-11 17:00:00', '江西省赣州市于都县现代农业示范园', '2026-04-10', 'RECALLED',
 '该批次草莓已召回并保留公开提醒。', '召回样本三。', '销售端反馈批号不一致，已召回。',
 NULL, NULL, '2026-04-15 14:30:00', '2026-04-10 09:10:00', '2026-04-15 14:30:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(16, 9, 'DRAFT', '大米待发布批次建档。', '稻香企业管理员', '2026-04-16 08:30:00', '2026-04-16 08:30:00'),
(17, 10, 'DRAFT', '脐橙待发布批次建档。', '果业企业管理员', '2026-04-16 08:40:00', '2026-04-16 08:40:00'),
(18, 11, 'FROZEN', '阳光葡萄冷链异常冻结。', '监管查看人员', '2026-04-14 09:20:00', '2026-04-14 09:20:00'),
(19, 12, 'FROZEN', '花生复检不合格冻结。', '监管查看人员', '2026-04-14 09:50:00', '2026-04-14 09:50:00'),
(20, 13, 'FROZEN', '脐橙包装标签异常冻结。', '监管查看人员', '2026-04-15 09:00:00', '2026-04-15 09:00:00'),
(21, 14, 'FROZEN', '大米仓储温湿度异常冻结。', '监管查看人员', '2026-04-13 10:00:00', '2026-04-13 10:00:00'),
(22, 15, 'FROZEN', '葡萄冷链单据异常冻结。', '监管查看人员', '2026-04-13 10:20:00', '2026-04-13 10:20:00'),
(23, 16, 'RECALLED', '脐橙包装信息异常召回。', '监管查看人员', '2026-04-15 14:00:00', '2026-04-15 14:00:00'),
(24, 17, 'RECALLED', '草莓销售端批号不一致召回。', '监管查看人员', '2026-04-15 14:30:00', '2026-04-15 14:30:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(28, 9, 1, 'ARCHIVE', '大米待发布批次建档完成', '2026-04-16 08:45:00', '稻香企业管理员', '进贤县高标准稻田基地', TRUE,
 '{"summary":"完成大米批次建档，产地、规格和企业主体信息已核对。"}', '["/images/products/rice-batch.svg"]', '2026-04-16 08:45:00'),
(29, 9, 1, 'QUALITY', '大米待发布批次质检通过', '2026-04-16 10:50:00', '质检员', '江西省农产品质检中心', TRUE,
 '{"summary":"水分、杂质和重金属指标合格，可进入公开发布。"}', '["/images/products/rice-batch.svg"]', '2026-04-16 10:50:00'),
(30, 10, 2, 'ARCHIVE', '脐橙待发布批次建档完成', '2026-04-16 08:55:00', '果业企业管理员', '信丰县脐橙产业园', TRUE,
 '{"summary":"完成脐橙批次建档，果园、采收日期和规格信息已核对。"}', '["/images/products/orange-batch.svg"]', '2026-04-16 08:55:00'),
(31, 10, 2, 'QUALITY', '脐橙待发布批次质检通过', '2026-04-16 11:00:00', '质检员', '赣南果品质检室', TRUE,
 '{"summary":"农残、糖度和外观抽检合格，可进入公开发布。"}', '["/images/products/orange-batch.svg"]', '2026-04-16 11:00:00'),
(32, 11, 2, 'REGULATION', '阳光葡萄批次冻结', '2026-04-14 09:20:00', '监管查看人员', '赣州冷链中转仓', TRUE,
 '{"summary":"冷链温度记录出现异常，批次冻结等待进一步处置。"}', '["/images/products/orange-batch.svg"]', '2026-04-14 09:20:00'),
(33, 12, 1, 'REGULATION', '花生批次冻结', '2026-04-14 09:50:00', '监管查看人员', '赣州农产品复检点', TRUE,
 '{"summary":"复检发现指标异常，批次冻结并等待企业补充说明。"}', '["/images/products/rice-batch.svg"]', '2026-04-14 09:50:00'),
(34, 13, 2, 'REGULATION', '脐橙风险处理中', '2026-04-15 09:00:00', '监管查看人员', '信丰县脐橙产业园', TRUE,
 '{"summary":"包装标签复核中，企业已提交处理进度。"}', '["/images/products/orange-batch.svg"]', '2026-04-15 09:00:00'),
(35, 14, 1, 'REGULATION', '大米风险已整改', '2026-04-15 11:20:00', '监管查看人员', '进贤县仓储点', TRUE,
 '{"summary":"仓储温湿度记录已补齐，整改材料等待恢复发布。"}', '["/images/products/rice-batch.svg"]', '2026-04-15 11:20:00'),
(36, 15, 2, 'REGULATION', '葡萄风险已整改', '2026-04-15 11:40:00', '监管查看人员', '赣州冷链中转仓', TRUE,
 '{"summary":"冷链单据复核完成，整改材料等待恢复发布。"}', '["/images/products/orange-batch.svg"]', '2026-04-15 11:40:00'),
(37, 16, 2, 'REGULATION', '脐橙召回通知发布', '2026-04-15 14:00:00', '监管查看人员', '信丰县监管联络点', TRUE,
 '{"summary":"包装信息异常，批次召回并同步公开提醒。"}', '["/images/products/orange-batch.svg"]', '2026-04-15 14:00:00'),
(38, 17, 3, 'REGULATION', '草莓召回通知发布', '2026-04-15 14:30:00', '监管查看人员', '于都县监管联络点', TRUE,
 '{"summary":"销售端批号不一致，批次召回并同步公开提醒。"}', '["/images/products/orange-batch.svg"]', '2026-04-15 14:30:00');

INSERT INTO quality_report (id, batch_id, report_no, agency, result, report_json, created_at) VALUES
(7, 9, 'QA-20260416-RICE-READY-A', '江西省农产品质检中心', 'PASS', '{"highlights":["水分指标合格","杂质指标合格","二维码已生成，等待发布"]}', '2026-04-16 10:55:00'),
(8, 10, 'QA-20260416-ORANGE-READY-B', '赣南果品质检室', 'PASS', '{"highlights":["农残指标合格","糖度抽检合格","二维码已生成，等待发布"]}', '2026-04-16 11:05:00'),
(9, 11, 'QA-20260413-GRAPE-FROZEN-A', '赣州果品质检室', 'PASS', '{"highlights":["农残合格","冷链异常待处理","暂缓公开"]}', '2026-04-13 14:20:00'),
(10, 12, 'QA-20260413-PEANUT-FROZEN-B', '赣州市农产品复检点', 'FAIL', '{"highlights":["复检指标异常","批次已冻结","等待企业整改"]}', '2026-04-13 15:20:00'),
(11, 13, 'QA-20260414-ORANGE-PROCESSING-B', '赣南果品质检室', 'PASS', '{"highlights":["质量指标合格","包装标签复核中","风险处理中"]}', '2026-04-14 14:40:00'),
(12, 14, 'QA-20260412-RICE-RECTIFIED-A', '江西省农产品质检中心', 'PASS', '{"highlights":["整改后复核合格","仓储记录已补齐","等待恢复发布"]}', '2026-04-12 17:20:00'),
(13, 15, 'QA-20260412-GRAPE-RECTIFIED-B', '赣州果品质检室', 'PASS', '{"highlights":["整改后复核合格","冷链单据已补齐","等待恢复发布"]}', '2026-04-12 17:40:00'),
(14, 16, 'QA-20260411-ORANGE-RECALL-B', '赣南果品质检室', 'FAIL', '{"highlights":["包装信息异常","已触发召回","公开页保留提醒"]}', '2026-04-11 16:50:00'),
(15, 17, 'QA-20260411-STRAWBERRY-RECALL-C', '于都县农产品复检点', 'FAIL', '{"highlights":["批号信息不一致","已触发召回","公开页保留提醒"]}', '2026-04-11 17:20:00');

INSERT INTO qr_code (id, batch_id, qr_token, status, created_at, remark, status_reason, expired_at, last_query_at, pv) VALUES
(6, 9, 'rice-202604-ready-a', 'ACTIVE', '2026-04-16 11:00:00', '大米待发布二维码', NULL, NULL, NULL, 0),
(7, 10, 'orange-202604-ready-b', 'ACTIVE', '2026-04-16 11:10:00', '脐橙待发布二维码', NULL, NULL, NULL, 0),
(8, 11, 'grape-202604-frozen-a', 'SUSPENDED', '2026-04-13 14:30:00', '阳光葡萄冻结二维码', '冷链异常冻结，暂缓公开。', NULL, '2026-04-16 10:50:00', 3),
(9, 12, 'peanut-202604-frozen-b', 'SUSPENDED', '2026-04-13 15:30:00', '花生冻结二维码', '复检不合格冻结，暂缓公开。', NULL, '2026-04-16 10:55:00', 2),
(10, 13, 'orange-202604-processing-b', 'SUSPENDED', '2026-04-14 14:50:00', '脐橙处理中二维码', '标签复核处理中，暂缓公开。', NULL, '2026-04-16 11:00:00', 2),
(11, 14, 'rice-202604-rectified-a', 'SUSPENDED', '2026-04-12 17:30:00', '大米已整改二维码', '整改完成，等待恢复公开。', NULL, '2026-04-16 11:05:00', 2),
(12, 15, 'grape-202604-rectified-b', 'SUSPENDED', '2026-04-12 17:50:00', '葡萄已整改二维码', '整改完成，等待恢复公开。', NULL, '2026-04-16 11:10:00', 2),
(13, 16, 'orange-202604-recall-b', 'RECALLED', '2026-04-11 17:00:00', '脐橙召回二维码', '包装信息异常，已召回。', NULL, '2026-04-16 11:15:00', 4),
(14, 17, 'strawberry-202604-recall-c', 'RECALLED', '2026-04-11 17:30:00', '草莓召回二维码', '批号信息不一致，已召回。', NULL, '2026-04-16 11:20:00', 5);

INSERT INTO qr_query_log (id, qr_id, batch_id, query_time, ip, ua, referer) VALUES
(9, 8, 11, '2026-04-16 10:50:00', '192.168.0.71', 'WeChat', 'scan'),
(10, 9, 12, '2026-04-16 10:55:00', '192.168.0.72', 'WeChat', 'scan'),
(11, 10, 13, '2026-04-16 11:00:00', '192.168.0.73', 'Chrome', 'scan'),
(12, 11, 14, '2026-04-16 11:05:00', '192.168.0.74', 'Chrome', 'scan'),
(13, 12, 15, '2026-04-16 11:10:00', '192.168.0.75', 'Mobile Safari', 'scan'),
(14, 13, 16, '2026-04-16 11:15:00', '192.168.0.76', 'WeChat', 'scan'),
(15, 14, 17, '2026-04-16 11:20:00', '192.168.0.77', 'WeChat', 'scan');

INSERT INTO batch_risk_action (id, batch_id, action_type, reason, comment, operator_name, created_at) VALUES
(5, 13, 'PROCESSING', '包装标签复核中', '企业已提交包装线记录，等待监管复核。', '果业企业管理员', '2026-04-15 09:20:00'),
(6, 14, 'PROCESSING', '仓储记录异常', '已通知企业补充仓储温湿度记录。', '监管查看人员', '2026-04-13 10:20:00'),
(7, 14, 'RECTIFIED', '整改材料已补齐', '企业补齐仓储温湿度记录，等待恢复发布。', '稻香企业管理员', '2026-04-15 11:20:00'),
(8, 15, 'PROCESSING', '冷链单据异常', '已通知企业补充冷链运输单据。', '监管查看人员', '2026-04-13 10:40:00'),
(9, 15, 'RECTIFIED', '整改材料已补齐', '企业补齐冷链运输单据，等待恢复发布。', '果业企业管理员', '2026-04-15 11:40:00'),
(10, 16, 'COMMENT', '包装信息异常召回', '批次已召回，公开页保留风险提示。', '监管查看人员', '2026-04-15 14:05:00'),
(11, 17, 'COMMENT', '销售端批号不一致召回', '批次已召回，公开页保留风险提示。', '监管查看人员', '2026-04-15 14:35:00');

INSERT INTO operation_audit_log (id, operator_user_id, operator_name, role_code, company_id, action_type, target_type, target_id, target_name, result, summary, created_at) VALUES
(16, 1, '平台管理员', 'PLATFORM_ADMIN', NULL, 'COMPANY_DISABLE', 'COMPANY', 4, '南昌绿谷农业合作社', 'SUCCESS', '补充停用企业样本。', '2026-04-16 08:00:00'),
(17, 1, '平台管理员', 'PLATFORM_ADMIN', NULL, 'COMPANY_ARCHIVE', 'COMPANY', 6, '九江湖口粮油有限公司', 'SUCCESS', '补充归档企业样本。', '2026-04-16 08:05:00'),
(18, 2, '稻香企业管理员', 'ENTERPRISE_ADMIN', 1, 'BATCH_READY', 'BATCH', 9, 'RICE-202604-READY-A', 'SUCCESS', '大米批次资料齐全，进入待发布。', '2026-04-16 11:30:00'),
(19, 3, '果业企业管理员', 'ENTERPRISE_ADMIN', 2, 'BATCH_READY', 'BATCH', 10, 'ORANGE-202604-READY-B', 'SUCCESS', '脐橙批次资料齐全，进入待发布。', '2026-04-16 11:40:00'),
(20, 6, '监管查看人员', 'REGULATOR', NULL, 'RISK_PROCESSING', 'BATCH', 13, 'ORANGE-202604-PROCESSING-B', 'SUCCESS', '脐橙风险批次标记处理中。', '2026-04-15 09:20:00'),
(21, 6, '监管查看人员', 'REGULATOR', NULL, 'RISK_RECTIFIED', 'BATCH', 14, 'RICE-202604-RECTIFIED-A', 'SUCCESS', '大米风险批次标记已整改。', '2026-04-15 11:20:00'),
(22, 6, '监管查看人员', 'REGULATOR', NULL, 'RISK_RECTIFIED', 'BATCH', 15, 'GRAPE-202604-RECTIFIED-B', 'SUCCESS', '葡萄风险批次标记已整改。', '2026-04-15 11:40:00'),
(23, 6, '监管查看人员', 'REGULATOR', NULL, 'RISK_RECALL', 'BATCH', 16, 'ORANGE-202604-RECALL-B', 'SUCCESS', '脐橙异常批次召回。', '2026-04-15 14:00:00'),
(24, 6, '监管查看人员', 'REGULATOR', NULL, 'RISK_RECALL', 'BATCH', 17, 'STRAWBERRY-202604-RECALL-C', 'SUCCESS', '草莓异常批次召回。', '2026-04-15 14:30:00');
