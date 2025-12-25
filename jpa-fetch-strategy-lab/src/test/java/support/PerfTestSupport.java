package support;


import com.example.support.HibernateStats;
import com.example.support.MemoryMeter;
import com.example.support.PerfResult;
import com.example.support.PerfResultWriters;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class PerfTestSupport {

    private static final List<PerfResult> RESULTS = new ArrayList<>();

    protected PerfResult measure(String label, HibernateStats stats, Runnable action) {
        System.gc();

        stats.clear();
        long beforeHeap = MemoryMeter.usedBytes();
        long start = System.nanoTime();

        action.run();

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        long afterHeap = MemoryMeter.usedBytes();

        return new PerfResult(
                label,
                elapsedMs,
                (afterHeap - beforeHeap),
                stats.queryCount(),
                stats.entityLoadCount(),
                stats.collectionFetchCount()
        );
    }

    protected void recordAndPrint(PerfResult r) {
        RESULTS.add(r);
        log.info(
                "{} | {}ms | heapΔ={} | queries={} | entityLoad={} | collFetch={}",
                r.label(),
                r.elapsedMs(),
                r.heapDeltaBytes(),
                r.queryCount(),
                r.entityLoadCount(),
                r.collectionFetchCount()
        );
    }

    @AfterAll
    static void writeArtifacts() {
        if (RESULTS.isEmpty()) return;

        // 모듈 build 아래로 저장
        Path dir = Path.of("build", "perf-results");
        PerfResultWriters.writeCsv(dir.resolve("results.csv"), RESULTS);
        PerfResultWriters.writeMarkdownTable(dir.resolve("results.md"), RESULTS);
    }
}