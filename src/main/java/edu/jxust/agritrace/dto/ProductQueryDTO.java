package edu.jxust.agritrace.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {

    private String productName;
    private String category;
    private String originPlace;

    /**
     * 可选：模糊搜索关键字（会对 productName/category/originPlace/description 进行 like）
     */
    private String keyword;
}
