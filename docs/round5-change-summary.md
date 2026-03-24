# Round5 Change Summary

## Scope

This round only delivered:

- minimum company management
- minimum product management
- upload governance for trace images and quality attachments
- stronger risk-state linkage across workbench and public trace

README, overall visual direction, migration tooling, and new heavy modules were intentionally left unchanged.

## Company Management Changes

Backend files:

- `backend/src/main/java/edu/jxust/agritrace/controller/CompanyController.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/CompanyListQueryRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/CompanySaveRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/StatusUpdateRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/entity/MasterDataStatus.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/MasterDataService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/MasterDataServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/OrgCompanyPO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/CompanyAdminVO.java`
- `sql/08_round5_master_data.sql`
- `sql/09_round5_seed_dev.sql`
- `docker/init/06_round5_master_data.sql`
- `docker/init/07_round5_seed_dev.sql`

What changed:

- added company list, detail, create, update, and status toggle APIs
- company management now supports `name`, `licenseNo`, `contactPerson`, `contactPhone`, `address`, and `status`
- batch company lookup now aligns with management data and only returns enabled companies

Admin-web files:

- `admin-web/src/api/master-data.js`
- `admin-web/src/pages/CompanyManageView.vue`
- `admin-web/src/router/index.js`
- `admin-web/src/pages/DashboardView.vue`
- `admin-web/src/pages/BatchListView.vue`

What changed:

- added a minimum company maintenance page
- added maintenance entry links from dashboard and batch list
- company data can now be created, edited, and enabled or disabled in the admin site

## Product Management Changes

Backend files:

- `backend/src/main/java/edu/jxust/agritrace/controller/ProductController.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/ProductListQueryRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/dto/ProductSaveRequest.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/mapper/po/BaseProductPO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/ProductAdminVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/MasterDataServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/BatchServiceImpl.java`

What changed:

- added product list, detail, create, update, filter-by-company, and status toggle APIs
- product master data now maintains `companyId`, `productName`, `productCode`, `category`, `originPlace`, `coverImage`, `specification`, `unit`, and `status`
- batch create and update now reject disabled company or product selections
- product ownership is validated before batch creation

Admin-web files:

- `admin-web/src/pages/ProductManageView.vue`
- `admin-web/src/api/master-data.js`
- `admin-web/src/router/index.js`

What changed:

- added a minimum product maintenance page
- added company filter and create/edit flow
- batch selection is no longer dependent only on SQL seed rows

## Upload Governance Changes

Backend files:

- `backend/src/main/java/edu/jxust/agritrace/config/TraceProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/edu/jxust/agritrace/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/AttachmentStorageService.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/BatchServiceImpl.java`

What changed:

- trace images now only allow common image formats
- quality attachments now only allow PDF and common image formats
- trace image size is limited to 5 MB
- quality attachment size is limited to 10 MB
- upload errors now return clear messages for unsupported type and file-too-large cases
- orphan attachments are cleaned when a new upload starts if they remain unbound past the configured retention window

Admin-web files:

- `admin-web/src/pages/BatchListView.vue`
- `admin-web/src/pages/BatchWorkbenchView.vue`

What changed:

- upload inputs now guide users toward supported file types
- upload remains the primary path; URL fallback is still kept for compatibility where it already existed
- upload failures surface backend error messages directly

## Risk-State Linkage Changes

Backend files:

- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/support/BatchRiskResolver.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchRiskSummaryVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/vo/BatchWorkbenchVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/vo/PublicRiskVO.java`
- `backend/src/main/java/edu/jxust/agritrace/module/publictrace/service/impl/PublicTraceServiceImpl.java`
- `backend/src/main/java/edu/jxust/agritrace/module/batch/service/impl/BatchServiceImpl.java`

What changed:

- workbench and public trace now use the same risk mapping logic
- risk output now includes `status`, `riskLevel`, `reason`, `updatedAt`, and `tip`
- frozen and recalled states are now explicit in both workbench and public trace data
- quality-fail batches are mapped to a pending risk state on the backend

Frontend files:

- `admin-web/src/pages/BatchWorkbenchView.vue`
- `trace-web/src/views/TraceDetailView.vue`

What changed:

- batch workbench now shows a dedicated risk section when the batch is abnormal or pending
- public trace page now shows a stronger first-screen abnormal banner with status, reason, update time, and a short action hint
- normal public pages are left clean and are not blocked by abnormal UI when no risk exists

## Tests Added Or Updated

- `backend/src/test/java/edu/jxust/agritrace/module/batch/MasterDataControllerIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/batch/BatchControllerIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/batch/BatchServicePersistenceIntegrationTest.java`
- `backend/src/test/java/edu/jxust/agritrace/module/publictrace/PublicTraceControllerIntegrationTest.java`
- `backend/src/test/resources/schema.sql`
- `backend/src/test/resources/data.sql`

Coverage added this round:

- company create, update, and status toggle
- product create, update, and company ownership validation
- valid upload passes
- invalid type upload is rejected
- oversized upload is rejected
- frozen public trace risk structure
- recalled workbench risk structure
- managed company -> managed product -> batch creation chain

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
  - `sql/08_round5_master_data.sql`
  - `sql/09_round5_seed_dev.sql`
- started backend with Spring Boot against the temporary MySQL instance
- verified:
  - company list / create / update
  - product list / create / update / filter-by-company
  - batch creation with explicit company and product
  - valid file upload
  - invalid file type rejection
  - oversized upload rejection
  - QR generation
  - public trace abnormal risk output
  - workbench risk and scan stats refresh

## Verification Results

Passed:

- backend compile
- backend tests
- backend startup with real MySQL
- admin-web build
- trace-web build
- company maintenance APIs
- product maintenance APIs
- batch creation with real company/product master data
- valid trace image upload
- valid quality attachment upload
- invalid trace upload rejection
- oversized trace upload rejection
- frozen workbench risk output
- recalled workbench risk output
- frozen public trace risk output
- recalled public trace risk output

Observed real linked result:

- created company `Round5 Live Company Updated`
- created product `Round5 Live Orange Premium`
- created batch `ROUND5-LIVE-20260324-191533`
- valid trace and quality files both returned stable file URLs
- invalid trace upload returned `trace image only supports PNG, JPG, JPEG, WEBP or GIF`
- oversized trace upload returned `trace image exceeds the 5 MB limit`
- workbench showed `FROZEN` then `RECALLED`
- public trace risk output matched backend workbench risk output
- after public access, workbench scan stats refreshed to `PV=2` and `UV=2`

## Issues Still Not Solved

- company and product management is still intentionally minimal; there is no delete flow
- orphan attachment cleanup currently runs opportunistically during upload rather than by scheduler
- product cover image still uses URL input instead of a dedicated image-upload flow
- existing historical text encoding in some seeded Chinese content is still visible in terminal output, although API behavior is correct

## Recommended Next Round

Best next step:

- add minimum delete/archive strategy for company and product master data
- improve risk follow-up handling with enterprise correction records and regulator notes
- add file lifecycle cleanup visibility so admins can see which uploads were cleaned or are still unbound
