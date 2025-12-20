package com.example.jpa_concurrency_performance_lab.domain.order;

public record PerfResult(
        String strategy,
        PerfRunSpec spec,
        long elapsedMs,
        long preparedStatementCount,
        long flushCount,
        long entityDeleteCount,
        int managedEntityCountPeak,
        long heapUsedBytesPeak
) {}
