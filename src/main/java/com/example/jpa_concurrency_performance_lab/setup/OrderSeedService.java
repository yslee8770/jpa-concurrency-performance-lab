package com.example.jpa_concurrency_performance_lab.setup;

import com.example.jpa_concurrency_performance_lab.domain.order.PurchaseOrder;
import com.example.jpa_concurrency_performance_lab.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderSeedService {

    private final PurchaseOrderRepository orderRepository;
    private final EntityManager em;

    @Transactional
    public Long seedOneOrderWithLines(int childSize) {
        PurchaseOrder order = PurchaseOrder.create("CUSTOMER-1");
        var drafts = OrderDatasetFactory.drafts(childSize);

        for (var d : drafts) {
            order.addLine(d.sku(), d.unitPrice(), d.quantity());
        }

        orderRepository.save(order);
        em.flush();
        em.clear();
        return order.getId();
    }
}

