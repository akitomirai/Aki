package edu.jxust.agritrace.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.dto.ChangePasswordRequest;
import edu.jxust.agritrace.module.auth.dto.LoginRequest;
import edu.jxust.agritrace.module.auth.dto.UserProfileUpdateRequest;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import edu.jxust.agritrace.module.auth.mapper.po.SysUserPO;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.auth.service.AuthService;
import edu.jxust.agritrace.module.auth.service.JwtTokenService;
import edu.jxust.agritrace.module.auth.vo.LoginResponseVO;
import edu.jxust.agritrace.module.auth.vo.LoginUserVO;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
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
    private final OrgCompanyMapper orgCompanyMapper;
    private final OperationLogService operationLogService;

    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            OrgCompanyMapper orgCompanyMapper,
            OperationLogService operationLogService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.orgCompanyMapper = orgCompanyMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public LoginResponseVO login(LoginRequest request) {
        String username = trimRequired(request.getUsername(), "用户名不能为空");
        SysUserPO user = findUserByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        ensureBoundCompanyEnabledForLogin(user);
        if (isOperator(user) && !isMobileLogin(request)) {
            throw new UnauthorizedException("现场操作员仅支持移动端登录，请使用移动端入口");
        }
        if (!isOperator(user) && isMobileChannel(request)) {
            throw new UnauthorizedException("该账号不是现场操作员，请使用后台登录入口");
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

    private boolean isOperator(SysUserPO user) {
        return "OPERATOR".equalsIgnoreCase(defaultValue(user.getRoleCode(), ""));
    }

    private boolean requiresEnabledCompany(SysUserPO user) {
        String roleCode = defaultValue(user == null ? null : user.getRoleCode(), "");
        return "ENTERPRISE_ADMIN".equalsIgnoreCase(roleCode) || "OPERATOR".equalsIgnoreCase(roleCode);
    }

    private void ensureBoundCompanyEnabledForLogin(SysUserPO user) {
        if (!requiresEnabledCompany(user)) {
            return;
        }
        if (user.getCompanyId() == null) {
            throw new UnauthorizedException("账号未绑定企业，请联系平台管理员");
        }
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(user.getCompanyId());
        if (companyPO == null || !"ENABLED".equalsIgnoreCase(defaultValue(companyPO.getStatus(), ""))) {
            throw new UnauthorizedException("所属企业已停用或归档，请联系平台管理员");
        }
    }

    private boolean isMobileLogin(LoginRequest request) {
        return isMobileChannel(request) && Boolean.TRUE.equals(request.getMobileDevice());
    }

    private boolean isMobileChannel(LoginRequest request) {
        return "MOBILE".equalsIgnoreCase(defaultValue(request.getLoginChannel(), ""));
    }

    @Override
    public LoginUserVO getProfile() {
        AuthUserSession currentUser = requireCurrentUser();
        SysUserPO user = findUserRequired(currentUser.userId());
        ensureUserEnabled(user);
        return toLoginUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUserVO updateProfile(UserProfileUpdateRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        SysUserPO user = findUserRequired(currentUser.userId());
        ensureUserEnabled(user);

        user.setRealName(trimToNull(request.realName()));
        user.setPhone(trimToNull(request.phone()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        SysUserPO latest = sysUserMapper.selectById(user.getId());
        operationLogService.record(new OperationLogRecord(
                latest.getId(),
                resolveDisplayName(latest),
                latest.getRoleCode(),
                latest.getCompanyId(),
                "USER_PROFILE_UPDATE",
                "USER",
                latest.getId(),
                defaultValue(latest.getUsername(), resolveDisplayName(latest)),
                "SUCCESS",
                resolveDisplayName(latest) + " 更新了个人资料"
        ));
        return toLoginUserVO(latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUserVO changePassword(ChangePasswordRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        SysUserPO user = findUserRequired(currentUser.userId());
        ensureUserEnabled(user);

        String currentPassword = trimRequired(request.currentPassword(), "原密码不能为空");
        String newPassword = trimRequired(request.newPassword(), "新密码不能为空");
        String confirmPassword = trimRequired(request.confirmPassword(), "确认新密码不能为空");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少需要 6 位");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
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
            throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试");
        }
        return user;
    }

    private void ensureUserEnabled(SysUserPO user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("当前账号已停用，请联系管理员");
        }
    }

    private AuthUserSession requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserSession userSession) {
            return userSession;
        }
        throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试");
    }

    private LoginUserVO toLoginUserVO(SysUserPO user) {
        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setId(user.getId());
        loginUser.setUsername(defaultValue(user.getUsername(), ""));
        loginUser.setRealName(defaultValue(user.getRealName(), ""));
        loginUser.setPhone(defaultValue(user.getPhone(), ""));
        loginUser.setRoleCode(defaultValue(user.getRoleCode(), ""));
        loginUser.setRoleName(resolveRoleName(user.getRoleCode()));
        loginUser.setCompanyId(user.getCompanyId());
        loginUser.setCompanyName(resolveCompanyName(user.getCompanyId()));
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveDisplayName(SysUserPO user) {
        return defaultValue(user == null ? null : user.getRealName(), user == null ? "" : user.getUsername());
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String resolveRoleName(String roleCode) {
        return switch (defaultValue(roleCode, "")) {
            case "PLATFORM_ADMIN" -> "平台管理员";
            case "ENTERPRISE_ADMIN" -> "企业管理员";
            case "OPERATOR" -> "现场操作员";
            case "REGULATOR" -> "监管人员";
            default -> "系统用户";
        };
    }

    private String resolveCompanyName(Long companyId) {
        if (companyId == null) {
            return "";
        }
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(companyId);
        return companyPO == null ? "" : defaultValue(companyPO.getName(), "");
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }
}
