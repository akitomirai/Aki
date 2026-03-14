package edu.jxust.agritrace.module.invite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码实体
 * 对应表：invite_code
 */
@Data
@TableName("invite_code")
public class InviteCode {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 邀请码
     */
    private String code;

    /**
     * 邀请码类型：ENTERPRISE_USER/REGULATOR_USER
     */
    private String inviteType;

    /**
     * 组织类型：COMPANY/REGULATOR_ORG
     */
    private String orgType;

    /**
     * 组织ID（按org_type指向不同表）
     */
    private Long orgId;

    /**
     * 受邀后授予角色
     */
    private String roleCode;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 状态：UNUSED/USED/EXPIRED/DISABLED
     */
    private String status;

    /**
     * 使用人ID
     */
    private Long usedBy;

    /**
     * 使用时间
     */
    private LocalDateTime usedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
