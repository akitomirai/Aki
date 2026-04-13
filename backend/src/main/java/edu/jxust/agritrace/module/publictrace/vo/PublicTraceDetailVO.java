package edu.jxust.agritrace.module.publictrace.vo;

import edu.jxust.agritrace.module.batch.vo.TraceChainVerificationVO;

import java.util.List;

public record PublicTraceDetailVO(
        String qrToken,
        PublicTraceSummaryVO summary,
        List<PublicTimelineItemVO> timeline,
        PublicQualityVO quality,
        PublicCompanyVO company,
        PublicRiskVO risk,
        TraceChainVerificationVO verification,
        List<String> consumerTips
) {
}
