package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 64) String realName,
        @Size(max = 32) String phone
) {
}
