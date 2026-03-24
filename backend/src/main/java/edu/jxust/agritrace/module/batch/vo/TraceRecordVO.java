package edu.jxust.agritrace.module.batch.vo;

public record TraceRecordVO(
        Long id,
        String stageCode,
        String stageName,
        String title,
        String eventTime,
        String operatorName,
        String location,
        boolean visibleToConsumer,
        String summary
) {
}
