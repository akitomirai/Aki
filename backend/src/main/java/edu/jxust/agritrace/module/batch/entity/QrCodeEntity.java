package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;

public record QrCodeEntity(
        Long id,
        String token,
        String status,
        String publicUrl,
        String imageUrl,
        LocalDateTime generatedAt,
        LocalDateTime lastScanAt,
        long pv,
        long uv
) {
}
