package edu.jxust.agritrace.module.batch.dto;

import java.util.List;

public record FieldDraftSaveRequest(
        String stage,
        String title,
        String eventTime,
        String operatorName,
        String location,
        String summary,
        String imageUrl,
        List<Long> attachmentIds,
        List<FieldDraftFileItemDTO> uploadedFiles,
        Boolean visibleToConsumer
) {
}
