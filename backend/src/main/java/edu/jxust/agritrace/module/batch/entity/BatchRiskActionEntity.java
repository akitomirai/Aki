package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;

public record BatchRiskActionEntity(
        Long id,
        RiskActionType actionType,
        String reason,
        String comment,
        String operatorName,
        LocalDateTime createdAt
) {
}
