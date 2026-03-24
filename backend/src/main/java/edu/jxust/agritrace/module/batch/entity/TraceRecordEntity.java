package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;

public record TraceRecordEntity(
        Long id,
        String stageCode,
        String stageName,
        String title,
        LocalDateTime eventTime,
        String operatorName,
        String location,
        boolean visibleToConsumer,
        String summary
) {
}
