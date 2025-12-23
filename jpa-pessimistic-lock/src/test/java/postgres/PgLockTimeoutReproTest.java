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
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PessimisticLockTestApplication.class)
@ActiveProfiles("pg")
class PgLockTimeoutReproTest {

    private static final Duration HOLDER_HOLD = Duration.ofSeconds(3);
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(1);

    @Autowired SeatRepository seatRepository;
    @Autowired DbCleaner dbCleaner;
    @Autowired SeatLockTxFacade seatLockTxFacade;
    @Autowired DbErrorClassifier dbErrorClassifier;

    long seatId;

    @BeforeEach
    void setUp() {
        dbCleaner.clearAll();
        seatId = seatRepository.save(Seat.create()).getId();
    }

    @Test
    void lock_timeout_should_cancel_statement_when_lock_is_held_longer_than_local_timeout() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<?> holder = pool.submit(() -> seatLockTxFacade.lockSingleAndHold(seatId, HOLDER_HOLD));

        Thread.sleep(150);

        Future<Throwable> waiter = pool.submit(() -> {
            try {
                seatLockTxFacade.pgLockSingleWithLocalLockTimeout(seatId, LOCK_TIMEOUT);
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
                .isEqualTo(DbError.PG_LOCK_TIMEOUT);
    }
}
