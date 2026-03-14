package edu.jxust.agritrace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Result.fail(401, "未登录或登录已失效")))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, Result.fail(403, "权限不足，拒绝访问")))
                )

                .authorizeHttpRequests(auth -> auth

                        // 登录接口
                        .requestMatchers("/api/auth/**").permitAll()

                        // 前台接口（扫码、反馈等）
                        .requestMatchers("/api/public/**").permitAll()

                        // swagger（如果有）
                        .requestMatchers(
                                "/doc.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 其它接口必须登录
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, Result<Void> result) {
        try {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
        } catch (Exception ignored) {
        }
    }
}
