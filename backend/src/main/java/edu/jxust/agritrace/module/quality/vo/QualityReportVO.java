package edu.jxust.agritrace.module.quality.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质检报告 VO
 */
@Data
public class QualityReportVO {

    private Long id;

    private Long batchId;

    private String reportNo;

    private String agency;

    private String result;

    private String reportFileUrl;

    private String reportJson;

    private LocalDateTime createdAt;
}