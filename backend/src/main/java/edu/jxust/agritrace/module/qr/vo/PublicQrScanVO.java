package edu.jxust.agritrace.module.qr.vo;

import lombok.Data;

/**
 * 前台扫码查询返回 VO
 */
@Data
public class PublicQrScanVO {

    /**
     * 二维码ID
     */
    private Long qrId;

    /**
     * 二维码状态
     */
    private String qrStatus;

    /**
     * 二维码状态说明
     */
    private String qrStatusReason;

    /**
     * 批次简要信息
     */
    private PublicBatchSimpleVO batch;
}