package edu.jxust.agritrace.module.auth.vo;

public record UserAdminVO(
        Long id,
        String username,
        String realName,
        String roleCode,
        String roleName,
        Long companyId,
        String companyName,
        Integer status,
        String statusLabel,
        Boolean needChangePassword,
        String passwordStatusLabel,
        String passwordUpdatedAt,
        String updatedAt
) {
}
