package edu.jxust.agritrace.module.feedback.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费者反馈 VO
 */
@Data
public class FeedbackVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 二维码ID
     */
    private Long qrId;

    /**
     * 反馈类型
     */
    private String feedbackType;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 来源渠道
     */
    private String sourceChannel;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 是否公开展示
     */
    private Boolean isPublic;

    /**
     * 处理人ID
     */
    private Long handledBy;

    /**
     * 处理时间
     */
    private LocalDateTime handledAt;

    /**
     * 处理结果
     */
    private String handleResult;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}