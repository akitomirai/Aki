package edu.jxust.agritrace.module.batch.dto;

import edu.jxust.agritrace.module.batch.entity.TraceStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TraceRecordCreateRequest(
        @NotNull TraceStage stage,
        String title,
        String eventTime,
        @NotBlank String operatorName,
        @NotBlank String location,
        @NotBlank String summary,
        String imageUrl,
        List<Long> attachmentIds,
        boolean visibleToConsumer
) {
}
