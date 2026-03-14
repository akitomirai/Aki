package edu.jxust.agritrace.module.qr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生成二维码 DTO
 */
@Data
public class QrGenerateDTO {

    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 备注
     */
    private String remark;
}