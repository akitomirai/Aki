package edu.jxust.agritrace.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductVO {

    private Long id;
    private String productName;
    private String category;
    private String originPlace;
    private String description;
    private LocalDateTime createTime;
}
