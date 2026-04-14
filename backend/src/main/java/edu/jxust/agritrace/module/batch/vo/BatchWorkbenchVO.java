package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record BatchWorkbenchVO(
        BatchOverviewVO batch,
        BatchTaskSummaryVO task,
        ProductSummaryVO product,
        CompanySummaryVO company,
        BatchStatusSummaryVO status,
        BatchRiskSummaryVO risk,
        RiskHandlingSectionVO riskHandling,
        TraceSectionVO trace,
        TraceTimelineSectionVO traceTimeline,
        QualitySectionVO quality,
        QrSummaryVO qr,
        ScanStatsSectionVO scanStats,
        List<BatchStatusLogVO> statusHistory,
        List<BatchActionVO> actions
) {
}
