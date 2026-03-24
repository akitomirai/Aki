package edu.jxust.agritrace.module.batch.vo;

public record QrInfoVO(
        Long id,
        String token,
        String status,
        String publicUrl,
        String generatedAt,
        String lastScanAt
) {
}
