package edu.jxust.agritrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.trace")
public class TraceProperties {

    private String publicBaseUrl = "http://127.0.0.1:5173";
    private String backendBaseUrl = "http://127.0.0.1:8080";
    private String qrStorageDir = "storage/qr";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getBackendBaseUrl() {
        return backendBaseUrl;
    }

    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }

    public String getQrStorageDir() {
        return qrStorageDir;
    }

    public void setQrStorageDir(String qrStorageDir) {
        this.qrStorageDir = qrStorageDir;
    }
}
