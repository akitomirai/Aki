package edu.jxust.agritrace.module.batch.vo;

public record TraceChainVerificationVO(
        boolean passed,
        String statusLabel,
        String message,
        int totalRecords,
        Long failedRecordId,
        String latestHash,
        String checkedAt
) {
}
