package com.keycloak.oauth2_demo.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Bỏ qua log các tài nguyên tĩnh để không gây rác log
        if (uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".ico") || uri.endsWith(".png") || uri.endsWith(".jpg")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String clientIp = getClientIp(request);
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";

        log.info("[HTTP-IN ▶] {} {}{} | IP: {}", method, uri, queryString, clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            log.info("[HTTP-IN ◀] {} {}{} | Status: {} | Duration: {}ms", method, uri, queryString, status, duration);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
