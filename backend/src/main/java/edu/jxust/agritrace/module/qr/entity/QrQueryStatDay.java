package edu.jxust.agritrace.module.qr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 二维码日统计（表：qr_query_stat_day）
 * 用于趋势展示与报表，避免每次都扫大日志表。
 */
@Data
@TableName("qr_query_stat_day")
public class QrQueryStatDay {
    private Long id;
    private Long qrId;
    private LocalDate day;
    private Long pv;
    private Long uv;
}