package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record TraceTimelineSectionVO(
        int totalCount,
        int phaseCount,
        String currentPhaseLabel,
        String currentStatusLabel,
        List<TraceTimelineItemVO> items
) {
}
