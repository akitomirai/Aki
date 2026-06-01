package edu.jxust.agritrace.module.publictrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublicFeedbackHandleRequest(
        @NotBlank(message = "不能为空")
        @Pattern(regexp = "PENDING|PROCESSING|CLOSED", message = "只能是 PENDING、PROCESSING 或 CLOSED")
        String status,
        @Size(max = 200, message = "长度不能超过 200 个字")
        String handleResult
) {
}
