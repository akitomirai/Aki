package edu.jxust.agritrace.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类：
 * - createToken：生成 token
 * - parse：解析并验签 token
 */
public final class JwtUtil {

    private JwtUtil() {}

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token
     */
    public static String createToken(String secret, String issuer, long expireMinutes, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expireMinutes * 60);

        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(claims)
                .signWith(key(secret))
                .compact();
    }

    /**
     * 解析并验签 JWT token
     */
    public static Claims parse(String secret, String token) {
        return Jwts.parser()
                .verifyWith(key(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}