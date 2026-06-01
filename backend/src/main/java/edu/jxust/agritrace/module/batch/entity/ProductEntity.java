package edu.jxust.agritrace.module.batch.entity;

public record ProductEntity(
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
