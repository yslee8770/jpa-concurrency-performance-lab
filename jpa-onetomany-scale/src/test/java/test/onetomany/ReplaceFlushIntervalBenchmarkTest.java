package test.onetomany;

import com.example.OneToManyLabApplication;
import com.example.dto.PerfRunSpec;
import com.example.strategy.OneToManyReplacePerfRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest(classes = OneToManyLabApplication.class)
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
