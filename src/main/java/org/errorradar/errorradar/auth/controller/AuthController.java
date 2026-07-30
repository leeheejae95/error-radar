package org.errorradar.errorradar.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.errorradar.errorradar.auth.dto.AuthResponse;
import org.errorradar.errorradar.auth.dto.LoginRequest;
import org.errorradar.errorradar.auth.dto.RefreshRequest;
import org.errorradar.errorradar.auth.dto.SignupRequest;
import org.errorradar.errorradar.auth.service.AuthService;
import org.errorradar.errorradar.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름으로 계정을 생성합니다. 기본 권한은 USER입니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다.", authService.signup(request)));
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인하고 Access/Refresh Token을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("로그인 성공.", authService.login(request)));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("토큰 갱신 성공.", authService.refresh(request)));
    }

    @Operation(summary = "로그아웃", description = "Redis에 저장된 Refresh Token을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("로그아웃 되었습니다."));
    }
}
