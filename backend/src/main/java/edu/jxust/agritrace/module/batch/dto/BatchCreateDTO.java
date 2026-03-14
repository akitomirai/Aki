package edu.jxust.agritrace.module.batch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新增批次 DTO
 */
@Data
public class BatchCreateDTO {

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
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
}