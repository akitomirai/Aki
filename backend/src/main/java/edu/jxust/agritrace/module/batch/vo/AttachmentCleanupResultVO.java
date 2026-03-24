package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record AttachmentCleanupResultVO(
        long cleanedCount,
        List<Long> cleanedIds,
        long failedCount,
        List<Long> failedIds
) {
}
