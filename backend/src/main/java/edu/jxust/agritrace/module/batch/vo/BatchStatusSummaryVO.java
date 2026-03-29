package edu.jxust.agritrace.module.batch.vo;

public record BatchStatusSummaryVO(
        String code,
        String label,
        String currentNode,
        String reason,
        String operatorName,
        String changedAt
) {
}
