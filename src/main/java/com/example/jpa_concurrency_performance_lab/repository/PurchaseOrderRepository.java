package com.example.jpa_concurrency_performance_lab.repository;

import com.example.jpa_concurrency_performance_lab.domain.order.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {}
