package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;

public record ScanRecordEntity(
        LocalDateTime scanTime,
        String ip,
        String userAgent,
        String referer
) {
}
