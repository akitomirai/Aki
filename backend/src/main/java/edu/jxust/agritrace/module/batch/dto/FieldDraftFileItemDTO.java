package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.Size;

public record FieldDraftFileItemDTO(
        Long id,
        @Size(max = 255)
        String fileName,
        @Size(max = 500)
        String filePath,
        @Size(max = 500)
        String fileUrl,
        @Size(max = 128)
        String contentType,
        Long size,
        @Size(max = 64)
        String businessType,
        Long businessId
) {
}
