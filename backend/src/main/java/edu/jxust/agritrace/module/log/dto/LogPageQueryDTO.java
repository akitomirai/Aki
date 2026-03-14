package edu.jxust.agritrace.module.log.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志分页查询 DTO
 */
@Data
public class LogPageQueryDTO {
    private long current = 1;
    private long size = 10;
    private String module;
    private String operatorName;
    private String action;
    private String resultStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
