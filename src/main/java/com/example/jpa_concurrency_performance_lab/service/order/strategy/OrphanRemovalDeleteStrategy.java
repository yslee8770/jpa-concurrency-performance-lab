package com.example.jpa_concurrency_performance_lab.service.order.strategy;

import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;
import com.example.jpa_concurrency_performance_lab.domain.order.PurchaseOrder;
import com.example.jpa_concurrency_performance_lab.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrphanRemovalDeleteStrategy implements OneToManyDeleteStrategy {

    private final PurchaseOrderRepository orderRepository;
    private final EntityManager em;

    @Override
    public String name() {
        return "ORPHAN_REMOVAL";
    }

    @Override
    @Transactional
    public void deleteAll(Long orderId, PerfRunSpec spec) {
        PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();

        if (spec.collectionLoaded()) {
            order.forceLoadLinesCount(); // Loaded=true
        }

        // orphanRemoval 경로
        order.replaceAllLines(java.util.List.of());

        em.flush();
    }
}
