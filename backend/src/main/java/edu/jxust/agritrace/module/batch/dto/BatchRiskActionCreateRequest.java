package edu.jxust.agritrace.module.batch.dto;

import edu.jxust.agritrace.module.batch.entity.RiskActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BatchRiskActionCreateRequest(
        @NotNull RiskActionType actionType,
        String reason,
        String comment,
        @NotBlank String operatorName
) {
}
