package edu.jxust.agritrace.module.log.service.support;

import java.util.Locale;

public final class OperationLogLabels {

    private OperationLogLabels() {
    }

    public static String actionLabel(String actionType) {
        return switch (normalize(actionType, "")) {
            case "AUTH_LOGIN_SUCCESS" -> "登录成功";
            case "USER_CREATE" -> "新建用户";
            case "USER_UPDATE" -> "编辑用户";
            case "USER_ENABLE" -> "启用用户";
            case "USER_DISABLE" -> "停用用户";
            case "USER_RESET_PASSWORD" -> "重置密码";
            case "USER_ACCESS_DENIED" -> "用户管理越权拒绝";
            case "COMPANY_ACCESS_DENIED" -> "企业资料越权拒绝";
            case "PRODUCT_ACCESS_DENIED" -> "产品资料越权拒绝";
            case "LOG_ACCESS_DENIED" -> "日志访问越权拒绝";
            case "BATCH_ACCESS_DENIED" -> "批次访问被拒绝";
            case "BATCH_EDIT_DENIED" -> "批次编辑被拒绝";
            case "BATCH_ASSIGN" -> "分配操作员";
            case "BATCH_REASSIGN" -> "改派操作员";
            case "BATCH_UNASSIGN" -> "清空分配";
            case "BATCH_ASSIGN_DENIED" -> "任务分配被拒绝";
            case "QUALITY_UPLOAD" -> "上传质检";
            case "QUALITY_UPLOAD_DENIED" -> "质检上传被拒绝";
            case "QR_GENERATE" -> "生成二维码";
            case "QR_PUBLISH_DENIED" -> "二维码/发布操作被拒绝";
            case "BATCH_PUBLISH" -> "发布批次";
            case "RISK_FREEZE" -> "冻结批次";
            case "RISK_COMMENT" -> "补处理说明";
            case "RISK_RECTIFICATION" -> "补整改记录";
            case "RISK_PROCESSING" -> "标记处理中";
            case "RISK_RECTIFIED" -> "标记已整改";
            case "RISK_RESUME_PUBLISH" -> "恢复发布";
            case "RISK_ACTION_DENIED" -> "风险处理被拒绝";
            case "TRACE_RECORD_SUBMIT" -> "提交现场记录";
            case "TRACE_RECORD_DENIED" -> "现场记录提交被拒绝";
            case "TRACE_IMAGE_UPLOAD" -> "图片上传成功";
            default -> "系统操作";
        };
    }

    public static String targetTypeLabel(String targetType) {
        return switch (normalize(targetType, "")) {
            case "USER" -> "用户";
            case "COMPANY" -> "企业";
            case "PRODUCT" -> "产品";
            case "BATCH" -> "批次";
            case "QUALITY" -> "质检";
            case "QR" -> "二维码";
            case "TRACE_RECORD" -> "现场记录";
            case "ATTACHMENT" -> "图片";
            case "AUTH" -> "登录账号";
            case "LOG" -> "操作日志";
            default -> "系统对象";
        };
    }

    public static String resultLabel(String result) {
        return "FAILED".equals(normalize(result, "SUCCESS")) ? "失败" : "成功";
    }

    public static String roleName(String roleCode) {
        return switch (normalize(roleCode, "")) {
            case "PLATFORM_ADMIN" -> "平台管理员";
            case "ENTERPRISE_ADMIN" -> "企业管理员";
            case "OPERATOR" -> "现场操作员";
            case "REGULATOR" -> "监管人员";
            default -> "系统用户";
        };
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
