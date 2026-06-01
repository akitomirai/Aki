package edu.jxust.agritrace.module.dashboard.vo;

public record BackupBatchItemVO(
        Long batchId,
        String batchCode,
        String productName,
        String companyName,
        String originPlace,
        String status,
        String statusLabel,
        String qualityResult,
        String qrToken,
        long queryCount,
        String publicTraceUrl
) {
}
