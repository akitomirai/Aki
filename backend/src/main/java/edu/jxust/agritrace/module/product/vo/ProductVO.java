package edu.jxust.agritrace.module.product.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品返回对象
 */
@Data
public class ProductVO {

    private Long id;

    private String name;

    private String category;

    private String spec;

    private String unit;

    private LocalDateTime createdAt;
}
