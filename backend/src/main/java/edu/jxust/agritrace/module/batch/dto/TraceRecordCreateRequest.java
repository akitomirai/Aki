package edu.jxust.agritrace.module.batch.dto;

import edu.jxust.agritrace.module.batch.entity.TraceStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TraceRecordCreateRequest(
        @NotNull TraceStage stage,
        @Size(max = 80) String title,
        @Size(max = 32) String eventTime,
        @NotBlank @Size(max = 32) String operatorName,
        @NotBlank @Size(max = 120) String location,
        @NotBlank @Size(max = 300) String summary,
        @Size(max = 500) String imageUrl,
        @Size(max = 9) List<Long> attachmentIds,
        boolean visibleToConsumer
) {
}
