package edu.jxust.agritrace.module.publictrace.service.impl;

import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.TraceRecordEntity;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.service.support.BatchRiskResolver;
import edu.jxust.agritrace.module.batch.service.support.TraceDisplayLabels;
import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import edu.jxust.agritrace.module.publictrace.service.PublicTraceService;
import edu.jxust.agritrace.module.publictrace.vo.PublicCompanyVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicQualityVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicRiskVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicTimelineItemVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicTraceDetailVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicTraceSummaryVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class PublicTraceServiceImpl implements PublicTraceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BatchService batchService;
    private final BatchRiskResolver batchRiskResolver;

    public PublicTraceServiceImpl(BatchService batchService, BatchRiskResolver batchRiskResolver) {
        this.batchService = batchService;
        this.batchRiskResolver = batchRiskResolver;
    }

    @Override
    public PublicTraceDetailVO getTraceDetailByToken(String token, PublicTraceAccessContext accessContext) {
        batchService.recordPublicTraceAccess(token, accessContext);
        BatchEntity batch = batchService.getBatchEntityByToken(token);
        QualityReportEntity latestQuality = batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);
        BatchRiskResolver.RiskSnapshot risk = batchRiskResolver.resolve(batch);

        return new PublicTraceDetailVO(
                token,
                new PublicTraceSummaryVO(
                        batch.getProduct().name(),
                        batch.getProduct().imageUrl(),
                        batch.getBatchCode(),
                        batch.getCompany().name(),
                        batch.getOriginPlace(),
                        toStatusLabel(batch.getStatus()),
                        latestQuality == null ? TraceDisplayLabels.qualityStatus(null) : toQualityLabel(latestQuality.result()),
                        formatDate(batch.getProductionDate().atStartOfDay()),
                        formatDateTime(batch.getPublishedAt()),
                        "扫码后可直接查看批次状态、质检结论和关键追溯信息。"
                ),
                batch.getTraceRecords().stream()
                        .filter(TraceRecordEntity::visibleToConsumer)
                        .sorted(Comparator.comparing(TraceRecordEntity::eventTime))
                        .limit(6)
                        .map(record -> new PublicTimelineItemVO(
                                record.stage().name(),
                                record.stage().label(),
                                record.title(),
                                formatDateTime(record.eventTime()),
                                record.location(),
                                record.summary(),
                                record.imageUrl()
                        ))
                        .toList(),
                new PublicQualityVO(
                        latestQuality == null ? "PENDING" : latestQuality.result(),
                        latestQuality == null ? TraceDisplayLabels.qualityStatus(null) : toQualityLabel(latestQuality.result()),
                        latestQuality == null ? "企业尚未上传公开质检摘要，请以后续补充信息为准。" : buildQualitySummary(latestQuality),
                        latestQuality == null ? null : latestQuality.agency(),
                        latestQuality == null ? null : latestQuality.reportNo(),
                        latestQuality == null ? null : formatDateTime(latestQuality.reportTime()),
                        latestQuality == null ? List.of("等待企业补充公开质检要点。") : latestQuality.highlights()
                ),
                new PublicCompanyVO(
                        batch.getCompany().name(),
                        batch.getCompany().licenseNo(),
                        batch.getCompany().contactName(),
                        batch.getCompany().contactPhone(),
                        batch.getCompany().address()
                ),
                new PublicRiskVO(
                        risk.hasRisk(),
                        risk.status(),
                        risk.statusLabel(),
                        risk.riskLevel(),
                        risk.title(),
                        risk.reason(),
                        formatDateTime(risk.updatedAt()),
                        risk.tip()
                ),
                List.of(
                        "先查看批次当前状态和质检结论。",
                        "如批次已暂停流通或召回，请先按风险提示处理。",
                        "公开页仅展示面向消费者的关键信息，不展示后台内部留痕。"
                )
        );
    }

    private String toStatusLabel(BatchStatus status) {
        return TraceDisplayLabels.batchStatus(status);
    }

    private String toQualityLabel(String result) {
        return TraceDisplayLabels.qualityStatus(result);
    }

    private String buildQualitySummary(QualityReportEntity report) {
        if (report.highlights() == null || report.highlights().isEmpty()) {
            return toQualityLabel(report.result()) + "。当前暂无更多公开质检要点。";
        }
        return toQualityLabel(report.result()) + "。重点信息：" + String.join("，", report.highlights());
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }
}
