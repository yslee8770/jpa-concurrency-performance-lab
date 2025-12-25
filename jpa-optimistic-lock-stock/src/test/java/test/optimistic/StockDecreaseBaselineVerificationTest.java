package test.optimistic;

import com.example.OptimisticLockTestApplication;
import com.example.domain.Stock;
import com.example.domain.exception.OutOfStockException;
import com.example.repository.StockRepository;
import com.example.service.StockDecreaseService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = OptimisticLockTestApplication.class)
class StockDecreaseBaselineVerificationTest {

    @Autowired
    StockRepository stockRepository;
    @Autowired
    StockDecreaseService stockDecreaseService;

    @Test
    void stock_1_concurrent_100_without_retry_baseline() throws Exception {
        // given
        Stock stock = stockRepository.save(Stock.of(1));
        long stockId = stock.getId();

        int requests = 100;

        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requests);

        AtomicLong success = new AtomicLong();
        AtomicLong outOfStock = new AtomicLong();
        AtomicLong optimisticFail = new AtomicLong();

        long t0 = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    stockDecreaseService.decreaseOnceWithoutRetry(stockId, 1);
                    success.incrementAndGet();

                } catch (OptimisticLockingFailureException e) {
                    optimisticFail.incrementAndGet();

                } catch (OutOfStockException e) {
                    outOfStock.incrementAndGet();

                } catch (Exception e) {
                    throw new RuntimeException("Unexpected exception", e);
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

        // 낙관적 락 때문에 성공은 1개로 고정되는 게 정상
        assertThat(success.get()).isEqualTo(1);
        // baseline의 핵심: 충돌이 밖으로 튀는 걸 "숫자"로 남긴다
        assertThat(optimisticFail.get()).isGreaterThan(0);

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(t1 - t0);

        log.info("=== BASELINE(without retry) ===");
        log.info("SUCCESS={}", success.get());
        log.info("OUT_OF_STOCK={}", outOfStock.get());
        log.info("OPTIMISTIC_FAILURE={}", optimisticFail.get());
        log.info("elapsedMs={}", elapsedMs);
    }
}
