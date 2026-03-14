package edu.jxust.agritrace.module.batch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次实体
 * 对应表：trace_batch
 */
@Data
@TableName("trace_batch")
public class TraceBatch {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次编码，唯一
     */
    private String batchCode;

    /**
     * 产品ID，关联 base_product.id
     */
    private Long productId;

    /**
     * 企业ID，关联 org_company.id
     */
    private Long companyId;

    /**
     * 产地
     */
    private String originPlace;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 批次状态：DRAFT/ACTIVE/FROZEN/RECALLED...
     */
    private String status;

    /**
     * 监管状态：NONE/NORMAL/PENDING_RECTIFY/RISK/RECALLED...
     */
    private String regulationStatus;

    /**
     * 普通备注
     */
    private String remark;

    /**
     * 对外说明
     */
    private String publicRemark;

    /**
     * 内部备注
     */
    private String internalRemark;

    /**
     * 状态变更原因
     */
    private String statusReason;

    /**
     * 启用时间
     */
    private LocalDateTime publishedAt;

    /**
     * 冻结时间
     */
    private LocalDateTime frozenAt;

    /**
     * 召回时间
     */
    private LocalDateTime recalledAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;
}