package edu.jxust.agritrace.module.batch.vo;

public record BatchRiskSummaryVO(
        boolean hasRisk,
        String status,
        String statusLabel,
        String riskLevel,
        String title,
        String reason,
        String updatedAt,
        String tip
) {
}
