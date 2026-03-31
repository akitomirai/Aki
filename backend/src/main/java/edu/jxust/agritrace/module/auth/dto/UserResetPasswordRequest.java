package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UserResetPasswordRequest(
        @NotBlank String newPassword
) {
}
