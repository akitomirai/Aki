package edu.jxust.agritrace.module.log.dto;

public record OperationLogRecord(
        Long operatorUserId,
        String operatorName,
        String roleCode,
        Long companyId,
        String actionType,
        String targetType,
        Long targetId,
        String targetName,
        String result,
        String summary
) {
}
