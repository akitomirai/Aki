package edu.jxust.agritrace.module.batch.entity;

public record FileAssetEntity(
        Long id,
        String fileName,
        String filePath,
        String fileUrl,
        String contentType,
        long size,
        String businessType,
        Long businessId
) {
}
