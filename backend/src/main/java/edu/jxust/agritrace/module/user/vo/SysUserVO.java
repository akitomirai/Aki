package edu.jxust.agritrace.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String roleCode;

    private Long companyId;

    private Long regulatorOrgId;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
