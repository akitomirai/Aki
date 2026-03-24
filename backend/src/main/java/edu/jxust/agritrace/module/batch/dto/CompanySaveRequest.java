package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanySaveRequest(
        @NotBlank String name,
        String licenseNo,
        @NotBlank String contactPerson,
        @NotBlank String contactPhone,
        @NotBlank String address,
        String status
) {
}
