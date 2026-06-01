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
        return join(effectiveBackendBaseUrl(), "/api/public/qr-images/" + token);
    }

    public String buildAttachmentUrl(Long fileId) {
        return join(effectiveBackendBaseUrl(), "/api/public/files/" + fileId);
    }

    private String effectiveBackendBaseUrl() {
        String backendBaseUrl = normalize(traceProperties.getBackendBaseUrl());
        String publicBaseUrl = normalize(traceProperties.getPublicBaseUrl());
        if ((backendBaseUrl.isBlank() || backendBaseUrl.contains("127.0.0.1") || backendBaseUrl.contains("localhost"))
                && !publicBaseUrl.isBlank()
                && !publicBaseUrl.contains("127.0.0.1")
                && !publicBaseUrl.contains("localhost")) {
            return publicBaseUrl;
        }
        return backendBaseUrl;
    }

    private String join(String baseUrl, String path) {
        String normalizedBase = normalize(baseUrl);
        return normalizedBase + path;
    }

    private String normalize(String baseUrl) {
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        return normalizedBase;
    }
}
