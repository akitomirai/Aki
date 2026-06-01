package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record FieldDraftVO(
        Long id,
        Long batchId,
        String batchCode,
        String productName,
        String productCode,
        String companyName,
        String currentNode,
        String stage,
        String title,
        String eventTime,
        String operatorName,
        String location,
        String summary,
        String imageUrl,
        List<Long> attachmentIds,
        List<FileAssetVO> uploadedFiles,
        boolean visibleToConsumer,
        int imageCount,
        String updatedAt
) {
}
