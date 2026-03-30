package edu.jxust.agritrace.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.dto.LoginRequest;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import edu.jxust.agritrace.module.auth.mapper.po.SysUserPO;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.auth.service.AuthService;
import edu.jxust.agritrace.module.auth.service.JwtTokenService;
import edu.jxust.agritrace.module.auth.vo.LoginResponseVO;
import edu.jxust.agritrace.module.auth.vo.LoginUserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public LoginResponseVO login(LoginRequest request) {
        SysUserPO user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        LoginUserVO loginUser = new LoginUserVO();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRoleCode(user.getRoleCode());
        loginUser.setRoleName(resolveRoleName(user.getRoleCode()));
        loginUser.setCompanyId(user.getCompanyId());

        AuthUserSession userSession = new AuthUserSession(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRoleCode(),
                user.getCompanyId()
        );

        LoginResponseVO response = new LoginResponseVO();
        response.setToken(jwtTokenService.createToken(userSession));
        response.setUser(loginUser);
        return response;
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
}
