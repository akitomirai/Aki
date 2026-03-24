package edu.jxust.agritrace.module.publictrace.service.impl;

import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.TraceRecordEntity;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.publictrace.service.PublicTraceService;
import edu.jxust.agritrace.module.publictrace.vo.PublicCompanyVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicExceptionVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicQualityVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicTimelineItemVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicTraceDetailVO;
import edu.jxust.agritrace.module.publictrace.vo.PublicTraceSummaryVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class PublicTraceServiceImpl implements PublicTraceService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BatchService batchService;

    public PublicTraceServiceImpl(BatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public PublicTraceDetailVO getTraceDetailByToken(String token) {
        BatchEntity batch = batchService.getBatchEntityByToken(token);
        QualityReportEntity latestQuality = batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);

        return new PublicTraceDetailVO(
                token,
                new PublicTraceSummaryVO(
                        batch.getProduct().name(),
                        batch.getBatchCode(),
                        batch.getCompany().name(),
                        batch.getOriginPlace(),
                        toStatusLabel(batch),
                        latestQuality == null ? "待上传" : latestQuality.result(),
                        formatDateTime(batch.getPublishedAt()),
                        "扫码即可看懂这个批次从哪里来、做过什么、现在是否可放心购买。"
                ),
                batch.getTraceRecords().stream()
                        .filter(TraceRecordEntity::visibleToConsumer)
                        .sorted(Comparator.comparing(TraceRecordEntity::eventTime))
                        .map(record -> new PublicTimelineItemVO(
                                record.stageName(),
                                record.title(),
                                formatDateTime(record.eventTime()),
                                record.location(),
                                record.summary()
                        ))
                        .toList(),
                new PublicQualityVO(
                        latestQuality == null ? "待上传" : latestQuality.result(),
                        latestQuality == null ? "暂无" : latestQuality.agency(),
                        latestQuality == null ? null : latestQuality.reportNo(),
                        latestQuality == null ? null : formatDateTime(latestQuality.reportTime()),
                        latestQuality == null ? List.of("当前未展示检测报告") : latestQuality.highlights()
                ),
                new PublicCompanyVO(
                        batch.getCompany().name(),
                        batch.getCompany().licenseNo(),
                        batch.getCompany().contactName(),
                        batch.getCompany().contactPhone(),
                        batch.getCompany().address()
                ),
                buildException(batch),
                List.of(
                        "优先查看批次状态、关键时间线和质检结果。",
                        "如发现异常状态，请以企业通知或监管信息为准。",
                        "公开页只展示消费者看得懂、与购买决策相关的信息。"
                )
        );
    }

    private PublicExceptionVO buildException(BatchEntity batch) {
        if (batch.getStatus() == BatchStatus.FROZEN) {
            return new PublicExceptionVO(true, "warning", "该批次已冻结，当前不建议继续流通。", defaultMessage(batch.getStatusReason(), "请等待企业补充说明或监管复核结果。"));
        }
        if (batch.getStatus() == BatchStatus.RECALLED) {
            return new PublicExceptionVO(true, "danger", "该批次已召回，请勿继续购买或食用。", defaultMessage(batch.getStatusReason(), "如已购买，请联系销售方或监管机构。"));
        }
        return new PublicExceptionVO(false, "normal", "该批次当前无公开异常。", "如需进一步确认，可查看企业联系方式。");
    }

    private String toStatusLabel(BatchEntity batch) {
        return switch (batch.getStatus()) {
            case DRAFT -> "未发布";
            case PUBLISHED -> "已发布";
            case FROZEN -> "已冻结";
            case RECALLED -> "已召回";
        };
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String defaultMessage(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
