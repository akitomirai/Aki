package edu.jxust.agritrace.module.batch.service.support;

import edu.jxust.agritrace.module.batch.entity.BatchStatus;

import java.util.Locale;

public final class TraceDisplayLabels {

    private TraceDisplayLabels() {
    }

    public static String batchStatus(BatchStatus status) {
        if (status == null) {
            return "草稿";
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case PUBLISHED -> "已发布";
            case FROZEN -> "已冻结";
            case RECALLED -> "已召回";
        };
    }

    public static String taskStatus(String taskStatus) {
        return switch (normalize(taskStatus, "PENDING")) {
            case "DRAFT" -> "草稿待续";
            case "COMPLETED" -> "今日已完成";
            default -> "待处理";
        };
    }

    public static String draftStatus(boolean draftPending) {
        return draftPending ? "草稿待续" : "无草稿";
    }

    public static String riskStatus(String status) {
        return switch (normalize(status, "NORMAL")) {
            case "FROZEN" -> "已冻结";
            case "RECALLED" -> "已召回";
            case "PROCESSING", "RISK_PENDING" -> "风险处理中";
            case "RECTIFIED" -> "已完成整改";
            default -> "当前无风险";
        };
    }

    public static String qualityStatus(String result) {
        return switch (normalize(result, "PENDING")) {
            case "PASS" -> "合格";
            case "FAIL" -> "不合格";
            default -> "待上传";
        };
    }

    public static String qrStatus(String status) {
        return "NOT_GENERATED".equals(normalize(status, "NOT_GENERATED")) ? "待生成" : "已生成";
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
