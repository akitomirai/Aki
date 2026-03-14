package edu.jxust.agritrace.module.user.dto;

import lombok.Data;

@Data
public class SysUserQueryDTO {

    private String username;

    private String realName;

    private String phone;

    private String roleCode;

    private Integer status;

    private Long companyId;

    private Long regulatorOrgId;
}
