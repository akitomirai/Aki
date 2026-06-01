package edu.jxust.agritrace.module.dashboard.vo;

import java.util.List;

public record DashboardOverviewVO(
        int totalBatches,
        int publishedBatches,
        int draftBatches,
        int riskBatches,
        String coreFlowMessage,
        List<String> currentFocus,
        long productTotal,
        long batchTotal,
        long publishedBatchTotal,
        long unpublishedBatchTotal,
        long pendingQualityTotal,
        long qualityPassedTotal,
        long riskBatchTotal,
        long queryTotal,
        PublishStatusSummaryVO publishStatusSummary,
        List<TracePublishRecordVO> publishRecords,
        QrScanAnalysisVO qrScanAnalysis,
        TraceStatisticsAnalysisVO analysis,
        List<ProductTraceAnalysisVO> productTraceAnalysis,
        List<OriginTraceHeatVO> originTraceHeat,
        QualityRiskAnalysisVO qualityRiskAnalysis
) {
}
