package edu.jxust.agritrace.module.qr.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 前台扫码返回的批次简要信息
 */
@Data
public class PublicBatchSimpleVO {

    private Long id;
    private String batchCode;
    private String productName;
    private String productCategory;
    private String productSpec;
    private String productUnit;
    private String companyName;
    private String originPlace;
    private LocalDate startDate;
    private String status;
    private String regulationStatus;
    private String publicRemark;
}