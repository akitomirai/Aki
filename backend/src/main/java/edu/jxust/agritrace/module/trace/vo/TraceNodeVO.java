package edu.jxust.agritrace.module.trace.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TraceNodeVO {

    private Long id;
    private Long batchId;
    private Long companyId;
    private String companyName;
    private String bizRole;
    private String nodeType;
    private String title;
    private String content;
    private LocalDateTime eventTime;
    private Boolean isPublic;
}
