package com.example.jpa_concurrency_performance_lab.order;

import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;
import com.example.jpa_concurrency_performance_lab.service.order.strategy.OneToManyReplacePerfRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@ActiveProfiles("test2")
@Slf4j
@SpringBootTest
@EnableJpaAuditing
public class ReplaceFlushIntervalBenchmarkTest{

    @Autowired
    OneToManyReplacePerfRunner runner;

    @Test
    void run_replace_benchmark_with_flush_intervals() {
        List<PerfRunSpec> specs = List.of(
                new PerfRunSpec(1_000, true, 100, 1_000),
                new PerfRunSpec(1_000, true, 500, 1_000),
                new PerfRunSpec(1_000, true, 1000, 1_000),

                new PerfRunSpec(5_000, false, 100, 5_000),
                new PerfRunSpec(5_000, false, 500, 5_000),
                new PerfRunSpec(5_000, false, 1000, 5_000)

        );

        var results = runner.runReplaceBenchmarks(specs, 1);
        results.forEach(r -> log.info("{}", r));
    }
}
