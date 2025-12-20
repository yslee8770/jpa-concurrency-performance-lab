package com.example.jpa_concurrency_performance_lab.product.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.jpa_concurrency_performance_lab.setup.UpdateDataSeeder;
import com.example.jpa_concurrency_performance_lab.config.TestJpaAuditingConfig;
import com.example.jpa_concurrency_performance_lab.dto.BenchmarkResult;
import com.example.jpa_concurrency_performance_lab.dto.UpdateRange;
import com.example.jpa_concurrency_performance_lab.service.product.strategy.UpdateBenchmarkRunner;
import com.example.jpa_concurrency_performance_lab.util.QueryCountUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJpaAuditingConfig.class)
class BulkUpdateBenchmarkTest {

    @Autowired
    UpdateDataSeeder seeder;
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
            System.out.printf("%s | %dms | %s%n",
                    r.strategy(),
                    r.elapsedMs(),
                    QueryCountUtil.format(r.statementCount())
            );
        }
    }
}