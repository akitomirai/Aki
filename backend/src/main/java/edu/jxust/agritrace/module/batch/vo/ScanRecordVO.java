package edu.jxust.agritrace.module.batch.vo;

public record ScanRecordVO(
        String scanTime,
        String ip,
        String device,
        String referer
) {
}
