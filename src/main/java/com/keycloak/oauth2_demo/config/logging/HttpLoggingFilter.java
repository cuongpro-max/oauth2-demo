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
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    // Chỉ log body cho các API (không log HTML pages)
    private static final String API_PREFIX = "/api/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Bỏ qua log các tài nguyên tĩnh để không gây rác log
        if (uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".ico")
                || uri.endsWith(".png") || uri.endsWith(".jpg")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String clientIp = getClientIp(request);
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";

        log.info("[HTTP-IN ▶] {} {}{} | IP: {}", method, uri, queryString, clientIp);

        // Bọc response để có thể đọc body sau khi xử lý
        boolean isApi = uri.startsWith(API_PREFIX);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, isApi ? responseWrapper : response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = isApi ? responseWrapper.getStatus() : response.getStatus();

            log.info("[HTTP-IN ◀] {} {}{} | Status: {} | Duration: {}ms",
                    method, uri, queryString, status, duration);

            // Log response body (chỉ cho API, đã mask thông tin nhạy cảm)
            if (isApi) {
                byte[] bodyBytes = responseWrapper.getContentAsByteArray();
                if (bodyBytes.length > 0) {
                    String rawBody = new String(bodyBytes, StandardCharsets.UTF_8);
                    log.info("[HTTP-IN ◀ BODY] {} {} → {}", method, uri,
                            SensitiveMasker.mask(rawBody));
                }
                // Quan trọng: copy body về response thật để client nhận được
                responseWrapper.copyBodyToResponse();
            }
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
