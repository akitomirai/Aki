package edu.jxust.agritrace.dto;

import lombok.Data;

@Data
public class ProductCreateDTO {

    private String productName;
    private String category;
    private String originPlace;
    private String description;
}
