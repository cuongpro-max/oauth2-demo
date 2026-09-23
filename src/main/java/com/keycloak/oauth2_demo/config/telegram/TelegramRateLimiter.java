package com.keycloak.oauth2_demo.config.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kiểm soát tần suất gửi thông báo lên Telegram, tránh spam.
 *
 * Hai lớp bảo vệ:
 *  1. Cooldown theo loại exception: cùng loại exception chỉ gửi 1 lần / N giây
 *  2. Rate limit tổng: tối đa M tin nhắn / phút bất kể loại nào
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramRateLimiter {

    // Cooldown (giây) cho mỗi loại exception — mặc định 60 giây
    @Value("${telegram.cooldown-seconds:60}")
    private long cooldownSeconds;

    // Số tin nhắn tối đa mỗi phút — mặc định 5
    @Value("${telegram.max-per-minute:5}")
    private int maxPerMinute;

    // Key: tên class exception → thời điểm gửi gần nhất
    private final ConcurrentHashMap<String, Long> lastSentMap = new ConcurrentHashMap<>();

    // Đếm số lần gửi trong phút hiện tại
    private final AtomicInteger countThisMinute = new AtomicInteger(0);
    private volatile long currentMinuteStart = Instant.now().getEpochSecond();

    /**
     * Kiểm tra xem có được phép gửi thông báo không.
     *
     * @param exceptionType Tên class exception (key để cooldown)
     * @return true nếu được phép gửi, false nếu bị chặn do spam
     */
    public boolean allowSend(String exceptionType) {
        long now = Instant.now().getEpochSecond();

        // --- Lớp 1: Reset bộ đếm nếu sang phút mới ---
        if (now - currentMinuteStart >= 60) {
            currentMinuteStart = now;
            countThisMinute.set(0);
        }

        // --- Lớp 2: Rate limit tổng (max N/phút) ---
        if (countThisMinute.get() >= maxPerMinute) {
            log.debug("[TelegramRateLimiter] Đã đạt giới hạn {}/phút, bỏ qua: {}", maxPerMinute, exceptionType);
            return false;
        }

        // --- Lớp 3: Cooldown theo loại exception ---
        Long lastSent = lastSentMap.get(exceptionType);
        if (lastSent != null && (now - lastSent) < cooldownSeconds) {
            long remaining = cooldownSeconds - (now - lastSent);
            log.debug("[TelegramRateLimiter] Cooldown còn {}s cho exception: {}", remaining, exceptionType);
            return false;
        }

        // Được phép gửi → cập nhật state
        lastSentMap.put(exceptionType, now);
        countThisMinute.incrementAndGet();
        return true;
    }
}
