package edu.jxust.agritrace.module.qr.vo;

import lombok.Data;

@Data
public class QrDashboardStatsVO {

    private Long batchCount;
    private Long qrCount;
    private Long pvCount;
    private Long uvCount;
}
