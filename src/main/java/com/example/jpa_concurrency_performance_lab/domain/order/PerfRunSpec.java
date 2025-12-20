package com.example.jpa_concurrency_performance_lab.domain.order;

public record PerfRunSpec(
        int childSize,
        boolean collectionLoaded,
        int flushInterval,
        int replaceSize
) {}
