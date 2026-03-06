package edu.jxust.agritrace.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求工具类：
 * - 获取 IP、UA、Referer
 * 说明：真实线上会有反向代理，这里做常用 header 兼容。
 */
public final class RequestUtil {

    private RequestUtil() {}

    /**
     * 获取客户端 IP（兼容代理头）
     */
    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // XFF 可能是 "ip1, ip2, ip3"
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp;

        return request.getRemoteAddr();
    }

    /** 获取 User-Agent */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /** 获取 Referer */
    public static String getReferer(HttpServletRequest request) {
        return request.getHeader("Referer");
    }
}