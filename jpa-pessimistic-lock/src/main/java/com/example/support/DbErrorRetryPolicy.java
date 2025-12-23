package com.example.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class DbErrorRetryPolicy implements RetryPolicy {

    private final DbErrorClassifier classifier;

    @Override
    public RetryDecision decide(Throwable error, int attempt) {
        DbError type = classifier.classify(error);

        if (type == DbError.MYSQL_DEADLOCK || type == DbError.PG_DEADLOCK) {
            return RetryDecision.RETRY;
        }
        if (type == DbError.MYSQL_LOCK_WAIT_TIMEOUT || type == DbError.PG_LOCK_TIMEOUT) {
            return RetryDecision.FAIL_FAST;
        }
        return RetryDecision.FAIL_FAST;
    }
}
