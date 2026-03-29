package edu.jxust.agritrace.module.publictrace.vo;

public record PublicRiskVO(
        boolean hasRisk,
        String status,
        String riskLevel,
        String title,
        String reason,
        String updatedAt,
        String tip
) {
}
