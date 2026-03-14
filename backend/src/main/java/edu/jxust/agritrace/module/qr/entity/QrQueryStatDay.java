package edu.jxust.agritrace.module.qr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 二维码日统计实体
 * 对应表：qr_query_stat_day
 */
@Data
@TableName("qr_query_stat_day")
public class QrQueryStatDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long qrId;
    private LocalDate day;
    private Long pv;
    private Long uv;
}