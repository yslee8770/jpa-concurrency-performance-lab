package test.fetch;

import com.example.FetchStrategyLabApplication;
import com.example.fixture.TestDataFactory;
import com.example.repository.OrderQueryRepository;
import com.example.support.HibernateStats;
import com.example.support.PerfResult;
import support.PerfTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@Tag("oom")
@ActiveProfiles("fetchjoin")
@SpringBootTest(classes = FetchStrategyLabApplication.class)
class JoinDepthMemoryPressureTest extends PerfTestSupport {

    @Autowired
    private TestDataFactory factory;
    @Autowired
    private OrderQueryRepository repo;
    @Autowired
    private HibernateStats stats;


    @BeforeEach
    void seed() {
        factory.seed(2000, 100, 10, 5); // depth로 압박 크게
    }

    @Test
    void deep_fetch_graph_should_increase_heap_pressure() {
        List<Long> ids = repo.findOrderIds(0, 50);

        PerfResult r = measure("deepFetch(lines+options+payments)", stats, () -> {
            var orders = repo.fetchDeepGraphByIds(ids);
            orders.forEach(o -> {
                o.getOrderLines().forEach(l -> l.getOptions().size());
                o.getPayments().size();
            });
        });

        recordAndPrint(r);
    }
}
