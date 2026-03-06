package edu.jxust.agritrace.module.trace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 溯源事件实体（表：trace_event）
 * 说明：数据库字段是 JSON 类型，但我们在 Java 层用 String 存储 JSON 字符串最稳。
 */
@Data
@TableName("trace_event")
public class TraceEvent {

    private Long id;
    private Long batchId;
    private String stage;
    private LocalDateTime eventTime;
    private Long operatorId;
    private String location;

    /** 对应 content_json（JSON 字符串） */
    private String contentJson;

    /** 对应 attachments_json（JSON 字符串） */
    private String attachmentsJson;

    private LocalDateTime createdAt;
}