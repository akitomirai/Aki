package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.user.dto.SysUserQueryDTO;
import edu.jxust.agritrace.module.user.dto.SysUserStatusUpdateDTO;
import edu.jxust.agritrace.module.user.service.SysUserService;
import edu.jxust.agritrace.module.user.vo.SysUserVO;
import edu.jxust.agritrace.module.user.vo.SysUserDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 平台端-用户管理控制器
 */
@Tag(name = "平台端-用户管理")
@RestController
@RequestMapping("/api/platform/user")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class SysUserPlatformController {

    private final SysUserService sysUserService;

    public SysUserPlatformController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 查询用户列表
     *
     * @param dto 查询条件
     * @return 用户列表
     */
    @Operation(summary = "查询用户列表")
    @GetMapping("/list")
    public Result<List<SysUserVO>> list(SysUserQueryDTO dto) {
        return Result.ok(sysUserService.list(dto));
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @Operation(summary = "查询用户详情")
    @GetMapping("/detail/{id}")
    public Result<SysUserDetailVO> detail(@Parameter(description = "用户ID") @PathVariable Long id) {
        return Result.ok(sysUserService.getById(id));
    }

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param dto 状态更新信息
     * @return 成功
     */
    @OperationLogAnnotation(
            module = "USER",
            action = "UPDATE_USER_STATUS",
            targetType = "SYS_USER"
    )
    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody SysUserStatusUpdateDTO dto) {
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException("不能修改自己的状态");
        }
        sysUserService.updateStatus(id, dto, null, null);
        return Result.ok();
    }

    /**
     * 重置用户密码为系统默认密码
     */
    @OperationLogAnnotation(
            module = "USER",
            action = "RESET_USER_PASSWORD",
            targetType = "SYS_USER"
    )
    @Operation(summary = "重置用户密码为系统默认密码")
    @PutMapping("/{id}/password/reset")
    public Result<Void> resetPassword(@Parameter(description = "用户ID") @PathVariable Long id) {
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException("不能重置自己的密码");
        }
        sysUserService.resetPassword(id, null, null);
        return Result.ok();
    }

    /**
     * 删除用户
     */
    @OperationLogAnnotation(
            module = "USER",
            action = "DELETE_USER",
            targetType = "SYS_USER"
    )
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException("不能删除自己");
        }
        sysUserService.delete(id, null, null);
        return Result.ok();
    }
}
