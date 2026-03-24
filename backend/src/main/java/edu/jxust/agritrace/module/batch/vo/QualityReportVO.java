package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record QualityReportVO(
        Long id,
        String reportNo,
        String agency,
        String result,
        String reportTime,
        List<String> highlights
) {
}
