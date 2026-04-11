package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record FieldDraftSaveRequest(
        @Size(max = 20) String stage,
        @Size(max = 80) String title,
        @Size(max = 32) String eventTime,
        @Size(max = 32) String operatorName,
        @Size(max = 120) String location,
        @Size(max = 300) String summary,
        @Size(max = 500) String imageUrl,
        @Size(max = 9) List<Long> attachmentIds,
        @Size(max = 9) List<FieldDraftFileItemDTO> uploadedFiles,
        Boolean visibleToConsumer
) {
}
