package edu.jxust.agritrace.module.dashboard.vo;

public record QualityRiskAnalysisVO(
        long qualityPassedCount,
        long qualityFailedCount,
        long pendingQualityCount,
        long riskBatchCount,
        long frozenOrRecalledCount,
        double qualityPassRate,
        double riskRate
) {
}
