package edu.jxust.agritrace.module.qr.dto;

import lombok.Data;

/**
 * PV 趋势点（按天）
 */
@Data
public class QrPvTrendPoint {
    /** 日期：yyyy-MM-dd */
    private String day;
    /** 当天 PV（次数） */
    private Long pv;

    private Long uv;
}