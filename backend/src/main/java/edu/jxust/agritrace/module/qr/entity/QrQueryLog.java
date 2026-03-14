package edu.jxust.agritrace.module.qr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 二维码扫码日志实体
 * 对应表：qr_query_log
 */
@Data
@TableName("qr_query_log")
public class QrQueryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long qrId;
    private Long batchId;
    private LocalDateTime queryTime;
    private String ip;
    private String ua;
    private String geoCountry;
    private String geoProvince;
    private String geoCity;
    private String geoLng;
    private String geoLat;
    private String referer;
}