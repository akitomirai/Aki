package edu.jxust.agritrace.config;

import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import edu.jxust.agritrace.module.auth.mapper.po.SysUserPO;
import edu.jxust.agritrace.module.auth.service.JwtTokenService;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final SysUserMapper sysUserMapper;
    private final OrgCompanyMapper orgCompanyMapper;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            SysUserMapper sysUserMapper,
            OrgCompanyMapper orgCompanyMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.sysUserMapper = sysUserMapper;
        this.orgCompanyMapper = orgCompanyMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                AuthUserSession tokenSession = jwtTokenService.parseToken(token);
                AuthUserSession userSession = resolveCurrentSession(tokenSession);
                if (userSession == null) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userSession,
                        token,
                        List.of(new SimpleGrantedAuthority("ROLE_" + userSession.roleCode()))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private AuthUserSession resolveCurrentSession(AuthUserSession tokenSession) {
        if (tokenSession == null || tokenSession.userId() == null) {
            return null;
        }
        SysUserPO user = sysUserMapper.selectById(tokenSession.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1 || isBlank(user.getRoleCode())) {
            return null;
        }
        if (requiresEnabledCompany(user) && !isBoundCompanyEnabled(user.getCompanyId())) {
            return null;
        }
        return new AuthUserSession(
                user.getId(),
                defaultValue(user.getUsername(), tokenSession.username()),
                defaultValue(user.getRealName(), tokenSession.realName()),
                user.getRoleCode().trim(),
                user.getCompanyId()
        );
    }

    private String defaultValue(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean requiresEnabledCompany(SysUserPO user) {
        String roleCode = defaultValue(user == null ? null : user.getRoleCode(), "");
        return "ENTERPRISE_ADMIN".equalsIgnoreCase(roleCode) || "OPERATOR".equalsIgnoreCase(roleCode);
    }

    private boolean isBoundCompanyEnabled(Long companyId) {
        if (companyId == null) {
            return false;
        }
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(companyId);
        return companyPO != null && "ENABLED".equalsIgnoreCase(defaultValue(companyPO.getStatus(), ""));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
