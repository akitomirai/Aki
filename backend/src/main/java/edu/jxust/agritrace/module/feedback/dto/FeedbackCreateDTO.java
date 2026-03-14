package edu.jxust.agritrace.module.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 消费者提交反馈 DTO
 */
@Data
public class FeedbackCreateDTO {

    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    /**
     * 二维码ID，可为空
     */
    private Long qrId;

    /**
     * 反馈类型：
     * SUGGESTION / COMPLAINT / REPORT_RISK
     */
    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;

    /**
     * 反馈内容
     */
    @NotBlank(message = "反馈内容不能为空")
    private String content;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;
}