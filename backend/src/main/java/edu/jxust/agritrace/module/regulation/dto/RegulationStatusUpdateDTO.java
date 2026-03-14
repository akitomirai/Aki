package edu.jxust.agritrace.module.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新监管状态 DTO
 */
@Data
public class RegulationStatusUpdateDTO {

    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    /**
     * 新监管状态
     * NONE / NORMAL / PENDING_RECTIFY / RISK / RECALLED
     */
    @NotBlank(message = "监管状态不能为空")
    private String regulationStatus;

    /**
     * 原因
     */
    @NotBlank(message = "原因不能为空")
    private String reason;
}