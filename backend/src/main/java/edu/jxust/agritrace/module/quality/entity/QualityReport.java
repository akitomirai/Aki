package edu.jxust.agritrace.module.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质检报告实体（表：quality_report）
 */
@Data
@TableName("quality_report")
public class QualityReport {

    private Long id;
    private Long batchId;

    /** 报告编号 */
    private String reportNo;

    /** 检测机构 */
    private String agency;

    /** 结果：合格/不合格 */
    private String result;

    /** 报告文件URL（来自上传接口返回的 url） */
    private String reportFileUrl;

    /** 报告扩展 JSON（可选） */
    private String reportJson;

    private LocalDateTime createdAt;
}