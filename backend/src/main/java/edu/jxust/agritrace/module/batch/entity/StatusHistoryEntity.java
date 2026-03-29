package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;

public record StatusHistoryEntity(
        BatchStatus status,
        String reason,
        String operatorName,
        LocalDateTime operatedAt
) {
}
