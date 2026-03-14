package edu.jxust.agritrace.module.qr.dto;

import lombok.Data;

/**
 * 扫码查询 DTO
 */
@Data
public class QrQueryScanDTO {

    /**
     * 二维码 token
     */
    private String token;
}