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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = OptimisticLockTestApplication.class)
class StockDecreaseQuantity10WithRetryTest {

    @Autowired
    StockRepository stockRepository;
    @Autowired
    StockDecreaseService stockDecreaseService;

    @Test
    void stock_10_concurrent_100_with_retry_should_converge() throws Exception {
        // given
        Stock stock = stockRepository.save(Stock.of(10));
        long stockId = stock.getId();

        int requests = 100;

        ExecutorService pool = Executors.newFixedThreadPool(requests);

        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requests);

        AtomicLong success = new AtomicLong();
        AtomicLong outOfStock = new AtomicLong();
        AtomicLong optimisticMax = new AtomicLong();

        AtomicLong totalAttempts = new AtomicLong();
        AtomicLong totalConflicts = new AtomicLong();

        RetryPolicy policy = new RetryPolicy(30, 5, 50); // 기존 정책 그대로

        long t0 = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
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
            }));
        }

        ready.await();
        start.countDown();
        done.await();

        for (Future<?> f : futures) f.get();
        long t1 = System.nanoTime();

        pool.shutdown();

        // then
        Stock reloaded = stockRepository.findById(stockId).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualTo(0);

        assertThat(success.get()).isEqualTo(10);
        assertThat(outOfStock.get()).isEqualTo(90);

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(t1 - t0);

        log.info("=== Q10(with retry) ===");
        log.info("SUCCESS={}", success.get());
        log.info("OUT_OF_STOCK={}", outOfStock.get());
        log.info("OPTIMISTIC_CONFLICT_MAX_ATTEMPTS={}", optimisticMax.get());
        log.info("elapsedMs={}", elapsedMs);
        log.info("totalAttempts={}", totalAttempts.get());
        log.info("totalConflicts={}", totalConflicts.get());
        log.info("avgAttempts={}", totalAttempts.get() / (double) requests);
    }
}
