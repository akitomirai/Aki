package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDateTime;
import java.util.List;

public record QualityReportEntity(
        Long id,
        String reportNo,
        String agency,
        String result,
        LocalDateTime reportTime,
        List<String> highlights,
        List<FileAssetEntity> attachments
) {
}
