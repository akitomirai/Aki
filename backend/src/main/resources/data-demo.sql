INSERT INTO org_company (id, name, license_no, address, contact, phone, status, created_at) VALUES
(1, 'Demo Orchard Company', 'LIC-DEMO-001', 'Jiangxi Ganzhou Xinfeng Orchard', 'Liu Jianguo', '13800000001', 'ENABLED', '2026-03-01 09:00:00'),
(2, 'Demo Tea Cooperative', 'LIC-DEMO-002', 'Jiangxi Shangrao Wuyuan Tea Base', 'Zhang Min', '13800000002', 'ENABLED', '2026-03-01 09:20:00'),
(3, 'Demo Grain Center', 'LIC-DEMO-003', 'Jiangxi Nanchang Grain Storage Center', 'Chen Hao', '13800000003', 'ENABLED', '2026-03-01 09:40:00');

INSERT INTO base_product (id, company_id, product_code, name, category, origin_place, spec, unit, image_url, status, created_at) VALUES
(1, 1, 'DEMO-ORANGE-001', 'Navel Orange', 'Fruit', 'Jiangxi Ganzhou Xinfeng', '10kg/box', 'box', '/images/products/orange-batch.svg', 'ENABLED', '2026-03-01 10:00:00'),
(2, 2, 'DEMO-TEA-001', 'Spring Green Tea', 'Tea', 'Jiangxi Shangrao Wuyuan', '250g/tin', 'tin', '/images/products/green-tea-batch.svg', 'ENABLED', '2026-03-01 10:10:00'),
(3, 3, 'DEMO-RICE-001', 'Premium Rice', 'Grain', 'Jiangxi Nanchang', '5kg/bag', 'bag', '/images/products/rice-batch.svg', 'ENABLED', '2026-03-01 10:20:00');

INSERT INTO trace_batch (
  id, batch_code, product_id, company_id, origin_place, start_date, status,
  public_remark, internal_remark, status_reason, published_at, frozen_at, recalled_at, created_at, updated_at
) VALUES
(1, 'DEMO-ORANGE-202603-A1', 1, 1, 'Jiangxi Ganzhou Xinfeng Orchard', '2026-03-03', 'PUBLISHED',
 'This is the main healthy batch for scan demo.', 'Use this batch for the normal public trace demo.', NULL,
 '2026-03-08 10:00:00', NULL, NULL, '2026-03-03 08:00:00', '2026-03-08 10:00:00'),
(2, 'DEMO-ORANGE-202603-D1', 1, 1, 'Jiangxi Ganzhou Xinfeng Orchard', '2026-03-20', 'DRAFT',
 'This draft batch is waiting for trace, quality and QR steps.', 'Use this batch for admin flow demo.', NULL,
 NULL, NULL, NULL, '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
(3, 'DEMO-TEA-202603-F1', 2, 2, 'Jiangxi Shangrao Wuyuan Tea Base', '2026-03-09', 'FROZEN',
 'This batch is frozen and waiting for handling review.', 'Use this batch for risk handling and resume checklist demo.', 'Latest quality report failed and the batch is frozen for review.',
 '2026-03-12 09:00:00', '2026-03-18 14:20:00', NULL, '2026-03-09 08:30:00', '2026-03-18 14:20:00'),
(4, 'DEMO-RICE-202603-R1', 3, 3, 'Jiangxi Nanchang Grain Storage Center', '2026-03-05', 'RECALLED',
 'This batch is under public recall notice.', 'Use this batch for regulator and recall demo.', 'A logistics contamination risk was confirmed, so the batch has been recalled.',
 '2026-03-10 11:00:00', NULL, '2026-03-21 09:40:00', '2026-03-05 07:30:00', '2026-03-21 09:40:00');

INSERT INTO batch_status_log (id, batch_id, status, reason, operator_name, operated_at, created_at) VALUES
(1, 1, 'DRAFT', 'Batch created for public demo preparation.', 'Enterprise Admin', '2026-03-03 08:00:00', '2026-03-03 08:00:00'),
(2, 1, 'PUBLISHED', 'Quality and QR are ready for public release.', 'Enterprise Admin', '2026-03-08 10:00:00', '2026-03-08 10:00:00'),
(3, 2, 'DRAFT', 'Batch created and waiting for completion.', 'Enterprise Admin', '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
(4, 3, 'DRAFT', 'Batch created for tea process records.', 'Enterprise Admin', '2026-03-09 08:30:00', '2026-03-09 08:30:00'),
(5, 3, 'PUBLISHED', 'Quality passed in the first round and QR was ready.', 'Enterprise Admin', '2026-03-12 09:00:00', '2026-03-12 09:00:00'),
(6, 3, 'FROZEN', 'Latest quality report failed and the batch is frozen for review.', 'Quality Manager', '2026-03-18 14:20:00', '2026-03-18 14:20:00'),
(7, 4, 'DRAFT', 'Batch created for grain delivery.', 'Enterprise Admin', '2026-03-05 07:30:00', '2026-03-05 07:30:00'),
(8, 4, 'PUBLISHED', 'Published for outbound and scan demo.', 'Enterprise Admin', '2026-03-10 11:00:00', '2026-03-10 11:00:00'),
(9, 4, 'RECALLED', 'A logistics contamination risk was confirmed, so the batch has been recalled.', 'Risk Manager', '2026-03-21 09:40:00', '2026-03-21 09:40:00');

INSERT INTO trace_event (
  id, batch_id, company_id, stage, title, event_time, operator_name, location, is_public, content_json, attachments_json, created_at
) VALUES
(1, 1, 1, 'ARCHIVE', 'Batch filed in system', '2026-03-03 08:10:00', 'Liu Jianguo', 'Xinfeng Orchard Office', TRUE,
 '{"summary":"Enterprise, product and batch basics were confirmed for this lot."}', '["/images/products/orange-batch.svg"]', '2026-03-03 08:10:00'),
(2, 1, 1, 'PRODUCE', 'Harvest completed', '2026-03-04 09:20:00', 'Field Recorder', 'Block A Orchard', TRUE,
 '{"summary":"Harvest and selection were completed, and the operators were confirmed."}', '["/images/products/orange-batch.svg"]', '2026-03-04 09:20:00'),
(3, 1, 1, 'TRANSPORT', 'Cold-chain delivery started', '2026-03-06 15:30:00', 'Logistics Lead', 'Ganzhou Dispatch Point', TRUE,
 '{"summary":"The batch entered cold-chain transport and the handover was recorded."}', '["/images/products/orange-batch.svg"]', '2026-03-06 15:30:00'),
(4, 1, 1, 'MARKET', 'Retail shelf ready', '2026-03-08 09:15:00', 'Store Guide', 'Nanchang Demo Store', TRUE,
 '{"summary":"The batch was put on the shelf and is ready for consumer scan demo."}', '["/images/products/orange-batch.svg"]', '2026-03-08 09:15:00'),

(5, 2, 1, 'ARCHIVE', 'Draft batch filed', '2026-03-20 09:10:00', 'Enterprise Admin', 'Xinfeng Orchard Office', TRUE,
 '{"summary":"The draft batch has been created and is waiting for more records."}', '["/images/products/orange-batch.svg"]', '2026-03-20 09:10:00'),
(6, 2, 1, 'PRODUCE', 'Field record added', '2026-03-20 10:30:00', 'Field Recorder', 'Block C Orchard', TRUE,
 '{"summary":"A first production record was added, but quality and QR are still missing."}', '["/images/products/orange-batch.svg"]', '2026-03-20 10:30:00'),

(7, 3, 2, 'ARCHIVE', 'Tea batch filed', '2026-03-09 08:45:00', 'Zhang Min', 'Wuyuan Tea Office', TRUE,
 '{"summary":"The tea batch was registered with enterprise and product information."}', '["/images/products/green-tea-batch.svg"]', '2026-03-09 08:45:00'),
(8, 3, 2, 'QUALITY', 'Second quality review recorded', '2026-03-18 13:40:00', 'Quality Manager', 'Tea Quality Lab', TRUE,
 '{"summary":"The latest quality review found an issue, and the batch moved into handling."}', '["/images/products/green-tea-batch.svg"]', '2026-03-18 13:40:00'),

(9, 4, 3, 'ARCHIVE', 'Rice batch filed', '2026-03-05 07:45:00', 'Chen Hao', 'Nanchang Grain Center', TRUE,
 '{"summary":"Batch basics were registered before outbound delivery."}', '["/images/products/rice-batch.svg"]', '2026-03-05 07:45:00'),
(10, 4, 3, 'DELIVERY', 'Outbound delivery recorded', '2026-03-10 09:30:00', 'Outbound Manager', 'Nanchang Dispatch Dock', TRUE,
 '{"summary":"The rice batch left the warehouse and entered channel delivery."}', '["/images/products/rice-batch.svg"]', '2026-03-10 09:30:00'),
(11, 4, 3, 'REGULATION', 'Recall notice recorded', '2026-03-21 10:10:00', 'Risk Manager', 'Joint review room', TRUE,
 '{"summary":"A recall notice was recorded and the public page should highlight the current risk."}', '["/images/products/rice-batch.svg"]', '2026-03-21 10:10:00');

INSERT INTO quality_report (id, batch_id, report_no, agency, result, report_json, created_at) VALUES
(1, 1, 'QA-20260308-001', 'Jiangxi Quality Center', 'PASS',
 '{"highlights":["Pesticide residue passed","Microbiology passed","Appearance normal"]}', '2026-03-08 08:40:00'),
(2, 3, 'QA-20260318-002', 'Wuyuan Tea Quality Lab', 'FAIL',
 '{"highlights":["Moisture exceeded the internal threshold","Batch must stay frozen until review is finished"]}', '2026-03-18 13:20:00'),
(3, 4, 'QA-20260310-003', 'Nanchang Grain Lab', 'PASS',
 '{"highlights":["Initial warehouse quality passed","Recall reason came from logistics risk, not storage quality"]}', '2026-03-10 08:50:00');

INSERT INTO qr_code (id, batch_id, qr_token, status, created_at, remark, status_reason, last_query_at, pv) VALUES
(1, 1, 'demo-normal-2026', 'ACTIVE', '2026-03-08 09:00:00', 'Healthy demo QR', NULL, '2026-03-24 10:15:00', 6),
(2, 3, 'demo-frozen-2026', 'SUSPENDED', '2026-03-12 08:50:00', 'Frozen demo QR', 'The batch is frozen during handling.', '2026-03-24 11:00:00', 2),
(3, 4, 'demo-recall-2026', 'RECALLED', '2026-03-10 10:50:00', 'Recall demo QR', 'The batch has been recalled.', '2026-03-24 11:40:00', 4);

INSERT INTO qr_query_log (id, qr_id, batch_id, query_time, ip, ua, referer) VALUES
(1, 1, 1, '2026-03-24 10:15:00', '127.0.0.1', 'DemoBrowser/Desktop', 'http://127.0.0.1:5173'),
(2, 1, 1, '2026-03-24 10:25:00', '127.0.0.2', 'DemoBrowser/Mobile', 'http://127.0.0.1:5173'),
(3, 2, 3, '2026-03-24 11:00:00', '127.0.0.3', 'DemoBrowser/Desktop', 'http://127.0.0.1:5173'),
(4, 3, 4, '2026-03-24 11:40:00', '127.0.0.4', 'DemoBrowser/Mobile', 'http://127.0.0.1:5173');

INSERT INTO batch_risk_action (id, batch_id, action_type, reason, comment, operator_name, created_at) VALUES
(1, 3, 'COMMENT', 'Quality review failed and shipment is paused.', 'The batch was frozen immediately after the failed review.', 'Quality Manager', '2026-03-18 14:30:00'),
(2, 3, 'RECTIFICATION', 'Tea leaves were re-sorted and storage conditions were adjusted.', 'Rectification evidence has been collected for review.', 'Tea Supervisor', '2026-03-18 17:00:00'),
(3, 3, 'PROCESSING', 'Rectification is still under verification.', 'The enterprise is waiting for a second review result.', 'Quality Manager', '2026-03-19 09:30:00'),
(4, 4, 'COMMENT', 'A logistics contamination risk was confirmed during delivery review.', 'A public recall notice was issued immediately.', 'Risk Manager', '2026-03-21 09:45:00'),
(5, 4, 'RECTIFICATION', 'Affected inventory was isolated and downstream partners were notified.', 'Return collection and inventory reconciliation were completed.', 'Operations Lead', '2026-03-21 14:00:00'),
(6, 4, 'PROCESSING', 'Recall handling is still in progress.', 'The enterprise is collecting channel feedback.', 'Risk Manager', '2026-03-22 10:00:00'),
(7, 4, 'RECTIFIED', 'The enterprise completed the corrective actions and filed the report.', 'The recall remains visible to the public for audit and presentation explanation.', 'Risk Manager', '2026-03-23 16:20:00');
