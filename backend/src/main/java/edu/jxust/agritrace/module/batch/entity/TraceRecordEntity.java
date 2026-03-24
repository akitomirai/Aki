package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;

public record TraceRecordEntity(
        Long id,
        TraceStage stage,
        String title,
        LocalDateTime eventTime,
        String operatorName,
        String location,
        boolean visibleToConsumer,
        String summary,
        String imageUrl
) {
}
