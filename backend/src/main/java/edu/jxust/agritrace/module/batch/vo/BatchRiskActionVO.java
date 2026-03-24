package edu.jxust.agritrace.module.batch.vo;

public record BatchRiskActionVO(
        Long id,
        String actionType,
        String actionLabel,
        String reason,
        String comment,
        String operatorName,
        String createdAt
) {
}
