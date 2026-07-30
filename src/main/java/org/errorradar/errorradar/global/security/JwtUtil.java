package org.errorradar.errorradar.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.errorradar.errorradar.global.errorcode.ErrorCode;
import org.errorradar.errorradar.global.exception.CustomException;
import org.errorradar.errorradar.user.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String generateAccessToken(String email, Role role) {
        return buildToken(email, role.name(), accessTokenExpiry);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, null, refreshTokenExpiry);
    }

    private String buildToken(String subject, String role, long expiry) {
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(secretKey);
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public long getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }
}
