package edu.jxust.agritrace.module.batch.vo;

public record FileAssetVO(
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
