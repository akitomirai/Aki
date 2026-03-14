package edu.jxust.agritrace.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.quality.entity.QualityReport;
import edu.jxust.agritrace.module.regulation.entity.RegulationRecord;

import java.util.LinkedHashMap;
import java.util.Map;

public class NotarySnapshotBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private NotarySnapshotBuilder() {
    }

    public static String buildBatchSnapshot(TraceBatch batch) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", batch.getId());
            map.put("batchCode", batch.getBatchCode());
            map.put("productId", batch.getProductId());
            map.put("companyId", batch.getCompanyId());
            map.put("originPlace", batch.getOriginPlace());
            map.put("startDate", batch.getStartDate());
            map.put("status", batch.getStatus());
            map.put("regulationStatus", batch.getRegulationStatus());
            map.put("remark", batch.getRemark());
            map.put("publicRemark", batch.getPublicRemark());
            map.put("internalRemark", batch.getInternalRemark());
            map.put("statusReason", batch.getStatusReason());
            map.put("publishedAt", batch.getPublishedAt());
            map.put("frozenAt", batch.getFrozenAt());
            map.put("recalledAt", batch.getRecalledAt());
            map.put("createdBy", batch.getCreatedBy());
            map.put("updatedBy", batch.getUpdatedBy());
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("构建批次存证快照失败", e);
        }
    }

    public static String buildRegulationSnapshot(RegulationRecord record) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", record.getId());
            map.put("batchId", record.getBatchId());
            map.put("inspectorId", record.getInspectorId());
            map.put("inspectorName", record.getInspectorName());
            map.put("inspectTime", record.getInspectTime());
            map.put("inspectResult", record.getInspectResult());
            map.put("actionTaken", record.getActionTaken());
            map.put("remark", record.getRemark());
            map.put("attachmentUrl", record.getAttachmentUrl());
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("构建监管记录存证快照失败", e);
        }
    }

    public static String buildQualitySnapshot(QualityReport report) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", report.getId());
            map.put("batchId", report.getBatchId());
            map.put("reportNo", report.getReportNo());
            map.put("agency", report.getAgency());
            map.put("result", report.getResult());
            map.put("reportFileUrl", report.getReportFileUrl());
            map.put("reportJson", report.getReportJson());
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("构建质检报告存证快照失败", e);
        }
    }
}
