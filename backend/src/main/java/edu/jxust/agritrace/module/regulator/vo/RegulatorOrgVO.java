package edu.jxust.agritrace.module.regulator.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegulatorOrgVO {

    private Long id;

    private String name;

    private String code;

    private String address;

    private String contact;

    private String phone;

    private String status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
