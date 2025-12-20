package com.example.jpa_concurrency_performance_lab.order;

import com.example.jpa_concurrency_performance_lab.domain.order.PurchaseOrder;
import com.example.jpa_concurrency_performance_lab.repository.OrderLineRepository;
import com.example.jpa_concurrency_performance_lab.repository.PurchaseOrderRepository;
import com.example.jpa_concurrency_performance_lab.setup.OrderSeedService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test2")
@SpringBootTest
public class LoadingAxisTest {
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;

    @Autowired
    OrderSeedService seedService;
    @Autowired
    PurchaseOrderRepository orderRepository;
    @Autowired
    OrderLineRepository lineRepository;

    @Test
    @DisplayName("bulk delete: 컬렉션을 로딩하지 않으면(Loaded=false) stale 컬렉션 문제가 보이지 않을 수 있다")
    void bulk_delete_without_loading_collection_can_hide_stale_issue() {
        Long orderId = tx.execute(status -> seedService.seedOneOrderWithLines(1_000));

        tx.executeWithoutResult(status -> {
            PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();

            // ✅ 여기서 forceLoadLinesCount()를 호출하지 않는다. (Loaded = false)

            // when: bulk delete (orderId 기준)
            long deleted = lineRepository.deleteByOrder_Id(orderId);
            assertThat(deleted).isEqualTo(1_000);

            em.flush();

            // then: DB는 0
            assertThat(lineRepository.countByOrder_Id(orderId)).isZero();

            // then: 이제 처음으로 컬렉션을 접근하면 DB에서 로딩되므로 0으로 보일 수 있다
            assertThat(order.forceLoadLinesCount()).isZero();
        });
    }
}
