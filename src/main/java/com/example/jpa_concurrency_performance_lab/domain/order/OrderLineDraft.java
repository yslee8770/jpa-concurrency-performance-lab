package com.example.jpa_concurrency_performance_lab.domain.order;

public record OrderLineDraft(String sku, int unitPrice, int quantity) {}
