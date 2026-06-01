package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserResetPasswordRequest(
        @NotBlank @Size(max = 128) String newPassword
) {
}
