package edu.jxust.agritrace.module.qr.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 二维码列表项 DTO（后台详情页展示）
 */
@Data
public class QrListItem {

    private Long id;
    private Long batchId;
    private String qrToken;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private String remark;

    /** 累计 PV */
    private Long pv;

    /** 最新 UV（取今日或最近一次日统计） */
    private Long uv;
}