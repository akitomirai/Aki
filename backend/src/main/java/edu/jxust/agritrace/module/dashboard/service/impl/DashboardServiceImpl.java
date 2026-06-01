package edu.jxust.agritrace.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.ForbiddenException;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.mapper.BaseProductMapper;
import edu.jxust.agritrace.module.batch.mapper.BatchRiskActionMapper;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.batch.mapper.QrQueryLogMapper;
import edu.jxust.agritrace.module.batch.mapper.QualityReportMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceEventMapper;
import edu.jxust.agritrace.module.batch.mapper.po.BaseProductPO;
import edu.jxust.agritrace.module.batch.mapper.po.BatchRiskActionPO;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import edu.jxust.agritrace.module.batch.mapper.po.QrCodePO;
import edu.jxust.agritrace.module.batch.mapper.po.QrQueryLogPO;
import edu.jxust.agritrace.module.batch.mapper.po.QualityReportPO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceBatchPO;
import edu.jxust.agritrace.module.batch.service.support.TraceDisplayLabels;
import edu.jxust.agritrace.module.batch.service.support.TraceLinkBuilder;
import edu.jxust.agritrace.module.dashboard.service.DashboardService;
import edu.jxust.agritrace.module.dashboard.vo.BackupBatchItemVO;
import edu.jxust.agritrace.module.dashboard.vo.DashboardOverviewVO;
import edu.jxust.agritrace.module.dashboard.vo.DataBackupSnapshotVO;
import edu.jxust.agritrace.module.dashboard.vo.HotTraceQueryVO;
import edu.jxust.agritrace.module.dashboard.vo.ManagementTipVO;
import edu.jxust.agritrace.module.dashboard.vo.OriginTraceHeatVO;
import edu.jxust.agritrace.module.dashboard.vo.ProductTraceAnalysisVO;
import edu.jxust.agritrace.module.dashboard.vo.PublishStatusSummaryVO;
import edu.jxust.agritrace.module.dashboard.vo.QualityRiskAnalysisVO;
import edu.jxust.agritrace.module.dashboard.vo.QrScanAnalysisVO;
import edu.jxust.agritrace.module.dashboard.vo.QrScanTrendPointVO;
import edu.jxust.agritrace.module.dashboard.vo.TraceStatisticsAnalysisVO;
import edu.jxust.agritrace.module.dashboard.vo.TracePublishRecordVO;
import edu.jxust.agritrace.module.dashboard.vo.TraceCodeStatusQueryVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> RISK_STATUSES = Set.of(BatchStatus.FROZEN.name(), BatchStatus.RECALLED.name());

    private final TraceBatchMapper traceBatchMapper;
    private final BaseProductMapper baseProductMapper;
    private final OrgCompanyMapper orgCompanyMapper;
    private final QualityReportMapper qualityReportMapper;
    private final QrCodeMapper qrCodeMapper;
    private final QrQueryLogMapper qrQueryLogMapper;
    private final TraceEventMapper traceEventMapper;
    private final BatchRiskActionMapper batchRiskActionMapper;
    private final TraceLinkBuilder traceLinkBuilder;

    public DashboardServiceImpl(
            TraceBatchMapper traceBatchMapper,
            BaseProductMapper baseProductMapper,
            OrgCompanyMapper orgCompanyMapper,
            QualityReportMapper qualityReportMapper,
            QrCodeMapper qrCodeMapper,
            QrQueryLogMapper qrQueryLogMapper,
            TraceEventMapper traceEventMapper,
            BatchRiskActionMapper batchRiskActionMapper,
            TraceLinkBuilder traceLinkBuilder
    ) {
        this.traceBatchMapper = traceBatchMapper;
        this.baseProductMapper = baseProductMapper;
        this.orgCompanyMapper = orgCompanyMapper;
        this.qualityReportMapper = qualityReportMapper;
        this.qrCodeMapper = qrCodeMapper;
        this.qrQueryLogMapper = qrQueryLogMapper;
        this.traceEventMapper = traceEventMapper;
        this.batchRiskActionMapper = batchRiskActionMapper;
        this.traceLinkBuilder = traceLinkBuilder;
    }

    @Override
    public DashboardOverviewVO getOverview() {
        AuthUserSession currentUser = requireCurrentUser();
        ensureStatisticsReader(currentUser);

        List<String> focus = List.of(
                "企业建档、批次创建、质检上传、二维码发布和公开查询形成闭环。",
                "统计分析用于集中查看产品、批次、发布、质检、风险和公开访问情况。",
                "溯源发布列表可直接查看已发布或可展示批次的公开追溯入口。"
        );

        List<TraceBatchPO> batches = loadBatches(currentUser);
        List<BaseProductPO> products = loadProducts(currentUser);
        Map<Long, BaseProductPO> productMap = products.stream()
                .collect(Collectors.toMap(BaseProductPO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, OrgCompanyPO> companyMap = loadCompanies(batches).stream()
                .collect(Collectors.toMap(OrgCompanyPO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, QualityReportPO> latestQualityMap = latestQualityByBatch(batches);
        Map<Long, QrCodePO> latestQrMap = latestQrByBatch(batches);
        Map<Long, List<QrQueryLogPO>> qrLogsByBatch = qrLogsByBatch(batches);
        Map<Long, BatchRiskActionPO> latestRiskActionMap = latestRiskActionByBatch(batches);

        long productTotal = products.size();
        long batchTotal = batches.size();
        long publishedBatchTotal = batches.stream().filter(this::isPublished).count();
        long riskBatchTotal = batches.stream().filter(this::isRiskBatch).count();
        long pendingQualityTotal = batches.stream().filter(batch -> isPendingQuality(latestQualityMap.get(batch.getId()))).count();
        long qualityPassedTotal = latestQualityMap.values().stream().filter(this::isQualityPass).count();
        long inspectedQualityTotal = latestQualityMap.values().stream().filter(quality -> !isPendingQuality(quality)).count();
        long queryTotal = latestQrMap.values().stream()
                .mapToLong(qr -> resolveQueryCount(qr, qrLogsByBatch.get(qr.getBatchId())))
                .sum();
        long unpublishedBatchTotal = Math.max(0, batchTotal - publishedBatchTotal);

        List<TracePublishRecordVO> publishRecords = batches.stream()
                .filter(batch -> isTracePublishVisible(batch, latestQrMap.get(batch.getId())))
                .sorted(Comparator
                        .comparing((TraceBatchPO batch) -> latestTime(batch, latestQrMap.get(batch.getId())))
                        .reversed()
                        .thenComparing(TraceBatchPO::getId, Comparator.reverseOrder()))
                .map(batch -> toPublishRecord(
                        batch,
                        productMap.get(batch.getProductId()),
                        companyMap.get(batch.getCompanyId()),
                        latestQualityMap.get(batch.getId()),
                        latestQrMap.get(batch.getId()),
                        qrLogsByBatch.get(batch.getId()),
                        latestRiskActionMap.get(batch.getId())
                ))
                .toList();

        PublishStatusSummaryVO publishStatusSummary = new PublishStatusSummaryVO(
                publishedBatchTotal,
                unpublishedBatchTotal,
                pendingQualityTotal,
                batches.stream().filter(batch -> isRiskProcessing(batch, latestRiskActionMap.get(batch.getId()))).count(),
                riskBatchTotal
        );
        TraceStatisticsAnalysisVO analysis = buildAnalysis(
                batchTotal,
                publishedBatchTotal,
                unpublishedBatchTotal,
                pendingQualityTotal,
                qualityPassedTotal,
                inspectedQualityTotal,
                riskBatchTotal,
                queryTotal
        );
        QrScanAnalysisVO qrScanAnalysis = buildQrScanAnalysis(
                batches,
                productMap,
                latestQrMap,
                qrLogsByBatch
        );
        List<ProductTraceAnalysisVO> productTraceAnalysis = buildProductTraceAnalysis(
                batches,
                productMap,
                latestQualityMap,
                latestQrMap,
                qrLogsByBatch
        );
        List<OriginTraceHeatVO> originTraceHeat = buildOriginTraceHeat(
                batches,
                productMap,
                latestQrMap,
                qrLogsByBatch
        );
        QualityRiskAnalysisVO qualityRiskAnalysis = new QualityRiskAnalysisVO(
                qualityPassedTotal,
                latestQualityMap.values().stream()
                        .filter(quality -> "FAIL".equals(defaultValue(quality.getResult(), "").toUpperCase(Locale.ROOT)))
                        .count(),
                pendingQualityTotal,
                riskBatchTotal,
                riskBatchTotal,
                percentage(qualityPassedTotal, inspectedQualityTotal),
                percentage(riskBatchTotal, batchTotal)
        );

        return new DashboardOverviewVO(
                (int) batchTotal,
                (int) publishedBatchTotal,
                (int) batches.stream().filter(this::isDraft).count(),
                (int) riskBatchTotal,
                "当前主流程聚焦为：企业建档 -> 批次创建 -> 过程记录 -> 质检上传 -> 二维码生成 -> 发布 -> 扫码查看。",
                focus,
                productTotal,
                batchTotal,
                publishedBatchTotal,
                unpublishedBatchTotal,
                pendingQualityTotal,
                qualityPassedTotal,
                riskBatchTotal,
                queryTotal,
                publishStatusSummary,
                publishRecords,
                qrScanAnalysis,
                analysis,
                productTraceAnalysis,
                originTraceHeat,
                qualityRiskAnalysis
        );
    }

    @Override
    public DataBackupSnapshotVO createBackupSnapshot() {
        AuthUserSession currentUser = requireCurrentUser();
        ensureStatisticsReader(currentUser);

        List<TraceBatchPO> batches = loadBatches(currentUser);
        List<BaseProductPO> products = loadProducts(currentUser);
        Map<Long, BaseProductPO> productMap = products.stream()
                .collect(Collectors.toMap(BaseProductPO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, OrgCompanyPO> companyMap = loadCompanies(batches).stream()
                .collect(Collectors.toMap(OrgCompanyPO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, QualityReportPO> latestQualityMap = latestQualityByBatch(batches);
        Map<Long, QrCodePO> latestQrMap = latestQrByBatch(batches);
        Map<Long, List<QrQueryLogPO>> qrLogsByBatch = qrLogsByBatch(batches);

        List<BackupBatchItemVO> backupItems = batches.stream()
                .sorted(Comparator
                        .comparing((TraceBatchPO batch) -> latestTime(batch, latestQrMap.get(batch.getId())))
                        .reversed()
                        .thenComparing(TraceBatchPO::getId, Comparator.reverseOrder()))
                .map(batch -> toBackupBatchItem(
                        batch,
                        productMap.get(batch.getProductId()),
                        companyMap.get(batch.getCompanyId()),
                        latestQualityMap.get(batch.getId()),
                        latestQrMap.get(batch.getId()),
                        qrLogsByBatch.get(batch.getId())
                ))
                .toList();
        long queryTotal = qrLogsByBatch.values().stream().mapToLong(List::size).sum();
        String scope = isEnterpriseAdmin(currentUser)
                ? "企业数据范围：" + currentUser.companyId()
                : "平台全量数据范围";

        return new DataBackupSnapshotVO(
                "BK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                LocalDateTime.now(),
                defaultValue(currentUser.realName(), currentUser.username()),
                scope,
                companyMap.size(),
                products.size(),
                batches.size(),
                latestQualityMap.size(),
                latestQrMap.size(),
                countTraceEvents(batches),
                queryTotal,
                backupItems
        );
    }

    private List<TraceBatchPO> loadBatches(AuthUserSession currentUser) {
        LambdaQueryWrapper<TraceBatchPO> wrapper = new LambdaQueryWrapper<TraceBatchPO>()
                .orderByDesc(TraceBatchPO::getId);
        if (isEnterpriseAdmin(currentUser)) {
            wrapper.eq(TraceBatchPO::getCompanyId, currentUser.companyId());
        }
        return traceBatchMapper.selectList(wrapper);
    }

    private List<BaseProductPO> loadProducts(AuthUserSession currentUser) {
        LambdaQueryWrapper<BaseProductPO> wrapper = new LambdaQueryWrapper<BaseProductPO>()
                .orderByAsc(BaseProductPO::getId);
        if (isEnterpriseAdmin(currentUser)) {
            wrapper.eq(BaseProductPO::getCompanyId, currentUser.companyId());
        }
        return baseProductMapper.selectList(wrapper);
    }

    private List<OrgCompanyPO> loadCompanies(List<TraceBatchPO> batches) {
        List<Long> companyIds = batches.stream()
                .map(TraceBatchPO::getCompanyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (companyIds.isEmpty()) {
            return List.of();
        }
        return orgCompanyMapper.selectList(new LambdaQueryWrapper<OrgCompanyPO>()
                .in(OrgCompanyPO::getId, companyIds));
    }

    private Map<Long, QualityReportPO> latestQualityByBatch(List<TraceBatchPO> batches) {
        List<Long> batchIds = batchIds(batches);
        if (batchIds.isEmpty()) {
            return Map.of();
        }
        return qualityReportMapper.selectList(new LambdaQueryWrapper<QualityReportPO>()
                        .in(QualityReportPO::getBatchId, batchIds)
                        .orderByDesc(QualityReportPO::getCreatedAt)
                        .orderByDesc(QualityReportPO::getId))
                .stream()
                .collect(Collectors.toMap(QualityReportPO::getBatchId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }

    private Map<Long, QrCodePO> latestQrByBatch(List<TraceBatchPO> batches) {
        List<Long> batchIds = batchIds(batches);
        if (batchIds.isEmpty()) {
            return Map.of();
        }
        return qrCodeMapper.selectList(new LambdaQueryWrapper<QrCodePO>()
                        .in(QrCodePO::getBatchId, batchIds)
                        .orderByDesc(QrCodePO::getCreatedAt)
                        .orderByDesc(QrCodePO::getId))
                .stream()
                .collect(Collectors.toMap(QrCodePO::getBatchId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }

    private Map<Long, List<QrQueryLogPO>> qrLogsByBatch(List<TraceBatchPO> batches) {
        List<Long> batchIds = batchIds(batches);
        if (batchIds.isEmpty()) {
            return Map.of();
        }
        return qrQueryLogMapper.selectList(new LambdaQueryWrapper<QrQueryLogPO>()
                        .in(QrQueryLogPO::getBatchId, batchIds))
                .stream()
                .collect(Collectors.groupingBy(QrQueryLogPO::getBatchId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, BatchRiskActionPO> latestRiskActionByBatch(List<TraceBatchPO> batches) {
        List<Long> batchIds = batchIds(batches);
        if (batchIds.isEmpty()) {
            return Map.of();
        }
        return batchRiskActionMapper.selectList(new LambdaQueryWrapper<BatchRiskActionPO>()
                        .in(BatchRiskActionPO::getBatchId, batchIds)
                        .orderByDesc(BatchRiskActionPO::getCreatedAt)
                        .orderByDesc(BatchRiskActionPO::getId))
                .stream()
                .collect(Collectors.toMap(BatchRiskActionPO::getBatchId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }

    private TracePublishRecordVO toPublishRecord(
            TraceBatchPO batch,
            BaseProductPO product,
            OrgCompanyPO company,
            QualityReportPO quality,
            QrCodePO qr,
            List<QrQueryLogPO> logs,
            BatchRiskActionPO latestRiskAction
    ) {
        String qualityResult = quality == null ? "PENDING" : defaultValue(quality.getResult(), "PENDING").toUpperCase(Locale.ROOT);
        String qrStatus = qr == null ? "NOT_GENERATED" : defaultValue(qr.getStatus(), "READY").toUpperCase(Locale.ROOT);
        String publishStatus = normalizeStatus(batch.getStatus());
        String riskStatus = resolveRiskStatus(batch, latestRiskAction);
        String traceToken = qr == null ? null : qr.getQrToken();
        return new TracePublishRecordVO(
                batch.getId(),
                batch.getBatchCode(),
                product == null ? "-" : defaultValue(product.getName(), "-"),
                company == null ? "-" : defaultValue(company.getName(), "-"),
                TraceDisplayLabels.qualityStatus(qualityResult),
                qualityResult,
                qrStatus,
                TraceDisplayLabels.qrStatus(qrStatus),
                publishStatus,
                publishStatusLabel(batch),
                formatDateTime(batch.getPublishedAt()),
                resolveQueryCount(qr, logs),
                riskStatus,
                TraceDisplayLabels.riskStatus(riskStatus),
                traceToken,
                traceToken == null ? null : traceLinkBuilder.buildPublicTraceUrl(traceToken)
        );
    }

    private List<ProductTraceAnalysisVO> buildProductTraceAnalysis(
            List<TraceBatchPO> batches,
            Map<Long, BaseProductPO> productMap,
            Map<Long, QualityReportPO> latestQualityMap,
            Map<Long, QrCodePO> latestQrMap,
            Map<Long, List<QrQueryLogPO>> qrLogsByBatch
    ) {
        return batches.stream()
                .collect(Collectors.groupingBy(
                        batch -> defaultValue(productMap.get(batch.getProductId()) == null ? null : productMap.get(batch.getProductId()).getName(), "未命名产品"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<TraceBatchPO> productBatches = entry.getValue();
                    BaseProductPO product = productBatches.stream()
                            .map(batch -> productMap.get(batch.getProductId()))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
                    long publishedCount = productBatches.stream().filter(this::isPublished).count();
                    long qualityPassedCount = productBatches.stream()
                            .map(batch -> latestQualityMap.get(batch.getId()))
                            .filter(this::isQualityPass)
                            .count();
                    long riskCount = productBatches.stream().filter(this::isRiskBatch).count();
                    long queryCount = productBatches.stream()
                            .mapToLong(batch -> resolveQueryCount(latestQrMap.get(batch.getId()), qrLogsByBatch.get(batch.getId())))
                            .sum();
                    return new ProductTraceAnalysisVO(
                            entry.getKey(),
                            product == null ? "-" : defaultValue(product.getCategory(), "-"),
                            productBatches.size(),
                            publishedCount,
                            qualityPassedCount,
                            riskCount,
                            queryCount
                    );
                })
                .sorted(Comparator
                        .comparingLong(ProductTraceAnalysisVO::queryCount)
                        .reversed()
                        .thenComparing(ProductTraceAnalysisVO::productName))
                .limit(8)
                .toList();
    }

    private List<OriginTraceHeatVO> buildOriginTraceHeat(
            List<TraceBatchPO> batches,
            Map<Long, BaseProductPO> productMap,
            Map<Long, QrCodePO> latestQrMap,
            Map<Long, List<QrQueryLogPO>> qrLogsByBatch
    ) {
        Map<String, List<TraceBatchPO>> batchesByOrigin = batches.stream()
                .collect(Collectors.groupingBy(
                        batch -> {
                            BaseProductPO product = productMap.get(batch.getProductId());
                            return defaultValue(defaultValue(batch.getOriginPlace(), product == null ? null : product.getOriginPlace()), "未填写产地");
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        long maxQueryCount = batchesByOrigin.values().stream()
                .mapToLong(originBatches -> originBatches.stream()
                        .mapToLong(batch -> resolveQueryCount(latestQrMap.get(batch.getId()), qrLogsByBatch.get(batch.getId())))
                        .sum())
                .max()
                .orElse(0);
        return batchesByOrigin.entrySet()
                .stream()
                .map(entry -> {
                    long queryCount = entry.getValue().stream()
                            .mapToLong(batch -> resolveQueryCount(latestQrMap.get(batch.getId()), qrLogsByBatch.get(batch.getId())))
                            .sum();
                    double heatRate = maxQueryCount <= 0 ? 0 : percentage(queryCount, maxQueryCount);
                    return new OriginTraceHeatVO(
                            entry.getKey(),
                            entry.getValue().size(),
                            queryCount,
                            heatRate
                    );
                })
                .sorted(Comparator
                        .comparingLong(OriginTraceHeatVO::queryCount)
                        .reversed()
                        .thenComparing(OriginTraceHeatVO::originPlace))
                .limit(8)
                .toList();
    }

    private BackupBatchItemVO toBackupBatchItem(
            TraceBatchPO batch,
            BaseProductPO product,
            OrgCompanyPO company,
            QualityReportPO quality,
            QrCodePO qr,
            List<QrQueryLogPO> logs
    ) {
        String qualityResult = quality == null ? "PENDING" : defaultValue(quality.getResult(), "PENDING").toUpperCase(Locale.ROOT);
        String traceToken = qr == null ? null : qr.getQrToken();
        return new BackupBatchItemVO(
                batch.getId(),
                batch.getBatchCode(),
                product == null ? "-" : defaultValue(product.getName(), "-"),
                company == null ? "-" : defaultValue(company.getName(), "-"),
                defaultValue(batch.getOriginPlace(), product == null ? "-" : defaultValue(product.getOriginPlace(), "-")),
                normalizeStatus(batch.getStatus()),
                publishStatusLabel(batch),
                TraceDisplayLabels.qualityStatus(qualityResult),
                traceToken,
                resolveQueryCount(qr, logs),
                traceToken == null ? null : traceLinkBuilder.buildPublicTraceUrl(traceToken)
        );
    }

    private TraceStatisticsAnalysisVO buildAnalysis(
            long batchTotal,
            long publishedBatchTotal,
            long unpublishedBatchTotal,
            long pendingQualityTotal,
            long qualityPassedTotal,
            long inspectedQualityTotal,
            long riskBatchTotal,
            long queryTotal
    ) {
        double publishRate = percentage(publishedBatchTotal, batchTotal);
        double qualityPassRate = percentage(qualityPassedTotal, inspectedQualityTotal);
        double riskRate = percentage(riskBatchTotal, batchTotal);
        long pendingActionTotal = pendingQualityTotal + unpublishedBatchTotal + riskBatchTotal;
        return new TraceStatisticsAnalysisVO(
                publishRate,
                qualityPassRate,
                riskRate,
                pendingActionTotal,
                buildAnalysisText(batchTotal, publishedBatchTotal, publishRate, pendingQualityTotal, unpublishedBatchTotal, riskBatchTotal),
                buildManagementTips(batchTotal, publishedBatchTotal, unpublishedBatchTotal, pendingQualityTotal, riskBatchTotal, queryTotal)
        );
    }

    private String buildAnalysisText(
            long batchTotal,
            long publishedBatchTotal,
            double publishRate,
            long pendingQualityTotal,
            long unpublishedBatchTotal,
            long riskBatchTotal
    ) {
        if (batchTotal == 0) {
            return "当前系统暂无批次数据，建议先完成产品建档和批次创建，再推进质检、二维码生成与公开发布。";
        }
        return "当前系统共有 " + batchTotal + " 个批次，其中 " + publishedBatchTotal
                + " 个批次已完成溯源二维码发布，发布率为 " + formatRate(publishRate)
                + "%。仍有 " + pendingQualityTotal + " 个批次处于待质检状态，"
                + unpublishedBatchTotal + " 个批次未发布溯源信息，"
                + riskBatchTotal + " 个批次处于风险处理状态，建议管理员继续跟进待质检、未发布和风险批次。";
    }

    private List<ManagementTipVO> buildManagementTips(
            long batchTotal,
            long publishedBatchTotal,
            long unpublishedBatchTotal,
            long pendingQualityTotal,
            long riskBatchTotal,
            long queryTotal
    ) {
        List<ManagementTipVO> tips = new ArrayList<>();
        if (pendingQualityTotal > 0) {
            tips.add(new ManagementTipVO(
                    "quality",
                    "待质检批次处理",
                    "仍有 " + pendingQualityTotal + " 个批次尚未完成质检，建议进入质检管理页面补充检测结果。",
                    "warning",
                    "/quality"
            ));
        }
        if (unpublishedBatchTotal > 0) {
            tips.add(new ManagementTipVO(
                    "publish",
                    "溯源发布跟进",
                    "仍有 " + unpublishedBatchTotal + " 个批次未发布溯源二维码，建议确认质检状态后完成二维码发布。",
                    "warning",
                    "/qr"
            ));
        }
        if (riskBatchTotal > 0) {
            tips.add(new ManagementTipVO(
                    "risk",
                    "风险批次跟进",
                    "当前存在 " + riskBatchTotal + " 个风险批次，建议进入风险处理页面查看冻结、整改或召回状态。",
                    "danger",
                    "/risk"
            ));
        }
        if (queryTotal > 0) {
            tips.add(new ManagementTipVO(
                    "query",
                    "公开查询使用分析",
                    "公开追溯页面已产生 " + queryTotal + " 次访问记录，可结合查询次数判断溯源信息使用情况。",
                    "info",
                    "/batches"
            ));
        }
        if (publishedBatchTotal == 0 && batchTotal > 0) {
            tips.add(new ManagementTipVO(
                    "publish",
                    "首批溯源发布提醒",
                    "当前尚无已发布溯源批次，请优先完成质检、二维码生成和批次发布。",
                    "danger",
                    "/qr"
            ));
        }
        if (tips.isEmpty()) {
            tips.add(new ManagementTipVO(
                    "batch",
                    "批次状态持续维护",
                    "当前统计范围内无紧急待处理事项，可继续关注批次更新、公开查询和日常质检记录。",
                    "info",
                    "/batches"
            ));
        }
        return tips;
    }

    private QrScanAnalysisVO buildQrScanAnalysis(
            List<TraceBatchPO> batches,
            Map<Long, BaseProductPO> productMap,
            Map<Long, QrCodePO> latestQrMap,
            Map<Long, List<QrQueryLogPO>> qrLogsByBatch
    ) {
        Map<Long, TraceBatchPO> batchMap = batches.stream()
                .filter(batch -> batch.getId() != null)
                .collect(Collectors.toMap(TraceBatchPO::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        LocalDate today = LocalDate.now();
        List<QrQueryLogPO> allLogs = qrLogsByBatch.values().stream()
                .flatMap(List::stream)
                .toList();
        List<QrScanTrendPointVO> sevenDayTrend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            long count = allLogs.stream()
                    .filter(log -> log.getQueryTime() != null && day.equals(log.getQueryTime().toLocalDate()))
                    .count();
            sevenDayTrend.add(new QrScanTrendPointVO(day.toString(), count));
        }

        long totalScanCount = latestQrMap.values().stream()
                .mapToLong(qr -> resolveQueryCount(qr, qrLogsByBatch.get(qr.getBatchId())))
                .sum();
        long todayScanCount = allLogs.stream()
                .filter(log -> log.getQueryTime() != null && today.equals(log.getQueryTime().toLocalDate()))
                .count();

        List<HotTraceQueryVO> hotTraceCodes = latestQrMap.values().stream()
                .map(qr -> toHotTraceQuery(qr, batchMap.get(qr.getBatchId()), productMap, qrLogsByBatch.get(qr.getBatchId())))
                .filter(item -> item.queryCount() > 0)
                .sorted(Comparator
                        .comparingLong(HotTraceQueryVO::queryCount)
                        .reversed()
                        .thenComparing(HotTraceQueryVO::batchNo))
                .limit(5)
                .toList();

        long normalCount = 0;
        long riskCount = 0;
        long invalidCount = 0;
        for (QrCodePO qr : latestQrMap.values()) {
            long count = resolveQueryCount(qr, qrLogsByBatch.get(qr.getBatchId()));
            String status = normalizeQrQueryStatus(qr, batchMap.get(qr.getBatchId()));
            if ("risk".equals(status)) {
                riskCount += count;
            } else if ("invalid".equals(status)) {
                invalidCount += count;
            } else {
                normalCount += count;
            }
        }

        return new QrScanAnalysisVO(
                totalScanCount,
                todayScanCount,
                sevenDayTrend,
                hotTraceCodes,
                List.of(
                        new TraceCodeStatusQueryVO("normal", "正常码", normalCount),
                        new TraceCodeStatusQueryVO("risk", "风险码", riskCount),
                        new TraceCodeStatusQueryVO("invalid", "无效码", invalidCount)
                )
        );
    }

    private HotTraceQueryVO toHotTraceQuery(
            QrCodePO qr,
            TraceBatchPO batch,
            Map<Long, BaseProductPO> productMap,
            List<QrQueryLogPO> logs
    ) {
        BaseProductPO product = batch == null ? null : productMap.get(batch.getProductId());
        return new HotTraceQueryVO(
                defaultValue(qr.getQrToken(), "-"),
                batch == null ? "-" : defaultValue(batch.getBatchCode(), "-"),
                product == null ? "-" : defaultValue(product.getName(), "-"),
                resolveQueryCount(qr, logs)
        );
    }

    private String normalizeQrQueryStatus(QrCodePO qr, TraceBatchPO batch) {
        if (qr == null) {
            return "invalid";
        }
        String qrStatus = defaultValue(qr.getStatus(), "").toUpperCase(Locale.ROOT);
        if (Set.of("FROZEN", "RECALLED", "SUSPENDED").contains(qrStatus) || (batch != null && isRiskBatch(batch))) {
            return "risk";
        }
        if (Set.of("EXPIRED", "DISABLED", "INVALID", "DELETED").contains(qrStatus)) {
            return "invalid";
        }
        return "normal";
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String formatRate(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean isTracePublishVisible(TraceBatchPO batch, QrCodePO qr) {
        return true;
    }

    private boolean isPublished(TraceBatchPO batch) {
        return BatchStatus.PUBLISHED.name().equals(normalizeStatus(batch.getStatus()));
    }

    private boolean isDraft(TraceBatchPO batch) {
        return BatchStatus.DRAFT.name().equals(normalizeStatus(batch.getStatus()));
    }

    private boolean isRiskBatch(TraceBatchPO batch) {
        return RISK_STATUSES.contains(normalizeStatus(batch.getStatus()));
    }

    private boolean isPendingQuality(QualityReportPO quality) {
        return quality == null || "PENDING".equals(defaultValue(quality.getResult(), "PENDING").toUpperCase(Locale.ROOT));
    }

    private boolean isQualityPass(QualityReportPO quality) {
        return quality != null && "PASS".equals(defaultValue(quality.getResult(), "").toUpperCase(Locale.ROOT));
    }

    private boolean isRiskProcessing(TraceBatchPO batch, BatchRiskActionPO latestRiskAction) {
        if (BatchStatus.RECALLED.name().equals(normalizeStatus(batch.getStatus()))) {
            return false;
        }
        if (BatchStatus.FROZEN.name().equals(normalizeStatus(batch.getStatus()))) {
            return true;
        }
        String actionType = latestRiskAction == null ? "" : defaultValue(latestRiskAction.getActionType(), "").toUpperCase(Locale.ROOT);
        return "PROCESSING".equals(actionType) || "RECTIFICATION".equals(actionType);
    }

    private String resolveRiskStatus(TraceBatchPO batch, BatchRiskActionPO latestRiskAction) {
        String status = normalizeStatus(batch.getStatus());
        if (BatchStatus.FROZEN.name().equals(status)) {
            return "FROZEN";
        }
        if (BatchStatus.RECALLED.name().equals(status)) {
            return "RECALLED";
        }
        String actionType = latestRiskAction == null ? "" : defaultValue(latestRiskAction.getActionType(), "").toUpperCase(Locale.ROOT);
        if ("PROCESSING".equals(actionType) || "RECTIFICATION".equals(actionType)) {
            return "PROCESSING";
        }
        if ("RECTIFIED".equals(actionType)) {
            return "RECTIFIED";
        }
        return "NORMAL";
    }

    private String publishStatusLabel(TraceBatchPO batch) {
        String status = normalizeStatus(batch.getStatus());
        if (BatchStatus.PUBLISHED.name().equals(status)) {
            return "已发布";
        }
        if (BatchStatus.FROZEN.name().equals(status)) {
            return "已冻结";
        }
        if (BatchStatus.RECALLED.name().equals(status)) {
            return "已召回";
        }
        return "未发布";
    }

    private long resolveQueryCount(QrCodePO qr, List<QrQueryLogPO> logs) {
        if (qr == null) {
            return 0;
        }
        int logCount = logs == null ? 0 : logs.size();
        return Math.max(defaultLong(qr.getPv()), logCount);
    }

    private LocalDateTime latestTime(TraceBatchPO batch, QrCodePO qr) {
        LocalDateTime latest = batch.getPublishedAt();
        latest = laterOf(latest, qr == null ? null : qr.getCreatedAt());
        latest = laterOf(latest, batch.getUpdatedAt());
        latest = laterOf(latest, batch.getCreatedAt());
        return latest == null ? LocalDateTime.MIN : latest;
    }

    private LocalDateTime laterOf(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return right.isAfter(left) ? right : left;
    }

    private List<Long> batchIds(List<TraceBatchPO> batches) {
        return batches.stream()
                .map(TraceBatchPO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private long countTraceEvents(List<TraceBatchPO> batches) {
        List<Long> batchIds = batchIds(batches);
        if (batchIds.isEmpty()) {
            return 0;
        }
        return traceEventMapper.selectCount(new LambdaQueryWrapper<edu.jxust.agritrace.module.batch.mapper.po.TraceEventPO>()
                .in(edu.jxust.agritrace.module.batch.mapper.po.TraceEventPO::getBatchId, batchIds));
    }

    private String normalizeStatus(String status) {
        return defaultValue(status, BatchStatus.DRAFT.name()).toUpperCase(Locale.ROOT);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private long defaultLong(Long value) {
        return value == null ? 0 : value;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void ensureStatisticsReader(AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser) || isRegulator(currentUser)) {
            return;
        }
        throw new ForbiddenException("当前账号没有查看统计分析的权限。");
    }

    private AuthUserSession requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserSession userSession) {
            return userSession;
        }
        throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试。");
    }

    private boolean isPlatformAdmin(AuthUserSession currentUser) {
        return currentUser != null && "PLATFORM_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isEnterpriseAdmin(AuthUserSession currentUser) {
        return currentUser != null && "ENTERPRISE_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isRegulator(AuthUserSession currentUser) {
        return currentUser != null && "REGULATOR".equalsIgnoreCase(currentUser.roleCode());
    }
}
