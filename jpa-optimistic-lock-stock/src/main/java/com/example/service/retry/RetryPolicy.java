package com.example.service.retry;

public record RetryPolicy(long maxAttempts, long baseDelayMs, long maxDelayMs) {

    public long delayMs(long attempt) {
        long exp = baseDelayMs * (1L << Math.max(0, (int) attempt - 1));
        return Math.min(exp, maxDelayMs);
    }
}
