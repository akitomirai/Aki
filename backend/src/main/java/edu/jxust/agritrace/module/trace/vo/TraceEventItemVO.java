package edu.jxust.agritrace.module.trace.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台事件列表项 VO
 */
@Data
public class TraceEventItemVO {

    private Long id;
    private Long batchId;
    private String stage;
    private String title;
    private LocalDateTime eventTime;
    private Long operatorId;
    private String operatorName;
    private String location;
    private String sourceType;
    private Boolean isPublic;
    private String contentJson;
    private String attachmentsJson;
    private LocalDateTime createdAt;
}