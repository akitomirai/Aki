package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(@NotNull Integer status) {
}
