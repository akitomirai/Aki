package edu.jxust.agritrace.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.util.JwtUtil;
import edu.jxust.agritrace.module.auth.dto.LoginRequest;
import edu.jxust.agritrace.module.auth.entity.SysUser;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口：
 * - POST /api/auth/login：用户名密码登录，返回 JWT token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expire-minutes}")
    private long expireMinutes;

    public AuthController(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 登录：校验用户名/密码，返回 token
     */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())
                .last("limit 1"));

        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return Map.of("code", -1, "message", "用户不存在或已禁用");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return Map.of("code", -1, "message", "用户名或密码错误");
        }

        String token = JwtUtil.createToken(jwtSecret, issuer, expireMinutes, Map.of(
                "uid", user.getId(),
                "un", user.getUsername(),
                "role", user.getRoleCode()
        ));

        return Map.of("code", 0, "message", "ok", "token", token);
    }
}