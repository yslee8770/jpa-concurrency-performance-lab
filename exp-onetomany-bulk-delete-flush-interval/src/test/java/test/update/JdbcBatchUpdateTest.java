package test.update;

import com.example.UpdateLabApplication;
import com.example.domain.Product;
import com.example.repository.ProductRepository;
import com.example.setup.UpdateDataSeeder;
import com.example.strategy.JdbcBatchStrategy;
import com.example.util.QueryCountUtil;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UpdateLabApplication.class)
public class JdbcBatchUpdateTest {

    @Autowired
    UpdateDataSeeder seeder;
    @Autowired
    JdbcBatchStrategy jdbcBatch;
    @Autowired
    ProductRepository repository;
    @Autowired
    EntityManager em;

    @BeforeEach
    void setUp() {
        seeder.reseedProducts(1000, 1000, 1000);
    }

    @Test
    void jdbc_batch_updates_only_10_rows() {
        List<Long> targetIds = List.of(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L);

        QueryCountUtil.reset();
        long start = System.nanoTime();

        jdbcBatch.batchUpdatePriceByIds(targetIds, 3000, 50);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("JDBC_BATCH_PARTIAL | %dms | %s%n",
                elapsedMs,
                QueryCountUtil.format(QueryCountUtil.snapshot())
        );

        List<Product> updated = repository.findAllById(targetIds);
        Assertions.assertThat(updated).allMatch(p -> p.getPrice() == 3000);
    }

    @Test
    void jdbc_batch_skips_auditing_and_listener() {
        // given
        Product before = repository.findById(1L).orElseThrow();
        LocalDateTime lastModifiedBefore = before.getUpdatedAt();

        // when
        jdbcBatch.batchUpdatePriceByIds(List.of(1L), 3000, 10);

        em.clear();
        Product after = repository.findById(1L).orElseThrow();

        // then
        assertThat(after.getPrice()).isEqualTo(3000);
        assertThat(after.getUpdatedAt())
                .as("JDBC Batch는 auditing을 트리거하지 않는다")
                .isEqualTo(lastModifiedBefore);
    }
}
