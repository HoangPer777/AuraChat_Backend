package com.aurachat.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * Redirects /auth/** to /api/auth/** for backwards compatibility.
 * Uses HTTP 302 redirect instead of internal forward to avoid
 * Spring Security filter chain issues on forwarded requests.
 */
@Controller
public class AuthForwardController {

    @RequestMapping("/auth/**")
    public void forwardAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Extract the path after /auth (e.g., /auth/login → /login)
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String pathAfterAuth = requestUri.substring(contextPath.length() + "/auth".length());

        // Build query string if present
        String queryString = request.getQueryString();
        String redirectPath = contextPath + "/api/auth" + pathAfterAuth;
        if (queryString != null && !queryString.isEmpty()) {
            redirectPath += "?" + queryString;
        }

        response.sendRedirect(redirectPath);
    }
}

