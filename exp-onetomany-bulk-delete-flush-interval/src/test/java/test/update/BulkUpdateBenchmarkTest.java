package test.update;

import java.util.List;
import com.example.UpdateLabApplication;
import com.example.dto.BenchmarkResult;
import com.example.dto.UpdateRange;
import com.example.setup.TestDataSeeder;
import com.example.strategy.UpdateBenchmarkRunner;
import com.example.util.QueryCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = UpdateLabApplication.class)
class BulkUpdateBenchmarkTest {

    @Autowired
    TestDataSeeder seeder;
    @Autowired
    UpdateBenchmarkRunner runner;

    @BeforeEach
    void setUp() {
        seeder.reseedProducts(1000, 1000, 1000);
    }


    @Test
    void compare_1000_updates() {
        long from = 1L;
        long to = 1000L;

        int resetPrice = 1000;
        int newPrice = 2000;

        List<BenchmarkResult> results = runner.run(new UpdateRange(from, to, newPrice), resetPrice);
        for (BenchmarkResult r : results) {
            log.info("{} | {}ms | {}",
                    r.strategy(),
                    r.elapsedMs(),
                    QueryCountUtil.format(r.statementCount())
            );
        }
    }
}