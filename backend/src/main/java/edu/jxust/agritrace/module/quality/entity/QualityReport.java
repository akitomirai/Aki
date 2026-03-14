package edu.jxust.agritrace.module.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质检报告实体
 * 对应表 quality_report
 */
@Data
@TableName("quality_report")
public class QualityReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 报告编号
     */
    private String reportNo;

    /**
     * 检测机构
     */
    private String agency;

    /**
     * 检测结果
     * PASS / FAIL
     */
    private String result;

    /**
     * 报告文件地址
     */
    private String reportFileUrl;

    /**
     * 报告JSON
     */
    private String reportJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}