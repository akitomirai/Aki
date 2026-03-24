package edu.jxust.agritrace.module.publictrace.vo;

public record PublicTraceSummaryVO(
        String productName,
        String productImageUrl,
        String batchCode,
        String companyName,
        String originPlace,
        String statusLabel,
        String qualityResult,
        String productionDate,
        String marketDate,
        String slogan
) {
}
