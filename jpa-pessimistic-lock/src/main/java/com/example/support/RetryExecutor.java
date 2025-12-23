package com.example.support;

import java.time.Duration;

public final class RetryExecutor {

    private final RetryPolicy policy;
    private final int maxAttempts;
    private final Duration backoff;

    public RetryExecutor(RetryPolicy policy, int maxAttempts, Duration backoff) {
        if (policy == null) throw new IllegalArgumentException("policy must not be null");
        if (maxAttempts <= 0) throw new IllegalArgumentException("maxAttempts must be positive");
        if (backoff == null || backoff.isNegative()) throw new IllegalArgumentException("backoff must be null/negative");
        this.policy = policy;
        this.maxAttempts = maxAttempts;
        this.backoff = backoff;
    }

    public int run(CheckedRunnable action) {
        Throwable last = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.run();
                return attempt;
            } catch (Throwable t) {
                last = t;

                RetryDecision decision = policy.decide(t, attempt);
                if (decision == RetryDecision.FAIL_FAST || attempt == maxAttempts) {
                    throwUnchecked(last);
                }

                sleep(backoff);
            }
        }

        throwUnchecked(last);
        return -1;
    }

    private static void sleep(Duration d) {
        if (d == null || d.isZero()) return;
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }
}
