package edu.jxust.agritrace.module.batch.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次分页项 VO
 */
@Data
public class BatchPageItemVO {

    private Long id;
    private String batchCode;

    private Long productId;
    private String productName;

    private Long companyId;
    private String originPlace;
    private LocalDate startDate;

    private String status;
    private String regulationStatus;

    private String publicRemark;

    private LocalDateTime createdAt;
}