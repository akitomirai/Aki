package edu.jxust.agritrace.module.dashboard.vo;

public record ProductTraceAnalysisVO(
        String productName,
        String category,
        long batchCount,
        long publishedCount,
        long qualityPassedCount,
        long riskCount,
        long queryCount
) {
}
