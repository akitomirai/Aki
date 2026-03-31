package edu.jxust.agritrace.module.batch.vo;

public record BatchStatusLogVO(
        String status,
        String statusLabel,
        String reason,
        String operatorName,
        String operatedAt
) {
}
