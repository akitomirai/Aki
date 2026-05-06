package edu.jxust.agritrace.module.dashboard.vo;

public record TracePublishRecordVO(
        Long batchId,
        String batchNo,
        String productName,
        String enterpriseName,
        String qualityStatus,
        String qualityResult,
        String qrStatus,
        String qrStatusLabel,
        String publishStatus,
        String publishStatusLabel,
        String publishTime,
        long queryCount,
        String riskStatus,
        String riskStatusLabel,
        String traceToken,
        String publicUrl
) {
}
