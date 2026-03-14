package edu.jxust.agritrace.module.quality.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改质检报告
 */
@Data
public class QualityReportUpdateDTO {

    @NotNull(message = "质检报告ID不能为空")
    private Long id;

    private String reportNo;

    private String agency;

    /**
     * PASS / FAIL（当前表结构不做强约束）
     */
    private String result;

    private String reportFileUrl;

    private String reportJson;
}

