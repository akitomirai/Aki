package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchCreateRequest(
        @NotBlank @Size(max = 64) String batchCode,
        @NotNull Long productId,
        @NotNull Long companyId,
        @NotBlank @Size(max = 128) String originPlace,
        @NotBlank @Size(max = 32) String productionDate,
        @Size(max = 500) String publicRemark,
        @Size(max = 1000) String internalRemark
) {
}
