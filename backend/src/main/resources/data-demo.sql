INSERT INTO org_company (id, name, license_no, address, contact, phone, status, created_at) VALUES
(1, '赣南果业种植有限公司', 'LIC-JX-001', '江西省赣州市信丰县脐橙种植基地', '刘建国', '13800000001', 'ENABLED', '2026-03-01 09:00:00'),
(2, '婺源茶业合作社', 'LIC-JX-002', '江西省上饶市婺源县生态茶园基地', '张敏', '13800000002', 'ENABLED', '2026-03-01 09:20:00'),
(3, '南昌优质稻米中心', 'LIC-JX-003', '江西省南昌市优质稻米储运中心', '陈浩', '13800000003', 'ENABLED', '2026-03-01 09:40:00');

INSERT INTO sys_user (id, username, password, real_name, role_code, company_id, status, need_change_password, password_updated_at, created_at, updated_at) VALUES
(1, 'platform', '{noop}123456', '平台管理员', 'PLATFORM_ADMIN', NULL, 1, 0, '2026-03-01 08:00:00', '2026-03-01 08:00:00', '2026-03-01 08:00:00'),
(2, 'enterprise_admin', '{noop}123456', '企业管理员', 'ENTERPRISE_ADMIN', 1, 1, 0, '2026-03-01 08:10:00', '2026-03-01 08:10:00', '2026-03-01 08:10:00'),
(3, 'operator', '{noop}123456', '现场操作员', 'OPERATOR', 1, 1, 0, '2026-03-01 08:20:00', '2026-03-01 08:20:00', '2026-03-01 08:20:00'),
(4, 'regulator', '{noop}123456', '监管人员', 'REGULATOR', NULL, 1, 0, '2026-03-01 08:30:00', '2026-03-01 08:30:00', '2026-03-01 08:30:00'),
(5, 'operator_support', '{noop}123456', '现场操作员B', 'OPERATOR', 1, 1, 0, '2026-03-01 08:35:00', '2026-03-01 08:35:00', '2026-03-01 08:35:00');

INSERT INTO base_product (id, company_id, product_code, name, category, origin_place, spec, unit, image_url, status, created_at) VALUES
(1, 1, 'ORANGE-001', '赣南脐橙', '水果', '江西省赣州市信丰县', '10kg/箱', '箱', '/images/products/orange-batch.svg', 'ENABLED', '2026-03-01 10:00:00'),
(2, 2, 'TEA-001', '明前绿茶', '茶叶', '江西省上饶市婺源县', '250g/罐', '罐', '/images/products/green-tea-batch.svg', 'ENABLED', '2026-03-01 10:10:00'),
(3, 3, 'RICE-001', '优质大米', '粮油', '江西省南昌市', '5kg/袋', '袋', '/images/products/rice-batch.svg', 'ENABLED', '2026-03-01 10:20:00');

INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, assignee_user_id, assigned_at, task_status, task_completed_at, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, frozen_at, recalled_at, created_at, updated_at
) VALUES
(1, 'ORANGE-202603-A1', 1, 1, 5, '2026-03-03 08:00:00', 'COMPLETED', '2026-03-08 09:15:00', '江西省赣州市信丰果园基地', '2026-03-03', 'PUBLISHED',
 '当前批次已开放公开查询，可继续查看关键追溯节点。', '用于核对已发布批次与工作台、公开页的联动状态。', NULL,
 '2026-03-08 10:00:00', NULL, NULL, '2026-03-03 08:00:00', '2026-03-08 10:00:00'),
(2, 'ORANGE-202603-D1', 1, 1, 5, '2026-03-20 09:00:00', 'PENDING', NULL, '江西省赣州市信丰果园基地', '2026-03-20', 'PUBLISHED',
 '当前批次已开放公开查询，可继续查看现场记录、质检与二维码状态。', '用于管理员、操作员和公开页的全链路回归基线验证。', '质检合格且二维码已生成，当前批次已对外发布。',
 '2026-03-21 11:30:00', NULL, NULL, '2026-03-20 09:00:00', '2026-03-21 11:30:00'),
(3, 'TEA-202603-F1', 2, 2, NULL, NULL, 'PENDING', NULL, '江西省上饶市婺源县茶园基地', '2026-03-09', 'FROZEN',
 '当前批次已暂停流转，等待风险处理完成。', '用于核对冻结批次的整改处理流程。', '最近一次质检未通过，当前批次等待复检。',
 '2026-03-12 09:00:00', '2026-03-18 14:20:00', NULL, '2026-03-09 08:30:00', '2026-03-18 14:20:00'),
(4, 'RICE-202603-R1', 3, 3, NULL, NULL, 'PENDING', NULL, '江西省南昌市优质稻米中心', '2026-03-05', 'RECALLED',
 '当前批次已召回，请按风险提示处理。', '用于核对召回批次与公开提醒联动。', '物流环节已确认存在风险，当前召回处理中。',
 '2026-03-10 11:00:00', NULL, '2026-03-21 09:40:00', '2026-03-05 07:30:00', '2026-03-21 09:40:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(1, 1, 'DRAFT', '批次建档完成，准备补充追溯与质检资料。', '企业管理员', '2026-03-03 08:00:00', '2026-03-03 08:00:00'),
(2, 1, 'PUBLISHED', '质检结果和二维码均已准备完成，批次对外发布。', '企业管理员', '2026-03-08 10:00:00', '2026-03-08 10:00:00'),
(3, 2, 'DRAFT', '批次已创建，等待继续补充关键环节信息。', '企业管理员', '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
(10, 2, 'PUBLISHED', '质检结果和二维码均已准备完成，批次对外发布。', '企业管理员', '2026-03-21 11:30:00', '2026-03-21 11:30:00'),
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
(4, 2, 'QA-20260321-004', '江西省农产品质检中心', 'PASS',
 '{"highlights":["农残指标合格","微生物指标合格","果品外观符合公开销售要求"]}', '2026-03-21 10:50:00'),
(2, 3, 'QA-20260318-002', '婺源茶叶质检实验室', 'FAIL',
 '{"highlights":["含水率超出内部阈值","批次需保持暂停流通直至复核完成"]}', '2026-03-18 13:20:00'),
(3, 4, 'QA-20260310-003', '南昌粮油检测实验室', 'PASS',
 '{"highlights":["仓储阶段质检合格","当前风险来自物流环节而非仓储质量"]}', '2026-03-10 08:50:00');

INSERT INTO qr_code (id, batch_id, qr_token, status, created_at, remark, status_reason, last_query_at, pv) VALUES
(1, 1, 'demo-normal-2026', 'ACTIVE', '2026-03-08 09:00:00', '已启用公开查询二维码', NULL, '2026-03-24 10:15:00', 6),
(4, 2, 'orange-202603-d1', 'ACTIVE', '2026-03-21 11:00:00', '已启用公开查询二维码', NULL, '2026-03-24 10:45:00', 4),
(2, 3, 'demo-frozen-2026', 'SUSPENDED', '2026-03-12 08:50:00', '二维码已暂停使用', '批次处理中，暂不对外开放。', '2026-03-24 11:00:00', 2),
(3, 4, 'demo-recall-2026', 'RECALLED', '2026-03-10 10:50:00', '二维码保留召回提醒', '该批次已召回，请按风险提示处理。', '2026-03-24 11:40:00', 4);

INSERT INTO qr_query_log (id, qr_id, batch_id, query_time, ip, ua, referer) VALUES
(1, 1, 1, '2026-03-24 10:15:00', '127.0.0.1', 'Browser/Desktop', 'http://127.0.0.1:5173'),
(2, 1, 1, '2026-03-24 10:25:00', '127.0.0.2', 'Browser/Mobile', 'http://127.0.0.1:5173'),
(5, 4, 2, '2026-03-24 10:45:00', '127.0.0.5', 'Browser/Mobile', 'http://127.0.0.1:5173'),
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
INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, assignee_user_id, assigned_at, task_status, task_completed_at, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, frozen_at, recalled_at, created_at, updated_at
) VALUES
(5, 'ORANGE-202604-Q1', 1, 1, 3, '2026-04-02 08:30:00', 'PENDING', NULL, '江西省赣州市信丰果园基地', '2026-04-02', 'DRAFT',
 '主演示企业待质检批次，用于展示任务推进、现场填报和质检待办。', '用于企业管理员、现场操作员和平台管理员演示待处理链路。', '已完成建档并提交首轮现场记录，待上传质检并生成二维码。',
 NULL, NULL, NULL, '2026-04-02 08:20:00', '2026-04-02 11:10:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(11, 5, 'DRAFT', '主演示企业的新批次已完成建档，等待继续推进现场记录与质检数据。', '企业管理员', '2026-04-02 08:20:00', '2026-04-02 08:20:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(12, 2, 1, 'ARCHIVE', '任务分配完成', '2026-03-20 09:25:00', '平台管理员', '平台控制中心', TRUE,
 '{"summary":"主演示批次已分配给现场操作员B，后续现场填报、质检和发布流程均围绕该批次展开。","action":"ASSIGN_OPERATOR","role":"PLATFORM_ADMIN","result":"SUCCESS"}', '[]', '2026-03-20 09:25:00'),
(13, 2, 1, 'PRODUCE', '现场采后分拣完成', '2026-03-20 14:20:00', '现场操作员B', 'C区果园', TRUE,
 '{"summary":"完成分拣、装筐与标签核对，已留存现场图片并进入待送检状态。","action":"FIELD_SUBMIT","role":"OPERATOR","result":"SUCCESS"}', '["/images/products/orange-batch.svg"]', '2026-03-20 14:20:00'),
(14, 2, 1, 'TRANSPORT', '冷链交接完成', '2026-03-21 08:40:00', '现场操作员B', '信丰冷库收货口', TRUE,
 '{"summary":"批次已完成采后入库与冷链交接，进入质检与二维码准备环节。","action":"COLD_CHAIN_HANDOVER","role":"OPERATOR","result":"SUCCESS"}', '[]', '2026-03-21 08:40:00'),
(15, 2, 1, 'QUALITY', '质检结果已确认', '2026-03-21 10:50:00', '企业管理员', '江西省农产品质检中心', TRUE,
 '{"summary":"最新质检结果为合格，明星演示批次满足继续公开发布条件。","action":"QUALITY_APPROVED","role":"ENTERPRISE_ADMIN","result":"PASS"}', '[]', '2026-03-21 10:50:00'),
(16, 2, 1, 'MARKET', '二维码生成完成', '2026-03-21 11:00:00', '平台管理员', '后台发布中心', TRUE,
 '{"summary":"公开查询 token 已固定为 orange-202603-d1，可直接用于答辩扫码演示。","action":"QR_GENERATED","role":"PLATFORM_ADMIN","result":"SUCCESS"}', '[]', '2026-03-21 11:00:00'),
(17, 2, 1, 'MARKET', '公开发布完成', '2026-03-21 11:30:00', '企业管理员', '后台发布中心', TRUE,
 '{"summary":"明星演示批次已正式对外发布，后台工作台和公开追溯页可同步讲解。","action":"BATCH_PUBLISHED","role":"ENTERPRISE_ADMIN","result":"SUCCESS"}', '[]', '2026-03-21 11:30:00'),
(18, 5, 1, 'ARCHIVE', '待处理批次建档完成', '2026-04-02 08:25:00', '企业管理员', '信丰果园办公室', TRUE,
 '{"summary":"待处理批次已完成建档并分配给现场操作员，用于演示待质检链路。","action":"BATCH_CREATED","role":"ENTERPRISE_ADMIN","result":"SUCCESS"}', '[]', '2026-04-02 08:25:00'),
(19, 5, 1, 'PRODUCE', '现场采样完成', '2026-04-02 11:10:00', '现场操作员', 'D区果园', TRUE,
 '{"summary":"现场操作员已完成首轮采样与记录，当前批次等待上传质检结果。","action":"FIELD_SUBMIT","role":"OPERATOR","result":"SUCCESS"}', '[]', '2026-04-02 11:10:00');

INSERT INTO operation_audit_log (
  id, operator_user_id, operator_name, role_code, company_id, action_type, target_type, target_id, target_name, result, summary, created_at
) VALUES
(1, 2, '企业管理员', 'ENTERPRISE_ADMIN', 1, 'BATCH_CREATE', 'TRACE_BATCH', 2, 'ORANGE-202603-D1', 'SUCCESS', '主演示批次建档完成。', '2026-03-20 09:10:00'),
(2, 1, '平台管理员', 'PLATFORM_ADMIN', NULL, 'ASSIGN_OPERATOR', 'TRACE_BATCH', 2, 'ORANGE-202603-D1', 'SUCCESS', '主演示批次已分配给现场操作员B。', '2026-03-20 09:25:00'),
(3, 5, '现场操作员B', 'OPERATOR', 1, 'TRACE_SUBMIT', 'TRACE_BATCH', 2, 'ORANGE-202603-D1', 'SUCCESS', '主演示批次已提交现场采后记录。', '2026-03-20 14:20:00'),
(4, 2, '企业管理员', 'ENTERPRISE_ADMIN', 1, 'QUALITY_UPLOAD', 'TRACE_BATCH', 2, 'ORANGE-202603-D1', 'SUCCESS', '主演示批次质检结果已上传并确认合格。', '2026-03-21 10:50:00'),
(5, 1, '平台管理员', 'PLATFORM_ADMIN', NULL, 'QR_GENERATE', 'TRACE_BATCH', 2, 'ORANGE-202603-D1', 'SUCCESS', '主演示批次二维码与公开 token 已固定。', '2026-03-21 11:00:00'),
(6, 2, '企业管理员', 'ENTERPRISE_ADMIN', 1, 'BATCH_PUBLISH', 'TRACE_BATCH', 2, 'ORANGE-202603-D1', 'SUCCESS', '主演示批次已完成对外发布。', '2026-03-21 11:30:00'),
(7, 2, '企业管理员', 'ENTERPRISE_ADMIN', 1, 'BATCH_CREATE', 'TRACE_BATCH', 5, 'ORANGE-202604-Q1', 'SUCCESS', '待处理批次已完成建档。', '2026-04-02 08:25:00'),
(8, 3, '现场操作员', 'OPERATOR', 1, 'TRACE_SUBMIT', 'TRACE_BATCH', 5, 'ORANGE-202604-Q1', 'SUCCESS', '待处理批次已完成首轮现场记录，等待质检。', '2026-04-02 11:10:00');
