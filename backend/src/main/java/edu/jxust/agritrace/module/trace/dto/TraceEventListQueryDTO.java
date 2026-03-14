package edu.jxust.agritrace.module.trace.dto;

import lombok.Data;

/**
 * 溯源事件列表查询 DTO
 */
@Data
public class TraceEventListQueryDTO {

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 阶段
     */
    private String stage;

    /**
     * 来源类型
     */
    private String sourceType;
}