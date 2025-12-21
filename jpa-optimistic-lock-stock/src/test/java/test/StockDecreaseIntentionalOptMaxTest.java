package test;

import com.example.OptimisticLockTestApplication;
import com.example.domain.DecreaseResult;
import com.example.domain.DecreaseResultCode;
import com.example.domain.Stock;
import com.example.repository.StockRepository;
import com.example.service.StockDecreaseService;
import com.example.service.retry.RetryPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OptimisticLockTestApplication.class)
class StockDecreaseIntentionalOptMaxTest {

    @Autowired
    StockRepository stockRepository;
    @Autowired
    StockDecreaseService stockDecreaseService;

    @Test
    void should_produce_opt_max_when_retry_policy_is_too_weak() throws Exception {
        // given
        long initialQty = 10;
        int requests = 100;

        Stock stock = stockRepository.save(Stock.of(initialQty));
        long stockId = stock.getId();

        // 의도적으로 약한 정책: 충돌 나면 거의 바로 포기하게 만든다
        RetryPolicy weakPolicy = new RetryPolicy(1, 0, 0); // maxAttempts=1 (retry 사실상 없음)

        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requests);

        AtomicLong success = new AtomicLong();
        AtomicLong outOfStock = new AtomicLong();
        AtomicLong optMax = new AtomicLong();

        long t0 = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    DecreaseResult r = stockDecreaseService.decreaseWithRetry(stockId, 1, weakPolicy);

                    if (r.code() == DecreaseResultCode.SUCCESS) success.incrementAndGet();
                    else if (r.code() == DecreaseResultCode.OUT_OF_STOCK) outOfStock.incrementAndGet();
                    else if (r.code() == DecreaseResultCode.OPTIMISTIC_CONFLICT_MAX_ATTEMPTS) optMax.incrementAndGet();
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
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(t1 - t0);

        Stock reloaded = stockRepository.findById(stockId).orElseThrow();
        long remaining = reloaded.getQuantity();

        System.out.println("=== STEP5(intentional OPT_MAX) ===");
        System.out.println("SUCCESS=" + success.get());
        System.out.println("OUT_OF_STOCK=" + outOfStock.get());
        System.out.println("OPTIMISTIC_CONFLICT_MAX_ATTEMPTS=" + optMax.get());
        System.out.println("elapsedMs=" + elapsedMs);
        System.out.println("remainingQty=" + remaining);

        //  “3종 결과로만 수렴” 검증
        assertThat(success.get() + outOfStock.get() + optMax.get()).isEqualTo(requests);

        //  이 테스트의 핵심: 약한 정책이면 OPT_MAX가 실제로 발생해야 한다
        assertThat(optMax.get()).isGreaterThan(0);

        //  성공 수와 실제 재고 감소량이 일치해야 한다
        assertThat(initialQty - remaining).isEqualTo(success.get());

        // 재고는 0~initialQty 범위
        assertThat(remaining).isBetween(0L, initialQty);
    }
}
