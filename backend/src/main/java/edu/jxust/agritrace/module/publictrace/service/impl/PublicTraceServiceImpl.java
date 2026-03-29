package edu.jxust.agritrace.module.publictrace.service.impl;

import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.TraceRecordEntity;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.service.support.BatchRiskResolver;
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
                        latestQuality == null ? "Pending" : toQualityLabel(latestQuality.result()),
                        formatDate(batch.getProductionDate().atStartOfDay()),
                        formatDateTime(batch.getPublishedAt()),
                        "Scan once and understand the batch, quality and current status in a few seconds."
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
                        latestQuality == null ? "Pending" : toQualityLabel(latestQuality.result()),
                        latestQuality == null ? "No public quality summary has been uploaded yet." : buildQualitySummary(latestQuality),
                        latestQuality == null ? null : latestQuality.agency(),
                        latestQuality == null ? null : latestQuality.reportNo(),
                        latestQuality == null ? null : formatDateTime(latestQuality.reportTime()),
                        latestQuality == null ? List.of("Wait for the enterprise to upload quality highlights.") : latestQuality.highlights()
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
                        risk.riskLevel(),
                        risk.title(),
                        risk.reason(),
                        formatDateTime(risk.updatedAt()),
                        risk.tip()
                ),
                List.of(
                        "Read the batch status and quality result first.",
                        "If the batch is frozen or recalled, follow the risk guidance before any purchase or use.",
                        "The public page only shows key consumer-facing facts instead of internal back-office logs."
                )
        );
    }

    private String toStatusLabel(BatchStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case PUBLISHED -> "Published";
            case FROZEN -> "Frozen";
            case RECALLED -> "Recalled";
        };
    }

    private String toQualityLabel(String result) {
        return switch (result) {
            case "PASS" -> "Pass";
            case "FAIL" -> "Fail";
            default -> "Review";
        };
    }

    private String buildQualitySummary(QualityReportEntity report) {
        if (report.highlights() == null || report.highlights().isEmpty()) {
            return toQualityLabel(report.result()) + ". No additional highlight is available.";
        }
        return toQualityLabel(report.result()) + ". Highlights: " + String.join(", ", report.highlights());
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }
}
