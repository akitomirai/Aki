package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String realName,
        @NotBlank String roleCode,
        Long companyId
) {
}
