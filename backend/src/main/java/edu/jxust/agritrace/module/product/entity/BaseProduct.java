package edu.jxust.agritrace.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品实体
 * 对应表：base_product
 */
@Data
@TableName("base_product")
public class BaseProduct {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 产品名称
     */
    private String name;

    /**
     * 产品分类
     */
    private String category;

    /**
     * 产品规格
     */
    private String spec;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
