package com.erp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验（HS256，无状态登录态）。
 *
 * <p>密钥必须通过环境变量 JWT_SECRET 注入生产环境；
 * 默认值仅用于本地开发，长度须满足 HS256 最低 32 字节。
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtUtil(@Value("${erp.auth.jwt-secret:erp-local-dev-secret-key-change-me-please-32B}") String secret,
                   @Value("${erp.auth.token-ttl-hours:12}") long ttlHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlHours * 3600_000L;
    }

    public String issue(Integer userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 校验并解析 token，非法/过期返回 null（调用方据此返回 401）。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 校验失败：{}", e.getMessage());
            return null;
        }
    }
}
