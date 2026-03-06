package edu.jxust.agritrace.module.trace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次实体（表：trace_batch）
 * 批次是溯源主线：所有事件、二维码、质检用药都关联 batch_id。
 */
@Data
@TableName("trace_batch")
public class TraceBatch {

    /** 主键 */
    private Long id;

    /** 批次编码（唯一） */
    private String batchCode;

    /** 产品ID（关联 base_product.id） */
    private Long productId;

    /** 企业ID（关联 org_company.id） */
    private Long companyId;

    /** 产地 */
    private String originPlace;

    /** 批次开始日期 */
    private LocalDate startDate;

    /** 状态：ACTIVE/FINISHED/DISABLED */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}