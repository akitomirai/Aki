package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record QualitySectionVO(
        String status,
        String label,
        String summary,
        int reportCount,
        QualityReportVO latestReport,
        List<QualityReportVO> reports
) {
}
