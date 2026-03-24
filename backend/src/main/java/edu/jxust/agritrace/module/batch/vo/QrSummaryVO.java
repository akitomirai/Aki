package edu.jxust.agritrace.module.batch.vo;

public record QrSummaryVO(
        Long id,
        String token,
        String status,
        String publicUrl,
        String generatedAt,
        String lastScanAt,
        boolean generated
) {
}
