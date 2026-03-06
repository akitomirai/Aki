package edu.jxust.agritrace.module.qr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 二维码实体（表：qr_code）
 */
@Data
@TableName("qr_code")
public class QrCode {

    private Long id;
    private Long batchId;
    private String qrToken;
    private String status;        // ACTIVE/DISABLED
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private String remark;

    /** 累计 PV（推荐加字段） */
    private Long pv;
}