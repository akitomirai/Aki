package edu.jxust.agritrace.module.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增监管记录 DTO
 */
@Data
public class RegulationCreateDTO {

    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    /**
     * 检查时间
     */
    @NotNull(message = "检查时间不能为空")
    private LocalDateTime inspectTime;

    /**
     * 检查结论
     */
    @NotBlank(message = "检查结论不能为空")
    private String inspectResult;

    /**
     * 处理措施
     */
    private String actionTaken;

    /**
     * 备注
     */
    private String remark;

    /**
     * 附件地址
     */
    private String attachmentUrl;
}