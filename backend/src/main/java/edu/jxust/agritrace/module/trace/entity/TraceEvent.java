package edu.jxust.agritrace.module.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 溯源事件实体
 * 对应表：trace_event
 */
@Data
@TableName("trace_event")
public class TraceEvent {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    private Long companyId;

    private String bizRole;

    /**
     * 阶段
     * PRODUCE / PROCESS / TRANSPORT / SALE / INSPECT / SYSTEM
     */
    private String stage;

    /**
     * 事件标题
     */
    private String title;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 地点
     */
    private String location;

    /**
     * 来源类型
     * SYSTEM / ADMIN / REGULATOR / SCAN
     */
    private String sourceType;

    /**
     * 是否前台可见：1是 0否
     */
    private Boolean isPublic;

    /**
     * 事件内容 JSON 字符串
     */
    private String contentJson;

    /**
     * 附件 JSON 字符串
     */
    private String attachmentsJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
