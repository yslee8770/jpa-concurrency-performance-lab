package test.fetch;

import com.example.FetchStrategyLabApplication;
import com.example.fixture.TestDataFactory;
import com.example.repository.OrderQueryRepository;
import com.example.support.HibernateStats;
import com.example.support.PerfResult;
import support.PerfTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(classes = FetchStrategyLabApplication.class)
class BaselineNPlusOneTest extends PerfTestSupport {

    @Autowired
    private TestDataFactory factory;
    @Autowired
    private OrderQueryRepository orderQueryRepository;
    @Autowired
    private HibernateStats stats;

    @BeforeEach
    void seed() {
        factory.seed(2000, 10, 3, 3);
    }

    @Transactional
    @Test
    void baseline_touching_relations_should_trigger_n_plus_one() {
        List<Long> ids = orderQueryRepository.findOrderIds(0, 200);

        PerfResult r = measure("baseline", stats, () -> {
            var orders = orderQueryRepository.fetchToOneByIds(ids);
            for (var o : orders) {
                o.getMember().getName();
                o.getDelivery().getAddress();

                // lazy 컬렉션 접근 → N+1 또는 추가 로딩
                o.getOrderLines().size();
                o.getPayments().size();
            }
        });

        recordAndPrint(r);
    }
}
