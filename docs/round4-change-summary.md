# Round4 Change Summary

## Scope

This round stayed on the existing batch-centric flow and only implemented:

- explicit company and product selection for batch create and edit
- real local-file upload for trace images and quality attachments
- scan statistics aggregation for the batch workbench

README, page visual direction, and new heavy modules were intentionally left unchanged.

## Enterprise / Product / Batch Relation Changes

Updated backend files:

- `backend/src/main/java/edu/jxust/agritrace/controller/BatchController.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/BatchService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/BatchServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/BatchCreateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/BatchUpdateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/ProductEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/BaseProductPO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/CompanyOptionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ProductOptionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ProductSummaryVO.java`
- `sql/06_round4_attachment_and_relations.sql`
- `sql/07_round4_seed_dev.sql`
- `docker/init/04_round4_attachment_and_relations.sql`
- `docker/init/05_round4_seed_dev.sql`

What changed:

- batch create and update now require explicit `companyId` and `productId`
- backend no longer relies on auto-create fallback for company or product in the main batch flow
- added lookup APIs for company and product options
- product selection can be filtered by company
- backend validates that the selected company exists, the selected product exists, and the product belongs to the selected company
- batch workbench keeps returning real company and product summaries for the current batch

## File Upload Changes

Updated backend files:

- `backend/src/main/java/edu/jxust/agritrace/config/TraceProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/edu/jxust/agritrace/controller/PublicFileController.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/AttachmentBusinessType.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/FileAssetEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/BizAttachmentMapper.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/BizAttachmentPO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/AttachmentStorageService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/TraceLinkBuilder.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/TraceRecordCreateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/QualityReportCreateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/TraceRecordEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/QualityReportEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/QualityReportPO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/FileAssetVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/TraceRecordVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/QualityReportVO.java`

What changed:

- added real multipart upload endpoint for `trace-image` and `quality-attachment`
- files are stored on local disk with a stable backend access URL
- uploaded files are recorded in `biz_attachment`
- trace records can bind uploaded image attachments
- quality reports can bind uploaded attachments and expose them in workbench data
- old plain-text URL behavior remains compatible through existing `imageUrl` fields

## Scan Statistics Workbench Changes

Updated backend files:

- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/BatchEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/ScanRecordEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchWorkbenchVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ScanStatsSectionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ScanRecordVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ScanTrendPointVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/BatchServiceImpl.java`

What changed:

- batch workbench now returns `scanStats`
- scan summary includes total PV, total UV, last scan time, recent scan records, and a seven-day trend
- recent scan logs are aggregated from the existing QR query logs and exposed in a workbench-friendly shape

## Admin Web Integration Changes

Updated frontend files:

- `admin-web/src/api/batch.js`
- `admin-web/src/pages/BatchListView.vue`
- `admin-web/src/pages/BatchWorkbenchView.vue`

What changed:

- batch create and edit use real company and product selection
- trace record and quality dialogs upload real files instead of depending only on plain URL input
- workbench now shows scan statistics, recent scan records, trace attachments, and quality attachments
- UI direction was preserved; only minimal integration fixes were made

## Tests Added

- `backend/src/test/java/edu/jxust/agritrace/module/batch/BatchServicePersistenceIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/batch/BatchControllerIntegrationTest.java`
- `backend/src/test/resources/schema.sql`
- `backend/src/test/resources/data.sql`
- `backend/src/test/resources/application.yml`

Coverage added in this round:

- batch create validation for missing or invalid company and product selection
- attachment upload and binding for trace images
- attachment upload and binding for quality attachments
- scan statistics aggregation for PV, UV, and seven-day trend
- workbench aggregation coverage for company, product, QR, quality, and scan stats

## Commands Run

- `mvn -q -DskipTests compile`
- `mvn -q test`
- `npm run build` in `admin-web`
- `npm run build` in `trace-web`
- started a temporary MySQL 8 instance on `127.0.0.1:3307`
- imported:
  - `sql/01_schema.sql`
  - `sql/04_round3_batch_persistence.sql`
  - `sql/05_round3_seed_dev.sql`
  - `sql/06_round4_attachment_and_relations.sql`
  - `sql/07_round4_seed_dev.sql`
- started backend with Spring Boot against the temporary MySQL instance
- verified real API chain:
  - company lookup
  - product lookup
  - batch creation with explicit company and product
  - trace image upload
  - quality attachment upload
  - trace record creation with attachment
  - quality report creation with attachment
  - QR generation
  - public trace access
  - workbench scan stats refresh

## Verification Results

Passed:

- backend compile
- backend tests
- backend startup with real MySQL
- admin-web production build
- trace-web production build
- real file upload and file access
- real QR image access
- scan log aggregation back into the workbench

Observed real chain result:

- created batch `ROUND4-CHAIN-20260324-170840`
- generated QR token `round4-chain-20260324-170840`
- verified trace image and quality attachment both returned `200`
- verified QR image returned `200`
- accessed public trace three times with two user agents
- workbench showed `PV=3`, `UV=2`, and the current-day trend aggregated correctly

## Issues Still Not Solved

- attachment storage is still local disk storage; production deployment should move to object storage or a managed static file service
- console output on this machine still shows some mojibake for existing Chinese text, but API structure and runtime behavior are correct
- scan statistics are intentionally lightweight and do not include geographic analysis or advanced dashboards

## Recommended Next Round

Best next step:

- add real enterprise and product management editing flow on top of the current selection model
- add real file upload size/type validation and cleanup strategy for unused uploads
- strengthen public trace risk presentation and batch anomaly operations around the now-persisted data
