package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BatchCreateRequest(
        @NotBlank String batchCode,
        @NotNull Long productId,
        @NotNull Long companyId,
        @NotBlank String originPlace,
        @NotBlank String productionDate,
        String publicRemark,
        String internalRemark
) {
}
