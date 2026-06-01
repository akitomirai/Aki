package edu.jxust.agritrace.module.batch.vo;

public record ProductSummaryVO(
        Long id,
        Long companyId,
        String name,
        String productCode,
        String category,
        String specification,
        String unit,
        String imageUrl
) {
}
