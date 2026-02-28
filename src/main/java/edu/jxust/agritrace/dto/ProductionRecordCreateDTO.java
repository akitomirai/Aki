package edu.jxust.agritrace.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductionRecordCreateDTO {

    private Long batchId;
    private String operation;
    private String materialUsed;
    private Long operatorId;
    private LocalDateTime recordTime;
    private String remark;
}
