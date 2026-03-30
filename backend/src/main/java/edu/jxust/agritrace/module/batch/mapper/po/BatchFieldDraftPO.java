package edu.jxust.agritrace.module.batch.mapper.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("batch_field_draft")
public class BatchFieldDraftPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long operatorUserId;
    private String stage;
    private String title;
    private LocalDateTime eventTime;
    private String operatorName;
    private String location;
    private String summary;
    private String imageUrl;
    private String attachmentIdsJson;
    private String uploadedFilesJson;
    @TableField("visible_to_consumer")
    private Boolean visibleToConsumer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAttachmentIdsJson() {
        return attachmentIdsJson;
    }

    public void setAttachmentIdsJson(String attachmentIdsJson) {
        this.attachmentIdsJson = attachmentIdsJson;
    }

    public String getUploadedFilesJson() {
        return uploadedFilesJson;
    }

    public void setUploadedFilesJson(String uploadedFilesJson) {
        this.uploadedFilesJson = uploadedFilesJson;
    }

    public Boolean getVisibleToConsumer() {
        return visibleToConsumer;
    }

    public void setVisibleToConsumer(Boolean visibleToConsumer) {
        this.visibleToConsumer = visibleToConsumer;
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
