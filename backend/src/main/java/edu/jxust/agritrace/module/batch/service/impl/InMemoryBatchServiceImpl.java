package edu.jxust.agritrace.module.batch.service.impl;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
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
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.BatchActionVO;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchStatusLogVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import edu.jxust.agritrace.module.batch.vo.QrInfoVO;
import edu.jxust.agritrace.module.batch.vo.QualityReportVO;
import edu.jxust.agritrace.module.batch.vo.TraceRecordVO;
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
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryBatchServiceImpl implements BatchService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Map<Long, BatchEntity> batchStore = new LinkedHashMap<>();
    private final AtomicLong batchIdGenerator = new AtomicLong(100);
    private final AtomicLong traceIdGenerator = new AtomicLong(1000);
    private final AtomicLong qualityIdGenerator = new AtomicLong(2000);
    private final AtomicLong qrIdGenerator = new AtomicLong(3000);

    @PostConstruct
    public void initDemoData() {
        CompanyEntity company = new CompanyEntity(
                1L,
                "赣南脐橙示范基地",
                "LIC-DEMO-001",
                "刘建国",
                "13800000001",
                "江西省赣州市信丰县高标准果园"
        );
        ProductEntity orange = new ProductEntity(1L, "赣南脐橙", "水果", "10kg/箱", "箱");
        ProductEntity tea = new ProductEntity(2L, "江西绿茶", "茶叶", "250g/盒", "盒");
        ProductEntity rice = new ProductEntity(3L, "富硒大米", "粮食", "5kg/袋", "袋");

        BatchEntity published = buildBaseBatch(1L, "BATCH20260311001", orange, company, "江西省赣州市信丰县高标准果园", LocalDate.of(2026, 3, 1));
        published.setStatus(BatchStatus.PUBLISHED);
        published.setCurrentNode("已发布，消费者可扫码查看");
        published.setPublicRemark("该批次已完成建档、过程记录、质检与二维码发布，适合用于演示扫码追溯。");
        published.setInternalRemark("演示批次，突出批次工作台和公开追溯页。");
        published.setPublishedAt(LocalDateTime.of(2026, 3, 11, 11, 10));
        published.getTraceRecords().addAll(List.of(
                new TraceRecordEntity(1L, "ARCHIVE", "企业建档", "果园建档完成", LocalDateTime.of(2026, 3, 1, 8, 0), "刘建国", "信丰果园", true, "完成企业与产地建档，录入果园基础信息。"),
                new TraceRecordEntity(2L, "PRODUCE", "生产记录", "春季田间管理", LocalDateTime.of(2026, 3, 5, 9, 30), "王小林", "信丰果园", true, "记录修枝、灌溉和日常巡园情况。"),
                new TraceRecordEntity(3L, "WAREHOUSE", "仓储记录", "入库待发", LocalDateTime.of(2026, 3, 10, 10, 20), "周敏", "南昌仓配中心", true, "完成分级包装后入库待发。")
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
                new StatusHistoryEntity(BatchStatus.DRAFT, "创建批次并补齐基础信息", "刘建国", LocalDateTime.of(2026, 3, 1, 8, 0)),
                new StatusHistoryEntity(BatchStatus.PUBLISHED, "二维码生成完成并对外发布", "刘建国", LocalDateTime.of(2026, 3, 11, 11, 10))
        ));

        BatchEntity draft = buildBaseBatch(2L, "BATCH20260312001", tea, company, "江西省上饶市婺源县生态茶园", LocalDate.of(2026, 3, 5));
        draft.setStatus(BatchStatus.DRAFT);
        draft.setCurrentNode("待补录质检与二维码");
        draft.setPublicRemark("该批次尚未发布，扫码页暂不可见。");
        draft.setInternalRemark("优先补录质检，再一键生成二维码。");
        draft.getTraceRecords().add(new TraceRecordEntity(
                4L, "PRODUCE", "生产记录", "采摘完成", LocalDateTime.of(2026, 3, 12, 9, 0), "陈红", "婺源茶园", false, "已完成采摘，待录入包装与检测记录。"
        ));
        draft.getStatusHistory().add(new StatusHistoryEntity(BatchStatus.DRAFT, "草稿创建", "陈红", LocalDateTime.of(2026, 3, 12, 9, 0)));

        BatchEntity frozen = buildBaseBatch(3L, "BATCH20260312002", rice, company, "江西省宜春市富硒稻米基地", LocalDate.of(2026, 3, 2));
        frozen.setStatus(BatchStatus.FROZEN);
        frozen.setStatusReason("监管抽查发现仓储温湿度记录缺失，暂时冻结展示。");
        frozen.setCurrentNode("冻结中，待补充留痕");
        frozen.setPublicRemark("该批次当前处于冻结状态，请以企业通知为准。");
        frozen.setInternalRemark("补录仓储记录并完成复核后可恢复发布。");
        frozen.setPublishedAt(LocalDateTime.of(2026, 3, 13, 9, 20));
        frozen.setFrozenAt(LocalDateTime.of(2026, 3, 14, 14, 0));
        frozen.getTraceRecords().add(new TraceRecordEntity(
                5L, "WAREHOUSE", "仓储记录", "入库复核待补录", LocalDateTime.of(2026, 3, 13, 16, 0), "李仓", "宜春成品仓", true, "已入库，但留痕记录不完整。"
        ));
        frozen.getQualityReports().add(new QualityReportEntity(
                2L, "QA-20260314-009", "宜春农产品检测站", "PASS", LocalDateTime.of(2026, 3, 14, 10, 20), List.of("重金属合格", "外观合格")
        ));
        frozen.setQrCode(new QrCodeEntity(
                2L, "frozen-token-2026", "SUSPENDED", "http://127.0.0.1:5173/t/frozen-token-2026", LocalDateTime.of(2026, 3, 13, 9, 20), LocalDateTime.of(2026, 3, 20, 9, 0)
        ));
        frozen.getStatusHistory().addAll(List.of(
                new StatusHistoryEntity(BatchStatus.DRAFT, "批次创建", "刘建国", LocalDateTime.of(2026, 3, 12, 11, 0)),
                new StatusHistoryEntity(BatchStatus.PUBLISHED, "生成二维码后发布", "刘建国", LocalDateTime.of(2026, 3, 13, 9, 20)),
                new StatusHistoryEntity(BatchStatus.FROZEN, "监管抽查要求补充证明材料", "平台巡检", LocalDateTime.of(2026, 3, 14, 14, 0))
        ));

        batchStore.put(published.getId(), published);
        batchStore.put(draft.getId(), draft);
        batchStore.put(frozen.getId(), frozen);
    }

    @Override
    public List<BatchListItemVO> listBatches() {
        return batchStore.values().stream()
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
        ProductEntity product = new ProductEntity(batchId, request.productName(), request.category(), "待补录", "待补录");
        CompanyEntity company = new CompanyEntity(batchId, request.companyName(), "待补录", "待补录", "待补录", request.originPlace());

        BatchEntity batch = buildBaseBatch(
                batchId,
                request.batchCode(),
                product,
                company,
                request.originPlace(),
                LocalDate.parse(request.productionDate())
        );
        batch.setStatus(BatchStatus.DRAFT);
        batch.setCurrentNode("待补录过程记录");
        batch.setPublicRemark(defaultValue(request.publicRemark(), "创建完成后即可继续补录生产、质检和二维码信息。"));
        batch.setInternalRemark(defaultValue(request.internalRemark(), "新建批次，请优先补录过程与检测。"));
        batch.getStatusHistory().add(new StatusHistoryEntity(BatchStatus.DRAFT, "批次创建完成", "系统演示用户", LocalDateTime.now()));
        batchStore.put(batchId, batch);
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO changeStatus(Long batchId, BatchStatusActionRequest request) {
        BatchEntity batch = findBatch(batchId);
        batch.setStatus(request.targetStatus());
        batch.setStatusReason(request.reason());
        batch.getStatusHistory().add(new StatusHistoryEntity(request.targetStatus(), request.reason(), request.operatorName(), LocalDateTime.now()));
        if (request.targetStatus() == BatchStatus.PUBLISHED) {
            batch.setPublishedAt(LocalDateTime.now());
            batch.setCurrentNode("已发布，消费者可扫码查看");
        } else if (request.targetStatus() == BatchStatus.FROZEN) {
            batch.setFrozenAt(LocalDateTime.now());
            batch.setCurrentNode("已冻结，公开追溯页显示异常提醒");
        } else if (request.targetStatus() == BatchStatus.RECALLED) {
            batch.setRecalledAt(LocalDateTime.now());
            batch.setCurrentNode("已召回，公开追溯页显示召回提示");
        } else {
            batch.setCurrentNode("草稿整理中");
        }
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO addTraceRecord(Long batchId, TraceRecordCreateRequest request) {
        BatchEntity batch = findBatch(batchId);
        batch.getTraceRecords().add(new TraceRecordEntity(
                traceIdGenerator.incrementAndGet(),
                request.stageCode(),
                request.stageName(),
                request.title(),
                LocalDateTime.parse(request.eventTime()),
                request.operatorName(),
                request.location(),
                request.visibleToConsumer(),
                request.summary()
        ));
        batch.setCurrentNode(request.stageName());
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO addQualityReport(Long batchId, QualityReportCreateRequest request) {
        BatchEntity batch = findBatch(batchId);
        batch.getQualityReports().add(new QualityReportEntity(
                qualityIdGenerator.incrementAndGet(),
                request.reportNo(),
                request.agency(),
                request.result(),
                LocalDateTime.parse(request.reportTime()),
                request.highlights() == null ? List.of() : request.highlights()
        ));
        return toWorkbench(batch);
    }

    @Override
    public BatchWorkbenchVO generateQr(Long batchId) {
        BatchEntity batch = findBatch(batchId);
        String token = batch.getBatchCode().toLowerCase(Locale.ROOT);
        batch.setQrCode(new QrCodeEntity(
                qrIdGenerator.incrementAndGet(),
                token,
                "ACTIVE",
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

    private BatchListItemVO toListItem(BatchEntity batch) {
        String qrStatus = batch.getQrCode() == null ? "未生成" : batch.getQrCode().status();
        String qualityStatus = batch.getQualityReports().isEmpty() ? "待上传" : batch.getQualityReports().get(batch.getQualityReports().size() - 1).result();
        List<String> quickTags = new ArrayList<>();
        quickTags.add("批次中心");
        quickTags.add(batch.getTraceRecords().isEmpty() ? "待补过程" : "过程已记录");
        quickTags.add(batch.getQrCode() == null ? "待生成二维码" : "可扫码");
        return new BatchListItemVO(
                batch.getId(),
                batch.getBatchCode(),
                batch.getProduct().name(),
                batch.getCompany().name(),
                batch.getStatus().name(),
                batch.getCurrentNode(),
                formatDate(batch.getProductionDate()),
                formatDateTime(batch.getPublishedAt()),
                qrStatus,
                qualityStatus,
                quickTags
        );
    }

    private BatchWorkbenchVO toWorkbench(BatchEntity batch) {
        return new BatchWorkbenchVO(
                batch.getId(),
                batch.getBatchCode(),
                batch.getStatus().name(),
                batch.getStatusReason(),
                batch.getCurrentNode(),
                formatDate(batch.getProductionDate()),
                batch.getOriginPlace(),
                batch.getPublicRemark(),
                batch.getInternalRemark(),
                batch.getProduct().name(),
                batch.getProduct().category(),
                batch.getCompany().name(),
                batch.getCompany().licenseNo(),
                batch.getCompany().contactName(),
                batch.getCompany().contactPhone(),
                batch.getCompany().address(),
                batch.getQualityReports().isEmpty() ? "待上传" : batch.getQualityReports().get(batch.getQualityReports().size() - 1).result(),
                toQrInfo(batch.getQrCode()),
                batch.getTraceRecords().stream().sorted(Comparator.comparing(TraceRecordEntity::eventTime)).map(this::toTraceRecordVO).toList(),
                batch.getQualityReports().stream().sorted(Comparator.comparing(QualityReportEntity::reportTime)).map(this::toQualityVO).toList(),
                batch.getStatusHistory().stream().map(this::toStatusLogVO).toList(),
                buildActions(batch)
        );
    }

    private QrInfoVO toQrInfo(QrCodeEntity qrCode) {
        if (qrCode == null) {
            return null;
        }
        return new QrInfoVO(
                qrCode.id(),
                qrCode.token(),
                qrCode.status(),
                qrCode.publicUrl(),
                formatDateTime(qrCode.generatedAt()),
                formatDateTime(qrCode.lastScanAt())
        );
    }

    private TraceRecordVO toTraceRecordVO(TraceRecordEntity record) {
        return new TraceRecordVO(
                record.id(),
                record.stageCode(),
                record.stageName(),
                record.title(),
                formatDateTime(record.eventTime()),
                record.operatorName(),
                record.location(),
                record.visibleToConsumer(),
                record.summary()
        );
    }

    private QualityReportVO toQualityVO(QualityReportEntity report) {
        return new QualityReportVO(
                report.id(),
                report.reportNo(),
                report.agency(),
                report.result(),
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
        return List.of(
                new BatchActionVO("ADD_TRACE", "补录过程记录", true, "面向录入员，少字段快速补录"),
                new BatchActionVO("UPLOAD_QUALITY", "上传质检", true, "优先补齐能支撑发布的质量证明"),
                new BatchActionVO("GENERATE_QR", "生成二维码", !hasQr, hasQr ? "二维码已生成" : "生成后即可用于公开追溯"),
                new BatchActionVO("PUBLISH", "发布批次", batch.getStatus() != BatchStatus.PUBLISHED && hasQuality && hasQr, "需先完成质检与二维码"),
                new BatchActionVO("FREEZE", "冻结批次", batch.getStatus() == BatchStatus.PUBLISHED, "监管或企业发现风险时快速冻结"),
                new BatchActionVO("RECALL", "召回批次", batch.getStatus() != BatchStatus.RECALLED, "异常批次需保留原因和留痕")
        );
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
