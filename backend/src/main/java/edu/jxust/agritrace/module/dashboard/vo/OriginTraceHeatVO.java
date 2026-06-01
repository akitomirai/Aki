package edu.jxust.agritrace.module.dashboard.vo;

public record OriginTraceHeatVO(
        String originPlace,
        long batchCount,
        long queryCount,
        double heatRate
) {
}
