package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QualityReportCreateRequest(
        @NotBlank @Size(max = 64) String reportNo,
        @NotBlank @Size(max = 80) String agency,
        @NotBlank @Pattern(regexp = "PASS|FAIL|REVIEW", message = "result must be PASS, FAIL or REVIEW") String result,
        @Size(max = 32) String reportTime,
        @Size(max = 20, message = "highlights can contain up to 20 items") List<@Size(max = 120) String> highlights,
        @Size(max = 10, message = "attachmentIds can contain up to 10 items") List<Long> attachmentIds
) {
}
