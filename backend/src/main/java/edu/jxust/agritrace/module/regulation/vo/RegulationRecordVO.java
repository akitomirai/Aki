package edu.jxust.agritrace.module.regulation.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监管记录 VO
 */
@Data
public class RegulationRecordVO {

    private Long id;
    private Long batchId;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime inspectTime;
    private String inspectResult;
    private String actionTaken;
    private String remark;
    private String attachmentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}