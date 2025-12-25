package test.optimistic;

import com.example.OptimisticLockTestApplication;
import com.example.domain.DecreaseResult;
import com.example.domain.DecreaseResultCode;
import com.example.domain.Stock;
import com.example.repository.StockRepository;
import com.example.service.StockDecreaseService;
import com.example.service.retry.RetryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SpringBootTest(classes = OptimisticLockTestApplication.class)
class StockDecreasePolicyTuningTest {

    @Autowired
    StockRepository stockRepository;
    @Autowired
    StockDecreaseService stockDecreaseService;

    @Test
    void policy_tuning_quantity10_requests100() throws Exception {
        int requests = 100;
        long initialQty = 10;

        Map<String, RetryPolicy> policies = new LinkedHashMap<>();
        policies.put("P1(max10,backoff0)", new RetryPolicy(10, 0, 0));
        policies.put("P2(max30,backoff0)", new RetryPolicy(30, 0, 0));
        policies.put("P3(max30,exp5~50)", new RetryPolicy(30, 5, 50));

        for (var entry : policies.entrySet()) {
            String name = entry.getKey();
            RetryPolicy policy = entry.getValue();

            // 매 정책마다 초기 데이터 새로 세팅
            Stock stock = stockRepository.save(Stock.of(initialQty));
            long stockId = stock.getId();

            ExecutorService pool = Executors.newFixedThreadPool(requests);
            CountDownLatch ready = new CountDownLatch(requests);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(requests);

            AtomicLong success = new AtomicLong();
            AtomicLong outOfStock = new AtomicLong();
            AtomicLong optimisticMax = new AtomicLong();
            AtomicLong totalAttempts = new AtomicLong();
            AtomicLong totalConflicts = new AtomicLong();

            long t0 = System.nanoTime();

            for (int i = 0; i < requests; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();

                        DecreaseResult r = stockDecreaseService.decreaseWithRetry(stockId, 1, policy);

                        totalAttempts.addAndGet(r.attempts());
                        totalConflicts.addAndGet(r.optimisticConflicts());

                        if (r.code() == DecreaseResultCode.SUCCESS) success.incrementAndGet();
                        else if (r.code() == DecreaseResultCode.OUT_OF_STOCK) outOfStock.incrementAndGet();
                        else if (r.code() == DecreaseResultCode.OPTIMISTIC_CONFLICT_MAX_ATTEMPTS) optimisticMax.incrementAndGet();
                        else throw new IllegalStateException("Unexpected result: " + r.code());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            done.await();

            long t1 = System.nanoTime();
            pool.shutdown();

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(t1 - t0);

            log.info("=== {} ===", name);
            log.info("SUCCESS={}, OUT_OF_STOCK={}, OPT_MAX={}", success.get(), outOfStock.get(), optimisticMax.get());
            log.info("elapsedMs={}, totalAttempts={}, totalConflicts={}, avgAttempts={}", elapsedMs, totalAttempts.get(), totalConflicts.get(), totalAttempts.get() / (double) requests);
            log.info("\n");
        }
    }
}