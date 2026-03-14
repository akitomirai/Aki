package edu.jxust.agritrace.common.security;

import edu.jxust.agritrace.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException("未登录或登录已失效");
        }
        return loginUser;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getRoleCode() {
        return getLoginUser().getRoleCode();
    }

    public static Long getCompanyId() {
        return getLoginUser().getCompanyId();
    }

    public static Long getRegulatorOrgId() {
        return getLoginUser().getRegulatorOrgId();
    }

    public static boolean isPlatformAdmin() {
        return "PLATFORM_ADMIN".equals(getRoleCode());
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getRoleCode());
    }

    public static boolean isEnterpriseUser() {
        return "ENTERPRISE_USER".equals(getRoleCode());
    }

    public static boolean isRegulator() {
        return "REGULATOR".equals(getRoleCode());
    }
}