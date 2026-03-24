package edu.jxust.agritrace.module.batch.vo;

public record ProductAdminVO(
        Long id,
        Long companyId,
        String companyName,
        String productName,
        String productCode,
        String category,
        String originPlace,
        String coverImage,
        String specification,
        String unit,
        String status,
        String statusLabel,
        long batchCount,
        boolean canDelete
) {
}
