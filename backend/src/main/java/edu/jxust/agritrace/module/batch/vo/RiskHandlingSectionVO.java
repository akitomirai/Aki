package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record RiskHandlingSectionVO(
        String currentStage,
        String currentStageLabel,
        boolean canResume,
        List<BatchRiskActionVO> history
) {
}
