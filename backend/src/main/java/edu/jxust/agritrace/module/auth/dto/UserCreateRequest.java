package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 128) String password,
        @NotBlank @Size(max = 64) String realName,
        @NotBlank @Size(max = 32) String roleCode,
        Long companyId
) {
}
