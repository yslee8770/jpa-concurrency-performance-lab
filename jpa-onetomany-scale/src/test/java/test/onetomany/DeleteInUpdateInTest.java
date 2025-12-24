package test.onetomany;

import com.example.OneToManyLabApplication;
import com.example.doamin.PurchaseOrder;
import com.example.dto.OrderLineDraft;
import com.example.repository.OrderLineRepository;
import com.example.repository.PurchaseOrderRepository;
import com.example.setup.OrderDatasetFactory;
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
public class DeleteInUpdateInTest {
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;

    @Autowired
    OrderSeedService seedService;
    @Autowired
    PurchaseOrderRepository orderRepository;
    @Autowired
    OrderLineRepository lineRepository;

    @Test
    @DisplayName("delete-in/update-in: orderId 기준 bulk delete 후 필요한 라인만 insert하면 DB 정합성은 유지된다(컬렉션 로딩 안 하면 stale도 회피 가능)")
    void delete_in_update_in_semantics() {
        Long orderId = tx.execute(status -> seedService.seedOneOrderWithLines(1_000));

        List<OrderLineDraft> newDrafts = OrderDatasetFactory.drafts(200);

        tx.executeWithoutResult(status -> {
            // 일부러 컬렉션 로딩 안 함 (Loaded=false)
            PurchaseOrder order = orderRepository.findById(orderId).orElseThrow();

            // 1) delete-in (bulk delete)
            long deleted = lineRepository.deleteByOrder_Id(orderId);
            assertThat(deleted).isEqualTo(1_000);

            // 2) update-in/insert-in (여기서는 insert만)
            for (OrderLineDraft d : newDrafts) {
                // OrderLine.create()는 package-private이라 같은 패키지에서만 호출 가능
                // => 여기서는 PurchaseOrder의 addLine을 재사용해서 일관성 유지
                order.addLine(d.sku(), d.unitPrice(), d.quantity());
            }

            em.flush();

            // DB 기준으로 200
            assertThat(lineRepository.countByOrder_Id(orderId)).isEqualTo(200);

            // clear 후 재조회로 최종 정합성 고정
            em.clear();
            PurchaseOrder reloaded = orderRepository.findById(orderId).orElseThrow();
            assertThat(reloaded.forceLoadLinesCount()).isEqualTo(200);
        });
    }
}
