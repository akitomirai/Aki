package edu.jxust.agritrace.module.publictrace.vo;

public record PublicRiskVO(
        boolean hasRisk,
        String level,
        String title,
        String reason,
        String suggestion
) {
}
