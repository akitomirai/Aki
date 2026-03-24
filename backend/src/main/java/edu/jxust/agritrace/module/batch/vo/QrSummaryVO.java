package edu.jxust.agritrace.module.batch.vo;

public record QrSummaryVO(
        Long id,
        String token,
        String status,
        String publicUrl,
        String imageUrl,
        String generatedAt,
        String lastScanAt,
        long pv,
        long uv,
        boolean generated
) {
}
