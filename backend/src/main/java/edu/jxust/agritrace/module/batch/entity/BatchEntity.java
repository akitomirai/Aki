package edu.jxust.agritrace.module.batch.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BatchEntity {

    private Long id;
    private String batchCode;
    private ProductEntity product;
    private CompanyEntity company;
    private String originPlace;
    private LocalDate productionDate;
    private BatchStatus status;
    private String statusReason;
    private String publicRemark;
    private String internalRemark;
    private String currentNode;
    private LocalDateTime publishedAt;
    private LocalDateTime frozenAt;
    private LocalDateTime recalledAt;
    private final List<TraceRecordEntity> traceRecords = new ArrayList<>();
    private final List<QualityReportEntity> qualityReports = new ArrayList<>();
    private final List<StatusHistoryEntity> statusHistory = new ArrayList<>();
    private final List<BatchRiskActionEntity> riskActions = new ArrayList<>();
    private final List<ScanRecordEntity> scanRecords = new ArrayList<>();
    private QrCodeEntity qrCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public CompanyEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyEntity company) {
        this.company = company;
    }

    public String getOriginPlace() {
        return originPlace;
    }

    public void setOriginPlace(String originPlace) {
        this.originPlace = originPlace;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getPublicRemark() {
        return publicRemark;
    }

    public void setPublicRemark(String publicRemark) {
        this.publicRemark = publicRemark;
    }

    public String getInternalRemark() {
        return internalRemark;
    }

    public void setInternalRemark(String internalRemark) {
        this.internalRemark = internalRemark;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getFrozenAt() {
        return frozenAt;
    }

    public void setFrozenAt(LocalDateTime frozenAt) {
        this.frozenAt = frozenAt;
    }

    public LocalDateTime getRecalledAt() {
        return recalledAt;
    }

    public void setRecalledAt(LocalDateTime recalledAt) {
        this.recalledAt = recalledAt;
    }

    public List<TraceRecordEntity> getTraceRecords() {
        return traceRecords;
    }

    public List<QualityReportEntity> getQualityReports() {
        return qualityReports;
    }

    public List<StatusHistoryEntity> getStatusHistory() {
        return statusHistory;
    }

    public List<BatchRiskActionEntity> getRiskActions() {
        return riskActions;
    }

    public List<ScanRecordEntity> getScanRecords() {
        return scanRecords;
    }

    public QrCodeEntity getQrCode() {
        return qrCode;
    }

    public void setQrCode(QrCodeEntity qrCode) {
        this.qrCode = qrCode;
    }
}
