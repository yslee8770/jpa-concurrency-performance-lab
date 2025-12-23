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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PessimisticLockTestApplication.class)
@ActiveProfiles("mysql")
class MySqlDeadlockRetryConvergesTest {

    private static final Duration HOLD = Duration.ofMillis(200);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(8);

    @Autowired SeatRepository seatRepository;
    @Autowired DbCleaner dbCleaner;
    @Autowired SeatLockTxFacade seatLockTxFacade;
    @Autowired
    DbErrorRetryPolicy retryPolicy;

    long seat1Id;
    long seat2Id;

    @BeforeEach
    void setUp() {
        dbCleaner.clearAll();
        seat1Id = seatRepository.save(Seat.create()).getId();
        seat2Id = seatRepository.save(Seat.create()).getId();
    }

    @Test
    void deadlock_should_be_retryable_and_converge() throws Exception {


        RetryExecutor retry = new RetryExecutor(
                retryPolicy,
                7,
                Duration.ofMillis(50)
        );

        CyclicBarrier startGate = new CyclicBarrier(2);
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger totalAttempts = new AtomicInteger();
        AtomicInteger maxAttempt = new AtomicInteger();


        Callable<Void> t1 = () -> {
            await(startGate);
            int attempt = retry.run(() -> seatLockTxFacade.lockHoldLock(seat1Id, seat2Id, HOLD));
            completed.incrementAndGet();
            totalAttempts.addAndGet(attempt);
            maxAttempt.accumulateAndGet(attempt, Math::max);
            return null;
        };

        Callable<Void> t2 = () -> {
            await(startGate);
            int attempt = retry.run(() -> seatLockTxFacade.lockHoldLock(seat2Id, seat1Id, HOLD));
            completed.incrementAndGet();
            totalAttempts.addAndGet(attempt);
            maxAttempt.accumulateAndGet(attempt, Math::max);
            return null;
        };

        List<Throwable> errors = Concurrency.runConcurrently(2, List.of(t1, t2), TEST_TIMEOUT);

        assertThat(errors)
                .as("Retry should absorb deadlock failures so tasks complete without bubbling errors. errors=" + errors)
                .isEmpty();

        assertThat(errors).isEmpty();
        assertThat(completed.get()).isEqualTo(2);
        assertThat(maxAttempt.get()).isLessThanOrEqualTo(7);

        System.out.println("totalAttempts=" + totalAttempts.get() + ", maxAttempt=" + maxAttempt.get());
    }

    private static void await(CyclicBarrier gate) {
        try {
            gate.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
