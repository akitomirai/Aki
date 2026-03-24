INSERT INTO org_company (id, name, license_no, address, contact, phone, created_at) VALUES
(1, '赣南脐橙示范基地', 'LIC-DEMO-001', '江西省赣州市信丰县高标准果园', '刘建国', '13800000001', '2026-03-11 09:00:00');

INSERT INTO base_product (id, name, category, spec, unit, image_url, created_at) VALUES
(1, '赣南脐橙', '水果', '10kg/箱', '箱', '/images/products/orange-batch.svg', '2026-03-11 10:00:00');

INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, created_at, updated_at
) VALUES
(1, 'BATCH20260311001', 1, 1, '江西省赣州市信丰县高标准果园', '2026-03-01', 'PUBLISHED',
 '该批次已完成追溯信息归集，可用于扫码演示。', '主演示批次。', NULL, '2026-03-11 10:30:00',
 '2026-03-11 10:20:00', '2026-03-11 10:30:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(1, 1, 'DRAFT', '创建批次草稿', '企业管理员', '2026-03-01 08:00:00', '2026-03-01 08:00:00'),
(2, 1, 'PUBLISHED', '质检和二维码已准备完成，允许正式发布。', '企业管理员', '2026-03-11 10:30:00', '2026-03-11 10:30:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(1, 1, 1, 'ARCHIVE', '完成企业建档', '2026-03-01 08:00:00', '刘建国', '信丰果园', TRUE,
 '{"summary":"已录入企业主体、产地和批次基础信息。"}', '["/images/products/orange-batch.svg"]', '2026-03-01 08:00:00'),
(2, 1, 1, 'MARKET', '门店上架', '2026-03-11 09:00:00', '门店导购', '南昌示范门店', TRUE,
 '{"summary":"已具备对消费者展示的完整追溯信息。"}', '["/images/products/orange-batch.svg"]', '2026-03-11 09:00:00');

INSERT INTO quality_report (id, batch_id, report_no, agency, result, report_json, created_at) VALUES
(1, 1, 'QA-20260311-001', '江西省农产品质量检测中心', 'PASS', '{"highlights":["农残未检出","微生物合格","外观检验正常"]}', '2026-03-11 09:40:00');

INSERT INTO qr_code (id, batch_id, qr_token, status, created_at, remark, last_query_at, pv) VALUES
(1, 1, 'test-token-2026', 'ACTIVE', '2026-03-11 11:10:00', '主演示二维码', '2026-03-24 10:15:00', 1);

INSERT INTO qr_query_log (id, qr_id, batch_id, query_time, ip, ua, referer) VALUES
(1, 1, 1, '2026-03-24 10:15:00', '127.0.0.1', 'DemoBrowser/Desktop', 'http://127.0.0.1:5173');

UPDATE base_product SET company_id = 1 WHERE id = 1;
