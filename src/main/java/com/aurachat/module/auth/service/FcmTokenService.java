package com.aurachat.module.auth.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private static final int MAX_TOKENS_PER_USER = 10;

    private final UserRepository userRepository;

    public void registerToken(String userId, String token) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND, "User not found"));

        List<String> tokens = user.getFcmTokens() == null ? new ArrayList<>() : new ArrayList<>(user.getFcmTokens());
        tokens.remove(token);
        tokens.add(token);

        if (tokens.size() > MAX_TOKENS_PER_USER) {
            tokens = new ArrayList<>(tokens.subList(tokens.size() - MAX_TOKENS_PER_USER, tokens.size()));
        }

        user.setFcmTokens(tokens);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    public void removeToken(String userId, String token) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFcmTokens() == null || user.getFcmTokens().isEmpty()) {
                return;
            }

            List<String> tokens = new ArrayList<>(user.getFcmTokens());
            if (tokens.remove(token)) {
                user.setFcmTokens(tokens);
                user.setUpdatedAt(Instant.now());
                userRepository.save(user);
            }
        });
    }

    public void removeInvalidToken(String userId, String token) {
        removeToken(userId, token);
    }
}
