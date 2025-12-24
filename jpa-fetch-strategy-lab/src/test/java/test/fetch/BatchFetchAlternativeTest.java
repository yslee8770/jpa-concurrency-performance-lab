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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ActiveProfiles("batch")
@SpringBootTest(classes = FetchStrategyLabApplication.class)
class BatchFetchAlternativeTest extends PerfTestSupport {

    @Autowired
    private TestDataFactory factory;
    @Autowired
    private OrderQueryRepository repo;
    @Autowired
    private HibernateStats stats;


    @BeforeEach
    void seed() {
        factory.seed(5000, 20, 3, 5);
    }

    @Transactional
    @Test
    void to_one_fetch_join_plus_batch_fetching_should_replace_row_explosion() {
        List<Long> ids = repo.findOrderIds(0, 200);

        PerfResult r = measure("toOneFetchJoin + batchFetch(lazy collections)", stats, () -> {
            var orders = repo.fetchToOneByIds(ids);

            orders.forEach(o -> {
                o.getOrderLines().size();
                o.getPayments().size();
            });
        });

        recordAndPrint(r);
    }
}
