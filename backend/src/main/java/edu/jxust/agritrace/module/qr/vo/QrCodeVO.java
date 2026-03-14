package edu.jxust.agritrace.module.qr.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台二维码信息 VO
 */
@Data
public class QrCodeVO {

    private Long id;
    private Long batchId;
    private String qrToken;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private String remark;
    private String statusReason;
    private Long generatedBy;
    private LocalDateTime lastQueryAt;
    private Long pv;
}