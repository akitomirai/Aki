package edu.jxust.agritrace.module.batch.vo;

public record ProductSummaryVO(
        Long id,
        String name,
        String category,
        String specification,
        String unit,
        String imageUrl
) {
}
