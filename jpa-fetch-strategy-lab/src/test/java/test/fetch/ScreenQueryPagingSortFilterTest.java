package test.fetch;

import com.example.FetchStrategyLabApplication;
import com.example.fixture.TestDataFactory;
import com.example.repository.OrderQueryRepository;
import com.example.repository.OrderSearchCond;
import com.example.support.HibernateStats;
import com.example.support.PerfResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import support.PerfTestSupport;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("batch")
@SpringBootTest(classes = FetchStrategyLabApplication.class)
class ScreenQueryPagingSortFilterTest extends PerfTestSupport {

    @jakarta.annotation.Resource
    TestDataFactory factory;

    @jakarta.annotation.Resource
    OrderQueryRepository repo;

    @jakarta.annotation.Resource
    HibernateStats stats;

    @BeforeEach
    void seed() {
        factory.seed(5000, 20, 3, 5);
    }

    @Transactional
    @Test
    void screen_query_should_use_id_paging_then_to_one_fetch_join_and_batch_fetch_collections() {
        var cond = new OrderSearchCond("m-1", "TRY_1"); // 예시 필터
        List<Long> ids = repo.findOrderIds(cond, 0, 200);

        assertThat(new HashSet<>(ids)).hasSize(ids.size());

        PerfResult r = measure("screen(ID page -> toOne fetchJoin + batch)", stats, () -> {
            var orders = repo.fetchToOneByIds(ids);
            orders.forEach(o -> {
                o.getMember().getName();
                o.getDelivery().getAddress();
                o.getOrderLines().size();
                o.getPayments().size();
            });
        });

        recordAndPrint(r);

        // 화면 반환 객체 수는 "페이징 limit"과 같아야 함
    }
}
