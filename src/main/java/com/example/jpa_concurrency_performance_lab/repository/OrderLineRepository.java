package com.example.jpa_concurrency_performance_lab.repository;

import com.example.jpa_concurrency_performance_lab.domain.order.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    long countByOrder_Id(Long orderId);

    long deleteByOrder_Id(Long orderId);
}
