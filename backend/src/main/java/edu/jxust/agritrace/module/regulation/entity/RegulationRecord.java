package edu.jxust.agritrace.module.regulation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监管记录实体
 * 对应表：regulation_record
 */
@Data
@TableName("regulation_record")
public class RegulationRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 检查人ID
     */
    private Long inspectorId;

    /**
     * 检查人姓名
     */
    private String inspectorName;

    /**
     * 检查时间
     */
    private LocalDateTime inspectTime;

    /**
     * 检查结论
     */
    private String inspectResult;

    /**
     * 处理措施
     */
    private String actionTaken;

    /**
     * 备注
     */
    private String remark;

    /**
     * 附件地址
     */
    private String attachmentUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}