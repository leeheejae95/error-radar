package org.errorradar.errorradar.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.errorradar.errorradar.global.exception.CustomException;
import org.errorradar.errorradar.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hs256";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600000L, 604800000L);
    }

    @Test
    @DisplayName("AccessToken에 이메일과 role 클레임이 포함된다")
    void generateAccessToken_이메일_role_포함() {
        String token = jwtUtil.generateAccessToken("test@example.com", Role.ROLE_USER);

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("test@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("RefreshToken에 이메일은 포함되고 role 클레임은 없다")
    void generateRefreshToken_이메일_포함_role_없음() {
        String token = jwtUtil.generateRefreshToken("test@example.com");

        Claims claims = jwtUtil.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("test@example.com");
        assertThat(claims.get("role")).isNull();
    }

    @Test
    @DisplayName("getEmail은 토큰에서 이메일을 추출한다")
    void getEmail_이메일_추출_성공() {
        String token = jwtUtil.generateAccessToken("user@test.com", Role.ROLE_ADMIN);

        assertThat(jwtUtil.getEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("만료된 토큰 파싱 시 CustomException(만료된 토큰)을 던진다")
    void parseClaims_만료_토큰_예외_발생() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtUtil.parseClaims(expiredToken))
                .isInstanceOf(CustomException.class)
                .hasMessage("만료된 토큰입니다.");
    }

    @Test
    @DisplayName("위조된 토큰 파싱 시 CustomException(유효하지 않은 토큰)을 던진다")
    void parseClaims_위조_토큰_예외_발생() {
        assertThatThrownBy(() -> jwtUtil.parseClaims("invalid.token.value"))
                .isInstanceOf(CustomException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("AccessToken 만료 시간이 설정값과 일치한다")
    void generateAccessToken_만료시간_정상_설정() {
        long before = System.currentTimeMillis();
        String token = jwtUtil.generateAccessToken("test@example.com", Role.ROLE_USER);
        long after = System.currentTimeMillis();

        Claims claims = jwtUtil.parseClaims(token);
        long expiration = claims.getExpiration().getTime();

        // JWT는 만료시간을 초 단위로 저장하므로 최대 1초 오차 허용
        assertThat(expiration).isBetween(before + 3600000L - 1000L, after + 3600000L);
    }
}