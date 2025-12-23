package mysql;

import com.example.PessimisticLockTestApplication;
import com.example.domain.Seat;
import com.example.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.support.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PessimisticLockTestApplication.class)
@ActiveProfiles("mysql")
class MySqlOrderingPreventsDeadlockTest {

    private static final Duration HOLD_AFTER_FIRST_LOCK = Duration.ofMillis(200);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(5);

    @Autowired SeatRepository seatRepository;
    @Autowired DbCleaner dbCleaner;
    @Autowired SeatLockTxFacade seatLockTxFacade;
    @Autowired DbErrorClassifier dbErrorClassifier;


    long seat1Id;
    long seat2Id;

    @BeforeEach
    void setUp() {
        dbCleaner.clearAll();
        seat1Id = seatRepository.save(Seat.create()).getId();
        seat2Id = seatRepository.save(Seat.create()).getId();
    }

    @Test
    void ordering_should_prevent_deadlock_by_unifying_lock_acquisition_order() throws Exception {
        // A) crossed => deadlock expected
        Result crossed = runPair(
                () -> seatLockTxFacade.lockHoldLock(seat1Id, seat2Id, HOLD_AFTER_FIRST_LOCK),
                () -> seatLockTxFacade.lockHoldLock(seat2Id, seat1Id, HOLD_AFTER_FIRST_LOCK)
        );

        assertThat(crossed.deadlockCount).isGreaterThanOrEqualTo(1);
        // crossed는 보통 1 success + 1 deadlock이 기대됨
        assertThat(crossed.successCount + crossed.errorCount).isEqualTo(2);

        // reset
        setUp();

        // B) ordered => no deadlock expected
        Result ordered = runPair(
                () -> seatLockTxFacade.lockHoldLockOrdered(seat1Id, seat2Id, HOLD_AFTER_FIRST_LOCK),
                () -> seatLockTxFacade.lockHoldLockOrdered(seat2Id, seat1Id, HOLD_AFTER_FIRST_LOCK)
        );

        assertThat(ordered.deadlockCount).as("Ordered lock acquisition should break the deadlock cycle").isEqualTo(0);
        assertThat(ordered.errors)
                .as("Ordered execution should complete without errors in this scenario. errors=" + ordered.errors)
                .isEmpty();
        assertThat(ordered.successCount).isEqualTo(2);
    }

    private Result runPair(Runnable r1, Runnable r2) throws Exception {
        CyclicBarrier startGate = new CyclicBarrier(2);
        AtomicInteger success = new AtomicInteger();

        Callable<Void> t1 = () -> {
            await(startGate);
            r1.run();
            success.incrementAndGet();
            return null;
        };
        Callable<Void> t2 = () -> {
            await(startGate);
            r2.run();
            success.incrementAndGet();
            return null;
        };

        List<Throwable> errors = Concurrency.runConcurrently(2, List.of(t1, t2), TEST_TIMEOUT);

        int deadlock = (int) errors.stream().map(dbErrorClassifier::classify).filter(e -> e == DbError.MYSQL_DEADLOCK).count();

        return new Result(success.get(), errors.size(), deadlock, errors);
    }

    private static void await(CyclicBarrier gate) {
        try {
            gate.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record Result(int successCount, int errorCount, int deadlockCount, List<Throwable> errors) {}
}