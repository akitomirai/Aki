package edu.jxust.agritrace.module.batch.dto;

import edu.jxust.agritrace.module.batch.entity.RiskActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchRiskActionCreateRequest(
        @NotNull RiskActionType actionType,
        @Size(max = 500) String reason,
        @Size(max = 500) String comment,
        @NotBlank @Size(max = 32) String operatorName
) {
}
