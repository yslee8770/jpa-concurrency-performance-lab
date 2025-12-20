package com.example.jpa_concurrency_performance_lab.service.order.strategy;

import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;
import com.example.jpa_concurrency_performance_lab.repository.OrderLineRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeleteByOrderIdStrategy implements OneToManyDeleteStrategy {

    private final OrderLineRepository lineRepository;
    private final EntityManager em;

    @Override
    public String name() {
        return "DELETE_BY_ORDER_ID";
    }

    @Override
    @Transactional
    public void deleteAll(Long orderId, PerfRunSpec spec) {
        lineRepository.deleteByOrder_Id(orderId);
        em.flush();
    }
}
