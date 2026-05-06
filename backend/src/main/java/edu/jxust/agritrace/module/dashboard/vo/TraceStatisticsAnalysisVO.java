package edu.jxust.agritrace.module.dashboard.vo;

import java.util.List;

public record TraceStatisticsAnalysisVO(
        double publishRate,
        double qualityPassRate,
        double riskRate,
        long pendingActionTotal,
        String analysisText,
        List<ManagementTipVO> managementTips
) {
}
