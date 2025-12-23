package mysql;

import com.example.PessimisticLockTestApplication;
import com.example.domain.Seat;
import com.example.repository.SeatRepository;
import com.example.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PessimisticLockTestApplication.class)
@ActiveProfiles("mysql")
class MySqlTimeoutShouldFailFastTest {

    private static final Duration HOLDER_HOLD = Duration.ofSeconds(3);
    private static final int SESSION_TIMEOUT_SECONDS = 1;

    @Autowired SeatRepository seatRepository;
    @Autowired DbCleaner dbCleaner;
    @Autowired SeatLockTxFacade seatLockTxFacade;
    @Autowired DbErrorClassifier dbErrorClassifier;
    @Autowired
    DbErrorRetryPolicy retryPolicy;

    long seatId;

    @BeforeEach
    void setUp() {
        dbCleaner.clearAll();
        seatId = seatRepository.save(Seat.create()).getId();
    }

    @Test
    void lock_wait_timeout_should_be_fail_fast_by_default_policy() throws Exception {
        RetryExecutor retry = new RetryExecutor(
                retryPolicy,     // 1205는 FAIL_FAST
                5,
                Duration.ofMillis(50)
        );

        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<?> holder = pool.submit(() -> seatLockTxFacade.lockSingleAndHold(seatId, HOLDER_HOLD));

        Thread.sleep(150);

        Future<Throwable> waiter = pool.submit(() -> {
            try {
                retry.run(() -> seatLockTxFacade.lockSingleWithSessionLockWaitTimeoutSeconds(seatId, SESSION_TIMEOUT_SECONDS));
                return null;
            } catch (Throwable t) {
                return t;
            }
        });

        holder.get(10, TimeUnit.SECONDS);
        Throwable error = waiter.get(10, TimeUnit.SECONDS);

        pool.shutdownNow();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(dbErrorClassifier.classify(error))
                .as("timeout(1205) must fail-fast under default policy")
                .isEqualTo(DbError.MYSQL_LOCK_WAIT_TIMEOUT);
    }
}
