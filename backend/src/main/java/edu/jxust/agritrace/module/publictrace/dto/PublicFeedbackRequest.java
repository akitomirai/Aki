package edu.jxust.agritrace.module.publictrace.dto;

public record PublicFeedbackRequest(
        String productName,
        String batchNo,
        String traceCode,
        String feedbackType,
        String contact,
        String content,
        String createdAt
) {
}
