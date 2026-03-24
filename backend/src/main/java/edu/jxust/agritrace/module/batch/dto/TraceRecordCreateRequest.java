package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;

public record TraceRecordCreateRequest(
        @NotBlank String stageCode,
        @NotBlank String stageName,
        @NotBlank String title,
        @NotBlank String eventTime,
        @NotBlank String operatorName,
        @NotBlank String location,
        @NotBlank String summary,
        boolean visibleToConsumer
) {
}
