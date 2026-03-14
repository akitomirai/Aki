package edu.jxust.agritrace.module.qr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 二维码实体
 * 对应表：qr_code
 */
@Data
@TableName("qr_code")
public class QrCode {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 二维码令牌
     */
    private String qrToken;

    /**
     * 状态：ACTIVE / DISABLED / EXPIRED
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态原因
     */
    private String statusReason;

    /**
     * 生成人ID
     */
    private Long generatedBy;

    /**
     * 最后扫码时间
     */
    private LocalDateTime lastQueryAt;

    /**
     * 浏览量
     */
    private Long pv;
}