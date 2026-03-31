package edu.jxust.agritrace.module.batch.vo;

public record OperatorOptionVO(
        Long id,
        String username,
        String realName,
        String roleCode,
        Long companyId,
        String companyName
) {
}
