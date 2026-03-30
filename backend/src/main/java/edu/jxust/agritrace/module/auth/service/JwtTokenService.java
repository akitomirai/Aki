package edu.jxust.agritrace.module.auth.service;

import edu.jxust.agritrace.config.AuthProperties;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final AuthProperties authProperties;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.secretKey = Keys.hmacShaKeyFor(authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(AuthUserSession userSession) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userSession.username())
                .claim("uid", userSession.userId())
                .claim("realName", userSession.realName())
                .claim("roleCode", userSession.roleCode())
                .claim("companyId", userSession.companyId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(authProperties.getTokenExpireHours(), ChronoUnit.HOURS)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthUserSession parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return new AuthUserSession(
                extractLong(claims.get("uid")),
                claims.getSubject(),
                claims.get("realName", String.class),
                claims.get("roleCode", String.class),
                extractLong(claims.get("companyId"))
        );
    }

    private Long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
