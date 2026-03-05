package edu.jxust.agritrace.module.trace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 批次实体（对应表：trace_batch）
 * 说明：这里只放最少字段，用于快速验证数据库连通性。
 */
@Data
@TableName("trace_batch")
public class TraceBatch {
    /** 主键 */
    private Long id;
}