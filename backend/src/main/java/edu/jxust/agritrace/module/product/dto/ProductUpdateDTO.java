package edu.jxust.agritrace.module.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 产品修改请求参数
 */
@Data
public class ProductUpdateDTO {

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称长度不能超过128")
    private String name;

    @Size(max = 64, message = "产品分类长度不能超过64")
    private String category;

    @Size(max = 64, message = "产品规格长度不能超过64")
    private String spec;

    @Size(max = 16, message = "计量单位长度不能超过16")
    private String unit;
}
