package edu.jxust.agritrace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("product_batch")
public class ProductBatch {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private Long producerId;

    private String batchCode;
    private LocalDate productionDate;
    private String status;

    private LocalDateTime createTime;
}
