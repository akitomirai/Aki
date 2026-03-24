package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record BatchListItemVO(
        Long id,
        String batchCode,
        String productName,
        String productImageUrl,
        String companyName,
        String status,
        String statusLabel,
        String currentNode,
        String originPlace,
        String productionDate,
        String marketDate,
        String qrStatus,
        String qualityStatus,
        List<BatchActionVO> actions,
        List<String> quickTags
) {
}
