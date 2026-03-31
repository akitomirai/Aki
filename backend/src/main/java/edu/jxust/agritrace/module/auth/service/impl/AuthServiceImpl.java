package edu.jxust.agritrace.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.dto.ChangePasswordRequest;
import edu.jxust.agritrace.module.auth.dto.LoginRequest;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import edu.jxust.agritrace.module.auth.mapper.po.SysUserPO;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.auth.service.AuthService;
import edu.jxust.agritrace.module.auth.service.JwtTokenService;
import edu.jxust.agritrace.module.auth.vo.LoginResponseVO;
import edu.jxust.agritrace.module.auth.vo.LoginUserVO;
import edu.jxust.agritrace.module.log.dto.OperationLogRecord;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import edu.jxust.agritrace.module.log.service.support.OperationLogLabels;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuthServiceImpl implements AuthService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final OperationLogService operationLogService;

    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            OperationLogService operationLogService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.operationLogService = operationLogService;
    }

    @Override
    public LoginResponseVO login(LoginRequest request) {
        SysUserPO user = findUserByUsername(request.getUsername());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        AuthUserSession userSession = new AuthUserSession(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRoleCode(),
                user.getCompanyId()
        );

        LoginResponseVO response = new LoginResponseVO();
        response.setToken(jwtTokenService.createToken(userSession));
        response.setUser(toLoginUserVO(user));
        operationLogService.record(new OperationLogRecord(
                user.getId(),
                resolveDisplayName(user),
                user.getRoleCode(),
                user.getCompanyId(),
                "AUTH_LOGIN_SUCCESS",
                "AUTH",
                user.getId(),
                defaultValue(user.getUsername(), resolveDisplayName(user)),
                "SUCCESS",
                OperationLogLabels.roleName(user.getRoleCode()) + " " + resolveDisplayName(user) + " 登录成功"
        ));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUserVO changePassword(ChangePasswordRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        SysUserPO user = findUserRequired(currentUser.userId());
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("当前账号已停用，请联系管理员。");
        }

        String currentPassword = trimRequired(request.currentPassword(), "原密码不能为空。");
        String newPassword = trimRequired(request.newPassword(), "新密码不能为空。");
        String confirmPassword = trimRequired(request.confirmPassword(), "确认新密码不能为空。");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码不正确。");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少需要 6 位。");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的新密码不一致。");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与原密码相同。");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setNeedChangePassword(0);
        user.setPasswordUpdatedAt(now);
        user.setUpdatedAt(now);
        sysUserMapper.updateById(user);

        return toLoginUserVO(sysUserMapper.selectById(user.getId()));
    }

    private SysUserPO findUserByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getUsername, username)
                .last("limit 1"));
    }

    private SysUserPO findUserRequired(Long userId) {
        SysUserPO user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试。");
        }
        return user;
    }

    private AuthUserSession requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserSession userSession) {
            return userSession;
        }
        throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试。");
    }

    private LoginUserVO toLoginUserVO(SysUserPO user) {
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRoleCode(user.getRoleCode());
        loginUser.setRoleName(resolveRoleName(user.getRoleCode()));
        loginUser.setCompanyId(user.getCompanyId());
        loginUser.setNeedChangePassword(user.getNeedChangePassword() != null && user.getNeedChangePassword() == 1);
        loginUser.setPasswordUpdatedAt(formatDateTime(user.getPasswordUpdatedAt()));
        return loginUser;
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String resolveDisplayName(SysUserPO user) {
        return defaultValue(user == null ? null : user.getRealName(), user == null ? "" : user.getUsername());
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String resolveRoleName(String roleCode) {
        return switch (roleCode) {
            case "PLATFORM_ADMIN" -> "平台管理员";
            case "ENTERPRISE_ADMIN" -> "企业管理员";
            case "OPERATOR" -> "现场操作员";
            case "REGULATOR" -> "监管人员";
            default -> "系统用户";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }
}
