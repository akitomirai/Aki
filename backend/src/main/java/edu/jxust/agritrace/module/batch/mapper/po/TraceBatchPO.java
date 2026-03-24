package edu.jxust.agritrace.module.batch.mapper.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("trace_batch")
public class TraceBatchPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchCode;
    private Long productId;
    private Long companyId;
    private String originPlace;
    private LocalDate startDate;
    private String status;
    private String publicRemark;
    private String internalRemark;
    private String statusReason;
    private LocalDateTime publishedAt;
    private LocalDateTime frozenAt;
    private LocalDateTime recalledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getOriginPlace() {
        return originPlace;
    }

    public void setOriginPlace(String originPlace) {
        this.originPlace = originPlace;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
