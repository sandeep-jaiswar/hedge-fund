package com.hedgefund.ingestionui.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${HEDGE_API_KEY:}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip auth if no API key configured (dev mode)
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip auth for health/readiness endpoints
        String path = request.getRequestURI();
        if (path.endsWith("/health") || path.endsWith("/health/ready") || path.endsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader("X-API-Key");
        if (expectedApiKey.equals(providedKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid X-API-Key header\"}");
        }
    }
}
