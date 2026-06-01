package edu.jxust.agritrace.module.dashboard.vo;

public record HotTraceQueryVO(
        String traceToken,
        String batchNo,
        String productName,
        long queryCount
) {
}
