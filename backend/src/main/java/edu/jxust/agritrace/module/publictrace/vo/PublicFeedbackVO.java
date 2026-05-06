package edu.jxust.agritrace.module.publictrace.vo;

public record PublicFeedbackVO(
        Long id,
        String productName,
        String batchNo,
        String traceCode,
        String feedbackType,
        String contact,
        String content,
        String status,
        String handleResult,
        String handlerName,
        String handledAt,
        String createdAt,
        String receivedAt,
        String companyName
) {
}
