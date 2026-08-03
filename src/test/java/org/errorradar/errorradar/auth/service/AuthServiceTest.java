package org.errorradar.errorradar.auth.service;

import org.errorradar.errorradar.auth.dto.LoginRequest;
import org.errorradar.errorradar.auth.dto.RefreshRequest;
import org.errorradar.errorradar.auth.dto.SignupRequest;
import org.errorradar.errorradar.auth.dto.AuthResponse;
import org.errorradar.errorradar.global.exception.CustomException;
import org.errorradar.errorradar.global.metrics.ErrorRadarMetrics;
import org.errorradar.errorradar.global.security.JwtUtil;
import org.errorradar.errorradar.user.entity.Role;
import org.errorradar.errorradar.user.entity.User;
import org.errorradar.errorradar.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock ErrorRadarMetrics metrics;

    @InjectMocks AuthService authService;

    // ─── signup ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("회원가입 성공 시 accessToken과 refreshToken을 반환한다")
    void signup_성공() {
        SignupRequest request = mock(SignupRequest.class);
        given(request.getEmail()).willReturn("new@example.com");
        given(request.getPassword()).willReturn("password123");
        given(request.getUsername()).willReturn("tester");

        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(jwtUtil.generateAccessToken(eq("new@example.com"), any(Role.class))).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("new@example.com")).willReturn("refresh-token");
        given(jwtUtil.getRefreshTokenExpiry()).willReturn(604800000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        AuthResponse response = authService.signup(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("이미 사용 중인 이메일로 회원가입 시 DUPLICATE_EMAIL 예외를 던진다")
    void signup_중복_이메일_예외() {
        SignupRequest request = mock(SignupRequest.class);
        given(request.getEmail()).willReturn("dup@example.com");
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        then(userRepository).should(never()).save(any());
    }

    // ─── login ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("로그인 성공 시 토큰을 반환하고 성공 카운터를 증가시킨다")
    void login_성공() {
        User user = buildUser("user@example.com", "encoded", Role.ROLE_USER);
        LoginRequest request = mock(LoginRequest.class);
        given(request.getEmail()).willReturn("user@example.com");
        given(request.getPassword()).willReturn("password123");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded")).willReturn(true);
        given(jwtUtil.generateAccessToken(eq("user@example.com"), any(Role.class))).willReturn("access-token");
        given(jwtUtil.generateRefreshToken("user@example.com")).willReturn("refresh-token");
        given(jwtUtil.getRefreshTokenExpiry()).willReturn(604800000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        then(metrics).should().incrementLoginSuccess();
        then(metrics).should(never()).incrementLoginFail();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 USER_NOT_FOUND 예외를 던진다")
    void login_사용자_없음_예외() {
        LoginRequest request = mock(LoginRequest.class);
        given(request.getEmail()).willReturn("none@example.com");
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_PASSWORD 예외를 던지고 실패 카운터를 증가시킨다")
    void login_비밀번호_불일치_예외() {
        User user = buildUser("user@example.com", "encoded", Role.ROLE_USER);
        LoginRequest request = mock(LoginRequest.class);
        given(request.getEmail()).willReturn("user@example.com");
        given(request.getPassword()).willReturn("wrong");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("비밀번호가 올바르지 않습니다.");

        then(metrics).should().incrementLoginFail();
        then(metrics).should(never()).incrementLoginSuccess();
    }

    // ─── refresh ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("유효한 RefreshToken으로 토큰을 갱신한다")
    void refresh_성공() {
        User user = buildUser("user@example.com", "encoded", Role.ROLE_USER);
        RefreshRequest request = mock(RefreshRequest.class);
        given(request.getRefreshToken()).willReturn("old-refresh-token");

        given(jwtUtil.getEmail("old-refresh-token")).willReturn("user@example.com");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:user@example.com")).willReturn("old-refresh-token");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(jwtUtil.generateAccessToken(eq("user@example.com"), any(Role.class))).willReturn("new-access-token");
        given(jwtUtil.generateRefreshToken("user@example.com")).willReturn("new-refresh-token");
        given(jwtUtil.getRefreshTokenExpiry()).willReturn(604800000L);

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("Redis에 저장된 RefreshToken과 다르면 REFRESH_TOKEN_NOT_FOUND 예외를 던진다")
    void refresh_토큰_불일치_예외() {
        RefreshRequest request = mock(RefreshRequest.class);
        given(request.getRefreshToken()).willReturn("tampered-token");

        given(jwtUtil.getEmail("tampered-token")).willReturn("user@example.com");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:user@example.com")).willReturn(null);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(CustomException.class)
                .hasMessage("Refresh Token이 존재하지 않습니다.");
    }

    // ─── logout ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("로그아웃 시 Redis에서 RefreshToken을 삭제한다")
    void logout_redis_토큰_삭제() {
        authService.logout("user@example.com");

        then(redisTemplate).should().delete("refresh:user@example.com");
    }

    // ─── helper ────────────────────────────────────────────────────────────

    private User buildUser(String email, String encodedPassword, Role role) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .username("tester")
                .role(role)
                .build();
    }
}
