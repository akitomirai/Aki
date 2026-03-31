package edu.jxust.agritrace.module.log.vo;

public record OperationLogVO(
        Long id,
        String createdAt,
        Long operatorUserId,
        String operatorName,
        String roleCode,
        String roleName,
        Long companyId,
        String companyName,
        String actionType,
        String actionTypeLabel,
        String targetType,
        String targetTypeLabel,
        Long targetId,
        String targetName,
        String targetDisplay,
        String result,
        String resultLabel,
        String summary
) {
}
