package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record BatchWorkbenchVO(
        BatchOverviewVO batch,
        ProductSummaryVO product,
        CompanySummaryVO company,
        BatchStatusSummaryVO status,
        TraceSectionVO trace,
        QualitySectionVO quality,
        QrSummaryVO qr,
        List<BatchStatusLogVO> statusHistory,
        List<BatchActionVO> actions
) {
}
