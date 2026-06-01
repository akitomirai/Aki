package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanySaveRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 64) String licenseNo,
        @NotBlank @Size(max = 64) String contactPerson,
        @NotBlank @Size(max = 32) String contactPhone,
        @NotBlank @Size(max = 255) String address,
        @Size(max = 20) String status
) {
}
