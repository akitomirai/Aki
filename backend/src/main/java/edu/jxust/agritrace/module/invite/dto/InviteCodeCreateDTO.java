package edu.jxust.agritrace.module.invite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建邀请码DTO
 */
@Data
public class InviteCodeCreateDTO {

    /**
     * 邀请码类型：ENTERPRISE_USER/REGULATOR_USER
     */
    @NotBlank(message = "邀请码类型不能为空")
    private String inviteType;

    /**
     * 组织类型：COMPANY/REGULATOR_ORG
     */
    @NotBlank(message = "组织类型不能为空")
    private String orgType;

    /**
     * 组织ID（按org_type指向不同表）
     */
    @NotNull(message = "组织ID不能为空")
    @Positive(message = "组织ID必须为正数")
    private Long orgId;

    /**
     * 受邀后授予角色
     */
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /**
     * 过期时间
     */
    @NotNull(message = "过期时间不能为空")
    private LocalDateTime expireAt;

    /**
     * 备注
     */
    private String remark;
}
