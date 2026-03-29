package edu.jxust.agritrace.module.batch.service.support;

import edu.jxust.agritrace.config.TraceProperties;
import org.springframework.stereotype.Component;

@Component
public class TraceLinkBuilder {

    private final TraceProperties traceProperties;

    public TraceLinkBuilder(TraceProperties traceProperties) {
        this.traceProperties = traceProperties;
    }

    public String buildPublicTraceUrl(String token) {
        return join(traceProperties.getPublicBaseUrl(), "/t/" + token);
    }

    public String buildQrImageUrl(String token) {
        return join(traceProperties.getBackendBaseUrl(), "/api/public/qr-images/" + token);
    }

    public String buildAttachmentUrl(Long fileId) {
        return join(traceProperties.getBackendBaseUrl(), "/api/public/files/" + fileId);
    }

    private String join(String baseUrl, String path) {
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        return normalizedBase + path;
    }
}
