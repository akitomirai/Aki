package edu.jxust.agritrace.module.batch.vo;

public record ProductOptionVO(
        Long id,
        Long companyId,
        String name,
        String category,
        String originPlace,
        String specification,
        String unit,
        String imageUrl
) {
}
