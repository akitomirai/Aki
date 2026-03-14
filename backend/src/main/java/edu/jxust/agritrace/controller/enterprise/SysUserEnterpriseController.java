package edu.jxust.agritrace.controller.enterprise;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.invite.dto.InviteCodeCreateDTO;
import edu.jxust.agritrace.module.invite.service.InviteCodeService;
import edu.jxust.agritrace.module.invite.vo.InviteCodeVO;
import edu.jxust.agritrace.module.user.dto.SysUserQueryDTO;
import edu.jxust.agritrace.module.user.service.SysUserService;
import edu.jxust.agritrace.module.user.vo.SysUserVO;
import edu.jxust.agritrace.module.user.dto.SysUserStatusUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业端-用户管理接口
 */
@Tag(name = "企业端-用户管理")
@RestController
@RequestMapping("/api/enterprise/user")
@PreAuthorize("hasRole('ENTERPRISE_ADMIN') or hasRole('ADMIN')")
public class SysUserEnterpriseController {

    private final SysUserService sysUserService;
    private final InviteCodeService inviteCodeService;

    public SysUserEnterpriseController(SysUserService sysUserService, InviteCodeService inviteCodeService) {
        this.sysUserService = sysUserService;
        this.inviteCodeService = inviteCodeService;
    }

    /**
     * 查询本企业用户列表
     */
    @Operation(summary = "查询本企业用户列表")
    @GetMapping("/list")
    public Result<List<SysUserVO>> list(SysUserQueryDTO dto) {
        dto.setCompanyId(SecurityUtils.getCompanyId());
        return Result.ok(sysUserService.list(dto));
    }

    /**
     * 更新本企业用户状态
     */
    @OperationLogAnnotation(
            module = "USER",
            action = "UPDATE_USER_STATUS",
            targetType = "SYS_USER"
    )
    @Operation(summary = "更新本企业用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody SysUserStatusUpdateDTO dto) {
        // 禁止修改自己
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException("不能修改自己的状态");
        }
        sysUserService.updateStatus(id, dto, SecurityUtils.getCompanyId(), null);
        return Result.ok();
    }

    /**
     * 重置本企业用户密码为系统默认密码
     */
    @OperationLogAnnotation(
            module = "USER",
            action = "RESET_USER_PASSWORD",
            targetType = "SYS_USER"
    )
    @Operation(summary = "重置本企业用户密码为系统默认密码")
    @PutMapping("/{id}/password/reset")
    public Result<Void> resetPassword(@Parameter(description = "用户ID") @PathVariable Long id) {
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException("不能重置自己的密码");
        }
        sysUserService.resetPassword(id, SecurityUtils.getCompanyId(), null);
        return Result.ok();
    }

    /**
     * 删除本企业用户
     */
    @OperationLogAnnotation(
            module = "USER",
            action = "DELETE_USER",
            targetType = "SYS_USER"
    )
    @Operation(summary = "删除本企业用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        // 禁止删除自己
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException("不能删除自己");
        }
        sysUserService.delete(id, SecurityUtils.getCompanyId(), null);
        return Result.ok();
    }

    /**
     * 生成企业邀请码
     */
    @OperationLogAnnotation(
            module = "INVITE_CODE",
            action = "CREATE_INVITE_CODE",
            targetType = "INVITE_CODE"
    )
    @Operation(summary = "生成本企业邀请码")
    @PostMapping("/invite/create")
    public Result<String> createInvite(@Valid @RequestBody InviteCodeCreateDTO dto) {
        // 强制锁定本企业组织ID和类型
        dto.setOrgType("COMPANY");
        dto.setOrgId(SecurityUtils.getCompanyId());
        dto.setInviteType("ENTERPRISE_USER");
        
        // 默认角色限制（如果是企业管理员，只能邀请企业用户或同级管理员）
        if (!"ENTERPRISE_USER".equals(dto.getRoleCode()) && !"ENTERPRISE_ADMIN".equals(dto.getRoleCode())) {
            dto.setRoleCode("ENTERPRISE_USER");
        }
        
        if (dto.getExpireAt() == null) {
            dto.setExpireAt(LocalDateTime.now().plusDays(7));
        }
        
        return Result.ok(inviteCodeService.create(dto));
    }

    /**
     * 查询本企业已邀请列表
     */
    @Operation(summary = "查询本企业已邀请列表")
    @GetMapping("/invite/list")
    public Result<List<InviteCodeVO>> inviteList() {
        return Result.ok(inviteCodeService.listByOrgId(SecurityUtils.getCompanyId()));
    }
}
