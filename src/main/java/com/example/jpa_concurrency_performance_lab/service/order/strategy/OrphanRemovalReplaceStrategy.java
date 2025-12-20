package com.example.jpa_concurrency_performance_lab.service.order.strategy;

import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;
import com.example.jpa_concurrency_performance_lab.domain.order.PurchaseOrder;
import com.example.jpa_concurrency_performance_lab.repository.PurchaseOrderRepository;
import com.example.jpa_concurrency_performance_lab.setup.OrderDatasetFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrphanRemovalReplaceStrategy implements OneToManyReplaceStrategy {

    private final PurchaseOrderRepository orderRepository;
    private final EntityManager em;

    @Override
    public String name() {
        return "ORPHAN_REMOVAL_REPLACE";
    }

    @Override
    @Transactional
    public void replaceAll(Long orderId, PerfRunSpec spec) {
        PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();

        if (spec.collectionLoaded()) {
            order.forceLoadLinesCount(); // Loaded=true 축
        }

        // 컬렉션 교체(= delete + insert 유발)
        order.replaceAllLines(OrderDatasetFactory.drafts(spec.replaceSize()));

        em.flush();
    }
}
