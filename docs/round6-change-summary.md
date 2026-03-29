# Round 6 Change Summary

## Scope

This round focused on three areas only:

1. Master data lifecycle management for companies and products
2. Attachment governance finishing work
3. Risk handling closure for frozen and recalled batches

The existing batch-centered workflow, admin workbench, and public trace page were kept in place and only minimally adjusted.

## Master Data Lifecycle Management

Files updated:

- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/MasterDataStatus.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/MasterDataService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/MasterDataServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/controller/CompanyController.java`
- `backend/src/main/java/edu/jxust/agritrace/controller/ProductController.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/CompanyAdminVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ProductAdminVO.java`
- `admin-web/src/api/master-data.js`
- `admin-web/src/pages/CompanyManageView.vue`
- `admin-web/src/pages/ProductManageView.vue`

What changed:

- Added `ARCHIVED` into the master data lifecycle.
- Company and product list APIs now support lifecycle-oriented status filtering.
- Company delete now checks both product and batch references before removal.
- Product delete now checks batch references before removal.
- Company and product admin responses now include reference counts and `canDelete`.
- Admin pages now expose archive/delete actions and show clearer lifecycle state.

## Attachment Governance Finishing

Files updated:

- `backend/src/main/java/edu/jxust/agritrace/config/TraceProperties.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/AttachmentStorageService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/AttachmentGovernanceService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/AttachmentCleanupResultVO.java`
- `backend/src/main/java/edu/jxust/agritrace/controller/BatchController.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/resources/application.yml`
- `admin-web/src/api/batch.js`
- `admin-web/src/pages/BatchWorkbenchView.vue`

What changed:

- File type whitelist and size limits are now configuration driven.
- Trace images and quality attachments use separate allowlists and limits.
- Upload errors now stay within the unified API response structure.
- Added an independent orphan attachment cleanup service and API entry.
- Cleanup returns cleaned count, cleaned IDs, failed count, and failed IDs.
- Admin workbench now shows pending bind vs bound upload state and exposes a manual cleanup action.

## Risk Handling Closure

Files updated:

- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/RiskActionType.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/BatchRiskActionEntity.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/BatchRiskActionCreateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/BatchRiskActionMapper.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/BatchRiskActionPO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/BatchService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/BatchServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/BatchRiskResolver.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchRiskActionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/RiskHandlingSectionVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchWorkbenchVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/service/impl/PublicTraceServiceImpl.java`
- `trace-web/src/views/TraceDetailView.vue`
- `admin-web/src/pages/BatchWorkbenchView.vue`
- `sql/10_round6_lifecycle_and_risk.sql`
- `docker/init/08_round6_lifecycle_and_risk.sql`

What changed:

- Added lightweight risk action records for comments, rectification, processing, and rectified states.
- Added workbench-side risk handling aggregation with current stage, resume readiness, and history.
- Resume from `FROZEN` now requires handling context plus a rectified marker.
- Public trace risk output now cleanly maps to `FROZEN`, `PROCESSING`, `RECTIFIED`, `RECALLED`, and `NORMAL`.
- Admin batch workbench now has a dedicated risk handling section and action dialog.
- Public page now renders pending risk states with a calmer banner style.

## Test Updates

Files updated:

- `backend/src/test/java/edu/jxust/agritrace/module/batch/BatchControllerIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/batch/BatchServicePersistenceIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/batch/MasterDataControllerIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/publictrace/PublicTraceControllerIntegrationTest.java`
- `backend/src/test/resources/schema.sql`
- `backend/src/test/resources/data.sql`

Coverage added or refreshed:

- Company and product archive/delete validation
- Referenced company/product delete rejection
- File whitelist rejection
- File size rejection
- Empty file rejection
- Orphan attachment cleanup
- Risk handling closure from freeze to rectified to resume
- Public trace risk structure changes across risk stages

## Commands Run

- `mvn -q -DskipTests compile`
- `mvn -q test`
- `npm run build` in `admin-web`
- `npm run build` in `trace-web`
- Spring Boot startup against a temporary MySQL 8 instance on `127.0.0.1:3307`
- Real API validation through `http://127.0.0.1:18080`

## Verification Results

Passed:

- Backend compile
- Backend test suite
- Backend startup on real MySQL
- Company list/create/archive/delete
- Product list/create/archive/delete/by-company filter
- Referenced company delete rejection
- Referenced product delete rejection
- Batch creation with explicit company/product selection
- Trace image upload and binding
- Quality attachment upload and binding
- Illegal file type rejection
- Oversized file rejection
- Orphan attachment cleanup
- QR generation
- Freeze -> comment -> processing -> rectification -> rectified -> resume
- Public trace risk state changes
- Workbench scan stats reading after public access
- `admin-web` production build
- `trace-web` production build

Partially resolved:

- The historical root SQL file `sql/01_schema.sql` still contains legacy malformed statements. Real validation was completed with a minimal current-workflow schema on a temporary MySQL instance instead of relying on that old file.

## Remaining Issues

- Batch action labels and some legacy strings inside backend batch responses still contain historical encoded text and should be normalized in a later cleanup round.
- Attachment cleanup currently supports manual trigger and service reuse, but no scheduled trigger has been wired yet.
- Risk handling remains intentionally lightweight and does not include a regulator workflow or approval chain.

## Recommended Next Round

The next best round is:

1. Normalize remaining legacy response strings and demo seed data
2. Add a scheduled trigger for orphan attachment cleanup
3. Strengthen batch-level abnormal closure with a minimal recovery checklist and clearer action guidance
