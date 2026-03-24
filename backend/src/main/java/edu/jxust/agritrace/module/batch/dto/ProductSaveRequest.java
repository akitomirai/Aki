package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductSaveRequest(
        @NotNull Long companyId,
        @NotBlank String productName,
        String productCode,
        @NotBlank String category,
        @NotBlank String originPlace,
        String coverImage,
        String specification,
        String unit,
        String status
) {
}
