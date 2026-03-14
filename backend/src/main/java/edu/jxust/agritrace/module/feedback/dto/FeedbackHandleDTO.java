package edu.jxust.agritrace.module.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 后台处理反馈 DTO
 */
@Data
public class FeedbackHandleDTO {

    /**
     * 反馈ID
     */
    @NotNull(message = "反馈ID不能为空")
    private Long id;

    /**
     * 处理状态：
     * PROCESSING / CLOSED / REJECTED
     */
    @NotBlank(message = "处理状态不能为空")
    private String status;

    /**
     * 处理结果
     */
    private String handleResult;
}