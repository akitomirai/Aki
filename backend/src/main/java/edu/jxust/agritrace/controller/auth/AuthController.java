package edu.jxust.agritrace.controller.auth;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.auth.dto.LoginDTO;
import edu.jxust.agritrace.module.auth.dto.RegisterDTO;
import edu.jxust.agritrace.module.auth.service.AuthService;
import edu.jxust.agritrace.module.auth.vo.CurrentUserVO;
import edu.jxust.agritrace.module.auth.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {

        LoginVO vo = authService.login(dto);

        return Result.ok(vo);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<CurrentUserVO> getCurrentUser() {
        CurrentUserVO vo = authService.getCurrentUser();
        return Result.ok(vo);
    }

    @OperationLogAnnotation(module = "AUTH", action = "USER_REGISTER", targetType = "SYS_USER")
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok(true);
    }
}