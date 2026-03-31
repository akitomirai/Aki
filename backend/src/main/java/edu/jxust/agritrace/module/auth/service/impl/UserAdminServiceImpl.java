package edu.jxust.agritrace.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.dto.UserCreateRequest;
import edu.jxust.agritrace.module.auth.dto.UserListQueryRequest;
import edu.jxust.agritrace.module.auth.dto.UserResetPasswordRequest;
import edu.jxust.agritrace.module.auth.dto.UserStatusUpdateRequest;
import edu.jxust.agritrace.module.auth.dto.UserUpdateRequest;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import edu.jxust.agritrace.module.auth.mapper.po.SysUserPO;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.auth.service.UserAdminService;
import edu.jxust.agritrace.module.auth.vo.UserAdminVO;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import edu.jxust.agritrace.module.log.dto.OperationLogRecord;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "PLATFORM_ADMIN",
            "ENTERPRISE_ADMIN",
            "OPERATOR",
            "REGULATOR"
    );

    private static final Set<String> ENTERPRISE_MANAGEABLE_ROLES = Set.of(
            "ENTERPRISE_ADMIN",
            "OPERATOR"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SysUserMapper sysUserMapper;
    private final OrgCompanyMapper orgCompanyMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public UserAdminServiceImpl(
            SysUserMapper sysUserMapper,
            OrgCompanyMapper orgCompanyMapper,
            PasswordEncoder passwordEncoder,
            OperationLogService operationLogService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.orgCompanyMapper = orgCompanyMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<UserAdminVO> listUsers(UserListQueryRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureUserManager(currentUser);

        Long effectiveCompanyId = normalizeCompanyFilter(currentUser, request.getCompanyId());
        String effectiveRoleCode = normalizeRoleCode(request.getRoleCode(), false);
        if (isEnterpriseAdmin(currentUser) && effectiveRoleCode != null && !ENTERPRISE_MANAGEABLE_ROLES.contains(effectiveRoleCode)) {
            throw new UnauthorizedException("你只能查看本企业管理员和操作员。");
        }

        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(effectiveRoleCode != null, SysUserPO::getRoleCode, effectiveRoleCode)
                .eq(effectiveCompanyId != null, SysUserPO::getCompanyId, effectiveCompanyId)
                .eq(request.getStatus() != null, SysUserPO::getStatus, normalizeStatus(request.getStatus()))
                .orderByAsc(SysUserPO::getCompanyId)
                .orderByAsc(SysUserPO::getRoleCode)
                .orderByDesc(SysUserPO::getUpdatedAt)
                .orderByDesc(SysUserPO::getId);

        if (notBlank(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUserPO::getUsername, keyword)
                    .or()
                    .like(SysUserPO::getRealName, keyword));
        }

        List<SysUserPO> users = sysUserMapper.selectList(queryWrapper);
        Map<Long, String> companyNameMap = buildCompanyNameMap(users);
        return users.stream()
                .map(user -> toUserAdminVO(user, companyNameMap))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO createUser(UserCreateRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureUserManager(currentUser);

        String username = trimRequired(request.username(), "用户名不能为空。");
        String password = trimRequired(request.password(), "密码不能为空。");
        String realName = trimRequired(request.realName(), "姓名不能为空。");
        String roleCode = normalizeRoleCode(request.roleCode(), true);
        ensureRoleAllowedForManager(currentUser, roleCode);
        Long companyId = normalizeCompanyForRole(currentUser, roleCode, request.companyId());

        if (password.length() < 6) {
            throw new IllegalArgumentException("密码至少需要 6 位。");
        }
        ensureUsernameUnique(username, null);

        LocalDateTime now = LocalDateTime.now();
        SysUserPO userPO = new SysUserPO();
        userPO.setUsername(username);
        userPO.setPassword(passwordEncoder.encode(password));
        userPO.setRealName(realName);
        userPO.setRoleCode(roleCode);
        userPO.setCompanyId(companyId);
        userPO.setStatus(1);
        userPO.setNeedChangePassword(1);
        userPO.setPasswordUpdatedAt(now);
        userPO.setCreatedAt(now);
        userPO.setUpdatedAt(now);
        sysUserMapper.insert(userPO);

        SysUserPO latest = sysUserMapper.selectById(userPO.getId());
        writeUserOperationLog(
                currentUser,
                "USER_CREATE",
                latest,
                "新建用户 " + latest.getUsername() + "（" + resolveUserDisplayName(latest) + "），角色：" + roleName(latest.getRoleCode()) + "，所属企业：" + resolveCompanyName(latest.getCompanyId())
        );
        return toUserAdminVO(latest, buildCompanyNameMap(List.of(latest)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO updateUser(Long userId, UserUpdateRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureUserManager(currentUser);

        SysUserPO userPO = findUserRequired(userId);
        ensureManageableTarget(currentUser, userPO);

        String realName = trimRequired(request.realName(), "姓名不能为空。");
        String roleCode = normalizeRoleCode(request.roleCode(), true);
        ensureRoleAllowedForManager(currentUser, roleCode);
        Long companyId = normalizeCompanyForRole(currentUser, roleCode, request.companyId());

        if (Objects.equals(currentUser.userId(), userPO.getId()) && !Objects.equals(userPO.getRoleCode(), roleCode)) {
            throw new IllegalArgumentException("不能修改当前登录账号的角色。");
        }
        if (Objects.equals(currentUser.userId(), userPO.getId()) && !Objects.equals(userPO.getCompanyId(), companyId)) {
            throw new IllegalArgumentException("不能修改当前登录账号的所属企业。");
        }

        userPO.setRealName(realName);
        userPO.setRoleCode(roleCode);
        userPO.setCompanyId(companyId);
        userPO.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(userPO);

        SysUserPO latest = sysUserMapper.selectById(userPO.getId());
        writeUserOperationLog(
                currentUser,
                "USER_UPDATE",
                latest,
                "编辑用户 " + latest.getUsername() + "（" + resolveUserDisplayName(latest) + "），角色：" + roleName(latest.getRoleCode()) + "，所属企业：" + resolveCompanyName(latest.getCompanyId())
        );
        return toUserAdminVO(latest, buildCompanyNameMap(List.of(latest)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureUserManager(currentUser);

        SysUserPO userPO = findUserRequired(userId);
        ensureManageableTarget(currentUser, userPO);

        int status = normalizeStatus(request.status());
        if (Objects.equals(currentUser.userId(), userPO.getId()) && status != 1) {
            throw new IllegalArgumentException("不能停用当前登录账号。");
        }

        userPO.setStatus(status);
        userPO.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(userPO);

        SysUserPO latest = sysUserMapper.selectById(userPO.getId());
        writeUserOperationLog(
                currentUser,
                status == 1 ? "USER_ENABLE" : "USER_DISABLE",
                latest,
                (status == 1 ? "启用用户 " : "停用用户 ") + latest.getUsername() + "（" + resolveUserDisplayName(latest) + "）"
        );
        return toUserAdminVO(latest, buildCompanyNameMap(List.of(latest)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO resetPassword(Long userId, UserResetPasswordRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureUserManager(currentUser);

        SysUserPO userPO = findUserRequired(userId);
        ensureManageableTarget(currentUser, userPO);
        if (Objects.equals(currentUser.userId(), userPO.getId())) {
            throw new IllegalArgumentException("当前登录账号请使用“修改密码”完成密码更新。");
        }

        String newPassword = trimRequired(request.newPassword(), "新密码不能为空。");
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少需要 6 位。");
        }

        LocalDateTime now = LocalDateTime.now();
        userPO.setPassword(passwordEncoder.encode(newPassword));
        userPO.setNeedChangePassword(1);
        userPO.setPasswordUpdatedAt(now);
        userPO.setUpdatedAt(now);
        sysUserMapper.updateById(userPO);

        SysUserPO latest = sysUserMapper.selectById(userPO.getId());
        writeUserOperationLog(
                currentUser,
                "USER_RESET_PASSWORD",
                latest,
                "重置用户 " + latest.getUsername() + "（" + resolveUserDisplayName(latest) + "）密码，并要求下次登录先修改密码"
        );
        return toUserAdminVO(latest, buildCompanyNameMap(List.of(latest)));
    }

    private AuthUserSession requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserSession userSession) {
            return userSession;
        }
        throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试。");
    }

    private void ensureUserManager(AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser)) {
            return;
        }
        throw new UnauthorizedException("你没有用户管理权限。");
    }

    private void ensureManageableTarget(AuthUserSession currentUser, SysUserPO userPO) {
        if (isPlatformAdmin(currentUser)) {
            return;
        }
        if (!Objects.equals(currentUser.companyId(), userPO.getCompanyId())) {
            throw new UnauthorizedException("你只能管理本企业用户。");
        }
        if (!ENTERPRISE_MANAGEABLE_ROLES.contains(defaultValue(userPO.getRoleCode(), "").toUpperCase(Locale.ROOT))) {
            throw new UnauthorizedException("你只能管理本企业管理员和操作员。");
        }
    }

    private void ensureRoleAllowedForManager(AuthUserSession currentUser, String roleCode) {
        if (isPlatformAdmin(currentUser)) {
            return;
        }
        if (!ENTERPRISE_MANAGEABLE_ROLES.contains(roleCode)) {
            throw new UnauthorizedException("企业管理员只能创建和管理本企业管理员、操作员。");
        }
    }

    private Long normalizeCompanyFilter(AuthUserSession currentUser, Long requestedCompanyId) {
        if (!isEnterpriseAdmin(currentUser)) {
            return requestedCompanyId;
        }
        if (requestedCompanyId != null && !Objects.equals(requestedCompanyId, currentUser.companyId())) {
            throw new UnauthorizedException("你只能查看本企业用户。");
        }
        return currentUser.companyId();
    }

    private Long normalizeCompanyForRole(AuthUserSession currentUser, String roleCode, Long requestedCompanyId) {
        if ("PLATFORM_ADMIN".equals(roleCode) || "REGULATOR".equals(roleCode)) {
            return null;
        }
        Long effectiveCompanyId = isEnterpriseAdmin(currentUser) ? currentUser.companyId() : requestedCompanyId;
        if (effectiveCompanyId == null) {
            throw new IllegalArgumentException("企业管理员和操作员必须绑定所属企业。");
        }
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(effectiveCompanyId);
        if (companyPO == null) {
            throw new IllegalArgumentException("所选企业不存在。");
        }
        if (isEnterpriseAdmin(currentUser) && !Objects.equals(effectiveCompanyId, currentUser.companyId())) {
            throw new UnauthorizedException("你只能管理本企业用户。");
        }
        return effectiveCompanyId;
    }

    private SysUserPO findUserRequired(Long userId) {
        SysUserPO userPO = sysUserMapper.selectById(userId);
        if (userPO == null) {
            throw new IllegalArgumentException("用户不存在。");
        }
        return userPO;
    }

    private void ensureUsernameUnique(String username, Long ignoredId) {
        SysUserPO existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getUsername, username)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), ignoredId)) {
            throw new IllegalArgumentException("用户名已存在，请更换后再试。");
        }
    }

    private Map<Long, String> buildCompanyNameMap(List<SysUserPO> users) {
        Map<Long, String> companyNameMap = new LinkedHashMap<>();
        for (SysUserPO user : users) {
            if (user == null || user.getCompanyId() == null || companyNameMap.containsKey(user.getCompanyId())) {
                continue;
            }
            OrgCompanyPO companyPO = orgCompanyMapper.selectById(user.getCompanyId());
            companyNameMap.put(user.getCompanyId(), companyPO == null ? "" : defaultValue(companyPO.getName(), ""));
        }
        return companyNameMap;
    }

    private UserAdminVO toUserAdminVO(SysUserPO userPO, Map<Long, String> companyNameMap) {
        if (userPO == null) {
            return null;
        }
        boolean needChangePassword = userPO.getNeedChangePassword() != null && userPO.getNeedChangePassword() == 1;
        return new UserAdminVO(
                userPO.getId(),
                defaultValue(userPO.getUsername(), ""),
                defaultValue(userPO.getRealName(), defaultValue(userPO.getUsername(), "")),
                defaultValue(userPO.getRoleCode(), ""),
                roleName(userPO.getRoleCode()),
                userPO.getCompanyId(),
                companyNameMap.getOrDefault(userPO.getCompanyId(), ""),
                userPO.getStatus() == null ? 0 : userPO.getStatus(),
                userStatusLabel(userPO.getStatus()),
                needChangePassword,
                needChangePassword ? "首次登录需改密" : "密码已设置",
                formatDateTime(userPO.getPasswordUpdatedAt()),
                formatDateTime(userPO.getUpdatedAt())
        );
    }

    private String normalizeRoleCode(String value, boolean required) {
        if (!notBlank(value)) {
            if (required) {
                throw new IllegalArgumentException("角色不能为空。");
            }
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的角色类型。");
        }
        return normalized;
    }

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("用户状态只支持启用或停用。");
        }
        return status;
    }

    private boolean isPlatformAdmin(AuthUserSession currentUser) {
        return currentUser != null && "PLATFORM_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isEnterpriseAdmin(AuthUserSession currentUser) {
        return currentUser != null && "ENTERPRISE_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private String trimRequired(String value, String message) {
        if (!notBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void writeUserOperationLog(AuthUserSession currentUser, String actionType, SysUserPO targetUser, String summary) {
        if (currentUser == null || targetUser == null) {
            return;
        }
        operationLogService.record(new OperationLogRecord(
                currentUser.userId(),
                resolveActorName(currentUser),
                currentUser.roleCode(),
                targetUser.getCompanyId(),
                actionType,
                "USER",
                targetUser.getId(),
                targetUser.getUsername(),
                "SUCCESS",
                summary
        ));
    }

    private String resolveActorName(AuthUserSession currentUser) {
        return defaultValue(currentUser == null ? null : currentUser.realName(), defaultValue(currentUser == null ? null : currentUser.username(), "系统用户"));
    }

    private String resolveUserDisplayName(SysUserPO userPO) {
        return defaultValue(userPO == null ? null : userPO.getRealName(), userPO == null ? "" : userPO.getUsername());
    }

    private String resolveCompanyName(Long companyId) {
        if (companyId == null) {
            return "平台直属";
        }
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(companyId);
        return companyPO == null ? "未知企业" : defaultValue(companyPO.getName(), "未知企业");
    }

    private String roleName(String roleCode) {
        return switch (defaultValue(roleCode, "").toUpperCase(Locale.ROOT)) {
            case "PLATFORM_ADMIN" -> "平台管理员";
            case "ENTERPRISE_ADMIN" -> "企业管理员";
            case "OPERATOR" -> "现场操作员";
            case "REGULATOR" -> "监管人员";
            default -> "系统用户";
        };
    }

    private String userStatusLabel(Integer status) {
        return status != null && status == 1 ? "启用" : "停用";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }
}
