package edu.jxust.agritrace.module.batch.vo;

import java.util.List;

public record BatchWorkbenchVO(
        Long id,
        String batchCode,
        String batchStatus,
        String statusReason,
        String currentNode,
        String productionDate,
        String originPlace,
        String publicRemark,
        String internalRemark,
        String productName,
        String productCategory,
        String companyName,
        String companyLicenseNo,
        String companyContactName,
        String companyContactPhone,
        String companyAddress,
        String qualityStatus,
        QrInfoVO qrInfo,
        List<TraceRecordVO> traceRecords,
        List<QualityReportVO> qualityReports,
        List<BatchStatusLogVO> statusHistory,
        List<BatchActionVO> actions
) {
}
