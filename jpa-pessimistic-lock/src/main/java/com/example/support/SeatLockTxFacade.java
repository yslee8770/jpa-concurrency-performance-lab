package com.example.support;

import com.example.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SeatLockTxFacade {

    private final SeatRepository seatRepository;
    private final PlatformTransactionManager txManager;
    private final JdbcTemplate jdbcTemplate;

    public void lockHoldLock(long firstSeatId, long secondSeatId, Duration holdAfterFirstLock) {
        if (holdAfterFirstLock == null || holdAfterFirstLock.isNegative()) {
            throw new IllegalArgumentException("holdAfterFirstLock must be null/negative");
        }

        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(status -> {
            seatRepository.findByIdForUpdate(firstSeatId).orElseThrow();

            sleep(holdAfterFirstLock);

            seatRepository.findByIdForUpdate(secondSeatId).orElseThrow();
            return null;
        });
    }

    public void lockHoldLockOrdered(long seatId1, long seatId2, Duration holdAfterFirstLock) {
        long first = Math.min(seatId1, seatId2);
        long second = Math.max(seatId1, seatId2);
        lockHoldLock(first, second, holdAfterFirstLock);
    }

    public void lockSingleAndHold(long seatId, Duration hold) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(status -> {
            seatRepository.findByIdForUpdate(seatId).orElseThrow();
            sleep(hold);
            return null;
        });
    }

    public void lockSingleWithSessionLockWaitTimeoutSeconds(long seatId, int timeoutSeconds) {
        if (timeoutSeconds <= 0) throw new IllegalArgumentException("timeoutSeconds must be positive");

        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(status -> {
            jdbcTemplate.execute("SET SESSION innodb_lock_wait_timeout = " + timeoutSeconds);
            seatRepository.findByIdForUpdate(seatId).orElseThrow();
            return null;
        });
    }

    public void pgLockHoldLock(long firstSeatId, long secondSeatId, Duration holdAfterFirstLock) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(status -> {
            seatRepository.findByIdForUpdate(firstSeatId).orElseThrow();
            sleep(holdAfterFirstLock);
            seatRepository.findByIdForUpdate(secondSeatId).orElseThrow();
            return null;
        });
    }

    public void pgLockSingleWithLocalLockTimeout(long seatId, Duration lockTimeout) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(status -> {
            jdbcTemplate.execute("SET LOCAL lock_timeout = '" + lockTimeout.toMillis() + "ms'");
            seatRepository.findByIdForUpdate(seatId).orElseThrow();
            return null;
        });
    }

    private static void sleep(Duration d) {
        if (d == null || d.isZero()) return;
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
