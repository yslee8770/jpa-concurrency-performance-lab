package com.example.jpa_concurrency_performance_lab.service.order.strategy;

import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;

public interface OneToManyDeleteStrategy {
    String name();

    void deleteAll(Long orderId, PerfRunSpec spec);
}