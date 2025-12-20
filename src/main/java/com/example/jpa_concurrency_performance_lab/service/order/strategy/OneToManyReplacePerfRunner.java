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
import java.util.List;

@Component
@RequiredArgsConstructor
public class OneToManyReplacePerfRunner {

    private final TransactionTemplate tx;
    private final EntityManager em;

    private final OrderSeedService seedService;
    private final HibernateStatsProbe statsProbe;
    private final PersistenceContextProbe pcProbe;
    private final HeapProbe heapProbe;

    private final List<OneToManyReplaceStrategy> strategies;

    public List<PerfResult> runReplaceBenchmarks(List<PerfRunSpec> specs, int warmupRounds) {
        List<PerfResult> results = new ArrayList<>();

        for (PerfRunSpec spec : specs) {
            for (OneToManyReplaceStrategy strategy : strategies) {

                for (int i = 0; i < warmupRounds; i++) {
                    Long warmId = tx.execute(s -> seedService.seedOneOrderWithLines(spec.childSize()));
                    tx.executeWithoutResult(s -> strategy.replaceAll(warmId, spec));
                }

                PerfResult result = tx.execute(status -> {
                    Long orderId = seedService.seedOneOrderWithLines(spec.childSize());

                    Statistics stats = statsProbe.statistics(em);
                    statsProbe.clear(stats);

                    long heapPeak = 0;
                    int pcPeak = 0;

                    long startNs = System.nanoTime();
                    strategy.replaceAll(orderId, spec);
                    long endNs = System.nanoTime();

                    // end 시점 샘플
                    pcPeak = Math.max(pcPeak, pcProbe.managedEntityCount(em));
                    heapPeak = Math.max(heapPeak, heapProbe.usedHeapBytes());

                    return new PerfResult(
                            strategy.name(),
                            spec,
                            (endNs - startNs) / 1_000_000,
                            stats.getPrepareStatementCount(),
                            stats.getFlushCount(),
                            stats.getEntityDeleteCount(),
                            pcPeak,
                            heapPeak
                    );
                });

                results.add(result);
            }
        }
        return results;
    }
}
