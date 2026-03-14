package edu.jxust.agritrace.module.batch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_batch_participant")
public class TraceBatchParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;

    private Long companyId;

    private String bizRole;

    private Integer stageOrder;

    private Boolean isCreator;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
