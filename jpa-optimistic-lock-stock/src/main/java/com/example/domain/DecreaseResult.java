package com.example.domain;

public record DecreaseResult(
        DecreaseResultCode code,
        long attempts,
        long optimisticConflicts
) {
    public static DecreaseResult success(long attempts, long optimisticConflicts) {
        return new DecreaseResult(DecreaseResultCode.SUCCESS, attempts, optimisticConflicts);
    }

    public static DecreaseResult outOfStock(long attempts, long optimisticConflicts) {
        return new DecreaseResult(DecreaseResultCode.OUT_OF_STOCK, attempts, optimisticConflicts);
    }

    public static DecreaseResult optimisticMaxAttempts(long attempts, long optimisticConflicts) {
        return new DecreaseResult(DecreaseResultCode.OPTIMISTIC_CONFLICT_MAX_ATTEMPTS, attempts, optimisticConflicts);
    }
}