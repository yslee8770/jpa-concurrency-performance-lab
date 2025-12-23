package com.example.support;

@FunctionalInterface
public interface RetryPolicy {
    RetryDecision decide(Throwable error, int attempt);
}
