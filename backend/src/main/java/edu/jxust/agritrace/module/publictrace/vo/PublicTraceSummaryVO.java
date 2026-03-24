package edu.jxust.agritrace.module.publictrace.vo;

public record PublicTraceSummaryVO(
        String productName,
        String batchCode,
        String companyName,
        String originPlace,
        String statusLabel,
        String qualityLabel,
        String publishedAt,
        String slogan
) {
}
