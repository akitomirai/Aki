package edu.jxust.agritrace.module.publictrace.vo;

import java.util.List;

public record PublicQualityVO(
        String status,
        String resultLabel,
        String summary,
        String agency,
        String reportNo,
        String reportTime,
        List<String> highlights
) {
}
