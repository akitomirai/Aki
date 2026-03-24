package edu.jxust.agritrace.module.batch.vo;

public record ProductOptionVO(
        Long id,
        Long companyId,
        String name,
        String category,
        String specification,
        String unit,
        String imageUrl
) {
}
