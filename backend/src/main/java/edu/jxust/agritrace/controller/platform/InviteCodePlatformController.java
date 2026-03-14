package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.invite.dto.InviteCodeCreateDTO;
import edu.jxust.agritrace.module.invite.service.InviteCodeService;
import edu.jxust.agritrace.module.invite.vo.InviteCodeVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台端-邀请码管理接口
 */
@Tag(name = "平台端-邀请码管理")
@RestController
@RequestMapping("/api/platform/invite-code")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class InviteCodePlatformController {

    private final InviteCodeService inviteCodeService;

    public InviteCodePlatformController(InviteCodeService inviteCodeService) {
        this.inviteCodeService = inviteCodeService;
    }

    /**
     * 创建邀请码
     */
    @OperationLogAnnotation(
            module = "INVITE_CODE",
            action = "CREATE_INVITE_CODE",
            targetType = "INVITE_CODE"
    )
    @Operation(summary = "创建邀请码")
    @PostMapping("/create")
    public Result<String> create(@Valid @RequestBody InviteCodeCreateDTO dto) {
        if (dto.getExpireAt() == null) {
            dto.setExpireAt(LocalDateTime.now().plusDays(7));
        }
        String code = inviteCodeService.create(dto);
        return Result.ok(code);
    }

    /**
     * 查询邀请码列表
     */
    @Operation(summary = "查询邀请码列表")
    @GetMapping("/list")
    public Result<List<InviteCodeVO>> list() {
        return Result.ok(inviteCodeService.list());
    }

    /**
     * 查询平台端已邀请列表 (为了与企业端/监管端保持路径一致)
     */
    @Operation(summary = "查询平台端已邀请列表")
    @GetMapping("/invite/list")
    public Result<List<InviteCodeVO>> inviteList() {
        return Result.ok(inviteCodeService.list());
    }

    /**
     * 禁用邀请码
     */
    @OperationLogAnnotation(
            module = "INVITE_CODE",
            action = "DISABLE_INVITE_CODE",
            targetType = "INVITE_CODE"
    )
    @Operation(summary = "禁用邀请码")
    @PutMapping("/disable/{id}")
    public Result<Boolean> disable(@PathVariable Long id) {
        inviteCodeService.disable(id);
        return Result.ok(true);
    }

    /**
     * 查询邀请码详情
     */
    @Operation(summary = "查询邀请码详情")
    @GetMapping("/detail/{id}")
    public Result<InviteCodeVO> detail(@PathVariable Long id) {
        return Result.ok(inviteCodeService.getById(id));
    }
}
