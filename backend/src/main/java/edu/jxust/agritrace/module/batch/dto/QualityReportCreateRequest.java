package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record QualityReportCreateRequest(
        @NotBlank String reportNo,
        @NotBlank String agency,
        @NotBlank String result,
        String reportTime,
        List<String> highlights,
        List<Long> attachmentIds
) {
}
