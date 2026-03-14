package edu.jxust.agritrace.module.invite.dto;

import lombok.Data;

/**
 * 查询邀请码DTO
 */
@Data
public class InviteCodeQueryDTO {

    /**
     * 邀请码类型：ENTERPRISE_USER/REGULATOR_USER
     */
    private String inviteType;

    /**
     * 组织类型：COMPANY/REGULATOR_ORG
     */
    private String orgType;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 状态：UNUSED/USED/EXPIRED/DISABLED
     */
    private String status;

    /**
     * 邀请码
     */
    private String code;
}
