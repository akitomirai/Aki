package edu.jxust.agritrace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("production_record")
public class ProductionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;
    private String operation;
    private String materialUsed;
    private Long operatorId;
    private LocalDateTime recordTime;
    private String remark;
}
