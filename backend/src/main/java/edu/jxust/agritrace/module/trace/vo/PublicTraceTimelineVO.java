package edu.jxust.agritrace.module.trace.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 前台溯源时间轴 VO
 */
@Data
public class PublicTraceTimelineVO {

    private Long id;
    private String stage;
    private String title;
    private LocalDateTime eventTime;
    private String operatorName;
    private String location;
    private String sourceType;
    private String contentJson;
    private String attachmentsJson;
}