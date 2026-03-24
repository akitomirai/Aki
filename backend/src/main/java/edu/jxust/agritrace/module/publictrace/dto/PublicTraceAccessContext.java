package edu.jxust.agritrace.module.publictrace.dto;

public record PublicTraceAccessContext(
        String ip,
        String userAgent,
        String referer
) {

    public static PublicTraceAccessContext anonymous() {
        return new PublicTraceAccessContext(null, null, null);
    }
}
