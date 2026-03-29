package edu.jxust.agritrace.module.batch.entity;

public record ProductEntity(
        Long id,
        Long companyId,
        String name,
        String category,
        String specification,
        String unit,
        String imageUrl
) {
}
