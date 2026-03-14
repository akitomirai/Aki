package edu.jxust.agritrace.module.batch.dto;

import lombok.Data;

/**
 * 批次分页查询 DTO
 */
@Data
public class BatchPageQueryDTO {

    /**
     * 当前页码
     */
    private long current = 1;

    /**
     * 每页条数
     */
    private long size = 10;

    /**
     * 批次编码
     */
    private String batchCode;

    /**
     * 批次状态
     */
    private String status;

    /**
     * 监管状态
     */
    private String regulationStatus;

    /**
     * 产品名称（连表查询）
     */
    private String productName;
}