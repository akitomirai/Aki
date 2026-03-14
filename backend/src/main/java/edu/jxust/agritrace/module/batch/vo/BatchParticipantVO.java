package edu.jxust.agritrace.module.batch.vo;

import lombok.Data;

@Data
public class BatchParticipantVO {

    private Long id;

    private Long batchId;

    private Long companyId;

    private String companyName;

    private String bizRole;

    private Integer stageOrder;

    private Boolean isCreator;

    private String remark;
}
