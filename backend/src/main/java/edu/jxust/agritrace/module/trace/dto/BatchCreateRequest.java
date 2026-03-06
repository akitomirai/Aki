package edu.jxust.agritrace.module.trace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建批次请求 DTO。
 */
@Data
public class BatchCreateRequest {

    /** 产品ID */
    @NotNull(message = "productId不能为空")
    private Long productId;

    /** 企业ID */
    @NotNull(message = "companyId不能为空")
    private Long companyId;

    /** 产地 */
    private String originPlace;

    /** 开始日期 */
    private LocalDate startDate;

    /** 备注 */
    private String remark;
}