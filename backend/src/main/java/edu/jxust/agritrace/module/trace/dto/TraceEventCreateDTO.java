package edu.jxust.agritrace.module.trace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增溯源事件 DTO
 */
@Data
public class TraceEventCreateDTO {

    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    /**
     * 阶段
     */
    @NotBlank(message = "阶段不能为空")
    private String stage;

    private String bizRole;

    /**
     * 标题
     */
    @NotBlank(message = "事件标题不能为空")
    private String title;

    /**
     * 事件时间
     */
    @NotNull(message = "事件时间不能为空")
    private LocalDateTime eventTime;

    /**
     * 地点
     */
    private String location;

    /**
     * 是否公开展示
     */
    private Boolean isPublic = true;

    /**
     * 事件内容 JSON 字符串
     * 例如：{"fields":{"remark":"春播完成","workType":"播种"}}
     */
    @NotBlank(message = "事件内容不能为空")
    private String contentJson;

    /**
     * 附件 JSON 字符串
     * 例如：[]
     */
    private String attachmentsJson = "[]";
}
