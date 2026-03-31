package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String realName,
        @NotBlank String roleCode,
        Long companyId
) {
}
