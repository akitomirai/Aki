package edu.jxust.agritrace.module.batch.vo;

public record CompanyAdminVO(
        Long id,
        String name,
        String licenseNo,
        String contactPerson,
        String contactPhone,
        String address,
        String status,
        String statusLabel,
        long productCount,
        long batchCount,
        boolean canDelete
) {
}
