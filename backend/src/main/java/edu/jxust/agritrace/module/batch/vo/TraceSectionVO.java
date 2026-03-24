package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record TraceSectionVO(
        int totalCount,
        String latestRecordedAt,
        String quickEntryHint,
        List<TraceRecordVO> recentRecords
) {
}
