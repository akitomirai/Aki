package edu.jxust.agritrace.module.qr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 二维码批量生成请求 DTO
 */
@Data
public class QrGenerateRequest {

    /** 批次ID */
    @NotNull(message = "batchId不能为空")
    private Long batchId;

    /** 生成数量 */
    @Min(value = 1, message = "count至少为1")
    private int count = 1;

    /** 过期天数（0表示不过期） */
    private int expireDays = 0;

    /** 备注（可选） */
    private String remark;
}