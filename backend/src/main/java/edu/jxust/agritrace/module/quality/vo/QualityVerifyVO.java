package edu.jxust.agritrace.module.quality.vo;

import lombok.Data;

/**
 * 批次质检校验结果（用于只读查询）
 */
@Data
public class QualityVerifyVO {

    private Long batchId;

    /**
     * 是否存在质检报告
     */
    private Boolean hasReport;

    /**
     * 最新报告ID
     */
    private Long latestReportId;

    /**
     * 最新检测结果
     */
    private String latestResult;
}

