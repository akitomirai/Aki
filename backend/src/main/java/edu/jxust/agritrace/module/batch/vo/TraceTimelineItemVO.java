package edu.jxust.agritrace.module.batch.vo;

public record TraceTimelineItemVO(
        String key,
        String phaseCode,
        String phaseLabel,
        String title,
        String eventTime,
        String operatorName,
        String operatorRole,
        String resultCode,
        String resultLabel,
        String summary,
        String sourceType,
        boolean highlighted
) {
}
