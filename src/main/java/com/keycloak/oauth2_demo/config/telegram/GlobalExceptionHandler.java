package com.keycloak.oauth2_demo.config.telegram;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bắt tất cả Exception từ REST API (@RestController) và:
 *  1. Ghi log ERROR vào file log
 *  2. Gửi thông báo Telegram (nếu rate limiter cho phép)
 *  3. Trả về JSON lỗi chuẩn cho client
 */
@Slf4j
@RestControllerAdvice(annotations = RestController.class)
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectProvider<TelegramNotifier> notifierProvider;
    private final ObjectProvider<TelegramRateLimiter> rateLimiterProvider;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, HttpServletRequest request) {
        String context = request.getMethod() + " " + request.getRequestURI();
        String exType = ex.getClass().getSimpleName();
        String message = ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi không xác định";

        // 1. Log đầy đủ stack trace vào file log
        log.error("[REST EXCEPTION] {} | {}: {}", context, exType, message, ex);

        // 2. Gửi Telegram nếu enabled và rate limiter cho phép
        TelegramRateLimiter rateLimiter = rateLimiterProvider.getIfAvailable();
        TelegramNotifier notifier = notifierProvider.getIfAvailable();

        // Key rate limit kết hợp context và loại lỗi để tránh chặn chéo các endpoint khác nhau
        String rateLimitKey = context + ":" + exType;
        if (notifier != null && rateLimiter != null && rateLimiter.allowSend(rateLimitKey)) {
            notifier.sendException(ex, context);
        }

        // 3. Phân loại HTTP Status phù hợp cho REST Client
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (message.contains("không chính xác") || message.contains("không khớp")
                || message.contains("Vui lòng") || message.contains("không hợp lệ")) {
            status = HttpStatus.BAD_REQUEST;
        } else if (message.contains("đã tồn tại")) {
            status = HttpStatus.CONFLICT;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status.value());
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
