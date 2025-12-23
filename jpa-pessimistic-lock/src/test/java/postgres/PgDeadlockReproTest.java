package postgres;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PessimisticLockTestApplication.class)
@ActiveProfiles("pg")
class PgDeadlockReproTest {

    private static final Duration HOLD = Duration.ofMillis(200);

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
    void deadlock_should_occur_when_lock_order_is_crossed() throws Exception {
        CyclicBarrier gate = new CyclicBarrier(2);

        Callable<Void> t1 = () -> {
            await(gate);
            seatLockTxFacade.pgLockHoldLock(seat1Id, seat2Id, HOLD);
            return null;
        };

        Callable<Void> t2 = () -> {
            await(gate);
            seatLockTxFacade.pgLockHoldLock(seat2Id, seat1Id, HOLD);
            return null;
        };

        List<Throwable> errors = Concurrency.runConcurrently(2, List.of(t1, t2));

        boolean hasDeadlock = errors.stream()
                .map(dbErrorClassifier::classify)
                .anyMatch(e -> e == DbError.PG_DEADLOCK);

        assertThat(hasDeadlock).isTrue();
    }

    private static void await(CyclicBarrier gate) {
        try { gate.await(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
