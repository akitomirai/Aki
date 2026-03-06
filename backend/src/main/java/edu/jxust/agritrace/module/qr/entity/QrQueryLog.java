package edu.jxust.agritrace.module.qr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 二维码查询日志实体（表：qr_query_log）
 * 用途：记录每次扫码查询的行为数据，为统计/分析提供原始素材。
 */
@Data
@TableName("qr_query_log")
public class QrQueryLog {

    private Long id;
    private Long qrId;
    private Long batchId;
    private LocalDateTime queryTime;

    private String ip;
    private String ua;

    /** 地理信息（后续可用第三方 IP 库填充） */
    private String geoCountry;
    private String geoProvince;
    private String geoCity;
    private String geoLng;
    private String geoLat;

    private String referer;
}