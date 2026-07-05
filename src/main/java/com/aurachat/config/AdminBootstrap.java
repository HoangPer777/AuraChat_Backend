package com.aurachat.config;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email:}")
    private String adminEmail;

    @Value("${admin.bootstrap.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) return;
        String email = adminEmail.trim().toLowerCase();
        User admin = userRepository.findByEmail(email).orElse(null);
        if (admin == null) {
            if (adminPassword == null || adminPassword.length() < 8) {
                log.warn("ADMIN_EMAIL is configured but ADMIN_PASSWORD is missing or shorter than 8 characters");
                return;
            }
            admin = User.builder()
                .email(email)
                .displayName("AuraChat Admin")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .emailVerified(true)
                .role("ADMIN")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        } else {
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");
            if (admin.getEmailVerified() == null || !admin.getEmailVerified()) {
                admin.setEmailVerified(true);
            }
            if (adminPassword != null && adminPassword.length() >= 8) {
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                log.info("Admin bootstrap updated password for email={}", email);
            }
            admin.setUpdatedAt(Instant.now());
        }
        userRepository.save(admin);
        log.info("Admin bootstrap ensured for email={}", email);
    }
}
