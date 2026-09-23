package com.keycloak.oauth2_demo.config.logging;

import java.util.regex.Pattern;

/**
 * Che (mask) các trường nhạy cảm trong chuỗi JSON/text trước khi log.
 * Dùng regex thay thế value của các key nhạy cảm bằng "***MASKED***".
 */
public class SensitiveMasker {

    // Giới hạn độ dài tối đa mỗi dòng log body
    private static final int MAX_BODY_LENGTH = 500;

    // Danh sách các key JSON nhạy cảm cần che
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(\"(?:access_token|refresh_token|id_token|password|client_secret|token|secret)\"\\s*:\\s*\")([^\"]{8,})(\")",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Mask các trường nhạy cảm trong JSON body.
     * Ví dụ: "access_token":"eyJhbGc..." → "access_token":"eyJhbGc...***MASKED***"
     */
    public static String mask(String body) {
        if (body == null || body.isBlank()) return "";

        // 1. Mask các trường nhạy cảm: giữ lại 8 ký tự đầu, che phần còn lại
        String masked = SENSITIVE_PATTERN.matcher(body).replaceAll(match ->
                match.group(1)                                    // "access_token":"
                + match.group(2).substring(0, Math.min(8, match.group(2).length())) // 8 ký tự đầu
                + "***MASKED***"
                + match.group(3)                                  // dấu "
        );

        // 2. Cắt bớt nếu quá dài
        if (masked.length() > MAX_BODY_LENGTH) {
            return masked.substring(0, MAX_BODY_LENGTH)
                    + "... [TRUNCATED, total=" + masked.length() + " chars]";
        }
        return masked;
    }
}
