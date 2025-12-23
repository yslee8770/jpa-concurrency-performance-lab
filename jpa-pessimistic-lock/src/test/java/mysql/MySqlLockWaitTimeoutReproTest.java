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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PessimisticLockTestApplication.class)
@ActiveProfiles("mysql")
class MySqlLockWaitTimeoutReproTest {

    private static final Duration HOLDER_HOLD = Duration.ofSeconds(3);
    private static final int LOCK_WAIT_TIMEOUT_SECONDS = 1;

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
    void lock_wait_timeout_1205_should_occur_when_lock_is_held_longer_than_session_timeout() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        AtomicReference<Throwable> t1Error = new AtomicReference<>();
        AtomicReference<Throwable> t2Error = new AtomicReference<>();

        Future<?> holder = pool.submit(() -> {
            try {
                seatLockTxFacade.lockSingleAndHold(seatId, HOLDER_HOLD);
            } catch (Throwable t) {
                t1Error.set(t);
            }
        });

        Thread.sleep(150);

        Future<?> waiter = pool.submit(() -> {
            try {
                seatLockTxFacade.lockSingleWithSessionLockWaitTimeoutSeconds(seatId, LOCK_WAIT_TIMEOUT_SECONDS);
            } catch (Throwable t) {
                t2Error.set(t);
            }
        });

        holder.get(10, TimeUnit.SECONDS);
        waiter.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(t1Error.get())
                .as("Holder tx should not fail")
                .isNull();

        DbError classified = dbErrorClassifier.classify(t2Error.get());
        assertThat(classified)
                .as("Waiter tx should fail with MySQL lock wait timeout (1205)")
                .isEqualTo(DbError.MYSQL_LOCK_WAIT_TIMEOUT);
    }
}