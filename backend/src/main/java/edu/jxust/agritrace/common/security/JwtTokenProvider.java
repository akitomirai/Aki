package edu.jxust.agritrace.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expireMillis;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(LoginUser loginUser) {

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireMillis);

        return Jwts.builder()
                .claim("userId", loginUser.getUserId())
                .claim("username", loginUser.getUsername())
                .claim("realName", loginUser.getRealName())
                .claim("roleCode", loginUser.getRoleCode())
                .claim("companyId", loginUser.getCompanyId())
                .claim("regulatorOrgId", loginUser.getRegulatorOrgId())
                .setSubject(loginUser.getUsername())
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public LoginUser parseToken(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = claims.get("userId", Integer.class) == null
                ? claims.get("userId", Long.class)
                : claims.get("userId", Integer.class).longValue();

        Long companyId = claims.get("companyId", Integer.class) == null
                ? claims.get("companyId", Long.class)
                : claims.get("companyId", Integer.class).longValue();

        Long regulatorOrgId = claims.get("regulatorOrgId", Integer.class) == null
                ? claims.get("regulatorOrgId", Long.class)
                : claims.get("regulatorOrgId", Integer.class).longValue();

        return new LoginUser(
                userId,
                claims.get("username", String.class),
                claims.get("realName", String.class),
                claims.get("roleCode", String.class),
                companyId,
                regulatorOrgId
        );
    }

    public boolean validateToken(String token) {
        try {

            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}