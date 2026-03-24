package edu.jxust.agritrace.module.publictrace.vo;

import java.util.List;

public record PublicTraceDetailVO(
        String qrToken,
        PublicTraceSummaryVO summary,
        List<PublicTimelineItemVO> timeline,
        PublicQualityVO quality,
        PublicCompanyVO company,
        PublicExceptionVO exception,
        List<String> consumerTips
) {
}
