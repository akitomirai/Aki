package edu.jxust.agritrace.module.dashboard.vo;

import java.time.LocalDateTime;
import java.util.List;

public record DataBackupSnapshotVO(
        String backupNo,
        LocalDateTime generatedAt,
        String generatedBy,
        String scope,
        long companyTotal,
        long productTotal,
        long batchTotal,
        long qualityReportTotal,
        long qrCodeTotal,
        long traceEventTotal,
        long qrQueryTotal,
        List<BackupBatchItemVO> batches
) {
}
