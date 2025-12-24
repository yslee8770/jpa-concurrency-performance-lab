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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@ActiveProfiles("fetchjoin")
@SpringBootTest(classes = FetchStrategyLabApplication.class)
class FetchJoinRowExplosionTest extends PerfTestSupport {

    @Autowired
    private TestDataFactory factory;
    @Autowired
    private OrderQueryRepository repo;
    @Autowired
    private HibernateStats stats;


    @BeforeEach
    void seed() {
        factory.seed(5000, 20, 3, 5); // 폭발이 보이게
    }

    @Test
    void fetch_join_single_collection_vs_two_collections() {
        List<Long> ids = repo.findOrderIds(0, 200);

        PerfResult one = measure("fetchJoin_linesOnly", stats, () -> {
            var orders = repo.fetchLinesByIds(ids);
            orders.forEach(o -> o.getOrderLines().size());
        });
        recordAndPrint(one);

        PerfResult two = measure("fetchJoin_linesAndPayments", stats, () -> {
            var orders = repo.fetchLinesAndPaymentsByIds(ids);
            orders.forEach(o -> {
                o.getOrderLines().size();
                o.getPayments().size();
            });
        });
        recordAndPrint(two);
    }
}
