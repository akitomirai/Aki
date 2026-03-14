package edu.jxust.agritrace.module.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批次状态动作 DTO
 * 用于冻结、召回等动作
 */
@Data
public class BatchStatusActionDTO {

    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    /**
     * 原因
     */
    @NotBlank(message = "原因不能为空")
    private String reason;
}