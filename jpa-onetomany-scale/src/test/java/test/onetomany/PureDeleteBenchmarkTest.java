package test.onetomany;


import com.example.OneToManyLabApplication;
import com.example.dto.PerfRunSpec;
import com.example.strategy.OneToManyPerfRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest(classes = OneToManyLabApplication.class)
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

