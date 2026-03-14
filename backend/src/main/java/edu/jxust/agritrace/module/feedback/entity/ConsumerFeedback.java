package edu.jxust.agritrace.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费者反馈实体
 * 对应表：consumer_feedback
 */
@Data
@TableName("consumer_feedback")
public class ConsumerFeedback {

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
     * 二维码ID
     */
    private Long qrId;

    /**
     * 反馈类型：
     * SUGGESTION / COMPLAINT / REPORT_RISK
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
     * 来源渠道：
     * SCAN_PAGE / MINI_PROGRAM / APP / WEB / OTHER
     */
    private String sourceChannel;

    /**
     * 处理状态：
     * PENDING / PROCESSING / CLOSED / REJECTED
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

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}