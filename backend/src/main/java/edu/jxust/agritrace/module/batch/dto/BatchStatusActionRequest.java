package edu.jxust.agritrace.module.batch.dto;

import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BatchStatusActionRequest(
        @NotNull BatchStatus targetStatus,
        @NotBlank String reason,
        @NotBlank String operatorName
) {
}
