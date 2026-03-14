package edu.jxust.agritrace.module.batch.dto;

import lombok.Data;

@Data
public class BatchParticipantItemDTO {

    private Long companyId;

    private String bizRole;

    private Integer stageOrder;

    private Boolean isCreator;

    private String remark;
}
