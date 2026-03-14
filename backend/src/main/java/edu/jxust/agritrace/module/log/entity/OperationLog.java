package edu.jxust.agritrace.module.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 模块
     */
    private String module;

    /**
     * 操作
     */
    private String action;

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 结果状态
     */
    private String resultStatus;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 用户代理
     */
    private String userAgent;

    private LocalDateTime createdAt;
}