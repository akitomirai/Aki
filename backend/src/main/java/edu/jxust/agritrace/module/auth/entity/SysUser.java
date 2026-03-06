package edu.jxust.agritrace.module.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户实体（表：sys_user）
 * 用于登录认证、角色权限控制。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    private Long id;

    /** 登录名 */
    private String username;

    /** BCrypt 密码哈希 */
    private String password;

    /** 角色编码：ADMIN/OPERATOR/REGULATOR */
    private String roleCode;

    /** 状态：1 启用，0 禁用 */
    private Integer status;
}