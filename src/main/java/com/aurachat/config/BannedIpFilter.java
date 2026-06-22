package com.aurachat.config;

import com.aurachat.module.auth.repository.BannedIpRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;

@Component
@RequiredArgsConstructor
public class BannedIpFilter extends OncePerRequestFilter {

    private final BannedIpRepository bannedIpRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
            || (!uri.startsWith("/api/") && !uri.startsWith("/ws"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        if (bannedIpRepository.existsByIpAddress(normalize(clientIp))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"errorCode\":\"ADMIN_005\",\"message\":\"IP address is banned\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the real client IP address, accounting for reverse proxies (e.g. nginx).
     * Checks X-Forwarded-For and X-Real-IP headers before falling back to remoteAddr.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String normalize(String ipAddress) {
        if (ipAddress == null) return "";
        try {
            String normalized = InetAddress.getByName(ipAddress).getHostAddress();
            int zoneIndex = normalized.indexOf('%');
            return zoneIndex >= 0 ? normalized.substring(0, zoneIndex) : normalized;
        } catch (Exception ignored) {
            return ipAddress;
        }
    }
}
