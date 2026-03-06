package edu.jxust.agritrace.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * API 风格 Security 配置：
 * 1) 禁用 formLogin / httpBasic（防止出现默认 /login 页面）
 * 2) 放行 /api/public/**
 * 3) 其他接口默认需要认证（后续接 JWT）
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 关闭 csrf（前后端分离常用）
        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());

        // ✅ 关键：禁用默认登录页与 Basic Auth
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());

        // 无状态
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 未认证统一返回 JSON 401
        http.exceptionHandling(eh -> eh.authenticationEntryPoint((req, resp, ex) -> {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"Unauthorized\"}");
        }));

        // 需要 import HttpMethod / UsernamePasswordAuthenticationFilter
        http.authorizeHttpRequests(auth -> auth
                // ✅ 关键：放行所有预检请求
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 放行文件请求
                .requestMatchers("/files/**").permitAll()
                // 放行公共接口
                .requestMatchers("/api/public/**", "/public/**").permitAll()
                // 放行登录
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                //其他都需要认证
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}