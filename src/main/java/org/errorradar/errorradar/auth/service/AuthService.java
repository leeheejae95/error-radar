package org.errorradar.errorradar.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.auth.dto.AuthResponse;
import org.errorradar.errorradar.auth.dto.LoginRequest;
import org.errorradar.errorradar.auth.dto.RefreshRequest;
import org.errorradar.errorradar.auth.dto.SignupRequest;
import org.errorradar.errorradar.global.errorcode.ErrorCode;
import org.errorradar.errorradar.global.exception.CustomException;
import org.errorradar.errorradar.global.metrics.ErrorRadarMetrics;
import org.errorradar.errorradar.global.security.JwtUtil;
import org.errorradar.errorradar.user.entity.Role;
import org.errorradar.errorradar.user.entity.User;
import org.errorradar.errorradar.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ErrorRadarMetrics metrics;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);
        log.info("회원가입 완료 - email: {}", user.getEmail());

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            metrics.incrementLoginFail();
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        metrics.incrementLoginSuccess();
        log.info("로그인 성공 - email: {}", user.getEmail());
        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String email = jwtUtil.getEmail(refreshToken);

        String stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + email);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("토큰 갱신 - email: {}", email);
        return issueTokens(user);
    }

    public void logout(String email) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);
        log.info("로그아웃 - email: {}", email);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getEmail(),
                refreshToken,
                jwtUtil.getRefreshTokenExpiry(),
                TimeUnit.MILLISECONDS
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
