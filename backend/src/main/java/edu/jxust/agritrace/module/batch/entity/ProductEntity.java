package edu.jxust.agritrace.module.batch.entity;

public record ProductEntity(
        Long id,
        String name,
        String category,
        String specification,
        String unit
) {
}
