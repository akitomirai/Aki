package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record BatchListItemVO(
        Long id,
        String batchCode,
        String productName,
        String companyName,
        String status,
        String currentNode,
        String productionDate,
        String publishedAt,
        String qrStatus,
        String qualityStatus,
        List<String> quickTags
) {
}
