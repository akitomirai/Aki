package edu.jxust.agritrace.module.notary.vo;

import lombok.Data;

@Data
public class NotaryVerifyVO {

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 数据库存储摘要
     */
    private String storedHash;

    /**
     * 当前重算摘要
     */
    private String currentHash;

    /**
     * 是否一致
     */
    private Boolean matched;

    /**
     * 校验结果说明
     */
    private String message;
}