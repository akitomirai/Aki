package edu.jxust.agritrace.config;

import edu.jxust.agritrace.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：
 * 1) 从 Authorization: Bearer <token> 读取 token
 * 2) 验签并解析 uid/un/role
 * 3) 写入 SecurityContext，表示当前请求已认证
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                Claims claims = JwtUtil.parse(jwtSecret, token);
                String username = String.valueOf(claims.get("un"));
                String role = String.valueOf(claims.get("role"));

                // 约定：ROLE_ 前缀是 Spring Security 的默认写法
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // principal 用 username 即可，后续需要 uid 再扩展
                var authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignore) {
                // token 无效：不注入认证信息，后续会被 Security 401
            }
        }

        filterChain.doFilter(request, response);
    }
}