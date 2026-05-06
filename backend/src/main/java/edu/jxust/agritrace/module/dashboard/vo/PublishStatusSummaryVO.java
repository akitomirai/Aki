package edu.jxust.agritrace.module.dashboard.vo;

public record PublishStatusSummaryVO(
        long published,
        long unpublished,
        long pendingQuality,
        long riskProcessing,
        long recalledOrFrozen
) {
}
