package edu.jxust.agritrace.module.quality.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增质检报告
 */
@Data
public class QualityReportCreateDTO {

    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    private String reportNo;

    private String agency;

    /**
     * PASS / FAIL
     */
    private String result;

    private String reportFileUrl;

    private String reportJson;
}