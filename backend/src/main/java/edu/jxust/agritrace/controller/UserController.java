package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.auth.dto.UserCreateRequest;
import edu.jxust.agritrace.module.auth.dto.UserListQueryRequest;
import edu.jxust.agritrace.module.auth.dto.UserResetPasswordRequest;
import edu.jxust.agritrace.module.auth.dto.UserStatusUpdateRequest;
import edu.jxust.agritrace.module.auth.dto.UserUpdateRequest;
import edu.jxust.agritrace.module.auth.service.UserAdminService;
import edu.jxust.agritrace.module.auth.vo.UserAdminVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAdminService userAdminService;

    public UserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ApiResponse<List<UserAdminVO>> listUsers(@ModelAttribute UserListQueryRequest request) {
        return ApiResponse.ok(userAdminService.listUsers(request));
    }

    @PostMapping
    public ApiResponse<UserAdminVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok("用户已创建。", userAdminService.createUser(request));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<UserAdminVO> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok("用户资料已更新。", userAdminService.updateUser(userId, request));
    }

    @PostMapping("/{userId}/status")
    public ApiResponse<UserAdminVO> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.ok("用户状态已更新。", userAdminService.updateUserStatus(userId, request));
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<UserAdminVO> resetPassword(@PathVariable Long userId, @Valid @RequestBody UserResetPasswordRequest request) {
        return ApiResponse.ok("用户密码已重置。", userAdminService.resetPassword(userId, request));
    }
}
