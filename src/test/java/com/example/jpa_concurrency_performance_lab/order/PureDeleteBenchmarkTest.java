package com.example.jpa_concurrency_performance_lab.order;


import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;
import com.example.jpa_concurrency_performance_lab.service.order.strategy.OneToManyPerfRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@Slf4j
@ActiveProfiles("test2")
@EnableJpaAuditing
@SpringBootTest
public class PureDeleteBenchmarkTest{

    @Autowired
    OneToManyPerfRunner runner;

    @Test
    void run_pure_delete_benchmark() {
        List<PerfRunSpec> specs = List.of(
                new PerfRunSpec(1_000, true, 0,0),
                new PerfRunSpec(5_000, true, 0,0),
                new PerfRunSpec(10_000, true, 0,0),

                new PerfRunSpec(10_000, false, 0,0)
        );

        var results = runner.runPureDeleteBenchmarks(specs, 1); // warmup 1회
        log.info(runner.formatAsTable(results));
    }
}

