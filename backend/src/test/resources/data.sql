INSERT INTO org_company (id, name, license_no, address, contact, phone, status, created_at) VALUES
(1, 'Demo Orchard Company', 'LIC-DEMO-001', 'Jiangxi Ganzhou Xinfeng Orchard', 'Liu Jianguo', '13800000001', 'ENABLED', '2026-03-11 09:00:00');

INSERT INTO base_product (id, company_id, product_code, name, category, origin_place, spec, unit, image_url, status, created_at) VALUES
(1, 1, 'ORANGE-001', 'Navel Orange', 'Fruit', 'Jiangxi Ganzhou Xinfeng', '10kg/box', 'box', '/images/products/orange-batch.svg', 'ENABLED', '2026-03-11 10:00:00');

INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, created_at, updated_at
) VALUES
(1, 'BATCH20260311001', 1, 1, 'Jiangxi Ganzhou Xinfeng Orchard', '2026-03-01', 'PUBLISHED',
 'This batch is ready for public scan demo.', 'Demo batch for automated tests.', NULL, '2026-03-11 10:30:00',
 '2026-03-11 10:20:00', '2026-03-11 10:30:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(1, 1, 'DRAFT', 'Draft batch created', 'Enterprise Admin', '2026-03-01 08:00:00', '2026-03-01 08:00:00'),
(2, 1, 'PUBLISHED', 'Quality and QR are ready', 'Enterprise Admin', '2026-03-11 10:30:00', '2026-03-11 10:30:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(1, 1, 1, 'ARCHIVE', 'Batch archived in system', '2026-03-01 08:00:00', 'Liu Jianguo', 'Xinfeng Orchard', TRUE,
 '{"summary":"The enterprise, product and batch base information has been recorded."}', '["/images/products/orange-batch.svg"]', '2026-03-01 08:00:00'),
(2, 1, 1, 'MARKET', 'Arrived at retail shelf', '2026-03-11 09:00:00', 'Store Guide', 'Nanchang Demo Store', TRUE,
 '{"summary":"The batch is ready for public display and scan demo."}', '["/images/products/orange-batch.svg"]', '2026-03-11 09:00:00');

INSERT INTO quality_report (id, batch_id, report_no, agency, result, report_json, created_at) VALUES
(1, 1, 'QA-20260311-001', 'Jiangxi Quality Center', 'PASS', '{"highlights":["No pesticide residue issue","Microbiology passed","Appearance normal"]}', '2026-03-11 09:40:00');

INSERT INTO qr_code (id, batch_id, qr_token, status, created_at, remark, last_query_at, pv) VALUES
(1, 1, 'test-token-2026', 'ACTIVE', '2026-03-11 11:10:00', 'Demo QR code', '2026-03-24 10:15:00', 1);

INSERT INTO qr_query_log (id, qr_id, batch_id, query_time, ip, ua, referer) VALUES
(1, 1, 1, '2026-03-24 10:15:00', '127.0.0.1', 'DemoBrowser/Desktop', 'http://127.0.0.1:5173');
