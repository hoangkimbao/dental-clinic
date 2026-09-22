package com.dentalclinic.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enterprise Brute Force Defense Service (F51).
 * Tracks failed login attempts by username and/or client IP.
 * Enforces:
 * - 5 allowed failed attempts
 * - Lockout triggered on attempt 6 (when failedAttempts >= 5)
 * - 15 minutes lockout window
 * - Automatic reset on successful login
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION_MS = 15 * 60 * 1000L; // 15 minutes lockout

    private static class AttemptRecord {
        AtomicInteger attempts = new AtomicInteger(0);
        long lockUntil = 0;
    }

    private final ConcurrentHashMap<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsCache.remove(key.toLowerCase().trim());
        }
    }

    public void loginFailed(String key) {
        if (key == null) return;
        String normalizedKey = key.toLowerCase().trim();
        long now = System.currentTimeMillis();
        AttemptRecord record = attemptsCache.computeIfAbsent(normalizedKey, k -> new AttemptRecord());

        if (record.lockUntil > now) {
            return;
        }

        int current = record.attempts.incrementAndGet();
        if (current >= MAX_ATTEMPTS) {
            record.lockUntil = now + LOCK_TIME_DURATION_MS;
        }
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        String normalizedKey = key.toLowerCase().trim();
        AttemptRecord record = attemptsCache.get(normalizedKey);
        if (record == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (record.lockUntil > now) {
            return true;
        }
        if (record.lockUntil != 0 && record.lockUntil <= now) {
            attemptsCache.remove(normalizedKey);
            return false;
        }
        return false;
    }

    public int getRemainingAttempts(String key) {
        if (key == null) return MAX_ATTEMPTS;
        AttemptRecord record = attemptsCache.get(key.toLowerCase().trim());
        if (record == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - record.attempts.get());
    }

    public void resetAll() {
        attemptsCache.clear();
    }
}
