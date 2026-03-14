package edu.jxust.agritrace.module.auth.vo;

import lombok.Data;

@Data
public class CurrentUserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String roleCode;
    private Long companyId;
    private Long regulatorOrgId;
}