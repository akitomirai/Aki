package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 修改批次 DTO
 */
@Data
public class BatchUpdateDTO {

    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long id;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产地
     */
    private String originPlace;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 批次状态
     */
    private String status;

    /**
     * 监管状态
     */
    private String regulationStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 对外说明
     */
    private String publicRemark;

    /**
     * 内部备注
     */
    private String internalRemark;

    /**
     * 状态变更原因
     */
    private String statusReason;
}