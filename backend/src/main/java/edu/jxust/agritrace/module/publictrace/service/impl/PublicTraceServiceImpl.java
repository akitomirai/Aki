package edu.jxust.agritrace.module.publictrace.service.impl;

import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.TraceRecordEntity;
import edu.jxust.agritrace.module.batch.service.BatchService;
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

    public PublicTraceServiceImpl(BatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public PublicTraceDetailVO getTraceDetailByToken(String token, PublicTraceAccessContext accessContext) {
        batchService.recordPublicTraceAccess(token, accessContext);
        BatchEntity batch = batchService.getBatchEntityByToken(token);
        QualityReportEntity latestQuality = batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);

        return new PublicTraceDetailVO(
                token,
                new PublicTraceSummaryVO(
                        batch.getProduct().name(),
                        batch.getProduct().imageUrl(),
                        batch.getBatchCode(),
                        batch.getCompany().name(),
                        batch.getOriginPlace(),
                        toStatusLabel(batch.getStatus()),
                        latestQuality == null ? "待上传" : toQualityLabel(latestQuality.result()),
                        formatDate(batch.getProductionDate().atStartOfDay()),
                        formatDateTime(batch.getPublishedAt()),
                        "扫码后优先看产品、批次、状态、质检和关键节点，不需要理解后台字段。"
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
                        latestQuality == null ? "待上传" : toQualityLabel(latestQuality.result()),
                        latestQuality == null ? "当前暂未公开展示质检摘要。" : buildQualitySummary(latestQuality),
                        latestQuality == null ? "暂无" : latestQuality.agency(),
                        latestQuality == null ? null : latestQuality.reportNo(),
                        latestQuality == null ? null : formatDateTime(latestQuality.reportTime()),
                        latestQuality == null ? List.of("建议稍后查看企业补充说明") : latestQuality.highlights()
                ),
                new PublicCompanyVO(
                        batch.getCompany().name(),
                        batch.getCompany().licenseNo(),
                        batch.getCompany().contactName(),
                        batch.getCompany().contactPhone(),
                        batch.getCompany().address()
                ),
                buildRisk(batch),
                List.of(
                        "先看批次状态和质检结果，再看关键时间线。",
                        "如果页面提示冻结或召回，请优先遵循风险提醒。",
                        "公开页只展示消费者真正关心的信息，不会把后台内部日志全部放出来。"
                )
        );
    }

    private PublicRiskVO buildRisk(BatchEntity batch) {
        if (batch.getStatus() == BatchStatus.RECALLED) {
            return new PublicRiskVO(
                    true,
                    "danger",
                    "该批次已召回",
                    defaultMessage(batch.getStatusReason(), "企业或监管已要求停止继续销售和食用。"),
                    "如已购买，请联系销售方或监管部门进一步处理。"
            );
        }
        if (batch.getStatus() == BatchStatus.FROZEN) {
            return new PublicRiskVO(
                    true,
                    "warning",
                    "该批次当前已冻结",
                    defaultMessage(batch.getStatusReason(), "企业正在补充留痕材料或等待监管复核。"),
                    "建议暂缓购买，等待批次状态恢复或进一步公示。"
            );
        }
        return new PublicRiskVO(
                false,
                "normal",
                "当前无公开风险提示",
                "该批次当前处于可正常查看状态。",
                "如需进一步确认，可联系企业查看更多信息。"
        );
    }

    private String toStatusLabel(BatchStatus status) {
        return switch (status) {
            case DRAFT -> "未发布";
            case PUBLISHED -> "已发布";
            case FROZEN -> "已冻结";
            case RECALLED -> "已召回";
        };
    }

    private String toQualityLabel(String result) {
        return switch (result) {
            case "PASS" -> "合格";
            case "FAIL" -> "不合格";
            default -> "待确认";
        };
    }

    private String buildQualitySummary(QualityReportEntity report) {
        if (report.highlights() == null || report.highlights().isEmpty()) {
            return toQualityLabel(report.result()) + "，暂未补充更多检测亮点。";
        }
        return toQualityLabel(report.result()) + "，重点结果：" + String.join("、", report.highlights());
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String defaultMessage(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
