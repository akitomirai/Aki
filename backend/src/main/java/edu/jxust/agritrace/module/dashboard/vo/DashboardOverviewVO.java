package edu.jxust.agritrace.module.dashboard.vo;

import java.util.List;

public record DashboardOverviewVO(
        int totalBatches,
        int publishedBatches,
        int draftBatches,
        int riskBatches,
        String coreFlowMessage,
        List<String> currentFocus
) {
}
