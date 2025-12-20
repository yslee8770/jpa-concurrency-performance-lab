package com.example.jpa_concurrency_performance_lab.service.order.strategy;

import com.example.jpa_concurrency_performance_lab.domain.order.OrderLine;
import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;
import com.example.jpa_concurrency_performance_lab.domain.order.PurchaseOrder;
import com.example.jpa_concurrency_performance_lab.repository.OrderLineRepository;
import com.example.jpa_concurrency_performance_lab.setup.OrderDatasetFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class DeleteThenInsertChunkStrategy implements OneToManyReplaceStrategy {

    private final OrderLineRepository lineRepository;
    private final EntityManager em;

    @Override
    public String name() {
        return "DELETE_BY_ORDER_ID__INSERT_CHUNK";
    }

    @Override
    @Transactional
    public void replaceAll(Long orderId, PerfRunSpec spec) {
        // 1) bulk delete
        lineRepository.deleteByOrder_Id(orderId);

        // 2) chunk insert (컬렉션 로딩 없이 orderRef로만 insert)
        PurchaseOrder orderRef = em.getReference(PurchaseOrder.class, orderId);
        var drafts = OrderDatasetFactory.drafts(spec.replaceSize());

        int flushInterval = spec.flushInterval();
        int buffered = 0;

        var buffer = new ArrayList<OrderLine>(Math.min(drafts.size(), Math.max(flushInterval, 100)));

        for (int i = 0; i < drafts.size(); i++) {
            var d = drafts.get(i);
            buffer.add(OrderLine.create(orderRef, d.sku(), d.unitPrice(), d.quantity()));
            buffered++;

            if (flushInterval > 0 && buffered % flushInterval == 0) {
                em.persist(buffer.get(buffer.size() - 1)); // 최소 persist 트리거용(아래 flush에서 반영)
                em.flush();
                em.clear();

                // clear 이후 다시 reference
                orderRef = em.getReference(PurchaseOrder.class, orderId);
                buffer.clear();
            }
        }

        // 남은 것 flush
        for (OrderLine line : buffer) {
            em.persist(line);
        }
        em.flush();
    }
}
