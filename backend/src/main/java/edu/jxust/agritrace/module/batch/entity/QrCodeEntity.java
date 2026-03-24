package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;

public record QrCodeEntity(
        Long id,
        String token,
        String status,
        String publicUrl,
        LocalDateTime generatedAt,
        LocalDateTime lastScanAt
) {
}
