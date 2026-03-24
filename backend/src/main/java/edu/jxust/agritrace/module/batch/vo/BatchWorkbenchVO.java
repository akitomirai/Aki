package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record BatchWorkbenchVO(
        BatchOverviewVO batch,
        ProductSummaryVO product,
        CompanySummaryVO company,
        BatchStatusSummaryVO status,
        BatchRiskSummaryVO risk,
        TraceSectionVO trace,
        QualitySectionVO quality,
        QrSummaryVO qr,
        ScanStatsSectionVO scanStats,
        List<BatchStatusLogVO> statusHistory,
        List<BatchActionVO> actions
) {
}
