package edu.jxust.agritrace.module.trace.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增溯源事件请求 DTO（方案B）
 * - content：直接接收 JSON 对象
 * - attachments：直接接收 JSON 数组（可选）
 */
@Data
public class EventCreateRequest {

    /** 批次ID */
    @NotNull(message = "batchId不能为空")
    private Long batchId;

    /** 环节：PRODUCE/PROCESS/INSPECT/WAREHOUSE/LOGISTICS/SALE */
    @NotBlank(message = "stage不能为空")
    private String stage;

    /** 事件时间（可不传，后端默认当前时间） */
    private LocalDateTime eventTime;

    /** 地点 */
    private String location;

    /** 事件内容（JSON对象），例如 {"fields":{...}} */
    @NotNull(message = "content不能为空")
    private JsonNode content;

    /** 附件（JSON数组），例如 [{"name":"xx","url":"xx","type":"pdf"}] */
    private JsonNode attachments;
}