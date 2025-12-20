package com.example.jpa_concurrency_performance_lab.service.order.strategy;

import com.example.jpa_concurrency_performance_lab.domain.order.PerfResult;
import com.example.jpa_concurrency_performance_lab.domain.order.PerfRunSpec;
import com.example.jpa_concurrency_performance_lab.measure.HeapProbe;
import com.example.jpa_concurrency_performance_lab.measure.HibernateStatsProbe;
import com.example.jpa_concurrency_performance_lab.measure.PersistenceContextProbe;
import com.example.jpa_concurrency_performance_lab.setup.OrderSeedService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OneToManyPerfRunner {

    private final TransactionTemplate tx;
    private final EntityManager em;

    private final OrderSeedService seedService;
    private final HibernateStatsProbe statsProbe;
    private final PersistenceContextProbe pcProbe;
    private final HeapProbe heapProbe;

    private final List<OneToManyDeleteStrategy> strategies;

    public List<PerfResult> runPureDeleteBenchmarks(List<PerfRunSpec> specs, int warmupRounds) {
        List<PerfResult> results = new ArrayList<>();

        for (PerfRunSpec spec : specs) {
            for (OneToManyDeleteStrategy strategy : strategies) {

                for (int i = 0; i < warmupRounds; i++) {
                    Long warmId = tx.execute(s -> seedService.seedOneOrderWithLines(spec.childSize()));
                    tx.executeWithoutResult(s -> strategy.deleteAll(warmId, spec));
                }

                // ---- measured run ----
                PerfResult result = tx.execute(status -> {
                    Long orderId = seedService.seedOneOrderWithLines(spec.childSize());

                    Statistics stats = statsProbe.statistics(em);
                    statsProbe.clear(stats);

                    int managedPeak = 0;
                    long heapPeak = 0;

                    long startNs = System.nanoTime();
                    strategy.deleteAll(orderId, spec);
                    long endNs = System.nanoTime();

                    // peak 샘플링(단일 구간 측정이라 end 시점 수집만)
                    managedPeak = Math.max(managedPeak, pcProbe.managedEntityCount(em));
                    heapPeak = Math.max(heapPeak, heapProbe.usedHeapBytes());

                    return new PerfResult(
                            strategy.name(),
                            spec,
                            (endNs - startNs) / 1_000_000,
                            stats.getPrepareStatementCount(),
                            stats.getFlushCount(),
                            stats.getEntityDeleteCount(),
                            managedPeak,
                            heapPeak
                    );
                });

                results.add(result);
            }
        }

        results.sort(Comparator
                .comparing((PerfResult r) -> r.spec().childSize())
                .thenComparing(r -> r.spec().collectionLoaded())
                .thenComparing(PerfResult::strategy));

        return results;
    }

    public String formatAsTable(List<PerfResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n")
                .append(String.format("%-20s | %7s | %6s | %12s | %8s | %10s | %10s | %12s\n",
                        "STRATEGY", "CHILD", "LOAD", "TIME(ms)", "SQL(ps)", "FLUSH", "DEL_CNT", "HEAP(bytes)"));
        sb.append("-".repeat(20)).append("-+-")
                .append("-".repeat(7)).append("-+-")
                .append("-".repeat(6)).append("-+-")
                .append("-".repeat(12)).append("-+-")
                .append("-".repeat(8)).append("-+-")
                .append("-".repeat(10)).append("-+-")
                .append("-".repeat(10)).append("-+-")
                .append("-".repeat(12)).append("\n");

        for (PerfResult r : results) {
            sb.append(String.format("%-20s | %7d | %6s | %12d | %8d | %10d | %10d | %12d\n",
                    r.strategy(),
                    r.spec().childSize(),
                    r.spec().collectionLoaded() ? "Y" : "N",
                    r.elapsedMs(),
                    r.preparedStatementCount(),
                    r.flushCount(),
                    r.entityDeleteCount(),
                    r.heapUsedBytesPeak()
            ));
        }
        return sb.toString();
    }
}
