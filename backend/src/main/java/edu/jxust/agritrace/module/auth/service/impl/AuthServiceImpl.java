package edu.jxust.agritrace.module.auth.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.JwtTokenProvider;
import edu.jxust.agritrace.common.security.LoginUser;
import edu.jxust.agritrace.module.auth.dto.LoginDTO;
import edu.jxust.agritrace.module.auth.dto.RegisterDTO;
import edu.jxust.agritrace.module.auth.service.AuthService;
import edu.jxust.agritrace.module.auth.vo.CurrentUserVO;
import edu.jxust.agritrace.module.auth.vo.LoginVO;
import edu.jxust.agritrace.module.invite.entity.InviteCode;
import edu.jxust.agritrace.module.invite.service.InviteCodeService;
import edu.jxust.agritrace.module.user.entity.SysUser;
import edu.jxust.agritrace.module.user.mapper.SysUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final InviteCodeService inviteCodeService;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           InviteCodeService inviteCodeService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.inviteCodeService = inviteCodeService;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }

        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRoleCode(),
                user.getCompanyId(),
                user.getRegulatorOrgId()
        );

        String token = jwtTokenProvider.createToken(loginUser);

        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setId(user.getId());
        currentUserVO.setUsername(user.getUsername());
        currentUserVO.setRealName(user.getRealName());
        currentUserVO.setPhone(user.getPhone());
        currentUserVO.setRoleCode(user.getRoleCode());
        currentUserVO.setCompanyId(user.getCompanyId());
        currentUserVO.setRegulatorOrgId(user.getRegulatorOrgId());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setTokenType("Bearer");
        loginVO.setUser(currentUserVO);

        return loginVO;
    }

    @Override
    public CurrentUserVO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException("未登录或登录已失效");
        }

        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setRealName(loginUser.getRealName());
        vo.setRoleCode(loginUser.getRoleCode());
        vo.setCompanyId(loginUser.getCompanyId());
        vo.setRegulatorOrgId(loginUser.getRegulatorOrgId());

        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException("账号不存在或已删除");
        }
        vo.setPhone(user.getPhone());

        return vo;
    }

    @Override
    public void logout() {
        // JWT 无状态，前端删除 token 即可
    }

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 检查用户名是否已存在
        if (sysUserMapper.countByUsernameAll(dto.getUsername()) > 0) {
            throw new BizException("用户名已存在");
        }

        // 无邀请码禁止注册
        if (dto.getInviteCode() == null || dto.getInviteCode().isEmpty()) {
            throw new BizException("注册需要邀请码");
        }

        // 验证邀请码并获取组织信息
        InviteCode inviteCode = inviteCodeService.getByCode(dto.getInviteCode());
        
        Long companyId = null;
        Long regulatorOrgId = null;
        String roleCode;
        
        // 根据组织类型设置对应的组织ID和角色
        String orgType = inviteCode.getOrgType();
        if ("COMPANY".equals(orgType)) {
            companyId = inviteCode.getOrgId();
            regulatorOrgId = null;
            roleCode = inviteCode.getRoleCode() != null ? inviteCode.getRoleCode() : "ENTERPRISE_USER";
        } else if ("REGULATOR_ORG".equals(orgType)) {
            regulatorOrgId = inviteCode.getOrgId();
            companyId = null;
            roleCode = inviteCode.getRoleCode() != null ? inviteCode.getRoleCode() : "REGULATOR";
        } else {
            throw new BizException("非法的组织类型：" + orgType);
        }

        // 一致性校验：roleCode 与组织ID必须匹配
        if ("PLATFORM_ADMIN".equals(roleCode)) {
            if (companyId != null || regulatorOrgId != null) {
                throw new BizException("平台管理员不能关联组织");
            }
        } else if ("ENTERPRISE_USER".equals(roleCode) || "ENTERPRISE_ADMIN".equals(roleCode)) {
            if (companyId == null) {
                throw new BizException("企业用户必须关联企业");
            }
            if (regulatorOrgId != null) {
                throw new BizException("企业用户不能关联监管机构");
            }
        } else if ("REGULATOR".equals(roleCode)) {
            if (regulatorOrgId == null) {
                throw new BizException("监管用户必须关联监管机构");
            }
            if (companyId != null) {
                throw new BizException("监管用户不能关联企业");
            }
        }

        // 创建新用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setRoleCode(roleCode);
        user.setCompanyId(companyId);
        user.setRegulatorOrgId(regulatorOrgId);
        user.setStatus(1); // 默认为启用状态
        user.setDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 插入用户并获取用户ID
        sysUserMapper.insert(user);
        Long userId = user.getId();

        // 标记邀请码为已使用，传入用户ID（使用条件更新防止并发重复使用）
        inviteCodeService.markAsUsed(inviteCode.getId(), userId);
    }
}
