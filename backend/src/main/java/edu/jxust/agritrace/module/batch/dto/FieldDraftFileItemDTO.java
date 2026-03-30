package edu.jxust.agritrace.module.batch.dto;

public record FieldDraftFileItemDTO(
        Long id,
        String fileName,
        String filePath,
        String fileUrl,
        String contentType,
        Long size,
        String businessType,
        Long businessId
) {
}
