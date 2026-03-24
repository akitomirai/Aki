package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record ScanStatsSectionVO(
        long pv,
        long uv,
        String lastScanAt,
        List<ScanRecordVO> recentRecords,
        List<ScanTrendPointVO> trend
) {
}
