package com.example.jpa_concurrency_performance_lab.service.order.strategy;

import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;

public interface OneToManyReplaceStrategy {
    String name();
    void replaceAll(Long orderId, PerfRunSpec spec);
}
