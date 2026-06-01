package edu.jxust.agritrace.module.dashboard.vo;

import java.util.List;

public record QrScanAnalysisVO(
        long totalScanCount,
        long todayScanCount,
        List<QrScanTrendPointVO> sevenDayTrend,
        List<HotTraceQueryVO> hotTraceCodes,
        List<TraceCodeStatusQueryVO> statusQueryCounts
) {
}
