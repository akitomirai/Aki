package edu.jxust.agritrace.module.batch.entity;

public record CompanyEntity(
        Long id,
        String name,
        String licenseNo,
        String contactName,
        String contactPhone,
        String address
) {
}
