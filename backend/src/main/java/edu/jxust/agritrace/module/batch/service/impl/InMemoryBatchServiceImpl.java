package edu.jxust.agritrace.module.batch.service.impl;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.CompanyEntity;
import edu.jxust.agritrace.module.batch.entity.ProductEntity;
import edu.jxust.agritrace.module.batch.entity.QrCodeEntity;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.StatusHistoryEntity;
import edu.jxust.agritrace.module.batch.entity.TraceRecordEntity;
import edu.jxust.agritrace.module.batch.entity.TraceStage;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.BatchActionVO;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchOverviewVO;
import edu.jxust.agritrace.module.batch.vo.BatchStatusLogVO;
import edu.jxust.agritrace.module.batch.vo.BatchStatusSummaryVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import edu.jxust.agritrace.module.batch.vo.CompanySummaryVO;
import edu.jxust.agritrace.module.batch.vo.ProductSummaryVO;
import edu.jxust.agritrace.module.batch.vo.QrSummaryVO;
import edu.jxust.agritrace.module.batch.vo.QualityReportVO;
import edu.jxust.agritrace.module.batch.vo.QualitySectionVO;
import edu.jxust.agritrace.module.batch.vo.TraceRecordVO;
import edu.jxust.agritrace.module.batch.vo.TraceSectionVO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryBatchServiceImpl implements BatchService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Map<Long, BatchEntity> batchStore = new LinkedHashMap<>();
    private final AtomicLong batchIdGenerator = new AtomicLong(100);
    private final AtomicLong companyIdGenerator = new AtomicLong(20);
    private final AtomicLong productIdGenerator = new AtomicLong(20);
    private final AtomicLong traceIdGenerator = new AtomicLong(1000);
    private final AtomicLong qualityIdGenerator = new AtomicLong(2000);
    private final AtomicLong qrIdGenerator = new AtomicLong(3000);

    @PostConstruct
    public void initDemoData() {
        CompanyEntity producer = new CompanyEntity(
                1L,
                "赣南脐橙示范基地",
                "LIC-DEMO-001",
                "刘建国",
                "13800000001",
                "江西省赣州市信丰县高标准果园"
        );
        ProductEntity orange = new ProductEntity(1L, "赣南脐橙", "水果", "10kg/箱", "箱", "/images/products/orange-batch.svg");
        ProductEntity tea = new ProductEntity(2L, "江西绿茶", "茶叶", "250g/盒", "盒", "/images/products/green-tea-batch.svg");
        ProductEntity rice = new ProductEntity(3L, "富硒大米", "粮食", "5kg/袋", "袋", "/images/products/rice-batch.svg");

        BatchEntity published = buildBaseBatch(1L, "BATCH20260311001", orange, producer, "江西省赣州市信丰县高标准果园", LocalDate.of(2026, 3, 1));
        published.setStatus(BatchStatus.PUBLISHED);
        published.setCurrentNode("已发布，可直接对外扫码查看");
        published.setPublicRemark("该批次已经完成建档、过程记录、质检与二维码发布，适合作为课堂演示和扫码样例。");
        published.setInternalRemark("优先展示批次工作台、质检摘要与二维码预览。");
        published.setPublishedAt(LocalDateTime.of(2026, 3, 11, 11, 10));
        published.getTraceRecords().addAll(List.of(
                new TraceRecordEntity(1L, TraceStage.ARCHIVE, "完成企业建档", LocalDateTime.of(2026, 3, 1, 8, 0), "刘建国", "信丰果园", true, "已录入企业主体、产地和批次基础信息。", "/images/products/orange-batch.svg"),
                new TraceRecordEntity(2L, TraceStage.PRODUCE, "春季田间管理", LocalDateTime.of(2026, 3, 5, 9, 30), "王小林", "信丰果园", true, "记录修枝、灌溉和巡园情况，方便后续扫码查看。", "/images/products/orange-batch.svg"),
                new TraceRecordEntity(3L, TraceStage.WAREHOUSE, "入库待发", LocalDateTime.of(2026, 3, 10, 10, 20), "周敏", "南昌仓配中心", true, "完成分级包装并进入仓储待发状态。", "/images/products/orange-batch.svg"),
                new TraceRecordEntity(4L, TraceStage.MARKET, "门店上架", LocalDateTime.of(2026, 3, 11, 9, 0), "门店导购", "南昌示范门店", true, "已具备对消费者展示的完整追溯信息。", "/images/products/orange-batch.svg")
        ));
        published.getQualityReports().add(new QualityReportEntity(
                1L,
                "QA-20260311-001",
                "江西省农产品质量检测中心",
                "PASS",
                LocalDateTime.of(2026, 3, 11, 9, 40),
                List.of("农残未检出", "微生物合格", "外观检验正常")
        ));
        published.setQrCode(new QrCodeEntity(
                1L,
                "test-token-2026",
                "ACTIVE",
                "http://127.0.0.1:5173/t/test-token-2026",
                LocalDateTime.of(2026, 3, 11, 11, 10),
                LocalDateTime.of(2026, 3, 24, 10, 15)
        ));
        published.getStatusHistory().addAll(List.of(
                new StatusHistoryEntity(BatchStatus.DRAFT, "完成批次草稿创建与企业建档", "刘建国", LocalDateTime.of(2026, 3, 1, 8, 0)),
                new StatusHistoryEntity(BatchStatus.PUBLISHED, "质检与二维码准备完成，允许消费者扫码查看", "刘建国", LocalDateTime.of(2026, 3, 11, 11, 10))
        ));

        BatchEntity draft = buildBaseBatch(2L, "BATCH20260312001", tea, producer, "江西省上饶市婺源县生态茶园", LocalDate.of(2026, 3, 5));
        draft.setStatus(BatchStatus.DRAFT);
        draft.setCurrentNode("待补录质检与二维码");
        draft.setPublicRemark("该批次尚未发布，当前不对外开放扫码查询。");
        draft.setInternalRemark("建议先补录一条仓储或出库记录，再上传质检并生成二维码。");
        draft.getTraceRecords().add(new TraceRecordEntity(
                5L, TraceStage.PRODUCE, "春茶采摘完成", LocalDateTime.of(2026, 3, 12, 9, 0), "陈红", "婺源茶园", false, "已完成采摘，等待包装与检测录入。", "/images/products/green-tea-batch.svg"
        ));
        draft.getStatusHistory().add(new StatusHistoryEntity(BatchStatus.DRAFT, "创建草稿批次，等待继续补录", "陈红", LocalDateTime.of(2026, 3, 12, 9, 0)));

        BatchEntity frozen = buildBaseBatch(3L, "BATCH20260312002", rice, producer, "江西省宜春市富硒稻米基地", LocalDate.of(2026, 3, 2));
        frozen.setStatus(BatchStatus.FROZEN);
        frozen.setStatusReason("监管抽查发现仓储温湿度记录缺失，先冻结对外展示。");
        frozen.setCurrentNode("冻结中，等待补充仓储留痕");
        frozen.setPublicRemark("当前批次处于冻结状态，请以企业通知或监管信息为准。");
        frozen.setInternalRemark("补齐仓储记录并复核通过后可恢复发布。");
        frozen.setPublishedAt(LocalDateTime.of(2026, 3, 13, 9, 20));
        frozen.setFrozenAt(LocalDateTime.of(2026, 3, 14, 14, 0));
        frozen.getTraceRecords().addAll(List.of(
                new TraceRecordEntity(6L, TraceStage.ARCHIVE, "基地建档完成", LocalDateTime.of(2026, 3, 2, 7, 40), "刘建国", "宜春富硒基地", true, "已完成批次基础信息建档。", "/images/products/rice-batch.svg"),
                new TraceRecordEntity(7L, TraceStage.WAREHOUSE, "入库复核待补录", LocalDateTime.of(2026, 3, 13, 16, 0), "李仓", "宜春成品仓", true, "已入库，但仓储温湿度留痕不完整。", "/images/products/rice-batch.svg")
        ));
        frozen.getQualityReports().add(new QualityReportEntity(
                2L,
                "QA-20260314-009",
                "宜春农产品检测站",
                "PASS",
                LocalDateTime.of(2026, 3, 14, 10, 20),
                List.of("重金属合格", "外观合格")
        ));
        frozen.setQrCode(new QrCodeEntity(
                2L,
                "frozen-token-2026",
                "SUSPENDED",
                "http://127.0.0.1:5173/t/frozen-token-2026",
                LocalDateTime.of(2026, 3, 13, 9, 20),
                LocalDateTime.of(2026, 3, 20, 9, 0)
        ));
        frozen.getStatusHistory().addAll(List.of(
                new StatusHistoryEntity(BatchStatus.DRAFT, "创建批次草稿", "刘建国", LocalDateTime.of(2026, 3, 12, 11, 0)),
                new StatusHistoryEntity(BatchStatus.PUBLISHED, "已完成发布并生成二维码", "刘建国", LocalDateTime.of(2026, 3, 13, 9, 20)),
                new StatusHistoryEntity(BatchStatus.FROZEN, "监管要求先补齐仓储异常原因", "平台巡检", LocalDateTime.of(2026, 3, 14, 14, 0))
        ));

        batchStore.put(published.getId(), published);
        batchStore.put(draft.getId(), draft);
        batchStore.put(frozen.getId(), frozen);
    }

    @Override
    public List<BatchListItemVO> listBatches(BatchListQueryRequest request) {
        return batchStore.values().stream()
                .filter(batch -> matchesFilter(batch, request))
                .sorted(Comparator.comparing(BatchEntity::getId))
                .map(this::toListItem)
                .toList();
    }

    @Override
    public BatchWorkbenchVO getBatchWorkbench(Long batchId) {
        return toWorkbench(findBatch(batchId));
    }

    @Override
    public BatchWorkbenchVO createBatch(BatchCreateRequest request) {
        Long batchId = batchIdGenerator.incrementAndGet();
        ProductEntity product = new ProductEntity(
                productIdGenerator.incrementAndGet(),
                request.productName(),
                request.category(),
                "待补录",
                "待补录",
                resolveProductImage(request.productName(), request.category())
        );
        CompanyEntity company = new CompanyEntity(
                companyIdGenerator.incrementAndGet(),
                request.companyName(),
                "待补录",
                "待补录",
                "待补录",
                request.originPlace()
        );

        BatchEntity batch = buildBaseBatch(batchId, request.batchCode(), product, company, request.originPlace(), parseDate(request.productionDate()));
        batch.setStatus(BatchStatus.DRAFT);
        batch.setCurrentNode("待补录追溯记录");
        batch.setPublicRemark(defaultValue(request.publicRemark(), "先补录关键节点、质检结果和二维码，再对外发布。"));
        batch.setInternalRemark(defaultValue(request.internalRemark(), "这是一个轻量批次草稿，优先走主流程。"));
        batch.getStatusHistory().add(new StatusHistoryEntity(BatchStatus.DRAFT, "创建批次草稿", "企业管理员", LocalDateTime.now()));
        batchStore.put(batchId, batch);
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO updateBatch(Long batchId, BatchUpdateRequest request) {
        BatchEntity batch = findBatch(batchId);
        batch.setProduct(new ProductEntity(
                batch.getProduct().id(),
                request.productName(),
                request.category(),
                batch.getProduct().specification(),
                batch.getProduct().unit(),
                resolveProductImage(request.productName(), request.category())
        ));
        batch.setCompany(new CompanyEntity(
                batch.getCompany().id(),
                request.companyName(),
                batch.getCompany().licenseNo(),
                batch.getCompany().contactName(),
                batch.getCompany().contactPhone(),
                batch.getCompany().address()
        ));
        batch.setOriginPlace(request.originPlace());
        batch.setProductionDate(parseDate(request.productionDate()));
        batch.setPublicRemark(defaultValue(request.publicRemark(), batch.getPublicRemark()));
        batch.setInternalRemark(defaultValue(request.internalRemark(), batch.getInternalRemark()));
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO changeStatus(Long batchId, BatchStatusActionRequest request) {
        BatchEntity batch = findBatch(batchId);
        BatchStatus currentStatus = batch.getStatus();
        BatchStatus targetStatus = request.targetStatus();
        validateStatusTransition(batch, currentStatus, targetStatus);

        batch.setStatus(targetStatus);
        batch.setStatusReason(request.reason());
        batch.getStatusHistory().add(new StatusHistoryEntity(targetStatus, request.reason(), request.operatorName(), LocalDateTime.now()));

        if (targetStatus == BatchStatus.PUBLISHED) {
            batch.setPublishedAt(LocalDateTime.now());
            batch.setCurrentNode("已发布，可直接对外扫码查看");
        } else if (targetStatus == BatchStatus.FROZEN) {
            batch.setFrozenAt(LocalDateTime.now());
            batch.setCurrentNode("已冻结，首屏需要展示异常横幅");
        } else if (targetStatus == BatchStatus.RECALLED) {
            batch.setRecalledAt(LocalDateTime.now());
            batch.setCurrentNode("已召回，公开页必须提示停止购买");
        }

        if (batch.getQrCode() != null) {
            if (targetStatus == BatchStatus.FROZEN) {
                batch.setQrCode(new QrCodeEntity(
                        batch.getQrCode().id(),
                        batch.getQrCode().token(),
                        "SUSPENDED",
                        batch.getQrCode().publicUrl(),
                        batch.getQrCode().generatedAt(),
                        batch.getQrCode().lastScanAt()
                ));
            } else if (targetStatus == BatchStatus.PUBLISHED) {
                batch.setQrCode(new QrCodeEntity(
                        batch.getQrCode().id(),
                        batch.getQrCode().token(),
                        "ACTIVE",
                        batch.getQrCode().publicUrl(),
                        batch.getQrCode().generatedAt(),
                        batch.getQrCode().lastScanAt()
                ));
            } else if (targetStatus == BatchStatus.RECALLED) {
                batch.setQrCode(new QrCodeEntity(
                        batch.getQrCode().id(),
                        batch.getQrCode().token(),
                        "RECALLED",
                        batch.getQrCode().publicUrl(),
                        batch.getQrCode().generatedAt(),
                        batch.getQrCode().lastScanAt()
                ));
            }
        }

        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO addTraceRecord(Long batchId, TraceRecordCreateRequest request) {
        BatchEntity batch = findBatch(batchId);
        TraceStage stage = request.stage();
        LocalDateTime eventTime = request.eventTime() == null || request.eventTime().isBlank()
                ? LocalDateTime.now()
                : LocalDateTime.parse(request.eventTime());
        String title = request.title() == null || request.title().isBlank()
                ? stage.label() + "补录"
                : request.title();

        batch.getTraceRecords().add(new TraceRecordEntity(
                traceIdGenerator.incrementAndGet(),
                stage,
                title,
                eventTime,
                request.operatorName(),
                request.location(),
                request.visibleToConsumer(),
                request.summary(),
                request.imageUrl()
        ));
        batch.setCurrentNode(stage.label());
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO addQualityReport(Long batchId, QualityReportCreateRequest request) {
        BatchEntity batch = findBatch(batchId);
        LocalDateTime reportTime = request.reportTime() == null || request.reportTime().isBlank()
                ? LocalDateTime.now()
                : LocalDateTime.parse(request.reportTime());
        batch.getQualityReports().add(new QualityReportEntity(
                qualityIdGenerator.incrementAndGet(),
                request.reportNo(),
                request.agency(),
                request.result(),
                reportTime,
                request.highlights() == null ? List.of() : request.highlights()
        ));
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO generateQr(Long batchId) {
        BatchEntity batch = findBatch(batchId);
        if (batch.getQrCode() != null) {
            return toWorkbench(batch);
        }

        String token = batch.getBatchCode().toLowerCase(Locale.ROOT);
        batch.setQrCode(new QrCodeEntity(
                qrIdGenerator.incrementAndGet(),
                token,
                batch.getStatus() == BatchStatus.FROZEN ? "SUSPENDED" : "ACTIVE",
                "http://127.0.0.1:5173/t/" + token,
                LocalDateTime.now(),
                null
        ));
        if (batch.getStatus() == BatchStatus.DRAFT) {
            batch.setCurrentNode("二维码已生成，待发布");
        }
        return toWorkbench(batch);
    }

    @Override
    public BatchEntity getBatchEntityById(Long batchId) {
        return findBatch(batchId);
    }

    @Override
    public BatchEntity getBatchEntityByToken(String token) {
        return batchStore.values().stream()
                .filter(batch -> batch.getQrCode() != null)
                .filter(batch -> batch.getQrCode().token().equals(token))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到对应追溯码"));
    }

    private BatchEntity buildBaseBatch(Long id, String batchCode, ProductEntity product, CompanyEntity company, String originPlace, LocalDate productionDate) {
        BatchEntity batch = new BatchEntity();
        batch.setId(id);
        batch.setBatchCode(batchCode);
        batch.setProduct(product);
        batch.setCompany(company);
        batch.setOriginPlace(originPlace);
        batch.setProductionDate(productionDate);
        return batch;
    }

    private BatchEntity findBatch(Long batchId) {
        BatchEntity batch = batchStore.get(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("未找到对应批次");
        }
        return batch;
    }

    private boolean matchesFilter(BatchEntity batch, BatchListQueryRequest request) {
        if (request == null) {
            return true;
        }

        if (!containsIgnoreCase(batch.getBatchCode(), request.getBatchCode())) {
            return false;
        }
        if (!containsIgnoreCase(batch.getProduct().name(), request.getProductName())) {
            return false;
        }
        if (!containsIgnoreCase(batch.getCompany().name(), request.getCompanyName())) {
            return false;
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            if (!batch.getStatus().name().equalsIgnoreCase(request.getStatus())) {
                return false;
            }
        }

        LocalDate dateFrom = parseNullableDate(request.getDateFrom());
        LocalDate dateTo = parseNullableDate(request.getDateTo());
        if (dateFrom != null && batch.getProductionDate().isBefore(dateFrom)) {
            return false;
        }
        return dateTo == null || !batch.getProductionDate().isAfter(dateTo);
    }

    private BatchListItemVO toListItem(BatchEntity batch) {
        QualityReportEntity latestQuality = latestQuality(batch);
        return new BatchListItemVO(
                batch.getId(),
                batch.getBatchCode(),
                batch.getProduct().name(),
                batch.getProduct().imageUrl(),
                batch.getCompany().name(),
                batch.getStatus().name(),
                toStatusLabel(batch.getStatus()),
                batch.getCurrentNode(),
                batch.getOriginPlace(),
                formatDate(batch.getProductionDate()),
                formatDateTime(batch.getPublishedAt()),
                batch.getQrCode() == null ? "未生成" : batch.getQrCode().status(),
                latestQuality == null ? "待上传" : toQualityLabel(latestQuality.result()),
                buildActions(batch),
                buildQuickTags(batch, latestQuality)
        );
    }

    private BatchWorkbenchVO toWorkbench(BatchEntity batch) {
        List<TraceRecordVO> sortedTraceRecords = batch.getTraceRecords().stream()
                .sorted(Comparator.comparing(TraceRecordEntity::eventTime).reversed())
                .map(this::toTraceRecordVO)
                .toList();
        List<QualityReportVO> sortedQualityReports = batch.getQualityReports().stream()
                .sorted(Comparator.comparing(QualityReportEntity::reportTime).reversed())
                .map(this::toQualityVO)
                .toList();
        List<BatchStatusLogVO> statusHistory = batch.getStatusHistory().stream()
                .sorted(Comparator.comparing(StatusHistoryEntity::operatedAt).reversed())
                .map(this::toStatusLogVO)
                .toList();
        QualityReportVO latestQuality = sortedQualityReports.isEmpty() ? null : sortedQualityReports.get(0);

        return new BatchWorkbenchVO(
                new BatchOverviewVO(
                        batch.getId(),
                        batch.getBatchCode(),
                        batch.getOriginPlace(),
                        formatDate(batch.getProductionDate()),
                        formatDateTime(batch.getPublishedAt()),
                        batch.getPublicRemark(),
                        batch.getInternalRemark(),
                        batch.getProduct().imageUrl()
                ),
                new ProductSummaryVO(
                        batch.getProduct().id(),
                        batch.getProduct().name(),
                        batch.getProduct().category(),
                        batch.getProduct().specification(),
                        batch.getProduct().unit(),
                        batch.getProduct().imageUrl()
                ),
                new CompanySummaryVO(
                        batch.getCompany().id(),
                        batch.getCompany().name(),
                        batch.getCompany().licenseNo(),
                        batch.getCompany().contactName(),
                        batch.getCompany().contactPhone(),
                        batch.getCompany().address()
                ),
                buildStatusSummary(batch, statusHistory),
                new TraceSectionVO(
                        sortedTraceRecords.size(),
                        sortedTraceRecords.isEmpty() ? null : sortedTraceRecords.get(0).eventTime(),
                        "优先用快速录入补齐关键节点，只填阶段、地点、说明和时间。",
                        sortedTraceRecords.stream().limit(6).toList()
                ),
                new QualitySectionVO(
                        latestQuality == null ? "PENDING" : latestQuality.result(),
                        latestQuality == null ? "待上传" : latestQuality.resultLabel(),
                        latestQuality == null ? "当前尚未上传质检摘要，发布前应先补齐检测结论。" : buildQualitySummary(latestQuality),
                        sortedQualityReports.size(),
                        latestQuality,
                        sortedQualityReports
                ),
                toQrSummary(batch.getQrCode()),
                statusHistory,
                buildActions(batch)
        );
    }

    private BatchStatusSummaryVO buildStatusSummary(BatchEntity batch, List<BatchStatusLogVO> statusHistory) {
        BatchStatusLogVO latestStatusLog = statusHistory.isEmpty() ? null : statusHistory.get(0);
        return new BatchStatusSummaryVO(
                batch.getStatus().name(),
                toStatusLabel(batch.getStatus()),
                batch.getCurrentNode(),
                defaultValue(batch.getStatusReason(), latestStatusLog == null ? "" : latestStatusLog.reason()),
                latestStatusLog == null ? null : latestStatusLog.operatorName(),
                latestStatusLog == null ? null : latestStatusLog.operatedAt()
        );
    }

    private QrSummaryVO toQrSummary(QrCodeEntity qrCode) {
        if (qrCode == null) {
            return new QrSummaryVO(null, null, "未生成", null, null, null, false);
        }
        return new QrSummaryVO(
                qrCode.id(),
                qrCode.token(),
                qrCode.status(),
                qrCode.publicUrl(),
                formatDateTime(qrCode.generatedAt()),
                formatDateTime(qrCode.lastScanAt()),
                true
        );
    }

    private TraceRecordVO toTraceRecordVO(TraceRecordEntity record) {
        return new TraceRecordVO(
                record.id(),
                record.stage().name(),
                record.stage().label(),
                record.title(),
                formatDateTime(record.eventTime()),
                record.operatorName(),
                record.location(),
                record.visibleToConsumer(),
                record.summary(),
                record.imageUrl()
        );
    }

    private QualityReportVO toQualityVO(QualityReportEntity report) {
        return new QualityReportVO(
                report.id(),
                report.reportNo(),
                report.agency(),
                report.result(),
                toQualityLabel(report.result()),
                formatDateTime(report.reportTime()),
                report.highlights()
        );
    }

    private BatchStatusLogVO toStatusLogVO(StatusHistoryEntity history) {
        return new BatchStatusLogVO(
                history.status().name(),
                history.reason(),
                history.operatorName(),
                formatDateTime(history.operatedAt())
        );
    }

    private List<BatchActionVO> buildActions(BatchEntity batch) {
        boolean hasQuality = !batch.getQualityReports().isEmpty();
        boolean hasQr = batch.getQrCode() != null;
        boolean canPublish = batch.getStatus() != BatchStatus.PUBLISHED && hasQuality && hasQr && batch.getStatus() != BatchStatus.RECALLED;
        boolean canFreeze = batch.getStatus() == BatchStatus.PUBLISHED;
        boolean canRecall = batch.getStatus() == BatchStatus.PUBLISHED || batch.getStatus() == BatchStatus.FROZEN;
        boolean canRegenerateQr = !hasQr;

        return List.of(
                new BatchActionVO("EDIT", "编辑基础信息", true, "只保留高频字段，避免冗长表单。", "neutral"),
                new BatchActionVO("ADD_TRACE", "新增追溯记录", true, "使用快速录入补齐关键节点。", "primary"),
                new BatchActionVO("UPLOAD_QUALITY", "上传质检", true, "发布前优先补齐高价值质检摘要。", "success"),
                new BatchActionVO("GENERATE_QR", hasQr ? "查看二维码" : "生成二维码", canRegenerateQr, hasQr ? "已生成二维码，可直接用于公开页。" : "同一批次只生成一次二维码，保持幂等。", "primary"),
                new BatchActionVO("VIEW_PUBLIC", "预览公开页", hasQr, hasQr ? "已具备扫码预览入口。" : "需先生成二维码。", "neutral"),
                new BatchActionVO("PUBLISH", "发布批次", canPublish, hasQuality && hasQr ? "满足发布条件，可直接发布。" : "需要先完成质检与二维码。", "success"),
                new BatchActionVO("FREEZE", "冻结批次", canFreeze, "发现异常时应快速冻结，并写明原因。", "warning"),
                new BatchActionVO("RECALL", "召回批次", canRecall, "召回后公开页首屏必须展示风险提醒。", "danger")
        );
    }

    private void validateStatusTransition(BatchEntity batch, BatchStatus currentStatus, BatchStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        if (currentStatus == BatchStatus.RECALLED) {
            throw new IllegalArgumentException("已召回批次不能再回退到其他状态");
        }
        if (targetStatus == BatchStatus.PUBLISHED && batch.getQrCode() == null) {
            throw new IllegalArgumentException("发布前请先生成二维码");
        }
        if (targetStatus == BatchStatus.PUBLISHED && batch.getQualityReports().isEmpty()) {
            throw new IllegalArgumentException("发布前请先上传质检摘要");
        }
        if (currentStatus == BatchStatus.DRAFT && targetStatus == BatchStatus.FROZEN) {
            throw new IllegalArgumentException("草稿批次不建议直接冻结，请先说明并发布或召回");
        }
    }

    private QualityReportEntity latestQuality(BatchEntity batch) {
        return batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);
    }

    private List<String> buildQuickTags(BatchEntity batch, QualityReportEntity latestQuality) {
        List<String> tags = new ArrayList<>();
        tags.add(toStatusLabel(batch.getStatus()));
        tags.add(batch.getQrCode() == null ? "待生成二维码" : "支持扫码");
        tags.add(latestQuality == null ? "待上传质检" : toQualityLabel(latestQuality.result()));
        if (batch.getStatus() == BatchStatus.FROZEN || batch.getStatus() == BatchStatus.RECALLED) {
            tags.add("风险批次");
        }
        return tags;
    }

    private String buildQualitySummary(QualityReportVO latestQuality) {
        if (latestQuality.highlights() == null || latestQuality.highlights().isEmpty()) {
            return latestQuality.resultLabel() + "，暂未补充更多检测亮点。";
        }
        return latestQuality.resultLabel() + "，重点结果：" + String.join("、", latestQuality.highlights());
    }

    private String toStatusLabel(BatchStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case PUBLISHED -> "已发布";
            case FROZEN -> "已冻结";
            case RECALLED -> "已召回";
        };
    }

    private String toQualityLabel(String result) {
        if (Objects.equals(result, "PASS")) {
            return "合格";
        }
        if (Objects.equals(result, "FAIL")) {
            return "不合格";
        }
        return "待确认";
    }

    private boolean containsIgnoreCase(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value);
    }

    private LocalDate parseNullableDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String resolveProductImage(String productName, String category) {
        String lowered = (defaultValue(productName, "") + " " + defaultValue(category, "")).toLowerCase(Locale.ROOT);
        if (lowered.contains("橙") || lowered.contains("fruit") || lowered.contains("水果")) {
            return "/images/products/orange-batch.svg";
        }
        if (lowered.contains("茶")) {
            return "/images/products/green-tea-batch.svg";
        }
        return "/images/products/rice-batch.svg";
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
