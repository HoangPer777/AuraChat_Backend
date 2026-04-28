package com.aurachat.module.auth.handler;

import com.aurachat.config.JwtUtil;
import com.aurachat.module.auth.entity.RefreshToken;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.RefreshTokenRepository;
import com.aurachat.module.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Sau khi OAuth2 callback thành công:
 * 1. Upsert user vào MongoDB
 * 2. Tạo JWT + refresh token
 * 3. Redirect về frontend kèm token trong query param
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Xác định provider từ registrationId lưu trong attribute
        String registrationId = (String) request.getSession()
            .getAttribute("oauth2_registration_id");

        String provider = resolveProvider(request);
        String providerId = resolveProviderId(oAuth2User, provider);
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // Upsert: tìm theo provider+providerId, nếu không có thì tìm theo email
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> userRepository.findByEmail(email)
                .orElse(null));

        if (user == null) {
            user = User.builder()
                .email(email)
                .displayName(name)
                .avatarUrl(picture)
                .provider(provider)
                .providerId(providerId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        } else {
            // Cập nhật avatar nếu đăng nhập lại
            user.setAvatarUrl(picture);
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setUpdatedAt(Instant.now());
        }
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
            .token(refreshToken)
            .userId(user.getId())
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .build());

        String targetUrl = redirectUri
            + "?accessToken=" + accessToken
            + "&refreshToken=" + refreshToken;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String resolveProvider(HttpServletRequest request) {
        String uri = request.getRequestURI(); // /login/oauth2/code/google
        if (uri.contains("google")) return "GOOGLE";
        if (uri.contains("facebook")) return "FACEBOOK";
        return "UNKNOWN";
    }

    private String resolveProviderId(OAuth2User user, String provider) {
        if ("FACEBOOK".equals(provider)) {
            return String.valueOf(user.getAttribute("id"));
        }
        // Google dùng "sub"
        return String.valueOf(user.getAttribute("sub"));
    }
}
