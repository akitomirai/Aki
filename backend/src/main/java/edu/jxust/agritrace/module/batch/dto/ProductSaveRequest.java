package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductSaveRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 128) String productName,
        @Size(max = 64) String productCode,
        @NotBlank @Size(max = 64) String category,
        @NotBlank @Size(max = 128) String originPlace,
        @Size(max = 255) String coverImage,
        @Size(max = 64) String specification,
        @Size(max = 16) String unit,
        @Size(max = 20) String status
) {
}
