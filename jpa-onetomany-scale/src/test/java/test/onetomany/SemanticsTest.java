package test.onetomany;

import com.example.OneToManyLabApplication;
import com.example.doamin.PurchaseOrder;
import com.example.repository.OrderLineRepository;
import com.example.repository.PurchaseOrderRepository;
import com.example.setup.OrderSeedService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OneToManyLabApplication.class)
public class SemanticsTest {

    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;

    @Autowired
    OrderSeedService seedService;
    @Autowired
    PurchaseOrderRepository orderRepository;
    @Autowired
    OrderLineRepository lineRepository;

    @Test
    @DisplayName("deleteAllInBatch: DB는 삭제되지만, clear 없으면 1차캐시 컬렉션은 stale 상태로 남는다")
    void deleteAllInBatch_without_clear_leaves_stale_collection() {
        // given
        Long orderId = tx.execute(status -> seedService.seedOneOrderWithLines(1_000));

        tx.executeWithoutResult(status -> {
            PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();

            // 컬렉션 로딩 여부를 명시적으로 고정 (Loaded = true)
            int loadedBefore = order.forceLoadLinesCount();
            assertThat(loadedBefore).isEqualTo(1_000);

            // when: bulk delete (영속성 컨텍스트 동기화 X)
            lineRepository.deleteAllInBatch(order.getLines());
            em.flush();

            // then: DB는 0건
            long dbCount = lineRepository.countByOrder_Id(orderId);
            assertThat(dbCount).isZero();

            // then: 하지만 엔티티 컬렉션은 stale (여전히 1000개라고 "보임")
            int loadedAfter = order.forceLoadLinesCount();
            assertThat(loadedAfter).isEqualTo(1_000);
        });
    }

    @Test
    @DisplayName("deleteAllInBatch: clear 후 재조회하면 엔티티 컬렉션도 DB와 일치한다")
    void deleteAllInBatch_with_clear_becomes_consistent() {
        // given
        Long orderId = tx.execute(status -> seedService.seedOneOrderWithLines(1_000));

        tx.executeWithoutResult(status -> {
            PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.forceLoadLinesCount()).isEqualTo(1_000);

            // when
            lineRepository.deleteAllInBatch(order.getLines());
            em.flush();

            // clear + re-fetch
            em.clear();
            PurchaseOrder reloaded = orderRepository.findById(orderId).orElseThrow();

            // then: DB와 엔티티 모두 0
            assertThat(lineRepository.countByOrder_Id(orderId)).isZero();
            assertThat(reloaded.forceLoadLinesCount()).isZero();
        });
    }

    @Test
    @DisplayName("orphanRemoval: 컬렉션을 비우면 flush 시 delete가 발생하고 엔티티/DB가 함께 일치한다")
    void orphanRemoval_is_consistent() {
        // given
        Long orderId = tx.execute(status -> seedService.seedOneOrderWithLines(1_000));

        tx.executeWithoutResult(status -> {
            PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.forceLoadLinesCount()).isEqualTo(1_000);

            // when: orphanRemoval 경로(owned 관계에서 안전)
            order.replaceAllLines(List.of()); // lines.clear() 포함
            em.flush();

            // then: 엔티티/DB 일치
            assertThat(lineRepository.countByOrder_Id(orderId)).isZero();
            assertThat(order.forceLoadLinesCount()).isZero();
        });
    }
}
