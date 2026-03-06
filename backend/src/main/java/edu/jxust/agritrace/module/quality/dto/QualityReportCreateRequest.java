package edu.jxust.agritrace.module.quality.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增质检报告请求 DTO（规整版）
 * - reportJson 直接接收 JSON 对象（前端无需转义）
 */
@Data
public class QualityReportCreateRequest {

    @NotNull(message = "batchId不能为空")
    private Long batchId;

    private String reportNo;
    private String agency;

    @NotBlank(message = "result不能为空")
    private String result;

    @NotBlank(message = "reportFileUrl不能为空")
    private String reportFileUrl;

    /** 报告扩展信息：直接 JSON */
    private JsonNode reportJson;
}