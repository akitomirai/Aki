package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;

public record BatchUpdateRequest(
        @NotBlank String productName,
        @NotBlank String category,
        @NotBlank String companyName,
        @NotBlank String originPlace,
        @NotBlank String productionDate,
        String publicRemark,
        String internalRemark
) {
}
