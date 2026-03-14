package edu.jxust.agritrace.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private Long companyId;
    private Long regulatorOrgId;
}