package edu.jxust.agritrace.module.event.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一事件创建 DTO
 * 用于自动写入 trace_event
 */
@Data
public class EventCreateDTO {

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 阶段
     * PRODUCE / PROCESS / TRANSPORT / SALE / INSPECT / SYSTEM
     */
    private String stage;

    /**
     * 标题
     */
    private String title;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 操作人ID，可为空
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 地点
     */
    private String location;

    /**
     * 来源类型
     * SYSTEM / ADMIN / REGULATOR / SCAN
     */
    private String sourceType;

    /**
     * 是否前台可见
     */
    private Boolean isPublic = true;

    /**
     * 内容 JSON
     */
    private String contentJson;

    /**
     * 附件 JSON
     */
    private String attachmentsJson = "[]";
}