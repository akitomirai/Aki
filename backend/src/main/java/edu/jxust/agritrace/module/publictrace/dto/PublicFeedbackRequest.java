package edu.jxust.agritrace.module.publictrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicFeedbackRequest(
        @Size(max = 100, message = "长度不能超过 100 个字")
        String productName,
        @Size(max = 100, message = "长度不能超过 100 个字")
        String batchNo,
        @Size(max = 128, message = "长度不能超过 128 个字")
        String traceCode,
        @Size(max = 40, message = "长度不能超过 40 个字")
        String feedbackType,
        @Size(max = 60, message = "长度不能超过 60 个字")
        String contact,
        @NotBlank(message = "不能为空")
        @Size(min = 10, max = 300, message = "长度需在 10 到 300 个字之间")
        String content,
        @Size(max = 64, message = "长度不能超过 64 个字")
        String createdAt
) {
}
