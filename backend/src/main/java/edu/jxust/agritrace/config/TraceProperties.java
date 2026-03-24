package edu.jxust.agritrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.trace")
public class TraceProperties {

    private String publicBaseUrl = "http://127.0.0.1:5173";
    private String backendBaseUrl = "http://127.0.0.1:8080";
    private String qrStorageDir = "storage/qr";
    private String attachmentStorageDir = "storage/attachments";
    private DataSize traceImageMaxSize = DataSize.ofMegabytes(5);
    private DataSize qualityAttachmentMaxSize = DataSize.ofMegabytes(10);
    private int attachmentOrphanCleanupHours = 24;

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

    public String getAttachmentStorageDir() {
        return attachmentStorageDir;
    }

    public void setAttachmentStorageDir(String attachmentStorageDir) {
        this.attachmentStorageDir = attachmentStorageDir;
    }

    public DataSize getTraceImageMaxSize() {
        return traceImageMaxSize;
    }

    public void setTraceImageMaxSize(DataSize traceImageMaxSize) {
        this.traceImageMaxSize = traceImageMaxSize;
    }

    public DataSize getQualityAttachmentMaxSize() {
        return qualityAttachmentMaxSize;
    }

    public void setQualityAttachmentMaxSize(DataSize qualityAttachmentMaxSize) {
        this.qualityAttachmentMaxSize = qualityAttachmentMaxSize;
    }

    public int getAttachmentOrphanCleanupHours() {
        return attachmentOrphanCleanupHours;
    }

    public void setAttachmentOrphanCleanupHours(int attachmentOrphanCleanupHours) {
        this.attachmentOrphanCleanupHours = attachmentOrphanCleanupHours;
    }
}
