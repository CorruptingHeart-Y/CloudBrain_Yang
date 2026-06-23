package com.neusoft.hospital.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${hospital.jwt.secret}")
    private String secret;

    @Value("${hospital.jwt.expire-hours:8}")
    private long expireHours;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = looksLikeBase64(secret) ? Decoders.BASE64.decode(secret) : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("hospital.jwt.secret 长度不足，HS256 要求至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generate(Integer employeeId) {
        long now = System.currentTimeMillis();
        long exp = expireHours * 3600_000L;
        return Jwts.builder()
                .subject(String.valueOf(employeeId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + exp))
                .signWith(key)
                .compact();
    }

    public Integer parseEmployeeId(String token) {
        Claims payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Integer.valueOf(payload.getSubject());
    }

    public long getExpireMillis() {
        return expireHours * 3600_000L;
    }

    public boolean isExpired(String token) {
        try {
            parseEmployeeId(token);
            return false;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public long remainingSeconds(String token) {
        try {
            Date exp = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (JwtException e) {
            return 0;
        }
    }

    private static boolean looksLikeBase64(String s) {
        if (s == null || s.length() < 8) return false;
        return s.matches("^[A-Za-z0-9+/]+={0,2}$") && s.length() % 4 == 0;
    }
}
