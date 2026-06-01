package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.auth.dto.ChangePasswordRequest;
import edu.jxust.agritrace.module.auth.dto.LoginRequest;
import edu.jxust.agritrace.module.auth.dto.UserProfileUpdateRequest;
import edu.jxust.agritrace.module.auth.service.AuthService;
import edu.jxust.agritrace.module.auth.vo.LoginResponseVO;
import edu.jxust.agritrace.module.auth.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        request.setMobileDevice(isMobileUserAgent(servletRequest.getHeader("User-Agent")));
        return ApiResponse.ok("登录成功", authService.login(request));
    }

    private boolean isMobileUserAgent(String userAgent) {
        return userAgent != null && userAgent.matches("(?i).*(android|iphone|ipad|ipod|mobile|harmonyos).*");
    }

    @GetMapping("/profile")
    public ApiResponse<LoginUserVO> getProfile() {
        return ApiResponse.ok(authService.getProfile());
    }

    @PatchMapping("/profile")
    public ApiResponse<LoginUserVO> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.ok("个人资料已更新", authService.updateProfile(request));
    }

    @PostMapping("/change-password")
    public ApiResponse<LoginUserVO> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.ok("密码修改成功", authService.changePassword(request));
    }
}
