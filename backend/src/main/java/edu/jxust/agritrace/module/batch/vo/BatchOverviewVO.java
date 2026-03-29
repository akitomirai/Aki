package edu.jxust.agritrace.module.batch.vo;

public record BatchOverviewVO(
        Long id,
        String batchCode,
        String originPlace,
        String productionDate,
        String marketDate,
        String publicRemark,
        String internalRemark,
        String coverImageUrl
) {
}
