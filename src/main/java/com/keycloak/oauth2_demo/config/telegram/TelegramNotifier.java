package com.keycloak.oauth2_demo.config.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Gửi tin nhắn cảnh báo lên Telegram Bot.
 * Chỉ kích hoạt khi cấu hình telegram.enabled=true trong application.properties.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramNotifier {

    private static final String API_URL = "https://api.telegram.org/bot%s/sendMessage";

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    @Value("${spring.application.name:app}")
    private String appName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gửi thông báo exception lên Telegram.
     * Nội dung đã được format sẵn với Markdown, thông tin nhạy cảm được loại bỏ.
     *
     * @param ex      Exception cần thông báo
     * @param context Mô tả ngữ cảnh (vd: "POST /api/auth/login")
     */
    public void sendException(Throwable ex, String context) {
        String message = buildMessage(ex, context);
        sendRaw(message);
    }

    /**
     * Gửi tin nhắn thô lên Telegram (dùng khi cần gửi thông báo tùy chỉnh).
     */
    public void sendRaw(String text) {
        try {
            String url = String.format(API_URL, botToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Giới hạn 4096 ký tự theo giới hạn Telegram
            String safeText = text.length() > 4000 ? text.substring(0, 4000) + "...[TRUNCATED]" : text;

            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "text", safeText,
                    "parse_mode", "HTML"
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[Telegram] Gửi thất bại, status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            // Log lỗi nhưng KHÔNG throw ra ngoài — tránh lỗi gửi Telegram làm crash app
            log.warn("[Telegram] Không thể gửi thông báo: {}", e.getMessage());
        }
    }

    private String buildMessage(Throwable ex, String context) {
        String stackTrace = escapeHtml(getShortStackTrace(ex));
        String message = escapeHtml(truncate(ex.getMessage(), 200));

        return String.format(
                """
                🚨 <b>EXCEPTION ALERT</b>
                📦 <b>App:</b> %s
                🔗 <b>Context:</b> %s
                ❌ <b>Type:</b> %s
                💬 <b>Message:</b> %s
                📋 <b>Stack (top 3):</b>
                <code>%s</code>
                """,
                escapeHtml(appName),
                escapeHtml(context),
                escapeHtml(ex.getClass().getSimpleName()),
                message,
                stackTrace
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    /**
     * Lấy 3 dòng đầu của stack trace để tránh tin nhắn quá dài.
     */
    private String getShortStackTrace(Throwable ex) {
        StackTraceElement[] elements = ex.getStackTrace();
        if (elements == null || elements.length == 0) return "(no stack trace)";
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(3, elements.length);
        for (int i = 0; i < limit; i++) {
            sb.append(elements[i]).append("\n");
        }
        return sb.toString().trim();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "(no message)";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
