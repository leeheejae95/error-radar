package org.errorradar.errorradar.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.user.entity.Role;
import org.errorradar.errorradar.user.entity.User;
import org.errorradar.errorradar.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Admin 계정 설정이 없어 초기화를 건너뜁니다. (admin.email, admin.password 확인)");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin 계정이 이미 존재합니다. - email: {}", adminEmail);
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .username("admin")
                .role(Role.ROLE_ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Admin 계정 생성 완료 - email: {}", adminEmail);
    }
}
