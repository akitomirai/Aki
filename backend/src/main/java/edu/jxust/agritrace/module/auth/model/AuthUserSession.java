package edu.jxust.agritrace.module.auth.model;

public record AuthUserSession(
        Long userId,
        String username,
        String realName,
        String roleCode,
        Long companyId
) {
}
